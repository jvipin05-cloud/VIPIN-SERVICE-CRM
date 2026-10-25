
package com.vipin.servicecrm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {

    SharedPreferences prefs;
    JSONArray records;

    EditText customer, mobile, invoice, product, date;
    EditText spare, net, warranty, amc, collection;

    TextView totals, recordList;
    int filterMonth = -1;
    int filterYear = -1;

    final int BLUE = Color.rgb(25, 75, 150);
    final int WHITE = Color.WHITE;

    String hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(
                ("VIPIN_CRM_SALT_" + password)
                .getBytes(StandardCharsets.UTF_8));

            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                result.append(String.format("%02x", b & 0xff));
            }
            return result.toString();
        } catch (Exception e) {
            return "";
        }
    }

    int dp(int n) {
        return (int)(n * getResources()
            .getDisplayMetrics().density);
    }

    LinearLayout layout() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(16), dp(15), dp(16), dp(25));
        l.setBackgroundColor(Color.rgb(239, 245, 255));
        return l;
    }

    void heading(LinearLayout box, String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(24);
        t.setTextColor(WHITE);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(8), dp(18), dp(8), dp(18));
        t.setBackgroundColor(BLUE);
        box.addView(t);
    }

    void label(LinearLayout box, String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(15);
        t.setTextColor(Color.rgb(30, 60, 100));
        t.setPadding(0, dp(10), 0, dp(3));
        box.addView(t);
    }

    EditText input(String hint, int type) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setTextSize(16);
        e.setTextColor(Color.BLACK);
        e.setHintTextColor(Color.GRAY);
        e.setInputType(type);
        e.setPadding(dp(12), dp(8), dp(12), dp(8));
        e.setBackgroundTintList(
            ColorStateList.valueOf(Color.rgb(40, 110, 190)));
        return e;
    }

    Button addButton(LinearLayout box, String text, int color) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(WHITE);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setBackgroundTintList(ColorStateList.valueOf(color));

        LinearLayout.LayoutParams p =
            new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, dp(5), 0, dp(5));
        box.addView(b, p);
        return b;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("VIPIN_CRM", MODE_PRIVATE);

        if (!prefs.contains("password")) {
            prefs.edit().putString("password",
                hash("Jvipin")).apply();
        }

        try {
            records = new JSONArray(
                prefs.getString("records", "[]"));
        } catch (Exception e) {
            records = new JSONArray();
        }

        showLogin();
    }

    void showLogin() {
        LinearLayout l = layout();
        heading(l, "VIPIN SERVICE CRM");

        label(l, "ENTER YOUR PASSWORD");

        EditText pass = input("Password",
            InputType.TYPE_CLASS_TEXT |
            InputType.TYPE_TEXT_VARIATION_PASSWORD);
        l.addView(pass);

        Button login = addButton(l, "LOGIN",
            Color.rgb(0, 145, 90));

        login.setOnClickListener(v -> {
            if (hash(pass.getText().toString()).equals(
                prefs.getString("password", ""))) {
                showCRM();
            } else {
                pass.setError("Wrong Password");
                Toast.makeText(this, "Incorrect Password",
                    Toast.LENGTH_SHORT).show();
            }
        });

        setContentView(l);
    }

    void showCRM() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = layout();
        scroll.addView(root);

        heading(root, "VIPIN SERVICE CRM");
        label(root, "CUSTOMER ENTRY");

        customer = input("Customer Name",
            InputType.TYPE_CLASS_TEXT);
        root.addView(customer);

        mobile = input("Mobile Number",
            InputType.TYPE_CLASS_PHONE);
        root.addView(mobile);

        invoice = input("Bill Number / Invoice Number",
            InputType.TYPE_CLASS_TEXT);
        root.addView(invoice);

        product = input("Product Name",
            InputType.TYPE_CLASS_TEXT);
        root.addView(product);

        label(root, "SERVICE DATE");
        date = input("Select Date",
            InputType.TYPE_CLASS_TEXT);
        date.setFocusable(false);
        date.setClickable(true);
        date.setOnClickListener(v -> openDatePicker());
        root.addView(date);

        label(root, "OLD AMOUNT DETAILS");

        label(root, "SPARE PARTS AMOUNT");
        spare = input("Spare Parts Amount",
            InputType.TYPE_CLASS_NUMBER |
            InputType.TYPE_NUMBER_FLAG_DECIMAL);
        root.addView(spare);

        label(root, "NET AMOUNT");
        net = input("Net Amount",
            InputType.TYPE_CLASS_NUMBER |
            InputType.TYPE_NUMBER_FLAG_DECIMAL);
        root.addView(net);

        label(root, "WARRANTY AMOUNT");
        warranty = input("Warranty Amount",
            InputType.TYPE_CLASS_NUMBER |
            InputType.TYPE_NUMBER_FLAG_DECIMAL);
        root.addView(warranty);

        label(root, "NEW AMOUNT DETAILS");

        label(root, "AMC AMOUNT");
        amc = input("AMC Amount",
            InputType.TYPE_CLASS_NUMBER |
            InputType.TYPE_NUMBER_FLAG_DECIMAL);
        root.addView(amc);

        label(root, "COLLECTION AMOUNT");
        collection = input("Collection Amount",
            InputType.TYPE_CLASS_NUMBER |
            InputType.TYPE_NUMBER_FLAG_DECIMAL);
        root.addView(collection);

        Button save = addButton(root, "SAVE CUSTOMER RECORD",
            Color.rgb(0, 145, 80));
        save.setOnClickListener(v -> saveRecord());

        label(root, "BUSINESS TOTALS");

        totals = new TextView(this);
        totals.setTextSize(16);
        totals.setTextColor(Color.rgb(0, 90, 65));
        totals.setPadding(dp(12), dp(12), dp(12), dp(12));
        totals.setBackgroundColor(Color.rgb(220, 255, 235));
        root.addView(totals);

        Button allRecords = addButton(root, "SHOW ALL RECORDS",
            Color.rgb(30, 100, 180));
        allRecords.setOnClickListener(v -> {
            filterMonth = -1;
            filterYear = -1;
            updateTotals();
            showRecords();
        });

        Button monthRecords = addButton(root, "MONTH / YEAR RECORDS",
            Color.rgb(125, 70, 180));
        monthRecords.setOnClickListener(v -> chooseMonthYear());

        recordList = new TextView(this);
        recordList.setTextSize(14);
        recordList.setTextColor(Color.BLACK);
        recordList.setPadding(dp(10), dp(10), dp(10), dp(10));
        recordList.setBackgroundColor(WHITE);
        root.addView(recordList);

        updateTotals();
        showRecords();

        Button export = addButton(root, "EXPORT EXCEL CSV",
            Color.rgb(0, 130, 100));
        export.setOnClickListener(v -> exportCSV());

        Button change = addButton(root, "CHANGE PASSWORD",
            Color.rgb(230, 140, 0));
        change.setOnClickListener(v -> changePassword());

        Button logout = addButton(root, "LOGOUT",
            Color.rgb(190, 50, 50));
        logout.setOnClickListener(v -> showLogin());

        Button clear = addButton(root, "CLEAR FORM",
            Color.rgb(90, 100, 115));
        clear.setOnClickListener(v -> clearForm());

        setContentView(scroll);
    }

    void openDatePicker() {
        Calendar c = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
            this,
            (view, year, month, day) -> {
                date.setText(String.format(Locale.getDefault(),
                    "%02d/%02d/%04d", day, month + 1, year));
            },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    double amount(EditText e) {
        try {
            return Double.parseDouble(
                e.getText().toString().trim());
        } catch (Exception ex) {
            return 0;
        }
    }

    void saveRecord() {
        if (customer.getText().toString().trim().isEmpty()) {
            customer.setError("Enter Customer Name");
            return;
        }

        if (date.getText().toString().trim().isEmpty()) {
            date.setError("Select Date");
            return;
        }

        try {
            JSONObject r = new JSONObject();

            r.put("customer", customer.getText().toString());
            r.put("mobile", mobile.getText().toString());
            r.put("invoice", invoice.getText().toString());
            r.put("product", product.getText().toString());
            r.put("date", date.getText().toString());

            r.put("spare", amount(spare));
            r.put("net", amount(net));
            r.put("warranty", amount(warranty));
            r.put("amc", amount(amc));
            r.put("collection", amount(collection));

            records.put(r);

            prefs.edit().putString("records",
                records.toString()).apply();

            updateTotals();
            showRecords();
            clearForm();

            Toast.makeText(this, "Record Saved Successfully",
                Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Save Error",
                Toast.LENGTH_SHORT).show();
        }
    }

    boolean matchesFilter(JSONObject r) {
        if (filterMonth == -1 || filterYear == -1) {
            return true;
        }

        try {
            String d = r.optString("date");
            SimpleDateFormat sdf =
                new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Calendar c = Calendar.getInstance();
            c.setTime(sdf.parse(d));

            return c.get(Calendar.MONTH) == filterMonth &&
                c.get(Calendar.YEAR) == filterYear;

        } catch (Exception e) {
            return false;
        }
    }

    void chooseMonthYear() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setPadding(dp(15), dp(10), dp(15), dp(10));

        NumberPicker monthPicker = new NumberPicker(this);
        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(11);
        monthPicker.setDisplayedValues(new String[]{
            "January", "February", "March", "April",
            "May", "June", "July", "August",
            "September", "October", "November", "December"
        });

        NumberPicker yearPicker = new NumberPicker(this);
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        yearPicker.setMinValue(2000);
        yearPicker.setMaxValue(currentYear + 10);

        monthPicker.setValue(Calendar.getInstance()
            .get(Calendar.MONTH));
        yearPicker.setValue(currentYear);

        box.addView(monthPicker,
            new LinearLayout.LayoutParams(0, dp(150), 1));
        box.addView(yearPicker,
            new LinearLayout.LayoutParams(0, dp(150), 1));

        new AlertDialog.Builder(this)
            .setTitle("Select Month and Year")
            .setView(box)
            .setPositiveButton("SHOW", (d, w) -> {
                filterMonth = monthPicker.getValue();
                filterYear = yearPicker.getValue();
                updateTotals();
                showRecords();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    void updateTotals() {
        double s = 0, n = 0, w = 0, a = 0, c = 0;
        int count = 0;

        for (int i = 0; i < records.length(); i++) {
            JSONObject r = records.optJSONObject(i);
            if (r != null && matchesFilter(r)) {
                count++;
                s += r.optDouble("spare");
                n += r.optDouble("net");
                w += r.optDouble("warranty");
                a += r.optDouble("amc");
                c += r.optDouble("collection");
            }
        }

        String period = filterMonth == -1
            ? "ALL RECORDS"
            : new String[]{
                "January", "February", "March", "April",
                "May", "June", "July", "August",
                "September", "October", "November", "December"
            }[filterMonth] + " " + filterYear;

        totals.setText(
            period +
            "\nTotal Customers: " + count +
            "\nSpare Parts Total: Rs. " + s +
            "\nNet Amount Total: Rs. " + n +
            "\nWarranty Amount Total: Rs. " + w +
            "\nAMC Total: Rs. " + a +
            "\nCollection Total: Rs. " + c
        );
    }

    void showRecords() {
        if (recordList == null) return;

        StringBuilder text = new StringBuilder();
        text.append("CUSTOMER RECORDS\n\n");

        int count = 0;

        for (int i = records.length() - 1; i >= 0; i--) {
            JSONObject r = records.optJSONObject(i);

            if (r != null && matchesFilter(r)) {
                count++;

                text.append("Record ").append(count).append("\n");
                text.append("Customer: ")
                    .append(r.optString("customer")).append("\n");
                text.append("Mobile: ")
                    .append(r.optString("mobile")).append("\n");
                text.append("Bill Number: ")
                    .append(r.optString("invoice")).append("\n");
                text.append("Product: ")
                    .append(r.optString("product")).append("\n");
                text.append("Date: ")
                    .append(r.optString("date")).append("\n");
                text.append("Spare Parts: Rs. ")
                    .append(r.optDouble("spare")).append("\n");
                text.append("Net Amount: Rs. ")
                    .append(r.optDouble("net")).append("\n");
                text.append("Warranty: Rs. ")
                    .append(r.optDouble("warranty")).append("\n");
                text.append("AMC: Rs. ")
                    .append(r.optDouble("amc")).append("\n");
                text.append("Collection: Rs. ")
                    .append(r.optDouble("collection")).append("\n");
                text.append("--------------------------\n");
            }
        }

        if (count == 0) {
            text.append("No Records Found");
        }

        recordList.setText(text.toString());
    }

    void clearForm() {
        customer.setText("");
        mobile.setText("");
        invoice.setText("");
        product.setText("");
        date.setText("");
        spare.setText("");
        net.setText("");
        warranty.setText("");
        amc.setText("");
        collection.setText("");
    }

    void changePassword() {
        EditText p = input("Enter New Password",
            InputType.TYPE_CLASS_TEXT |
            InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(p)
            .setPositiveButton("SAVE", (d, w) -> {
                String value = p.getText().toString();

                if (value.length() < 4) {
                    Toast.makeText(this,
                        "Minimum 4 characters required",
                        Toast.LENGTH_LONG).show();
                    return;
                }

                prefs.edit().putString("password",
                    hash(value)).apply();

                Toast.makeText(this,
                    "Password Changed Successfully",
                    Toast.LENGTH_LONG).show();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    String csvValue(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    void exportCSV() {
        if (records.length() == 0) {
            Toast.makeText(this, "No Records",
                Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE,
            "VIPIN_SERVICE_CRM.csv");

        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode,
        int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 &&
            resultCode == RESULT_OK && data != null) {

            try {
                java.io.OutputStream out =
                    getContentResolver().openOutputStream(
                        data.getData());

                StringBuilder csv = new StringBuilder();

                csv.append("Customer,Mobile,Bill Number,Product,Date,")
                    .append("Spare Parts,Net Amount,Warranty Amount,")
                    .append("AMC,Collection\n");

                for (int i = 0; i < records.length(); i++) {
                    JSONObject r = records.getJSONObject(i);

                    csv.append(csvValue(r.optString("customer"))).append(",");
                    csv.append(csvValue(r.optString("mobile"))).append(",");
                    csv.append(csvValue(r.optString("invoice"))).append(",");
                    csv.append(csvValue(r.optString("product"))).append(",");
                    csv.append(csvValue(r.optString("date"))).append(",");
                    csv.append(r.optDouble("spare")).append(",");
                    csv.append(r.optDouble("net")).append(",");
                    csv.append(r.optDouble("warranty")).append(",");
                    csv.append(r.optDouble("amc")).append(",");
                    csv.append(r.optDouble("collection")).append("\n");
                }

                if (out != null) {
                    out.write(("\uFEFF" + csv.toString())
                        .getBytes(StandardCharsets.UTF_8));
                    out.close();
                }

                Toast.makeText(this, "CSV Exported Successfully",
                    Toast.LENGTH_LONG).show();

            } catch (Exception e) {
                Toast.makeText(this, "Export Failed",
                    Toast.LENGTH_SHORT).show();
            }
        }
    }
}
