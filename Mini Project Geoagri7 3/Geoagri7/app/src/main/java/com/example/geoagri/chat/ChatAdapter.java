package com.example.geoagri.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.geoagri.R;
import com.example.geoagri.model.ChatMessage;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_BOT = 0;
    private static final int TYPE_USER = 1;

    private final List<ChatMessage> messages;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isUser() ? TYPE_USER : TYPE_BOT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_USER) {
            View view = inflater.inflate(R.layout.item_user, parent, false);
            return new UserViewHolder(view);
        }

        View view = inflater.inflate(R.layout.ai_message, parent, false);
        return new BotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);

        if (holder instanceof UserViewHolder) {
            UserViewHolder userHolder = (UserViewHolder) holder;
            userHolder.msgText.setText(message.getText());
            userHolder.timeText.setText(message.getTime());
            return;
        }

        BotViewHolder botHolder = (BotViewHolder) holder;
        if (message.isTypingIndicator()) {
            botHolder.msgText.setVisibility(View.GONE);
            botHolder.insightCard.setVisibility(View.GONE);
            botHolder.typingIndicatorRow.setVisibility(View.VISIBLE);
            botHolder.timeText.setText(message.getTime());
            return;
        }

        botHolder.msgText.setVisibility(View.VISIBLE);
        botHolder.typingIndicatorRow.setVisibility(View.GONE);
        botHolder.msgText.setText(message.getText());
        botHolder.timeText.setText(message.getTime());
        botHolder.insightCard.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        final TextView msgText;
        final TextView timeText;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            msgText = itemView.findViewById(R.id.msgText);
            timeText = itemView.findViewById(R.id.timeText);
        }
    }

    static class BotViewHolder extends RecyclerView.ViewHolder {
        final TextView msgText;
        final TextView timeText;
        final LinearLayout insightCard;
        final LinearLayout typingIndicatorRow;
        final ProgressBar typingProgress;

        BotViewHolder(@NonNull View itemView) {
            super(itemView);
            msgText = itemView.findViewById(R.id.msgText);
            timeText = itemView.findViewById(R.id.timeText);
            insightCard = itemView.findViewById(R.id.insightCard);
            typingIndicatorRow = itemView.findViewById(R.id.typingIndicatorRow);
            typingProgress = itemView.findViewById(R.id.typingProgress);
        }
    }
}
