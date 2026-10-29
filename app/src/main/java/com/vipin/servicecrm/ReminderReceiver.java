
package com.vipin.servicecrm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "vipin_crm_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        String details = intent.getStringExtra("details");
        int reminderId = intent.getIntExtra("reminder_id", 0);

        if (title == null || title.trim().isEmpty()) title = "VIPIN SERVICE CRM REMINDER";
        if (details == null) details = "आपका रिमाइंडर समय हो गया है।";

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "VIPIN CRM Reminders",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Customer calls, payments and service reminders");
            channel.enableVibration(true);
            channel.setLightColor(Color.BLUE);
            manager.createNotificationChannel(channel);
        }

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);

        builder.setSmallIcon(context.getApplicationInfo().icon)
                .setContentTitle(title)
                .setContentText(details)
                .setStyle(new Notification.BigTextStyle().bigText(details))
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL);

        manager.notify(reminderId, builder.build());
    }
}
