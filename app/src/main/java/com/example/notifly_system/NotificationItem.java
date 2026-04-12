package com.example.notifly_system;

public class NotificationItem {

    public String  id;
    public String  senderName;
    public String  message;
    public String  dateLabel;
    public String  category;     // "Unread" | "Announcements" | "Events" | "Starred"
    public boolean isStarred;
    public int     avatarResId;  // e.g. R.drawable.avatar_teal

    public NotificationItem(String id, String senderName, String message,
                             String dateLabel, String category,
                             boolean isStarred, int avatarResId) {
        this.id          = id;
        this.senderName  = senderName;
        this.message     = message;
        this.dateLabel   = dateLabel;
        this.category    = category;
        this.isStarred   = isStarred;
        this.avatarResId = avatarResId;
    }
}
