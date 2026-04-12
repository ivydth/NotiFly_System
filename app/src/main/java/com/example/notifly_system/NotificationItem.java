package com.example.notifly_system;

public class NotificationItem {
    public String  id;
    public String  senderName;
    public String  message;
    public String  dateLabel;
    public String  category;
    public String  originalCategory; // never changes — keeps item in its home list
    public boolean isStarred;
    public boolean isRead;           // true once the user opens/reads it
    public int     avatarResId;

    public NotificationItem(String id, String senderName, String message,
                             String dateLabel, String category,
                             boolean isStarred, int avatarResId) {
        this.id               = id;
        this.senderName       = senderName;
        this.message          = message;
        this.dateLabel        = dateLabel;
        this.category         = category;
        this.originalCategory = category; // locked at creation
        this.isStarred        = isStarred;
        this.isRead           = false;
        this.avatarResId      = avatarResId;
    }
}
