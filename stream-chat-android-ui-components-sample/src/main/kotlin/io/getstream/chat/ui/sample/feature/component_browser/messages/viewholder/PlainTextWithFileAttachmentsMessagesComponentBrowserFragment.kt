/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
 *
 * Licensed under the Stream License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/GetStream/stream-chat-android/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.getstream.chat.ui.sample.feature.component_browser.messages.viewholder

import com.getstream.sdk.chat.adapter.MessageListItem
import io.getstream.chat.android.client.models.Attachment
import io.getstream.chat.android.client.models.Message
import io.getstream.chat.android.client.utils.SyncStatus
import io.getstream.chat.android.core.internal.InternalStreamChatApi
import io.getstream.chat.ui.sample.R
import io.getstream.chat.ui.sample.feature.component_browser.utils.drawableResToUri

class PlainTextWithFileAttachmentsMessagesComponentBrowserFragment : BaseMessagesComponentBrowserFragment() {

    @OptIn(InternalStreamChatApi::class)
    override fun getItems(): List<MessageListItem.MessageItem> {
        val attachmentLink = Attachment(
            titleLink = drawableResToUri(requireContext(), R.drawable.stream_ui_sample_image_1),
            title = "Title",
            text = "Some description",
            authorName = "Stream",
        )
        return listOf(
            MessageListItem.MessageItem(
                message = Message(attachments = mutableListOf(attachmentPdf), text = "Some text"),
                positions = listOf(MessageListItem.Position.TOP),
                isMine = true
            ),
            MessageListItem.MessageItem(
                message = Message(
                    attachments = mutableListOf(attachment7z, attachmentPdf),
                    text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
                ),
                positions = listOf(MessageListItem.Position.MIDDLE),
                isMine = true
            ),
            MessageListItem.MessageItem(
                message = Message(
                    attachments = mutableListOf(attachmentTxt, attachmentPdf, attachmentPpt),
                    text = "Hi!"
                ),
                positions = listOf(MessageListItem.Position.BOTTOM),
                isMine = true
            ),
            MessageListItem.MessageItem(
                message = Message(
                    attachments = mutableListOf(attachmentDoc, attachmentXls),
                    text = "Lorem ipsum dolor sit amet"
                ),
                positions = listOf(MessageListItem.Position.TOP),
                isMine = false
            ),
            MessageListItem.MessageItem(
                message = Message(
                    attachments = mutableListOf(attachmentXls, attachmentPdf, attachment7z),
                    text = "Another message"
                ),
                positions = listOf(MessageListItem.Position.MIDDLE),
                isMine = false
            ),
            MessageListItem.MessageItem(
                message = Message(
                    attachments = mutableListOf(
                        attachmentPpt,
                        attachment7z,
                        attachmentTxt,
                        attachmentDoc
                    ),
                    text = "Bye!!!"
                ),
                positions = listOf(MessageListItem.Position.BOTTOM),
                isMine = false
            ),
            MessageListItem.MessageItem(
                message = Message(
                    attachments = mutableListOf(
                        attachmentPdf,
                        attachmentPpt,
                        attachment7z,
                        attachmentTxt,
                        attachmentDoc,
                        attachmentXls,
                    ),
                    text = "Bye!!!"
                ),
                positions = listOf(MessageListItem.Position.TOP, MessageListItem.Position.BOTTOM),
                isMine = true
            ),
            MessageListItem.MessageItem(
                message = Message(
                    attachments = mutableListOf(attachmentDoc, attachmentXls),
                    text = "Lorem ipsum dolor sit amet",
                    syncStatus = SyncStatus.FAILED_PERMANENTLY,
                ),
                positions = listOf(MessageListItem.Position.BOTTOM),
                isMine = false
            ),
            MessageListItem.MessageItem(
                message = Message(
                    attachments = mutableListOf(
                        attachmentDoc,
                        attachmentXls,
                        attachmentLink,
                    ),
                    text = "Lorem ipsum dolor sit amet https://www.google.com/",
                ),
                positions = listOf(MessageListItem.Position.BOTTOM),
                isMine = true
            ),
            MessageListItem.MessageItem(
                message = Message(
                    attachments = mutableListOf(
                        attachmentDoc,
                        attachmentXls,
                        attachmentLink,
                    ),
                    text = "Lorem ipsum dolor sit amet https://www.google.com/",
                ),
                positions = listOf(MessageListItem.Position.BOTTOM),
                isMine = false
            ),
        )
    }

    @InternalStreamChatApi
    companion object {
        private const val KILOBYTE = 1024
        fun Int.kiloBytes() = this * KILOBYTE

        val attachmentPdf = Attachment(
            type = "file",
            mimeType = "application/pdf",
            fileSize = 120.kiloBytes(),
            title = "Sample pdf file"
        )
        val attachmentPpt = Attachment(
            type = "file",
            mimeType = "application/vnd.ms-powerpoint",
            fileSize = 567.kiloBytes(),
            title = "Sample ppt file"
        )
        val attachment7z = Attachment(
            type = "file",
            mimeType = "application/x-7z-compressed",
            fileSize = 1920.kiloBytes(),
            title = "Sample archive file"
        )
        val attachmentTxt = Attachment(
            type = "file",
            mimeType = "text/plain",
            fileSize = 18.kiloBytes(),
            title = "Sample text file"
        )
        val attachmentDoc = Attachment(
            type = "file",
            mimeType = "application/msword",
            fileSize = 89.kiloBytes(),
            title = "Sample doc file"
        )
        val attachmentXls = Attachment(
            type = "file",
            mimeType = "application/vnd.ms-excel",
            fileSize = 5234.kiloBytes(),
            title = "Sample xls file"
        )
    }
}
