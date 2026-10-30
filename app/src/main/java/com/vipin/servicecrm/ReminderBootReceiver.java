package com.vipin.servicecrm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.json.JSONArray;
import org.json.JSONObject;

public class ReminderBootReceiver extends BroadcastReceiver {

    private static final String PREF = "vipin_service_crm";
    private static final String REMINDERS = "reminders";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }

        try {
            String saved = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                    .getString(REMINDERS, "[]");

            JSONArray array = new JSONArray(saved);
            AlarmManager alarm = (AlarmManager)
                    context.getSystemService(Context.ALARM_SERVICE);

            if (alarm == null) return;

            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);

                if (!"PENDING".equals(item.optString("STATUS"))) {
                    continue;
                }

                long id = item.optLong("ID");
                long when = item.optLong("WHEN");

                Intent reminderIntent =
                        new Intent(context, ReminderReceiver.class);

                reminderIntent.putExtra("type", item.optString("TYPE"));
                reminderIntent.putExtra("customer", item.optString("CUSTOMER"));
                reminderIntent.putExtra("mobile", item.optString("MOBILE"));
                reminderIntent.putExtra("details", item.optString("DETAILS"));

                int requestCode = (int) (id & 0x7fffffff);

                PendingIntent pending = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        reminderIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT |
                                PendingIntent.FLAG_IMMUTABLE
                );

                alarm.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        when,
                        pending
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
