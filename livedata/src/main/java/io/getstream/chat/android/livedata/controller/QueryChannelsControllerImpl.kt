package io.getstream.chat.android.livedata.controller

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.api.models.QuerySort
import io.getstream.chat.android.client.errors.ChatError
import io.getstream.chat.android.client.events.ChatEvent
import io.getstream.chat.android.client.events.CidEvent
import io.getstream.chat.android.client.events.NotificationAddedToChannelEvent
import io.getstream.chat.android.client.events.UserStartWatchingEvent
import io.getstream.chat.android.client.events.UserStopWatchingEvent
import io.getstream.chat.android.client.logger.ChatLogger
import io.getstream.chat.android.client.models.Channel
import io.getstream.chat.android.client.utils.FilterObject
import io.getstream.chat.android.client.utils.Result
import io.getstream.chat.android.livedata.ChatDomainImpl
import io.getstream.chat.android.livedata.entity.ChannelConfigEntity
import io.getstream.chat.android.livedata.entity.QueryChannelsEntity
import io.getstream.chat.android.livedata.request.QueryChannelsPaginationRequest
import io.getstream.chat.android.livedata.request.toQueryChannelsRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class QueryChannelsControllerImpl(
    override val filter: FilterObject,
    override val sort: QuerySort,
    internal val client: ChatClient,
    internal val domainImpl: ChatDomainImpl
) : QueryChannelsController {
    override var newChannelEventFilter: (Channel, FilterObject) -> Boolean = { _, _ -> true }
    override var recoveryNeeded: Boolean = false
    /**
     * A livedata object with the channels matching this query.
     */

    internal val queryEntity: QueryChannelsEntity = QueryChannelsEntity(filter, sort)
    val job = SupervisorJob()
    val scope = CoroutineScope(Dispatchers.IO + domainImpl.job + job)

    private val _endOfChannels = MutableLiveData<Boolean>(false)
    override val endOfChannels: LiveData<Boolean> = _endOfChannels

    private val _channels = MutableLiveData<Map<String, Channel>>()
    // Ensure we don't lose the sort in the channel
    override var channels: LiveData<List<Channel>> = Transformations.map(_channels) { cMap -> queryEntity.channelCids.mapNotNull { cMap[it] } }

    private val logger = ChatLogger.get("ChatDomain QueryChannelsController")

    private val _loading = MutableLiveData<Boolean>(false)
    override val loading: LiveData<Boolean> = _loading

    private val _loadingMore = MutableLiveData<Boolean>(false)
    override val loadingMore: LiveData<Boolean> = _loadingMore

    fun loadMoreRequest(limit: Int = 30, messageLimit: Int = 10): QueryChannelsPaginationRequest {
        val channels = _channels.value ?: mapOf()
        return QueryChannelsPaginationRequest(sort, channels.size, limit, messageLimit)
    }

    fun handleEvents(events: List<ChatEvent>) {
        for (event in events) {
            handleEvent(event)
        }
    }

    override fun addChannelIfFilterMatches(
        channel: Channel
    ) {
        if (newChannelEventFilter(channel, filter)) {
            val channelControllerImpl = domainImpl.channel(channel)
            channelControllerImpl.updateLiveDataFromChannel(channel)
            addChannels(listOf(channel), true)
        }
    }

    fun handleEvent(event: ChatEvent) {
        if (event is NotificationAddedToChannelEvent) {
            addChannelIfFilterMatches(event.channel)
        }
        if (event is CidEvent) {
            // skip events that are typically not impacting the query channels overview
            if (event is UserStartWatchingEvent || event is UserStopWatchingEvent) {
                return
            }
            // update the info for that channel from the channel repo
            logger.logI("received channel event $event")

            updateChannel(domainImpl.channel(event.cid).toChannel())
        }
    }

    private fun updateChannel(channel: Channel) {
        _channels.postValue((_channels.value ?: mapOf()) + mapOf(channel.cid to channel))
    }

    suspend fun loadMore(limit: Int = 30, messageLimit: Int = 10): Result<List<Channel>> {
        val pagination = loadMoreRequest(limit, messageLimit)
        return runQuery(pagination)
    }

    suspend fun query(limit: Int = 30, messageLimit: Int = 10): Result<List<Channel>> {
        return runQuery(QueryChannelsPaginationRequest(sort, 0, limit, messageLimit))
    }

    suspend fun runQueryOffline(pagination: QueryChannelsPaginationRequest): List<Channel>? {
        val queryEntity = domainImpl.repos.queryChannels.select(queryEntity.id)
        var channels: List<Channel>? = null

        if (queryEntity != null) {
            val channelPairs = domainImpl.selectAndEnrichChannels(queryEntity.channelCids.toList(), pagination)
            for (p in channelPairs) {
                val channelRepo = domainImpl.channel(p.channel)
                channelRepo.updateLiveDataFromChannelEntityPair(p)
            }
            channels = channelPairs.map { it.channel }
            logger.logI("found ${channelPairs.size} channels in offline storage")
        }

        if (channels != null) {
            // first page replaces the results, second page adds to them
            if (pagination.isFirstPage) {
                setChannels(channels)
            } else {
                addChannels(channels)
            }
        }
        return channels
    }

    suspend fun runQueryOnline(pagination: QueryChannelsPaginationRequest): Result<List<Channel>> {
        val request = pagination.toQueryChannelsRequest(filter, domainImpl.userPresence)
        // next run the actual query
        val response = client.queryChannels(request).execute()

        if (response.isSuccess) {
            recoveryNeeded = false

            // store the results in the database
            val channelsResponse = response.data()
            if (channelsResponse.size < pagination.channelLimit) {
                _endOfChannels.postValue(true)
            }
            // first things first, store the configs
            val configEntities = channelsResponse.associateBy { it.type }.values.map { ChannelConfigEntity(it.type, it.config) }
            domainImpl.repos.configs.insert(configEntities)
            logger.logI("api call returned ${channelsResponse.size} channels")

            // initialize channel repos for all of these channels
            for (c in channelsResponse) {
                val channelRepo = domainImpl.channel(c)
                channelRepo.updateLiveDataFromChannel(c)
            }

            domainImpl.storeStateForChannels(channelsResponse)

            if (pagination.isFirstPage) {
                setChannels(channelsResponse)
            } else {
                addChannels(channelsResponse)
            }
            domainImpl.repos.queryChannels.insert(queryEntity)
        } else {
            recoveryNeeded = true
            domainImpl.addError(response.error())
        }
        return response
    }

    suspend fun runQuery(pagination: QueryChannelsPaginationRequest): Result<List<Channel>> {
        val loader = if (pagination.isFirstPage) {
            _loading
        } else {
            _loadingMore
        }
        if (loader.value == true) {
            logger.logI("Another query channels request is in progress. Ignoring this request.")
            return Result(null, ChatError("Another query channels request is in progress. Ignoring this request."))
        }
        loader.postValue(true)
        // start by getting the query results from offline storage

        val channels = runQueryOffline(pagination)

        // we could either wait till we are online
        // or mark ourselves as needing recovery and trigger recovery
        val output: Result<List<Channel>>
        if (domainImpl.isOnline()) {
            val result = runQueryOnline(pagination)
            output = result
        } else {
            recoveryNeeded = true
            output = channels?.let { Result(it) } ?: Result(error = ChatError(message = "Channels Query wasn't run online and the offline storage is empty"))
        }
        loader.postValue(false)
        return output
    }

    private fun addChannels(newChannels: List<Channel>, onTop: Boolean = false) {
        // second page adds to the list of channels
        queryEntity.channelCids = if (onTop) {
            (newChannels.map { it.cid } + queryEntity.channelCids).distinct()
        } else {
            (queryEntity.channelCids + newChannels.map { it.cid }).distinct()
        }

        _channels.postValue(
            (_channels.value ?: mapOf()).let { previousChannels ->
                previousChannels + (queryEntity.channelCids - previousChannels.values.map { channel -> channel.cid })
                    .map { cid -> domainImpl.channel(cid).toChannel() }
                    .associateBy { channel -> channel.cid }
            }
        )
    }

    private fun setChannels(channel: List<Channel>) {
        // first page sets the channels/overwrites..
        queryEntity.channelCids = channel.map { it.cid }
        _channels.postValue(
            queryEntity.channelCids
                .map { domainImpl.channel(it).toChannel() }
                .associateBy { it.cid }
        )
    }
}
