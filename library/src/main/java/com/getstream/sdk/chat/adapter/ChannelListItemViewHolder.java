package com.getstream.sdk.chat.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.getstream.sdk.chat.Chat;
import com.getstream.sdk.chat.MarkdownImpl;
import com.getstream.sdk.chat.R;
import com.getstream.sdk.chat.model.ModelType;
import com.getstream.sdk.chat.utils.LlcMigrationUtils;
import com.getstream.sdk.chat.utils.StringUtility;
import com.getstream.sdk.chat.view.AvatarGroupView;
import com.getstream.sdk.chat.view.ChannelListView;
import com.getstream.sdk.chat.view.ChannelListViewStyle;
import com.getstream.sdk.chat.view.ReadStateView;

import java.text.SimpleDateFormat;
import java.util.List;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;
import io.getstream.chat.android.client.models.*;

public class ChannelListItemViewHolder extends BaseChannelListItemViewHolder {

    static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d");

    protected TextView tv_name, tv_last_message, tv_date;
    protected ReadStateView<ChannelListViewStyle> read_state;
    protected AvatarGroupView<ChannelListViewStyle> avatarGroupView;
    protected ImageView iv_attachment_type;
    protected View click_area;
    protected Context context;

    protected ChannelListView.UserClickListener userClickListener;
    protected ChannelListView.ChannelClickListener channelClickListener;
    protected ChannelListView.ChannelClickListener channelLongClickListener;
    protected ChannelListViewStyle style;

    protected MarkdownImpl.MarkdownListener markdownListener;

    public ChannelListItemViewHolder(@NonNull View itemView) {
        super(itemView);
        findReferences();
    }


    public void setUserClickListener(ChannelListView.UserClickListener l) {
        userClickListener = l;
    }

    public void setChannelClickListener(ChannelListView.ChannelClickListener l) {
        channelClickListener = l;
    }

    public void setChannelLongClickListener(ChannelListView.ChannelClickListener l) {
        channelLongClickListener = l;
    }

    protected void findReferences() {
        tv_name = itemView.findViewById(R.id.tv_name);
        tv_last_message = itemView.findViewById(R.id.tv_last_message);
        iv_attachment_type = itemView.findViewById(R.id.iv_attachment_type);
        tv_date = itemView.findViewById(R.id.tv_date);

        click_area = itemView.findViewById(R.id.click_area);
        avatarGroupView = itemView.findViewById(R.id.avatar_group);
        read_state = itemView.findViewById(R.id.read_state);
    }

    public void setStyle(ChannelListViewStyle style) {
        this.style = style;
    }

    public void setMarkdownListener(MarkdownImpl.MarkdownListener markdownListener) {
        this.markdownListener = markdownListener;
    }

    @Override
    public void bind(Context context, @NonNull Channel channel, int position, @Nullable ChannelItemPayloadDiff diff) {

        // setup the click listeners and the markdown builder
        this.context = context;

        // the UI depends on the
        // - lastMessage
        // - unread count
        // - read state for this channel

        if (diff.name) configChannelName(channel);
        if (diff.avatarView) configAvatarView(channel);
        if (diff.lastMessage) configLastMessage(channel);
        if (diff.lastMessageDate) configLastMessageDate(channel);
        if (diff.readState) configReadState(channel);

        // set Click listeners
        configClickListeners(channel);
        // apply style
        applyStyle(channel);
    }

    // set the channel name
    protected void configChannelName(Channel channel) {
        String channelName = LlcMigrationUtils.getChannelNameOrMembers(channel);
        tv_name.setText((!TextUtils.isEmpty(channelName) ? channelName : style.getChannelWithoutNameText()));
    }

    protected void configAvatarView(Channel channel) {
        List<User> otherUsers = LlcMigrationUtils.getOtherUsers(channel);
        avatarGroupView.setChannelAndLastActiveUsers(channel, otherUsers, style);
        // click listeners
        avatarGroupView.setOnClickListener(view -> {
            // if there is 1 user
            if (otherUsers.size() == 1 && this.userClickListener != null) {
                this.userClickListener.onUserClick(otherUsers.get(0));
            } else if (this.channelClickListener != null) {
                this.channelClickListener.onClick(channel);
            }
        });
    }

