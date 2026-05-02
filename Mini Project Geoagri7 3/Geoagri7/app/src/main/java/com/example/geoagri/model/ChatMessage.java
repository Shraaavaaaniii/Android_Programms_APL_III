package com.example.geoagri.model;

public class ChatMessage {
    private final String text;
    private final boolean user;
    private final String time;
    private final boolean showInsightCard;
    private final boolean typingIndicator;

    public ChatMessage(String text, boolean user) {
        this(text, user, "", false, false);
    }

    public ChatMessage(String text, boolean user, String time, boolean showInsightCard) {
        this(text, user, time, showInsightCard, false);
    }

    public ChatMessage(String text, boolean user, String time, boolean showInsightCard, boolean typingIndicator) {
        this.text = text;
        this.user = user;
        this.time = time;
        this.showInsightCard = showInsightCard;
        this.typingIndicator = typingIndicator;
    }

    public String getText() {
        return text;
    }

    public boolean isUser() {
        return user;
    }

    public String getTime() {
        return time;
    }

    public boolean shouldShowInsightCard() {
        return showInsightCard;
    }

    public boolean isTypingIndicator() {
        return typingIndicator;
    }
}
