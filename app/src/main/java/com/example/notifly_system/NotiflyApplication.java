package com.example.notifly_system;

import android.app.Application;

public class NotiflyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Create notification channels once at app startup
        NotiflyMessagingService.createChannels(this);

        // Start listening to Firebase /notifications as soon as app launches
        FirebaseNotifSyncService.getInstance().startListening();
    }
}
