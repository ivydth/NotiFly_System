package com.example.notifly_system;

public class NotificationItem {
    public String  id;
    public String  senderName;
    public String  message;
    public String  dateLabel;
    public String  category;
    public String  originalCategory;
    public boolean isStarred;
    public boolean isRead;
    public boolean isArchived;
    public long    timestamp;        // ← from Firebase
    public int     avatarResId;

    public NotificationItem(String id, String senderName, String message,
                             String dateLabel, String category,
                             boolean isStarred, int avatarResId) {
        this.id               = id;
        this.senderName       = senderName;
        this.message          = message;
        this.dateLabel        = dateLabel;
        this.category         = category;
        this.originalCategory = category;
        this.isStarred        = isStarred;
        this.isRead           = false;
        this.isArchived       = false;
        this.timestamp        = 0;
        this.avatarResId      = avatarResId;
    }
}
