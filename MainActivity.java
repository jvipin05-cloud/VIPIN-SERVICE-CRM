
package com.vipin.servicecrm;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {

    private static final String PREF = "VIPIN_CRM";
    private static final String RECORDS = "records";

    private final int BLUE = Color.rgb(25, 75, 180);
    private final int GREEN = Color.rgb(20, 150, 85);
    private final int ORANGE = Color.rgb(240, 135, 25);
    private final int PURPLE = Color.rgb(125, 65, 180);
    private final int RED = Color.rgb(210, 65, 65);
    private final int DARK = Color.rgb(35, 45, 65);
    private final int BG = Color.rgb(239, 244, 255);

    private SharedPreferences prefs;
    private JSONArray records;
    private LinearLayout root, recordList, totalsBox;
    private EditText customer, mobile, invoice, product;
    private EditText spare, net, warranty, amc, collection;
    private TextView dateText;
    private int selectedMonth, selectedYear;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences(PREF, MODE_PRIVATE);
        String defaultHash = hash("Jvipin");
        if (!prefs.contains("password")) {
            prefs.edit().putString("password", defaultHash).apply();
        }

        try {
            records = new JSONArray(prefs.getString(RECORDS, "[]"));
        } catch (Exception e) {
            records = new JSONArray();
        }

        Calendar c = Calendar.getInstance();
        selectedMonth = c.get(Calendar.MONTH) + 1;
        selectedYear = c.get(Calendar.YEAR);
        showLogin();
    }

    private String hash(String s) {
        try {
            java.security.MessageDigest md =
                    java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(("VIPIN_CRM_SALT_" + s).getBytes("UTF-8"));
            StringBuilder out = new StringBuilder();
            for (byte x : bytes) out.append(String.format("%02x", x));
            return out.toString();
        } catch (Exception e) {
            return s;
        }
    }

    private GradientDrawable bg(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private GradientDrawable gradient(int c1, int c2) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{c1, c2});
        d.setCornerRadius(28);
        return d;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        t.setPadding(8, 8, 8, 8);
        return t;
    }

    private LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout horizontal() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private void setupRoot() {
        root = vertical();
        root.setPadding(14, 12, 14, 18);
        root.setBackgroundColor(BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(root);
        setContentView(scroll);
    }

    private void header(String subtitle) {
        LinearLayout h = vertical();
        h.setPadding(18, 18, 18, 18);
        h.setBackground(gradient(BLUE, PURPLE));

        TextView title = text("VIPIN SERVICE CRM", 23, Color.WHITE, true);
        title.setGravity(Gravity.CENTER);
        h.addView(title);

        TextView sub = text(subtitle, 13, Color.WHITE, false);
        sub.setGravity(Gravity.CENTER);
        h.addView(sub);
        root.addView(h);
        space(root, 12);
    }

    private void space(LinearLayout parent, int height) {
        View v = new View(this);
        parent.addView(v, new LinearLayout.LayoutParams(1, height));
    }

    private LinearLayout card(String heading, int color) {
        LinearLayout c = vertical();
        c.setPadding(13, 10, 13, 13);
        c.setBackground(bg(Color.WHITE, 20));
        c.setElevation(4);

        TextView title = text(heading, 17, color, true);
        c.addView(title);

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 0, 0, 12);
        root.addView(c, p);
        return c;
    }

    private EditText field(LinearLayout parent, String hint,
                           String value, boolean number) {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setTextSize(15);
        e.setTextColor(DARK);
        e.setHintTextColor(Color.GRAY);
        e.setHint(hint);
        e.setPadding(13, 10, 13, 10);
        e.setBackground(bg(Color.rgb(245, 247, 253), 13));
        if (value != null) e.setText(value);
        if (number) e.setInputType(8194);
        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 4, 0, 8);
        parent.addView(e, p);
        return e;
    }

    private Button button(String label, int color, LinearLayout parent,
                          View.OnClickListener click) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTypeface(null, Typeface.BOLD);
        b.setBackground(bg(color, 16));
        b.setPadding(10, 8, 10, 8);
        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 5, 0, 5);
        parent.addView(b, p);
        b.setOnClickListener(click);
        return b;
    }

    private void showLogin() {
        setupRoot();
        space(root, 55);
        LinearLayout c = card("🔐  LOGIN", BLUE);
        TextView welcome = text("Welcome to VIPIN SERVICE CRM",
                18, PURPLE, true);
        welcome.setGravity(Gravity.CENTER);
        c.addView(welcome);

        EditText pass = field(c, "Enter Password", "", false);
        pass.setInputType(129);

        button("OPEN APP", GREEN, c, v -> {
            String entered = pass.getText().toString();
            if (hash(entered).equals(prefs.getString("password", hash("Jvipin")))) {
                showCRM();
            } else {
                Toast.makeText(this, "Wrong Password", Toast.LENGTH_SHORT).show();
            }
        });
        space(root, 20);
        TextView note = text("Offline Service Management", 13, PURPLE, true);
        note.setGravity(Gravity.CENTER);
        root.addView(note);
    }

    private void showCRM() {
        setupRoot();
        header("Customer • Service • AMC • Collection");

        LinearLayout customerCard = card("🟦 CUSTOMER DETAILS", BLUE);
        customer = field(customerCard, "Customer Name", "", false);
        mobile = field(customerCard, "Mobile Number", "", true);
        invoice = field(customerCard, "Bill / Invoice Number", "", false);
        product = field(customerCard, "Product Name", "", false);

        LinearLayout dateCard = card("📅 SERVICE DATE", PURPLE);
        dateText = text(new SimpleDateFormat("dd/MM/yyyy",
                Locale.getDefault()).format(new Date()), 17, DARK, true);
        dateText.setGravity(Gravity.CENTER);
        dateText.setBackground(bg(Color.rgb(240, 232, 255), 14));
        dateCard.addView(dateText);
        button("SELECT DATE", PURPLE, dateCard, v -> openDatePicker());

        LinearLayout amountCard = card("🟠 AMOUNT DETAILS", ORANGE);
        spare = field(amountCard, "Spare Parts Amount ₹", "", true);
        net = field(amountCard, "Net Amount ₹", "", true);
        warranty = field(amountCard, "Warranty Amount ₹", "", true);

        LinearLayout extraCard = card("🟢 AMC & COLLECTION (SEPARATE)", GREEN);
        amc = field(extraCard, "AMC Amount ₹", "", true);
        collection = field(extraCard, "Collection Amount ₹", "", true);

        LinearLayout actions = card("⚡ ACTIONS", BLUE);
        button("💾 SAVE RECORD", GREEN, actions, v -> saveRecord());
        button("📊 MONTH / YEAR RECORDS", PURPLE, actions, v -> showRecords());
        button("📁 EXPORT EXCEL CSV", ORANGE, actions, v -> exportCSV());
        button("🔒 CHANGE PASSWORD", RED, actions, v -> changePassword());
    }

    private void openDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            dateText.setText(String.format(Locale.getDefault(),
                    "%02d/%02d/%04d", day, month + 1, year));
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private double amount(EditText e) {
        try {
            return Double.parseDouble(e.getText().toString().trim());
        } catch (Exception ex) {
            return 0;
        }
    }

    private String value(EditText e) {
        return e.getText().toString().trim();
    }

    private void saveRecord() {
        if (value(customer).isEmpty()) {
            Toast.makeText(this, "Customer Name भरें", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject obj = new JSONObject();
        try {
            obj.put("customer", value(customer));
            obj.put("mobile", value(mobile));
            obj.put("invoice", value(invoice));
            obj.put("product", value(product));
            obj.put("date", dateText.getText().toString());
            obj.put("spare", amount(spare));
            obj.put("net", amount(net));
            obj.put("warranty", amount(warranty));
            obj.put("amc", amount(amc));
            obj.put("collection", amount(collection));

            records.put(obj);
            prefs.edit().putString(RECORDS, records.toString()).apply();

            Toast.makeText(this, "Record Saved Successfully",
                    Toast.LENGTH_LONG).show();
            showCRM();
        } catch (Exception e) {
            Toast.makeText(this, "Save Error", Toast.LENGTH_SHORT).show();
        }
    }

    private void showRecords() {
        setupRoot();
        header("MONTHLY / YEARLY REPORT");

        LinearLayout filter = card("🟣 SELECT MONTH & YEAR", PURPLE);
        NumberPicker month = new NumberPicker(this);
        month.setMinValue(1);
        month.setMaxValue(12);
        month.setValue(selectedMonth);

        NumberPicker year = new NumberPicker(this);
        year.setMinValue(2020);
        year.setMaxValue(2100);
        year.setValue(selectedYear);

        LinearLayout pickers = horizontal();
        LinearLayout mBox = vertical();
        LinearLayout yBox = vertical();
        mBox.addView(text("MONTH", 13, PURPLE, true));
        yBox.addView(text("YEAR", 13, PURPLE, true));
        mBox.addView(month);
        yBox.addView(year);
        pickers.addView(mBox, new LinearLayout.LayoutParams(0, -2, 1));
        pickers.addView(yBox, new LinearLayout.LayoutParams(0, -2, 1));
        filter.addView(pickers);

        button("SHOW RECORDS", PURPLE, filter, v -> {
            selectedMonth = month.getValue();
            selectedYear = year.getValue();
            showRecords();
        });

        totalsBox = card("💰 SEPARATE TOTAL AMOUNTS", GREEN);
        recordList = card("📋 CUSTOMER RECORDS", BLUE);
        updateRecords();

        LinearLayout actions = card("ACTIONS", BLUE);
        button("BACK TO HOME", BLUE, actions, v -> showCRM());
        button("EXPORT CSV", ORANGE, actions, v -> exportCSV());
    }

    private void updateRecords() {
        double tSpare = 0, tNet = 0, tWarranty = 0;
        double tAmc = 0, tCollection = 0;
        int count = 0;

        for (int i = 0; i < records.length(); i++) {
            JSONObject o = records.optJSONObject(i);
            if (o == null) continue;

            String date = o.optString("date", "");
            try {
                String[] parts = date.split("/");
                int m = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                if (m != selectedMonth || y != selectedYear) continue;
            } catch (Exception e) {
                continue;
            }

            count++;
            tSpare += o.optDouble("spare", 0);
            tNet += o.optDouble("net", 0);
            tWarranty += o.optDouble("warranty", 0);
            tAmc += o.optDouble("amc", 0);
            tCollection += o.optDouble("collection", 0);

            LinearLayout item = vertical();
            item.setPadding(10, 10, 10, 10);
            item.setBackground(bg(Color.rgb(239, 246, 255), 14));

            item.addView(text("👤 " + o.optString("customer"),
                    16, BLUE, true));
            item.addView(text("📱 " + o.optString("mobile") +
                    "     Bill: " + o.optString("invoice"),
                    13, DARK, false));
            item.addView(text("Product: " + o.optString("product") +
                    "   Date: " + date, 13, PURPLE, true));
            item.addView(text("Spare: ₹" + o.optDouble("spare", 0) +
                    "   Net: ₹" + o.optDouble("net", 0), 13, DARK, false));
            item.addView(text("Warranty: ₹" + o.optDouble("warranty", 0),
                    13, DARK, false));
            item.addView(text("AMC: ₹" + o.optDouble("amc", 0) +
                    "   Collection: ₹" + o.optDouble("collection", 0),
                    14, GREEN, true));

            LinearLayout.LayoutParams p =
                    new LinearLayout.LayoutParams(-1, -2);
            p.setMargins(0, 5, 0, 8);
            recordList.addView(item, p);
        }

        totalsBox.addView(text("Total Records: " + count, 15, BLUE, true));
        totalsBox.addView(text("Spare Parts Total: ₹" + tSpare,
                14, ORANGE, true));
        totalsBox.addView(text("Net Amount Total: ₹" + tNet,
                14, BLUE, true));
        totalsBox.addView(text("Warranty Total: ₹" + tWarranty,
                14, PURPLE, true));
        totalsBox.addView(text("AMC Total: ₹" + tAmc,
                15, GREEN, true));
        totalsBox.addView(text("Collection Total: ₹" + tCollection,
                15, RED, true));

        if (count == 0) {
            recordList.addView(text("इस महीने का कोई रिकॉर्ड नहीं है।",
                    14, DARK, false));
        }
    }

    private void exportCSV() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "VIPIN_SERVICE_CRM.csv");
        startActivityForResult(intent, 101);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                java.io.OutputStream os = getContentResolver().openOutputStream(uri);
                java.io.OutputStreamWriter writer =
                        new java.io.OutputStreamWriter(os, "UTF-8");

                writer.write("Customer,Mobile,Invoice,Product,Date,Spare,Net,Warranty,AMC,Collection\n");

                for (int i = 0; i < records.length(); i++) {
                    JSONObject o = records.getJSONObject(i);
                    writer.write(csv(o.optString("customer")) + "," +
                            csv(o.optString("mobile")) + "," +
                            csv(o.optString("invoice")) + "," +
                            csv(o.optString("product")) + "," +
                            csv(o.optString("date")) + "," +
                            o.optDouble("spare", 0) + "," +
                            o.optDouble("net", 0) + "," +
                            o.optDouble("warranty", 0) + "," +
                            o.optDouble("amc", 0) + "," +
                            o.optDouble("collection", 0) + "\n");
                }

                writer.flush();
                writer.close();
                Toast.makeText(this, "Excel CSV Saved",
                        Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Export Error",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String csv(String s) {
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private void changePassword() {
        LinearLayout box = vertical();
        EditText oldPass = new EditText(this);
        oldPass.setHint("Old Password");
        oldPass.setInputType(129);
        box.addView(oldPass);

        EditText newPass = new EditText(this);
        newPass.setHint("New Password");
        newPass.setInputType(129);
        box.addView(newPass);

        new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setView(box)
                .setPositiveButton("SAVE", (d, w) -> {
                    String oldValue = oldPass.getText().toString();
                    String newValue = newPass.getText().toString();

                    if (!hash(oldValue).equals(
                            prefs.getString("password", hash("Jvipin")))) {
                        Toast.makeText(this, "Old Password गलत है",
                                Toast.LENGTH_SHORT).show();
                    } else if (newValue.trim().isEmpty()) {
                        Toast.makeText(this, "New Password भरें",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        prefs.edit().putString("password", hash(newValue)).apply();
                        Toast.makeText(this, "Password Changed",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }
}
