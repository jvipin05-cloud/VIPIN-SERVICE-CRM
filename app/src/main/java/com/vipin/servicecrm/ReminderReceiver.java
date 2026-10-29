
package com.vipin.servicecrm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "vipin_crm_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {

        String type = intent.getStringExtra("type");
        String customer = intent.getStringExtra("customer");
        String mobile = intent.getStringExtra("mobile");
        String details = intent.getStringExtra("details");

        if (type == null) type = "CUSTOMER REMINDER";
        if (customer == null) customer = "CUSTOMER";
        if (mobile == null) mobile = "";
        if (details == null) details = "";

        NotificationManager manager =
                (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE);

        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "VIPIN SERVICE CRM REMINDERS",
                    NotificationManager.IMPORTANCE_HIGH);

            channel.setDescription("AMC, PAYMENT AND SERVICE REMINDERS");
            manager.createNotificationChannel(channel);
        }

        Intent openApp = new Intent(context, MainActivity.class);
        openApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(),
                openApp,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE);

        String message = customer;

        if (!mobile.isEmpty()) {
            message += " | " + mobile;
        }

        if (!details.isEmpty()) {
            message += "\n" + details;
        }

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context);
            builder.setPriority(Notification.PRIORITY_HIGH);
        }

        Notification notification = builder
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("VIPIN CRM - " + type)
                .setContentText(message)
                .setStyle(new Notification.BigTextStyle()
                        .bigText(message))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build();

        manager.notify(
                (int) System.currentTimeMillis(),
                notification);
    }
}
