package com.example.geoagri.chatbot;

import androidx.annotation.NonNull;

/**
 * Immutable chat message model used by RecyclerView.
 */
public final class MessageModel {

    public enum Sender {
        USER,
        AI,
        SYSTEM
    }

    private final String text;
    private final Sender sender;
    private final long timestamp;
    private final boolean typingIndicator;

    private MessageModel(@NonNull String text, @NonNull Sender sender, long timestamp, boolean typingIndicator) {
        this.text = text;
        this.sender = sender;
        this.timestamp = timestamp;
        this.typingIndicator = typingIndicator;
    }

    @NonNull
    public static MessageModel user(@NonNull String text) {
        return new MessageModel(text, Sender.USER, System.currentTimeMillis(), false);
    }

    @NonNull
    public static MessageModel ai(@NonNull String text) {
        return new MessageModel(text, Sender.AI, System.currentTimeMillis(), false);
    }

    @NonNull
    public static MessageModel system(@NonNull String text) {
        return new MessageModel(text, Sender.SYSTEM, System.currentTimeMillis(), false);
    }

    @NonNull
    public static MessageModel typing() {
        return new MessageModel("", Sender.AI, System.currentTimeMillis(), true);
    }

    @NonNull
    public String getText() {
        return text;
    }

    @NonNull
    public Sender getSender() {
        return sender;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isTypingIndicator() {
        return typingIndicator;
    }
}
