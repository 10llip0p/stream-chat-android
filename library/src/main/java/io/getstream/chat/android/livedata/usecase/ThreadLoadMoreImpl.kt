package io.getstream.chat.android.livedata.usecase

import io.getstream.chat.android.client.models.Message
import io.getstream.chat.android.livedata.ChatDomainImpl
import io.getstream.chat.android.livedata.utils.Call2
import io.getstream.chat.android.livedata.utils.CallImpl2
import io.getstream.chat.android.livedata.utils.validateCid
import java.security.InvalidParameterException

interface ThreadLoadMore {
    /**
     * Loads more messages for the specified thread
     *
     * @param cid: the full channel id IE messaging:123
     * @param parentId: the parentId of the thread
     * @param messageLimit: how many new messages to load
     *
     * @return A call object with List<Message> as the return type
     */
    operator fun invoke(cid: String, parentId: String, messageLimit: Int): Call2<List<Message>>
}

class ThreadLoadMoreImpl(var domainImpl: ChatDomainImpl) : ThreadLoadMore {
    override operator fun invoke(cid: String, parentId: String, messageLimit: Int): Call2<List<Message>> {
        validateCid(cid)
        if (parentId.isEmpty()) {
            throw InvalidParameterException("parentId cant be empty")
        }

        val channelController = domainImpl.channel(cid)
        val threadController = channelController.getThread(parentId)

        val runnable = suspend {
            threadController.loadOlderMessages(messageLimit)
        }
        return CallImpl2(
            runnable,
            channelController.scope
        )
    }
}
