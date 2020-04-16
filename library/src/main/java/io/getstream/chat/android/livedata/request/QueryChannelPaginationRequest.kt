package io.getstream.chat.android.livedata.request

import io.getstream.chat.android.client.api.models.Pagination
import io.getstream.chat.android.client.api.models.QueryChannelRequest
import io.getstream.chat.android.client.api.models.WatchChannelRequest


class QueryChannelPaginationRequest(var messageLimit: Int = 30) : QueryChannelRequest() {

    var messageFilterDirection: Pagination? = null
    var messageFilterValue: String = ""

    var memberLimit: Int = 30
    var memberOffset: Int = 0

    var watcherLimit: Int = 30
    var watcherOffset: Int = 0

    fun hasFilter(): Boolean {
        return messageFilterDirection != null
    }

    fun isFirstPage(): Boolean {
        return messageFilterDirection == null
    }

    internal fun toAnyChannelPaginationRequest(): AnyChannelPaginationRequest {
        return AnyChannelPaginationRequest().apply {
            this.messageLimit = messageLimit
            this.messageFilterDirection = messageFilterDirection
            this.memberLimit = memberLimit
            this.memberOffset = memberOffset
            this.watcherLimit = watcherLimit
            this.watcherOffset = watcherOffset
            this.channelLimit = 1
        }
    }

    fun toQueryChannelRequest(userPresence: Boolean): WatchChannelRequest {
        var request = WatchChannelRequest().withMessages(messageLimit)
        if (userPresence) {
            // TODO: reenable me once LLC is fixed
            //request = request.withPresence()
        }
        if (hasFilter()) {
            request.withMessages(messageFilterDirection!!, messageFilterValue, messageLimit)
        }
        return request
    }


}
