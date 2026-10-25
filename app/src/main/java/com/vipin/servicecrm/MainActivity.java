package com.vipin.servicecrm;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.InputType;
import android.view.Gravity;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {

    SharedPreferences prefs;
    JSONArray records;
    EditText customer, mobile, invoice, date;
    EditText spare, net, warranty;
    TextView totals;

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
        return (int)(n * getResources().getDisplayMetrics().density);
    }

    EditText input(String hint, int type) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setInputType(type);
        return e;
    }

    void heading(LinearLayout box, String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(23);
        t.setTextColor(Color.rgb(20, 75, 150));
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(15), 0, dp(15));
        box.addView(t);
    }

    Button addButton(LinearLayout box, String text) {
        Button b = new Button(this);
        b.setText(text);
        box.addView(b);
        return b;
    }

    LinearLayout layout() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(18), dp(15), dp(18), dp(20));
        l.setBackgroundColor(Color.WHITE);
        return l;
    }

    void label(LinearLayout l, String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(Color.BLACK);
        t.setTextSize(16);
        l.addView(t);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("VIPIN_CRM", MODE_PRIVATE);

        if (!prefs.contains("password")) {
            prefs.edit().putString("password", hash("Jvipin")).apply();
        }

        try {
            records = new JSONArray(prefs.getString("records", "[]"));
        } catch (Exception e) {
            records = new JSONArray();
        }

        showLogin();
    }

    void showLogin() {
        LinearLayout l = layout();
        heading(l, "VIPIN SERVICE CRM");
        label(l, "Enter Your Password");

        EditText pass = input("Password",
            InputType.TYPE_CLASS_TEXT |
            InputType.TYPE_TEXT_VARIATION_PASSWORD);
        l.addView(pass);

        Button login = addButton(l, "LOGIN");

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

        customer = input("Customer Name", InputType.TYPE_CLASS_TEXT);
        root.addView(customer);

        mobile = input("Mobile Number", InputType.TYPE_CLASS_PHONE);
        root.addView(mobile);

        invoice = input("Invoice Number", InputType.TYPE_CLASS_TEXT);
        root.addView(invoice);

        date = input("Service Date", InputType.TYPE_CLASS_DATETIME);
        root.addView(date);

        label(root, "SPARE PARTS AMOUNT");
        spare = input("Spare Parts Amount",
            InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        root.addView(spare);

        label(root, "NET AMOUNT");
        net = input("Net Amount",
            InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        root.addView(net);

        label(root, "WARRANTY AMOUNT");
        warranty = input("Warranty Amount",
            InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        root.addView(warranty);

        Button save = addButton(root, "SAVE CUSTOMER RECORD");
        save.setOnClickListener(v -> saveRecord());

        label(root, "BUSINESS TOTALS");
        totals = new TextView(this);
        totals.setTextSize(17);
        totals.setTextColor(Color.rgb(0, 100, 50));
        root.addView(totals);
        updateTotals();

        Button export = addButton(root, "EXPORT EXCEL CSV");
        export.setOnClickListener(v -> exportCSV());

        Button change = addButton(root, "CHANGE PASSWORD");
        change.setOnClickListener(v -> changePassword());

        Button logout = addButton(root, "LOGOUT");
        logout.setOnClickListener(v -> showLogin());

        Button clear = addButton(root, "CLEAR FORM");
        clear.setOnClickListener(v -> clearForm());

        setContentView(scroll);
    }

    double amount(EditText e) {
        try {
            return Double.parseDouble(e.getText().toString().trim());
        } catch (Exception ex) {
            return 0;
        }
    }

    void saveRecord() {
        if (customer.getText().toString().trim().isEmpty()) {
            customer.setError("Enter Customer Name");
            return;
        }

        try {
            JSONObject r = new JSONObject();
            r.put("customer", customer.getText().toString());
            r.put("mobile", mobile.getText().toString());
            r.put("invoice", invoice.getText().toString());
            r.put("date", date.getText().toString());
            r.put("spare", amount(spare));
            r.put("net", amount(net));
            r.put("warranty", amount(warranty));

            records.put(r);
            prefs.edit().putString("records", records.toString()).apply();

            updateTotals();
            clearForm();

            Toast.makeText(this, "Record Saved", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Save Error", Toast.LENGTH_SHORT).show();
        }
    }

    void updateTotals() {
        double s = 0, n = 0, w = 0;

        for (int i = 0; i < records.length(); i++) {
            JSONObject r = records.optJSONObject(i);
            if (r != null) {
                s += r.optDouble("spare");
                n += r.optDouble("net");
                w += r.optDouble("warranty");
            }
        }

        totals.setText(
            "\nTotal Customers: " + records.length() +
            "\nSpare Parts Total: Rs. " + s +
            "\nNet Amount Total: Rs. " + n +
            "\nWarranty Amount Total: Rs. " + w);
    }

    void clearForm() {
        customer.setText("");
        mobile.setText("");
        invoice.setText("");
        date.setText("");
        spare.setText("");
        net.setText("");
        warranty.setText("");
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
                prefs.edit().putString("password", hash(value)).apply();
                Toast.makeText(this,
                    "Password Changed Successfully",
                    Toast.LENGTH_LONG).show();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    void exportCSV() {
        if (records.length() == 0) {
            Toast.makeText(this, "No Records", Toast.LENGTH_SHORT).show();
            return;
        }

        android.content.Intent intent =
            new android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(android.content.Intent.EXTRA_TITLE, "VIPIN_SERVICE_CRM.csv");

        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
        android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            try {
                java.io.OutputStream out =
                    getContentResolver().openOutputStream(data.getData());

                StringBuilder csv = new StringBuilder();
                csv.append("Customer,Mobile,Invoice,Date,Spare Parts,Net Amount,Warranty Amount\n");

                for (int i = 0; i < records.length(); i++) {
                    JSONObject r = records.getJSONObject(i);
                    csv.append("\"").append(r.optString("customer"))
                        .append("\",\"").append(r.optString("mobile"))
                        .append("\",\"").append(r.optString("invoice"))
                        .append("\",\"").append(r.optString("date"))
                        .append("\",").append(r.optDouble("spare"))
                        .append(",").append(r.optDouble("net"))
                        .append(",").append(r.optDouble("warranty"))
                        .append("\n");
                }

                if (out != null) {
                    out.write(csv.toString().getBytes(StandardCharsets.UTF_8));
                    out.close();
                }

                Toast.makeText(this, "CSV Exported", Toast.LENGTH_LONG).show();

            } catch (Exception e) {
                Toast.makeText(this, "Export Failed", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
