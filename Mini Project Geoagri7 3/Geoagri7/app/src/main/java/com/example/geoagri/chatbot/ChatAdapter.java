package com.example.geoagri.chatbot;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.geoagri.R;

import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for user/AI messages and typing indicator.
 */
public final class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_USER = 1;
    private static final int TYPE_AI = 2;
    private static final int TYPE_TYPING = 3;

    private final List<MessageModel> messages;

    public ChatAdapter(@NonNull List<MessageModel> messages) {
        this.messages = messages;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return messages.get(position).getTimestamp() + position;
    }

    @Override
    public int getItemViewType(int position) {
        MessageModel message = messages.get(position);
        if (message.isTypingIndicator()) {
            return TYPE_TYPING;
        }
        if (message.getSender() == MessageModel.Sender.USER) {
            return TYPE_USER;
        }
        return TYPE_AI;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_USER) {
            View view = inflater.inflate(R.layout.item_message_user, parent, false);
            return new UserMessageViewHolder(view);
        }
        if (viewType == TYPE_TYPING) {
            View view = inflater.inflate(R.layout.item_typing_indicator, parent, false);
            return new TypingViewHolder(view);
        }
        View view = inflater.inflate(R.layout.item_message_ai, parent, false);
        return new AiMessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageModel message = messages.get(position);

        if (holder instanceof UserMessageViewHolder) {
            UserMessageViewHolder vh = (UserMessageViewHolder) holder;
            vh.messageText.setText(message.getText());
            vh.timeText.setText(formatTime(message.getTimestamp()));
            return;
        }

        if (holder instanceof AiMessageViewHolder) {
            AiMessageViewHolder vh = (AiMessageViewHolder) holder;
            vh.messageText.setText(message.getText());
            vh.timeText.setText(formatTime(message.getTimestamp()));
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @NonNull
    private String formatTime(long timestampMillis) {
        return DateFormat.format("hh:mm a", timestampMillis).toString().toUpperCase(Locale.getDefault());
    }

    static final class UserMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageText;
        private final TextView timeText;

        UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.textUserMessage);
            timeText = itemView.findViewById(R.id.textUserTime);
        }
    }

    static final class AiMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageText;
        private final TextView timeText;

        AiMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.textAiMessage);
            timeText = itemView.findViewById(R.id.textAiTime);
        }
    }

    static final class TypingViewHolder extends RecyclerView.ViewHolder {
        TypingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
