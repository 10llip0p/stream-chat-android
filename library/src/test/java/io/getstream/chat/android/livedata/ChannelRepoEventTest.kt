package io.getstream.chat.android.livedata

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import io.getstream.chat.android.livedata.utils.getOrAwaitValue
import org.junit.*
import org.junit.runner.RunWith

/**
 * This test suite verifies that ChatChannelRepo implements event handling correctly and updates it's local state
 * Offline storage for these events is handled at the ChannelRepo level, so this is only testing local state changes
 * Note that we don't rely on Room's livedata mechanism as this library needs to work without room enabled as well
 */
@RunWith(AndroidJUnit4::class)
class ChatChannelRepoEventTest: BaseTest() {

    @Before
    fun setup() {

        client = createClient()
        setupRepo(client, false)
    }

    @After
    fun tearDown() {
        db.close()
        client.disconnect()
    }

    @Test
    fun eventWatcherCountUpdates() {
        val event = data.userStartWatchingEvent
        channelRepo.handleEvent(event)
        Truth.assertThat(channelRepo.watcherCount.getOrAwaitValue()).isEqualTo(100)
        val users = event.channel?.watchers!!.map { it.user }
        Truth.assertThat(channelRepo.watchers.getOrAwaitValue()).isEqualTo(users)
    }

    @Test
    fun eventNewMessage() {
        channelRepo.handleEvent(data.newMessageEvent)
        Truth.assertThat(channelRepo.messages.getOrAwaitValue()).isEqualTo(listOf(data.newMessageEvent.message))
    }

    @Test
    fun eventUpdatedMessage() {

        channelRepo.handleEvent(data.newMessageEvent)
        val event = data.messageUpdatedEvent
        channelRepo.handleEvent(event)

        val messages = channelRepo.messages.getOrAwaitValue()
        Truth.assertThat(messages.size).isEqualTo(1)
        Truth.assertThat(messages).isEqualTo(listOf(event.message))
    }

    @Test
    @Ignore
    fun userChangesFavoriteColor() {
        channelRepo.handleEvent(data.newMessageEvent)
        channelRepo.handleEvent(data.reactionEvent)
        channelRepo.handleEvent(data.user1UpdatedEvent)
        val message = channelRepo.getMessage(data.message1.id)
        Truth.assertThat(message!!.user.extraData.get("color")).isEqualTo("green")
        Truth.assertThat(message!!.latestReactions.first().user!!.extraData["color"]).isEqualTo("green")

    }

    @Test
    fun memberAddedEvent() {
        // ensure the channel data is initialized:
        channelRepo.upsertMember(data.channel1.members[0])
        var members = channelRepo.members.getOrAwaitValue()
        Truth.assertThat(members.size).isEqualTo(1)
        // add a member, we should go from list size 1 to 2
        channelRepo.handleEvent(data.memberAddedToChannelEvent)
        members = channelRepo.members.getOrAwaitValue()
        Truth.assertThat(members.size).isEqualTo(2)
    }

    @Test
    fun typingEvents() {
        channelRepo.handleEvent(data.user1TypingStarted)
        channelRepo.handleEvent(data.user2TypingStarted)
        channelRepo.handleEvent(data.user1TypingStop)
        val typing = channelRepo.typing.getOrAwaitValue()
        Truth.assertThat(typing).isEqualTo(listOf(data.user2))
    }

    @Test
    fun readEvents() {
        channelRepo.handleEvent(data.user1Read)
        val reads = channelRepo.reads.getOrAwaitValue()
        Truth.assertThat(reads.size).isEqualTo(1)
        Truth.assertThat(reads[0].user.id).isEqualTo(data.user1.id)
    }

    @Test
    fun eventMessageWithThread() {
        channelRepo.handleEvent(data.newMessageEvent)
        channelRepo.handleEvent(data.newMessageWithThreadEvent)
        val parentId = data.newMessageWithThreadEvent.message.parentId!!

        val messages = channelRepo.getThreadMessages(parentId).getOrAwaitValue()
        Truth.assertThat(messages?.size).isEqualTo(2)
    }

    @Test
    fun eventUpdatedChannel() {
        channelRepo.handleEvent(data.channelUpdatedEvent)
        val channel = channelRepo.channel.getOrAwaitValue()
        Truth.assertThat(channel.extraData.get("color")).isEqualTo("green")
    }

    @Test
    fun eventReaction() {
        channelRepo.handleEvent(data.reactionEvent)
        val messages = channelRepo.messages.getOrAwaitValue()
        Truth.assertThat(messages.size).isEqualTo(1)
        Truth.assertThat(messages[0]).isEqualTo(data.reactionEvent.message)
    }
}