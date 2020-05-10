package io.getstream.chat.android.livedata.usecase

import io.getstream.chat.android.livedata.ChatDomainImpl
import io.getstream.chat.android.livedata.utils.Call2
import io.getstream.chat.android.livedata.utils.CallImpl2
import io.getstream.chat.android.livedata.utils.validateCid
import java.security.InvalidParameterException

interface MarkRead {
    /**
     * Marks the messages on the specified channel as read
     *
     * @param cid: the full channel id IE messaging:123
     *
     * @return A call object with Boolean as the return type.
     * True if the mark read event was sent.
     * False if there was no need to mark read (IE the messages are already marked as read)
     */
    operator fun invoke(cid: String): Call2<Boolean>
}

class MarkReadImpl(var domainImpl: ChatDomainImpl) : MarkRead {
    override operator fun invoke(cid: String): Call2<Boolean> {
        validateCid(cid)

        val channelRepo = domainImpl.channel(cid)

        val runnable = suspend {

            channelRepo.markRead()
        }
        return CallImpl2<Boolean>(
            runnable,
            channelRepo.scope
        )
    }
}
