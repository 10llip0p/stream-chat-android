package io.getstream.chat.android.core.poc.library

import android.text.TextUtils
import io.getstream.chat.android.core.poc.library.TokenProvider.TokenProviderListener
import io.getstream.chat.android.core.poc.library.api.ApiClientOptions
import io.getstream.chat.android.core.poc.library.api.RetrofitClient
import io.getstream.chat.android.core.poc.library.call.ChatCall
import io.getstream.chat.android.core.poc.library.requests.QueryUsers
import io.getstream.chat.android.core.poc.library.rest.ChannelQueryRequest
import io.getstream.chat.android.core.poc.library.rest.UpdateChannelRequest
import io.getstream.chat.android.core.poc.library.socket.ChatObservable
import io.getstream.chat.android.core.poc.library.socket.ChatSocketConnectionImpl
import io.getstream.chat.android.core.poc.library.socket.ConnectionData
import java.util.UUID.randomUUID


class StreamChatClient(
    val apiKey: String,
    val apiOptions: ApiClientOptions
) {

    private lateinit var api: ChatApiImpl
    private var anonymousConnection = false
    private val state = ClientState()
    private var tokenProvider: CachedTokenProvider? = null
    private var cacheUserToken: String = ""
    private var fetchingToken = false
    private var isAnonymous = false
    private var clientID = ""

    private val socket = ChatSocketConnectionImpl(apiKey, apiOptions.wssURL)

    fun getClientID(): String {
        return clientID
    }

    fun setAnonymousUser(callback: (Result<ConnectionData>) -> Unit) {
        isAnonymous = true

        state.user = User(id = randomUUID().toString())

        RetrofitClient.getClient(
            options = apiOptions,
            tokenProvider = null,
            anonymousAuth = true
        )?.create(
            RetrofitApi::class.java
        )?.let {
            api = ChatApiImpl(apiKey, it)
        }

        connect(callback, anonymousConnection = true)
    }

    fun setGuestUser(
        user: User,
        callback: (Result<ConnectionData>) -> Unit
    ) {
        isAnonymous = true

        RetrofitClient.getClient(
            options = apiOptions,
            tokenProvider = null,
            anonymousAuth = true
        )?.create(
            RetrofitApi::class.java
        )?.let {
            api = ChatApiImpl(apiKey, it)
        }

        api.setGuestUser(apiKey, user.id, user.name).enqueue { result ->
            if (result.isSuccess) {
                state.user = result.data().user

                initTokenProvider(object : TokenProvider {
                    override fun getToken(listener: TokenProviderListener) {
                        listener.onSuccess(result.data().access_token)
                    }
                })

                connect(callback)
            }
        }
    }

    fun setUser(
        user: User,
        provider: TokenProvider,
        callback: (Result<ConnectionData>) -> Unit
    ) {
        state.user = user

        initTokenProvider(provider)

        RetrofitClient.getClient(
            options = apiOptions,
            tokenProvider = tokenProvider,
            anonymousAuth = isAnonymous
        )?.create(
            RetrofitApi::class.java
        )?.let { client ->
            api = ChatApiImpl(apiKey, client)
        }

        connect(callback)

        /*socket.connect(user, this.tokenProvider!!).enqueue {

            if (it.isSuccess) {
                api.connectionId = it.data().connectionId
                api.userId = it.data().user.id
            }

            callback(it)
        }*/
    }

    fun events(): ChatObservable {
        return socket.events()
    }

    fun getState(): ClientState {
        return state
    }

    fun fromCurrentUser(entity: UserEntity): Boolean {
        val otherUserId = entity.getUserId() ?: return false
        return if (getUser() == null) false else TextUtils.equals(getUserId(), otherUserId)
    }

    fun getUserId(): String {
        return ""
    }

    fun getUser(): User? {
        return state.user
    }

    fun disconnect() {
        if (state.user == null) {
            //log
        } else {
            //log
        }

        socket.disconnect()

        // unset token facilities
        tokenProvider = null
        fetchingToken = false
        cacheUserToken = ""

        //builtinHandler.dispatchUserDisconnected()
        //for (handler in subRegistry.getSubscribers()) {
        //    handler.dispatchUserDisconnected()
        //}

        // clear local state
        state.reset()
        //activeChannelMap.clear()
    }

    fun queryChannel(
        channelType: String,
        channelId: String,
        request: ChannelQueryRequest
    ): ChatCall<Channel> {
        return api.queryChannel(channelType, channelId, request).map { attachClient(it) }
    }

    fun markRead(channelType: String, channelId: String, messageId: String): ChatCall<Unit> {
        return api.markRead(channelType, channelId, messageId)
    }

    fun showChannel(channelType: String, channelId: String): ChatCall<Unit> {
        return api.showChannel(channelType, channelId)
    }

    fun hideChannel(
        channelType: String,
        channelId: String,
        clearHistory: Boolean = false
    ): ChatCall<Unit> {
        return api.hideChannel(channelType, channelId, clearHistory)
    }

    fun stopWatching(channelType: String, channelId: String): ChatCall<Unit> {
        return api.stopWatching(channelType, channelId)
    }

    fun queryChannels(
        request: QueryChannelsRequest
    ): ChatCall<List<Channel>> {
        return api.queryChannels(request)
            .map { response -> response.getChannels() }
            .map { attachClient(it) }
    }

    fun updateChannel(
        channelType: String,
        channelId: String,
        updateMessage: Message,
        channelExtraData: Map<String, Any> = emptyMap()
    ): ChatCall<Channel> {
        val request = UpdateChannelRequest(channelExtraData, updateMessage)
        return api.updateChannel(channelType, channelId, request)
            .map { response -> response.channel }
            .map { attachClient(it) }
    }

    fun rejectInvite(channelType: String, channelId: String): ChatCall<Channel> {
        return api.rejectInvite(channelType, channelId).map { attachClient(it) }
    }

    fun acceptInvite(channelType: String, channelId: String, message: String): ChatCall<Channel> {
        return api.acceptInvite(channelType, channelId, message).map { attachClient(it) }
    }

    fun markAllRead(): ChatCall<Event> {
        return api.markAllRead().map {
            it.event
        }
    }

    fun reconnectWebSocket() {
        if (getUser() == null) {
            return
        }

//        if (webSocketService != null) {
//            return
//        }
        //connectionRecovered()

        //connect(anonymousConnection)
    }

    fun getUsers(queryUser: QueryUsers): ChatCall<List<User>> {
        return api.getUsers(
            apiKey = apiKey,
            connectionId = api.connectionId,
            queryUser = queryUser
        ).map { it.users }
    }

    private fun initTokenProvider(provider: TokenProvider) {
        val listeners = mutableListOf<TokenProviderListener>()
        this.tokenProvider = object : CachedTokenProvider {
            override fun getToken(listener: TokenProviderListener) {
                if (cacheUserToken.isNotEmpty()) {
                    listener.onSuccess(cacheUserToken)
                    return
                }

                if (fetchingToken) {
                    listeners.add(listener)
                    return
                } else {
                    // token is not in cache and there are no in-flight requests, go fetch it
                    fetchingToken = true
                }

                provider.getToken(object : TokenProviderListener {
                    override fun onSuccess(token: String) {
                        fetchingToken = false
                        listener.onSuccess(token)
                        for (l in listeners)
                            l.onSuccess(token)
                        listeners.clear()
                    }
                })
            }

            override fun tokenExpired() {
                cacheUserToken = ""
            }

        }
    }

    private fun connect(
        callback: (Result<ConnectionData>) -> Unit,
        anonymousConnection: Boolean? = false
    ) {
        if (anonymousConnection == true) {
            socket.connect().enqueue { connectionData ->

                if (connectionData.isSuccess) {
                    api.connectionId = connectionData.data().connectionId
                    api.userId = connectionData.data().user.id
                    state.user = connectionData.data().user
                }

                callback(connectionData)
            }
        } else {
            state.user?.let { user ->
                socket.connect(user, this.tokenProvider).enqueue {

                    if (it.isSuccess) {
                        api.connectionId = it.data().connectionId
                        api.userId = it.data().user.id
                    }

                    callback(it)
                }
            }
        }
    }

    private fun attachClient(channels: List<Channel>): List<Channel> {
        channels.forEach { attachClient(it) }
        return channels
    }

    private fun attachClient(channel: Channel): Channel {
        channel.client = this
        return channel
    }
}