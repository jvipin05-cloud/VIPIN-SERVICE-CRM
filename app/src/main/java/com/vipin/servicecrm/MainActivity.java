package com.vipin.servicecrm;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.view.*;
import android.view.animation.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private static final String PREF = "vipin_service_crm";
    private static final String RECORDS = "records";
    private static final String PASSWORD = "password";
    private static final String[] FIELDS = {
        "DATE", "BILL NUMBER", "PRODUCT NAME", "AMC", "COLLECTION",
        "CUSTOMER NAME", "MOBILE NUMBER", "ADDRESS", "SPARE PARTS",
        "AMOUNT", "SPARE AMOUNT", "WARRANTY AMOUNT", "NET AMOUNT",
        "PAYMENT MODE", "REMARKS"
    };
    private static final String[] PAYMENTS = {"CASH", "UPI", "PS UPI", "RKS UPI"};
    private final int[] COLORS = {0xFF142B55, 0xFF2563EB, 0xFF0F9D8A, 0xFFF4F7FC};
    private SharedPreferences prefs;
    private JSONArray records = new JSONArray();
    private LinearLayout root, content;
    private EditText[] inputs = new EditText[FIELDS.length];
    private Spinner paymentSpinner;
    private String photoUri = "";
    private int editing = -1;
    private String currentFilter = "";

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences(PREF, MODE_PRIVATE);
        loadRecords();
        if (prefs.getString(PASSWORD, "").isEmpty()) {
            prefs.edit().putString(PASSWORD, hash("1234")).apply();
        }
        showLogin();
    }

    private String hash(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes("UTF-8"));
            StringBuilder out = new StringBuilder();
            for (byte x : d) out.append(String.format(Locale.US, "%02x", x));
            return out.toString();
        } catch (Exception e) { return s; }
    }

    private void showLogin() {
        LinearLayout page = basePage();
        page.setGravity(Gravity.CENTER);
        LinearLayout panel = card();
        panel.setPadding(24, 28, 24, 28);
        TextView title = label("VIPIN SERVICE CRM", 25, 0xFFFFFFFF, true);
        title.setGravity(Gravity.CENTER);
        panel.addView(title);
        TextView sub = label("SECURE LOGIN", 13, 0xFFDCE7FF, true);
        sub.setGravity(Gravity.CENTER);
        panel.addView(sub);
        EditText pass = new EditText(this);
        pass.setSingleLine(true);
        pass.setHint("ENTER PASSWORD");
        pass.setHintTextColor(0xFFB9C7DD);
        pass.setTextColor(Color.WHITE);
        pass.setInputType(129);
        panel.addView(pass, params());
        Button login = button("LOGIN");
        panel.addView(login, params());
        TextView hint = label("FIRST LOGIN PASSWORD: 1234", 12, 0xFFDCE7FF, false);
        hint.setGravity(Gravity.CENTER);
        panel.addView(hint);
        login.setOnClickListener(v -> {
            String saved = prefs.getString(PASSWORD, hash("1234"));
            if (hash(pass.getText().toString()).equals(saved)) showHome();
            else toast("WRONG PASSWORD");
        });
        page.addView(panel, new LinearLayout.LayoutParams(-1, -2));
        setContentView(page);
    }

    private void showHome() {
        root = basePage();
        LinearLayout header = card();
        header.setOrientation(LinearLayout.VERTICAL);
        TextView title = label("VIPIN SERVICE CRM", 23, Color.WHITE, true);
        header.addView(title);
        TextView subtitle = label("SERVICE • COLLECTION • RECORDS", 12, 0xFFDCE7FF, false);
        header.addView(subtitle);
        root.addView(header, params());
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button add = button("+ NEW RECORD");
        Button change = button("PASSWORD");
        Button export = button("EXPORT CSV");
        actions.addView(add, new LinearLayout.LayoutParams(0, dp(48), 1));
        actions.addView(change, new LinearLayout.LayoutParams(0, dp(48), 1));
        actions.addView(export, new LinearLayout.LayoutParams(0, dp(48), 1));
        root.addView(actions, params());
        add.setOnClickListener(v -> showForm(-1));
        change.setOnClickListener(v -> changePassword());
        export.setOnClickListener(v -> exportCsv());

        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("SEARCH BILL / CUSTOMER / MOBILE / PRODUCT");
        search.setTextColor(0xFF17233B);
        search.setBackground(round(0xFFFFFFFF, 14));
        search.setPadding(dp(14), 0, dp(14), 0);
        root.addView(search, new LinearLayout.LayoutParams(-1, dp(50)));
        LinearLayout totals = new LinearLayout(this);
        totals.setOrientation(LinearLayout.VERTICAL);
        root.addView(totals, params());
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content, params());
        search.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int before,int count) {
                currentFilter = s.toString().toLowerCase(Locale.ROOT);
                renderRecords(totals, content);
            }
            public void afterTextChanged(android.text.Editable e){}
        });
        renderRecords(totals, content);
        ScrollView scroll = new ScrollView(this);
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        // Rebuild page inside a scroll container.
        root.removeAllViews();
        wrapper.addView(header, params());
        wrapper.addView(actions, params());
        wrapper.addView(search, params());
        wrapper.addView(totals, params());
        wrapper.addView(content, params());
        scroll.addView(wrapper);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(COLORS[3]);
        setContentView(scroll);
        wrapper.setPadding(dp(16), dp(18), dp(16), dp(28));
        wrapper.setAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
    }

    private void renderRecords(LinearLayout totals, LinearLayout list) {
        totals.removeAllViews();
        list.removeAllViews();
        String month = new SimpleDateFormat("yyyy-MM", Locale.US).format(new Date());
        String lastMonth = new SimpleDateFormat("yyyy-MM", Locale.US).format(
            new Date(System.currentTimeMillis() - 28L * 24 * 60 * 60 * 1000));
        double[] sums = new double[4];
        int shown = 0;
        for (int i = records.length() - 1; i >= 0; i--) {
            JSONObject r = records.optJSONObject(i);
            if (r == null) continue;
            String date = r.optString("DATE", "");
            String hay = "";
            for (String f : FIELDS) hay += " " + r.optString(f, "");
            if (!currentFilter.isEmpty() && !hay.toLowerCase(Locale.ROOT).contains(currentFilter)) continue;
            shown++;
            if (date.startsWith(lastMonth)) {
                sums[0] += number(r.optString("AMOUNT"));
                sums[1] += number(r.optString("SPARE AMOUNT"));
                sums[2] += number(r.optString("WARRANTY AMOUNT"));
                sums[3] += number(r.optString("NET AMOUNT"));
            }
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(dp(14), dp(12), dp(14), dp(12));
            item.setBackground(round(Color.WHITE, 16));
            TextView name = label(r.optString("CUSTOMER NAME", "NO NAME"), 17, COLORS[0], true);
            item.addView(name);
            item.addView(label("BILL: " + r.optString("BILL NUMBER") + "   DATE: " + date, 12, 0xFF64748B, false));
            item.addView(label("PRODUCT: " + r.optString("PRODUCT NAME"), 13, 0xFF334155, false));
            item.addView(label("AMOUNT: " + r.optString("AMOUNT") +
                "   SPARE: " + r.optString("SPARE AMOUNT") +
                "   WARRANTY: " + r.optString("WARRANTY AMOUNT") +
                "   NET: " + r.optString("NET AMOUNT"), 12, 0xFF0F766E, true));
            LinearLayout row = new LinearLayout(this);
            Button edit = smallButton("EDIT");
            Button pdf = smallButton("PDF");
            Button del = smallButton("DELETE");
            row.addView(edit, new LinearLayout.LayoutParams(0, dp(40), 1));
            row.addView(pdf, new LinearLayout.LayoutParams(0, dp(40), 1));
            row.addView(del, new LinearLayout.LayoutParams(0, dp(40), 1));
            item.addView(row);
            final int index = i;
            edit.setOnClickListener(v -> showForm(index));
            pdf.setOnClickListener(v -> createPdf(index));
            del.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("DELETE RECORD?")
                .setMessage("THIS RECORD WILL BE DELETED.")
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("DELETE", (d,w) -> {
                    JSONArray next = new JSONArray();
                    for (int j=0;j<records.length();j++) if (j != index) next.put(records.optJSONObject(j));
                    records = next; saveRecords(); showHome();
                }).show());
            LinearLayout.LayoutParams ip = params();
            ip.bottomMargin = dp(10);
            list.addView(item, ip);
        }
        LinearLayout summary = card();
        summary.setOrientation(LinearLayout.VERTICAL);
        summary.addView(label("LAST MONTH TOTAL", 17, Color.WHITE, true));
        String[] names = {"AMOUNT", "SPARE AMOUNT", "WARRANTY AMOUNT", "NET AMOUNT"};
        for (int i=0;i<4;i++) summary.addView(label(names[i] + " : " + money(sums[i]), 14, Color.WHITE, false));
        totals.addView(summary, params());
        if (shown == 0) {
            TextView empty = label("NO RECORD FOUND", 15, 0xFF64748B, true);
            empty.setGravity(Gravity.CENTER);
            list.addView(empty, params());
        }
    }

    private void showForm(int index) {
        editing = index;
        photoUri = "";
        JSONObject old = index >= 0 ? records.optJSONObject(index) : null;
        LinearLayout page = basePage();
        TextView heading = label(index >= 0 ? "EDIT RECORD" : "NEW SERVICE RECORD", 22, COLORS[0], true);
        page.addView(heading);
        ScrollView scroll = new ScrollView(this);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(0, dp(8), 0, dp(20));
        for (int i=0;i<FIELDS.length;i++) {
            TextView l = label(FIELDS[i], 12, 0xFF475569, true);
            form.addView(l);
            if (FIELDS[i].equals("PAYMENT MODE")) {
                paymentSpinner = new Spinner(this);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, PAYMENTS);
                paymentSpinner.setAdapter(adapter);
                String value = old == null ? "CASH" : old.optString(FIELDS[i], "CASH");
                for (int j=0;j<PAYMENTS.length;j++) if (PAYMENTS[j].equals(value)) paymentSpinner.setSelection(j);
                form.addView(paymentSpinner, params());
            } else {
                EditText e = new EditText(this);
                inputs[i] = e;
                e.setSingleLine(false);
                e.setMinLines(FIELDS[i].equals("ADDRESS") || FIELDS[i].equals("REMARKS") ? 2 : 1);
                e.setTextColor(0xFF17233B);
                e.setHint("ENTER " + FIELDS[i]);
                e.setBackground(round(Color.WHITE, 12));
                e.setPadding(dp(12), dp(8), dp(12), dp(8));
                if (FIELDS[i].equals("DATE")) {
                    e.setFocusable(false);
                    e.setOnClickListener(v -> pickDate(e));
                    e.setText(old == null ? today() : old.optString(FIELDS[i], today()));
                } else {
                    e.setText(old == null ? "" : old.optString(FIELDS[i], ""));
                    if (FIELDS[i].contains("AMOUNT") || FIELDS[i].equals("AMC") || FIELDS[i].equals("COLLECTION"))
                        e.setInputType(8194);
                    if (FIELDS[i].equals("MOBILE NUMBER")) e.setInputType(3);
                }
                form.addView(e, params());
            }
        }
        Button photo = button("ATTACH PHOTO");
        Button save = button(index >= 0 ? "UPDATE RECORD" : "SAVE RECORD");
        Button back = smallButton("BACK");
        form.addView(photo, params());
        form.addView(save, params());
        form.addView(back, params());
        photo.setOnClickListener(v -> {
            Intent in = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            in.setType("image/*"); in.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(in, 101);
        });
        save.setOnClickListener(v -> saveForm());
        back.setOnClickListener(v -> showHome());
        scroll.addView(form);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(page);
    }

    private void saveForm() {
        JSONObject obj = new JSONObject();
        try {
            for (int i=0;i<FIELDS.length;i++) {
                String value = FIELDS[i].equals("PAYMENT MODE") ?
                    paymentSpinner.getSelectedItem().toString() :
                    inputs[i].getText().toString().trim();
                if (FIELDS[i].equals("DATE") && value.isEmpty()) value = today();
                obj.put(FIELDS[i], value);
            }
            obj.put("PHOTO_URI", photoUri);
            JSONArray next = new JSONArray();
            for (int i=0;i<records.length();i++) if (i != editing) next.put(records.optJSONObject(i));
            next.put(obj);
            records = next;
            saveRecords();
            toast("RECORD SAVED");
            showHome();
        } catch (Exception e) { toast("SAVE ERROR: " + e.getMessage()); }
    }

    private void pickDate(EditText target) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (v,y,m,d) ->
            target.setText(String.format(Locale.US, "%04d-%02d-%02d", y, m+1, d)),
            c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            if (requestCode == 101) {
                try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) {}
                photoUri = uri.toString();
                toast("PHOTO ATTACHED");
            } else if (requestCode == 102 && pendingPdfIndex >= 0) {
                writePdf(uri, pendingPdfIndex);
                pendingPdfIndex = -1;
            } else if (requestCode == 103) {
                writeCsv(uri);
            }
        }
    }

    private void createPdf(int index) {
        JSONObject r = records.optJSONObject(index);
        if (r == null) return;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, "VIPIN_CRM_" + r.optString("BILL NUMBER", "RECORD") + ".pdf");
        pendingPdfIndex = index;
        startActivityForResult(intent, 102);
    }
    private int pendingPdfIndex = -1;

    private void writePdf(Uri uri, int index) {
        try {
            JSONObject r = records.optJSONObject(index);
            android.graphics.pdf.PdfDocument doc = new android.graphics.pdf.PdfDocument();
            android.graphics.pdf.PdfDocument.PageInfo info =
                new android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create();
            android.graphics.pdf.PdfDocument.Page page = doc.startPage(info);
            Canvas canvas = page.getCanvas();
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(COLORS[0]); p.setTextSize(22); p.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            canvas.drawText("VIPIN SERVICE CRM", 40, 55, p);
            p.setTextSize(11); p.setColor(Color.DKGRAY);
            canvas.drawText("SERVICE RECORD", 40, 78, p);
            int y = 115;
            for (String f : FIELDS) {
                p.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
                p.setColor(0xFF334155);
                canvas.drawText(f, 42, y, p);
                p.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
                canvas.drawText(r.optString(f, ""), 210, y, p);
                y += 35;
                if (y > 790) break;
            }
            doc.finishPage(page);
            OutputStream out = getContentResolver().openOutputStream(uri);
            doc.writeTo(out); out.close(); doc.close();
            toast("PDF SAVED");
        } catch (Exception e) { toast("PDF ERROR: " + e.getMessage()); }
    }

    private void changePassword() {
        LinearLayout box = new LinearLayout(this);
        box.setPadding(dp(16), dp(8), dp(16), dp(8));
        box.setOrientation(LinearLayout.VERTICAL);
        EditText old = new EditText(this); old.setHint("OLD PASSWORD"); old.setInputType(129);
        EditText n = new EditText(this); n.setHint("NEW PASSWORD"); n.setInputType(129);
        EditText confirm = new EditText(this); confirm.setHint("CONFIRM NEW PASSWORD"); confirm.setInputType(129);
        box.addView(old); box.addView(n); box.addView(confirm);
        new AlertDialog.Builder(this).setTitle("CHANGE PASSWORD").setView(box)
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("SAVE", (d,w) -> {
                if (!hash(old.getText().toString()).equals(prefs.getString(PASSWORD, hash("1234"))))
                    toast("OLD PASSWORD WRONG");
                else if (n.getText().length() < 4) toast("PASSWORD MINIMUM 4 CHARACTERS");
                else if (!n.getText().toString().equals(confirm.getText().toString())) toast("PASSWORD NOT MATCH");
                else { prefs.edit().putString(PASSWORD, hash(n.getText().toString())).apply(); toast("PASSWORD CHANGED"); }
            }).show();
    }

    private void exportCsv() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "VIPIN_SERVICE_CRM.csv");
        startActivityForResult(intent, 103);
    }
    private void writeCsv(Uri uri) {
        try {
            OutputStream out = getContentResolver().openOutputStream(uri);
            OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");
            for (int i=0;i<FIELDS.length;i++) {
                if (i>0) writer.write(",");
                writer.write("\"" + FIELDS[i].replace("\"", "\"\"") + "\"");
            }
            writer.write("\n");
            for (int j=0;j<records.length();j++) {
                JSONObject r = records.optJSONObject(j);
                for (int i=0;i<FIELDS.length;i++) {
                    if (i>0) writer.write(",");
                    String value = r == null ? "" : r.optString(FIELDS[i], "");
                    writer.write("\"" + value.replace("\"", "\"\"").replace("\n", " ") + "\"");
                }
                writer.write("\n");
            }
            writer.flush(); writer.close(); toast("CSV EXPORTED");
        } catch (Exception e) { toast("EXPORT ERROR: " + e.getMessage()); }
    }

    private void loadRecords() {
        try { records = new JSONArray(prefs.getString(RECORDS, "[]")); }
        catch (Exception e) { records = new JSONArray(); }
    }
    private void saveRecords() { prefs.edit().putString(RECORDS, records.toString()).apply(); }
    private double number(String s) { try { return Double.parseDouble(s.replace(",", "").trim()); } catch (Exception e) { return 0; } }
    private String money(double n) { return String.format(Locale.US, "₹%.2f", n); }
    private String today() { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()); }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
    private LinearLayout basePage() {
        LinearLayout p = new LinearLayout(this);
        p.setOrientation(LinearLayout.VERTICAL);
        p.setPadding(dp(16), dp(18), dp(16), dp(18));
        p.setBackgroundColor(COLORS[3]);
        return p;
    }
    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(18), dp(16), dp(18), dp(16));
        c.setBackground(round(COLORS[0], 20));
        return c;
    }
    private TextView label(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setPadding(0, dp(4), 0, dp(4));
        return t;
    }
    private GradientDrawable round(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private LinearLayout.LayoutParams params() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(8); return p;
    }
    private Button button(String text) {
        Button b = new Button(this); b.setText(text); b.setTextColor(Color.WHITE);
        b.setAllCaps(false); b.setBackground(round(0xFF2563EB, 14)); return b;
    }
    private Button smallButton(String text) {
        Button b = new Button(this); b.setText(text); b.setTextSize(11);
        b.setTextColor(Color.WHITE); b.setAllCaps(false);
        b.setBackground(round(0xFF0F766E, 12)); return b;
    }
}
