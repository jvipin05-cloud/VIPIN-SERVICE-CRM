package com.vipin.servicecrm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

public class ReminderBootReceiver extends BroadcastReceiver {

    private static final String PREF = "vipin_service_crm";
    private static final String REMINDERS = "reminders";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent == null) return;

        String action = intent.getAction();

        if (!Intent.ACTION_BOOT_COMPLETED.equals(action) &&
                !"android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
                        .equals(action)) {
            return;
        }

        rescheduleAll(context);
    }

    public static void rescheduleAll(Context context) {

        try {
            String saved = context.getSharedPreferences(
                    PREF, Context.MODE_PRIVATE)
                    .getString(REMINDERS, "[]");

            JSONArray array = new JSONArray(saved);

            AlarmManager alarm = (AlarmManager)
                    context.getSystemService(Context.ALARM_SERVICE);

            if (alarm == null) return;

            long now = System.currentTimeMillis();

            for (int i = 0; i < array.length(); i++) {

                JSONObject item = array.optJSONObject(i);
                if (item == null) continue;

                if (item.optBoolean("DONE", false) ||
                        item.optBoolean("FIRED", false)) {
                    continue;
                }

                long id = item.optLong("ID", -1L);
                long when = item.optLong("TIME", 0L);

                if (id < 0 || when <= 0) continue;

                Intent reminderIntent = new Intent(
                        context, ReminderReceiver.class);

                reminderIntent.putExtra("reminder_id", id);
                reminderIntent.putExtra(
                        "type", item.optString("TYPE"));
                reminderIntent.putExtra(
                        "customer", item.optString("CUSTOMER"));
                reminderIntent.putExtra(
                        "mobile", item.optString("MOBILE"));
                reminderIntent.putExtra(
                        "details", item.optString("DETAILS"));

                int requestCode = (int) (id & 0x7fffffff);

                PendingIntent pending = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        reminderIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT |
                                PendingIntent.FLAG_IMMUTABLE);

                try {
                    if (when <= now) {

                        // Reminder time passed while phone was switched off.
                        // Send it now; receiver checks FIRED and DONE.
                        context.sendBroadcast(reminderIntent);

                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                        alarm.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                when,
                                pending);

                    } else {

                        alarm.set(
                                AlarmManager.RTC_WAKEUP,
                                when,
                                pending);
                    }

                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
