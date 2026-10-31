
package com.vipin.servicecrm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String PREF = "vipin_service_crm";
    private static final String REMINDERS = "reminders";
    private static final String CHANNEL_ID = "vipin_crm_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent == null) return;

        long reminderId = intent.getLongExtra("reminder_id", -1L);
        String type = intent.getStringExtra("type");
        String customer = intent.getStringExtra("customer");
        String mobile = intent.getStringExtra("mobile");
        String details = intent.getStringExtra("details");

        if (type == null) type = "CUSTOMER REMINDER";
        if (customer == null) customer = "CUSTOMER";
        if (mobile == null) mobile = "";
        if (details == null) details = "";

        // Notification permission check
        if (Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(
                        "android.permission.POST_NOTIFICATIONS")
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationManager manager =
                (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE);

        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "VIPIN SERVICE CRM REMINDERS",
                    NotificationManager.IMPORTANCE_HIGH);

            channel.setDescription(
                    "AMC, PAYMENT AND SERVICE REMINDERS");

            manager.createNotificationChannel(channel);
        }

        Intent openApp = new Intent(context, MainActivity.class);
        openApp.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP);

        if (reminderId >= 0) {
            openApp.putExtra("reminder_id", reminderId);
        }

        int requestCode = reminderId >= 0
                ? (int) (reminderId & 0x7fffffff) : 8102;

        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                requestCode,
                openApp,
                PendingIntent.FLAG_UPDATE_CURRENT |
                PendingIntent.FLAG_IMMUTABLE);

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
                .setStyle(new Notification.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .build();

        int notificationId = reminderId >= 0
                ? (int) (reminderId & 0x7fffffff)
                : (int) System.currentTimeMillis();

        try {
            manager.notify(notificationId, notification);

            // Notification सफल होने के बाद ही FIRED करें।
            // Reminder को कभी अपने आप DELETE नहीं करना है।
            if (reminderId >= 0) {
                markReminderFired(context, reminderId);
            }

        } catch (SecurityException ignored) {
            // Permission न होने पर reminder saved रहेगा।
        }
    }

    private void markReminderFired(Context context, long reminderId) {

        SharedPreferences prefs =
                context.getSharedPreferences(PREF, Context.MODE_PRIVATE);

        try {
            JSONArray items = new JSONArray(
                    prefs.getString(REMINDERS, "[]"));

            for (int i = 0; i < items.length(); i++) {

                JSONObject item = items.optJSONObject(i);

                if (item != null &&
                        item.optLong("ID", -2L) == reminderId) {

                    item.put("FIRED", true);

                    prefs.edit()
                            .putString(REMINDERS, items.toString())
                            .apply();

                    break;
                }
            }

        } catch (Exception ignored) {
        }
    }
}
