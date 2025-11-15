package com.example.voicetaskmanager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ScheduleReminder {

    public static void setAlarm(Context ctx, long timeMillis, String taskId, String title) {
        try {
            if (ctx == null) return;
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            Intent i = new Intent(ctx, ReminderReceiver.class);
            i.putExtra("id", taskId);
            i.putExtra("title", title);

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;

            PendingIntent pi = PendingIntent.getBroadcast(ctx, taskId.hashCode(), i, flags);
            if (am != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pi);
                } else {
                    am.setExact(AlarmManager.RTC_WAKEUP, timeMillis, pi);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void cancel(Context ctx, String taskId) {
        if (ctx == null) return;
        Intent i = new Intent(ctx, ReminderReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getBroadcast(ctx, taskId.hashCode(), i, flags);
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(pi);
    }
}