    @SuppressLint("ResourceType")
    protected void configLastMessage(Channel channel) {
        Message lastMessage = LlcMigrationUtils.computeLastMessage(channel);
        iv_attachment_type.setVisibility(View.GONE);
        if (lastMessage == null) {
            tv_last_message.setText("");
            return;
        }

        if (!TextUtils.isEmpty(lastMessage.getText())) {
            if (markdownListener != null)
                markdownListener.setText(tv_last_message, StringUtility.getDeletedOrMentionedText(lastMessage));
            else
                MarkdownImpl.getInstance(context).setMarkdown(tv_last_message, StringUtility.getDeletedOrMentionedText(lastMessage));

            return;
        }

        if (lastMessage.getAttachments().isEmpty()) {
            tv_last_message.setText("");
            return;
        }

        Attachment attachment = lastMessage.getAttachments().get(0);
        iv_attachment_type.setVisibility(View.VISIBLE);

        String lastMessageText;
        @IdRes int attachmentType;

        switch (attachment.getType()) {
            case ModelType.attach_image:
                lastMessageText = context.getResources().getString(R.string.stream_last_message_attachment_photo);
                attachmentType = R.drawable.stream_ic_image;
                break;
            case ModelType.attach_file:
                if (attachment.getMimeType() != null && attachment.getMimeType().contains("video")) {
                    lastMessageText = context.getResources().getString(R.string.stream_last_message_attachment_video);
                    attachmentType = R.drawable.stream_ic_video;
                } else {
                    lastMessageText = !TextUtils.isEmpty(attachment.getTitle()) ? attachment.getTitle() : attachment.getFallback();
                    attachmentType = R.drawable.stream_ic_file;
                }
                break;
            case ModelType.attach_giphy:
                lastMessageText = context.getResources().getString(R.string.stream_last_message_attachment_giphy);
                attachmentType = R.drawable.stream_ic_gif;
                break;
            default:
                lastMessageText = !TextUtils.isEmpty(attachment.getTitle()) ? attachment.getTitle() : attachment.getFallback();
                attachmentType = R.drawable.stream_ic_file;
                break;
        }

        tv_last_message.setText(lastMessageText);
        iv_attachment_type.setImageDrawable(context.getDrawable(attachmentType));
    }

    protected void configLastMessageDate(Channel channel) {

        Message lastMessage = LlcMigrationUtils.computeLastMessage(channel);
        if (lastMessage == null) {
            tv_date.setText("");
            return;
        }

        if (lastMessage.isToday())
            tv_date.setText(lastMessage.getTime());
        else
            tv_date.setText(dateFormat.format(lastMessage.getCreatedAt()));
    }

    protected void configReadState(Channel channel) {
        List<ChannelUserRead> lastMessageReads = LlcMigrationUtils.getLastMessageReads(channel);
        read_state.setReads(lastMessageReads, true, style);
    }

    protected void configClickListeners(Channel channel) {

        click_area.setOnClickListener(view -> {
            if (this.channelClickListener != null)
                this.channelClickListener.onClick(channel);
        });

        click_area.setOnLongClickListener(view -> {
            if (this.channelLongClickListener != null)
                this.channelLongClickListener.onClick(channel);

            return true;
        });
    }

    protected void applyStyle(Channel channel) {
        tv_name.setTextSize(TypedValue.COMPLEX_UNIT_PX, style.channelTitleText.size);
        tv_last_message.setTextSize(TypedValue.COMPLEX_UNIT_PX, style.lastMessage.size);
        tv_date.setTextSize(TypedValue.COMPLEX_UNIT_PX, style.lastMessageDateText.size);

        User currentUser = Chat.getInstance().getClient().getCurrentUser();
        String currentUserId = currentUser.getId();

        Message lastMessage = LlcMigrationUtils.computeLastMessage(channel);
        boolean outgoing = (lastMessage != null && lastMessage.getUserId().equals(currentUserId));
        boolean readLastMessage = LlcMigrationUtils.readLastMessage(channel);

        if (readLastMessage || outgoing)
            applyReadStyle();
        else
            applyUnreadStyle();
    }

    protected void applyReadStyle() {
        // channel name
        style.channelTitleText.apply(tv_name);
        // last messsage
        style.lastMessage.apply(tv_last_message);
        style.lastMessageDateText.apply(tv_date);
        // last Message Attachment Type
        if (iv_attachment_type.getDrawable() != null)
            DrawableCompat.setTint(iv_attachment_type.getDrawable(), style.lastMessage.color);
    }

    protected void applyUnreadStyle() {
        // channel name
        style.channelTitleUnreadText.apply(tv_name);
        // last message
        style.lastMessageUnread.apply(tv_last_message);
        style.lastMessageDateUnreadText.apply(tv_date);
        // last Message Attachment Type
        if (iv_attachment_type.getDrawable() != null)
            DrawableCompat.setTint(iv_attachment_type.getDrawable(), style.lastMessageUnread.color);
    }

}
