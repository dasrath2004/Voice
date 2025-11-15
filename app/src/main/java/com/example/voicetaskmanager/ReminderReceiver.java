package com.example.voicetaskmanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent i) {
        String title = i.getStringExtra("title");
        String id = i.getStringExtra("id");
        if (title == null) title = "Task";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, NotificationHelper.CHANNEL_ID)
                .setContentTitle("Task reminder")
                .setContentText(title + " is due in 30 minutes")
                .setSmallIcon(R.drawable.ic_notification)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManagerCompat.from(ctx).notify(id != null ? id.hashCode() : (int)System.currentTimeMillis(), builder.build());
    }
}
