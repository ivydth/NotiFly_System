package com.example.notifly_system;

import android.app.Application;

public class NotiflyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Create notification channels once at app startup
        NotiflyMessagingService.createChannels(this);

        // Pass context to sync service BEFORE startListening()
        FirebaseNotifSyncService.getInstance().init(this);

        // Start listening to Firebase /notifications
        FirebaseNotifSyncService.getInstance().startListening();
    }
}
