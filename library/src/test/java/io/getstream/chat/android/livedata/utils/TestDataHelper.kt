package io.getstream.chat.android.livedata.utils

import io.getstream.chat.android.client.events.*
import io.getstream.chat.android.client.models.*
import java.util.*

class TestDataHelper {

    val apiKey = "b67pax5b2wdq"

    val connection1 = "test-connection"
    val user1 = User("broad-lake-3")
    val user3 = User("user-3")

    val user1Token =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VyX2lkIjoiYnJvYWQtbGFrZS0zIn0.SIb263bpikToka22ofV-9AakJhXzfeF8pU9cstvzInE"
    val user2 = User("test-user-2")
    val userMap = mutableMapOf("broad-lake-3" to user1, "test-user-2" to user2)
    val user1updated = User("broad-lake-3").apply { extraData = mutableMapOf("color" to "green") }

    val filter1 =
        Filters.and(Filters.eq("type", "messaging"), Filters.`in`("members", listOf(user1.id)))

    val attachment1 =
        Attachment(type = "image").apply { extraData = mutableMapOf("color" to "green") }

    val watcher1 = Watcher("test", user1, null)
    val member1 = Member(user = user1, role = "user")
    val member2 = Member(user = user2, role = "user")

    fun getOldDate(): Date {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, 1988)
        cal.set(Calendar.MONTH, Calendar.JANUARY)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return cal.time
    }

    val extraData1 = mutableMapOf("color" to "green", "score" to 1.1)

    val config1 = Config().apply { isTypingEvents = true; isReadEvents = true }
    val channel1 = Channel().apply {
        type = "messaging"
        id = "123-testing"
        cid = "messaging:123-testing"
        watcherCount = 100
        createdBy = user1
        watchers = listOf(watcher1)
        members = listOf(member1)
        config = config1
    }

    val channel1Updated = Channel().apply {
        type = "messaging"
        id = "123-testing"
        cid = "messaging:123-testing"
        watcherCount = 100
        createdBy = user1
        watchers = listOf(watcher1)
        members = listOf(member1)
        extraData = mutableMapOf("color" to "green")
        config=config1
    }

    val channel2 = Channel().apply {
        type = "messaging"
        id = "222-testing"
        cid = "messaging:222-testing"
        watcherCount = 22
        createdBy = user1
        watchers = listOf(watcher1)
        members = listOf(member1)
        config = config1
    }

    val reaction1 = Reaction("message-1", "like", 1).apply { user = user1; userId = user1.id }
    val reaction2 = Reaction("message-1", "like", 1).apply { user = user2 }

    val message1 = Message().apply {
        this.channel = channel1; cid = channel1.cid; text = "hi there"; id = "message-1"; user =
        user1; createdAt = calendar(2020, 1, 1)
    }

    fun createMessage(): Message {
        val text = "hi there " + UUID.randomUUID().toString()
        val message = Message().apply {
            this.channel = channel1; cid = channel1.cid; this.text = text; id = ""; user =
            user1; createdAt = Date()
        }
        return message
    }

    val message1Updated = Message().apply {
        channel = channel1; cid = channel1.cid; text = "im update now"; id = "message-1"; user =
        user1
    }
    val reactionMessage = Message().apply {
        channel = channel1; text = "im update now"; id = "message-1"; user = user1
        reactionCounts = mutableMapOf("like" to 1); ownReactions =
        mutableListOf(reaction1); latestReactions = mutableListOf(reaction1)
    }
    val messageThread =
        Message().apply { channel = channel1; parentId = "message-1"; id = "message-2" }
    val message2Older = Message().apply {
        this.channel = channel1; text = "message2"; id = "message-2"; user = user1; createdAt =
        calendar(2019, 1, 1)
    }
    val messageFromUser2 = Message().apply {
        this.channel = channel1; text = "messageFromUser2"; id = "message-2"; user = user2; createdAt =
        calendar(2020, 1, 1)
    }

    val connectedEvent = ConnectedEvent()
    val connectedEvent2 = ConnectedEvent().apply { totalUnreadCount = 3; unreadChannels = 2 }

    val disconnectedEvent = DisconnectedEvent()
    val newMessageEvent = NewMessageEvent().apply { message = message1; cid = channel1.cid }
    val newMessageEvent2 = NewMessageEvent().apply { message = message2Older; cid = channel1.cid }
    val newMessageFromUser2 = NewMessageEvent().apply { message = messageFromUser2; cid = channel1.cid }

    val newMessageEventNotification = NotificationMessageNew().apply { message = message1; cid = channel1.cid }

    val messageUpdatedEvent = MessageUpdatedEvent().apply { message = message1Updated }
    val userStartWatchingEvent = UserStartWatchingEvent().apply { channel = channel1; user = user1 }
    val reactionEvent = ReactionNewEvent().apply { message = reactionMessage; reaction = reaction1; cid = channel1.cid }
    val reactionEvent2 = ReactionNewEvent().apply { reaction = reaction2 }

    val newMessageWithThreadEvent = NewMessageEvent().apply { message = messageThread }
    val channelUpdatedEvent =
        ChannelUpdatedEvent().apply { channel = channel1Updated; cid = channel1Updated.cid }
    val user1TypingStarted = TypingStartEvent().apply { user = user1 }
    val user1TypingStartedOld = TypingStartEvent().apply { user = user1; receivedAt = getOldDate() }
    val user3TypingStartedOld = TypingStartEvent().apply { user = user3; receivedAt = getOldDate() }

    val channelHiddenEvent = ChannelHiddenEvent().apply { user = user1; channel = channel2 }
    val channelVisibleEvent = ChannelVisible().apply { user = user1; channel = channel2 }

    val user2TypingStarted = TypingStartEvent().apply { user = user2; receivedAt = Date() }
    val user1TypingStop = TypingStopEvent().apply { user = user1 }
    val user2TypingStop = TypingStopEvent().apply { user = user2 }
    val readEvent = MessageReadEvent().apply {
        message = message1; user = user1; cid = channel1.cid; createdAt = Date()
    }

    val user1Read = MessageReadEvent().apply { user = user1; createdAt = Date() }
    val memberAddedToChannelEvent =
        MemberAddedEvent().apply { member = member2; cid = channel1.cid }

    val notificationAddedToChannelEvent =
        NotificationAddedToChannelEvent().apply { user = user1; channel = channel1 }

    val user1UpdatedEvent = UserUpdated().apply { user = user1updated }
}

fun calendar(
    year: Int,
    month: Int,
    date: Int,
    hourOfDay: Int = 0,
    minute: Int = 0,
    seconds: Int = 0
): Date {
    val calendar = Calendar.getInstance()
    calendar.set(year, month, date, hourOfDay, minute, seconds)
    return calendar.time
}
