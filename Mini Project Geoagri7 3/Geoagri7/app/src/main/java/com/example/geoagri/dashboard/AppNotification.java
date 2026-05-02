package com.example.geoagri.dashboard;

import com.google.firebase.Timestamp;

public class AppNotification {
    private String title;
    private String message;
    private String userId;
    private Timestamp timestamp;
    private boolean read;

    public AppNotification() {
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getUserId() {
        return userId;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public boolean isRead() {
        return read;
    }
}
