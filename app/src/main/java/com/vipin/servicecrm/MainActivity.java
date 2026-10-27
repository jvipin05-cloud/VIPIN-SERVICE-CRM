package com.vipin.servicecrm;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.text.*;
import java.util.*;

public class MainActivity extends Activity {
    private static final String PREF = "vipin_service_crm";
    private static final String RECORDS = "records";
    private static final String PASSWORD = "password";
    private static final int REQ_PHOTO = 701;
    private static final int REQ_EXPORT = 702;

    private final String[] FIELDS = {
        "DATE", "BILL NUMBER", "PRODUCT NAME", "AMC", "COLLECTION",
        "CUSTOMER NAME", "MOBILE NUMBER", "ADDRESS", "SPARE PARTS",
        "AMOUNT", "SPARE AMOUNT", "WARRANTY AMOUNT", "NET AMOUNT",
        "PAYMENT MODE", "REMARKS"
    };
    private final String[] PAYMENTS = {"CASH", "UPI", "PS UPI", "RKS UPI"};
    private final int navy = Color.rgb(31, 48, 79);
    private final int gold = Color.rgb(246, 218, 153);
    private final int pale = Color.rgb(247, 249, 253);
    private final int ink = Color.rgb(39, 49, 67);

    private LinearLayout root, formLayout, recordsLayout;
    private EditText[] inputs = new EditText[15];
    private Spinner paymentSpinner;
    private JSONArray records = new JSONArray();
    private int editing = -1;
    private String[] photoUris = new String[3];
    private int photoSlot = 0;
    private EditText searchBox;
    private Spinner monthSpinner, yearSpinner;
    private TextView statusText;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(navy);
        getWindow().setNavigationBarColor(navy);
        loadRecords();
        showLogin();
    }

    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }

    private GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
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

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private void pageBase(String title) {
        root = vertical();
        root.setPadding(dp(16), dp(20), dp(16), dp(24));
        root.setBackgroundColor(pale);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(root);
        setContentView(scroll);

        TextView header = text(title, 21, navy, true);
        header.setGravity(Gravity.CENTER);
        header.setPadding(dp(12), dp(15), dp(12), dp(15));
        header.setBackground(shape(gold, 16, Color.rgb(225, 190, 112)));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(10);
        hp.bottomMargin = dp(18);
        root.addView(header, hp);
    }

    private EditText edit(String hint) {
        EditText e = new EditText(this);
        e.setSingleLine(false);
        e.setTextSize(15);
        e.setHint(hint);
        e.setTextColor(ink);
        e.setHintTextColor(Color.GRAY);
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        e.setBackground(shape(Color.WHITE, 10, Color.rgb(213, 220, 231)));
        return e;
    }

    private void addButton(LinearLayout parent, String label, int color, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(color == gold ? navy : Color.WHITE);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setBackground(shape(color, 12, 0));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(48));
        p.topMargin = dp(8);
        p.bottomMargin = dp(4);
        parent.addView(b, p);
        b.setOnClickListener(v -> action.run());
    }

    private void loadRecords() {
        String raw = getSharedPreferences(PREF, MODE_PRIVATE).getString(RECORDS, "[]");
        try { records = new JSONArray(raw); } catch (Exception e) { records = new JSONArray(); }
    }

    private void persist() {
        getSharedPreferences(PREF, MODE_PRIVATE).edit().putString(RECORDS, records.toString()).apply();
    }

    private String getPassword() {
        return getSharedPreferences(PREF, MODE_PRIVATE).getString(PASSWORD, "1234");
    }

    private void showLogin() {
        pageBase("VIPIN SERVICE CRM");
        TextView welcome = text("OFFLINE SERVICE RECORD SYSTEM", 14, navy, true);
        welcome.setGravity(Gravity.CENTER);
        root.addView(welcome);
        root.addView(text("ENTER PASSWORD", 13, ink, true));
        EditText pass = edit("Password");
        pass.setInputType(129);
        root.addView(pass);
        addButton(root, "LOGIN", navy, () -> {
            if (pass.getText().toString().equals(getPassword())) showHome();
            else Toast.makeText(this, "गलत पासवर्ड", Toast.LENGTH_SHORT).show();
        });
        addButton(root, "CHANGE PASSWORD", gold, this::changePassword);
        TextView note = text("पहली बार का पासवर्ड: 1234", 12, Color.GRAY, false);
        note.setGravity(Gravity.CENTER);
        root.addView(note);
    }

    private void changePassword() {
        LinearLayout box = vertical();
        EditText oldP = edit("CURRENT PASSWORD");
        EditText newP = edit("NEW PASSWORD");
        EditText confirm = edit("CONFIRM NEW PASSWORD");
        oldP.setInputType(129); newP.setInputType(129); confirm.setInputType(129);
        box.addView(oldP); box.addView(newP); box.addView(confirm);
        new AlertDialog.Builder(this).setTitle("CHANGE PASSWORD").setView(box)
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("SAVE", (d, w) -> {
                if (!oldP.getText().toString().equals(getPassword())) {
                    Toast.makeText(this, "Current password गलत है", Toast.LENGTH_SHORT).show();
                } else if (newP.length() < 4 || !newP.getText().toString().equals(confirm.getText().toString())) {
                    Toast.makeText(this, "नया पासवर्ड जाँचें (कम से कम 4 अक्षर)", Toast.LENGTH_SHORT).show();
                } else {
                    getSharedPreferences(PREF, MODE_PRIVATE).edit().putString(PASSWORD, newP.getText().toString()).apply();
                    Toast.makeText(this, "पासवर्ड बदल गया", Toast.LENGTH_SHORT).show();
                }
            }).show();
    }

    private void showHome() {
        editing = -1;
        pageBase("VIPIN SERVICE CRM");
        TextView intro = text("CUSTOMER SERVICE MANAGEMENT", 13, navy, true);
        intro.setGravity(Gravity.CENTER);
        root.addView(intro);
        addTotalCards();
        addButton(root, "＋  ADD NEW RECORD", navy, this::showEntry);
        addButton(root, "VIEW / SEARCH RECORDS", Color.rgb(49, 112, 154), this::showRecords);
        addButton(root, "EXPORT CSV (EXCEL)", Color.rgb(42, 133, 102), this::exportCSV);
        addButton(root, "CHANGE PASSWORD", gold, this::changePassword);
        addButton(root, "LOG OUT", Color.rgb(110, 119, 133), this::showLogin);
    }

    private void addTotalCards() {
        TextView label = text("LAST MONTH TOTAL", 15, navy, true);
        label.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(15); lp.bottomMargin = dp(8);
        root.addView(label, lp);

        LinearLayout row1 = horizontal();
        LinearLayout row2 = horizontal();
        String[] names = {"AMOUNT", "SPARE AMOUNT", "WARRANTY AMOUNT", "NET AMOUNT"};
        double[] totals = lastMonthTotals();
        for (int i = 0; i < 4; i++) {
            LinearLayout card = vertical();
            card.setGravity(Gravity.CENTER);
            card.setPadding(dp(5), dp(7), dp(5), dp(7));
            card.setBackground(shape(i % 2 == 0 ? Color.rgb(226, 239, 255) : Color.rgb(255, 239, 213), 12, Color.rgb(222, 225, 232)));
            TextView n = text(names[i], 10, navy, true);
            n.setGravity(Gravity.CENTER);
            TextView v = text(String.format(Locale.US, "₹%.2f", totals[i]), 14, ink, true);
            v.setGravity(Gravity.CENTER);
            card.addView(n); card.addView(v);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dp(72), 1);
            cp.setMargins(dp(4), dp(4), dp(4), dp(4));
            (i < 2 ? row1 : row2).addView(card, cp);
        }
        root.addView(row1);
        root.addView(row2);
    }

    private double[] lastMonthTotals() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, -1);
        int m = c.get(Calendar.MONTH), y = c.get(Calendar.YEAR);
        String month = String.format(Locale.US, "%04d-%02d", y, m + 1);
        double[] t = new double[4];
        String[] keys = {"AMOUNT", "SPARE AMOUNT", "WARRANTY AMOUNT", "NET AMOUNT"};
        for (int i = 0; i < records.length(); i++) {
            JSONObject r = records.optJSONObject(i);
            if (r == null || !r.optString("DATE").startsWith(month)) continue;
            for (int j = 0; j < keys.length; j++) t[j] += number(r.optString(keys[j]));
        }
        return t;
    }

    private double number(String s) {
        try { return Double.parseDouble(s.replace(",", "").replace("₹", "").trim()); }
        catch (Exception e) { return 0; }
    }

    private void showEntry() {
        if (editing < 0) photoUris = new String[3];
        pageBase(editing >= 0 ? "EDIT SERVICE RECORD" : "NEW SERVICE RECORD");
        formLayout = root;
        for (int i = 0; i < FIELDS.length; i++) {
            TextView label = text(FIELDS[i], 12, navy, true);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
            p.topMargin = dp(7);
            root.addView(label, p);
            if (FIELDS[i].equals("PAYMENT MODE")) {
                paymentSpinner = new Spinner(this);
                ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, PAYMENTS);
                paymentSpinner.setAdapter(a);
                root.addView(paymentSpinner);
            } else {
                inputs[i] = edit(FIELDS[i]);
                if (i == 0) {
                    inputs[i].setFocusable(false);
                    final EditText dateInput = inputs[i];
dateInput.setOnClickListener(v -> pickDate(dateInput));
                }
                if (i == 9 || i == 10 || i == 11 || i == 12 || i == 3 || i == 4)
                    inputs[i].setInputType(8194 | 4096);
                if (i == 6) inputs[i].setInputType(3);
                root.addView(inputs[i]);
            }
        }
        if (editing >= 0) fillEditRecord();
        addButton(root, "ATTACH PHOTO (UP TO 3)", Color.rgb(86, 105, 145), this::choosePhoto);
        statusText = text(photoStatus(), 11, Color.GRAY, false);
        root.addView(statusText);
        addButton(root, "SAVE RECORD", Color.rgb(42, 133, 102), this::saveRecord);
        addButton(root, "CANCEL / BACK", gold, this::showHome);
    }

    private void pickDate(EditText target) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) ->
            target.setText(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)),
            c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void fillEditRecord() {
        JSONObject r = records.optJSONObject(editing);
        if (r == null) return;
        for (int i = 0; i < FIELDS.length; i++) {
            if (FIELDS[i].equals("PAYMENT MODE")) {
                String value = r.optString(FIELDS[i], "CASH");
                for (int j = 0; j < PAYMENTS.length; j++) if (PAYMENTS[j].equals(value)) paymentSpinner.setSelection(j);
            } else inputs[i].setText(r.optString(FIELDS[i]));
        }
        for (int i = 0; i < 3; i++) photoUris[i] = r.optString("PHOTO" + (i + 1), "");
    }

    private void choosePhoto() {
        int slot = -1;
        for (int i = 0; i < 3; i++) if (photoUris[i] == null || photoUris[i].isEmpty()) { slot = i; break; }
        if (slot < 0) {
            new AlertDialog.Builder(this).setTitle("PHOTO SLOT")
                .setItems(new String[]{"PHOTO 1", "PHOTO 2", "PHOTO 3"}, (d, which) -> openPhoto(which)).show();
        } else openPhoto(slot);
    }

    private void openPhoto(int slot) {
        photoSlot = slot;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQ_PHOTO);
    }

    private String photoStatus() {
        int count = 0;
        for (String s : photoUris) if (s != null && !s.isEmpty()) count++;
        return "ATTACHED PHOTOS: " + count + " / 3";
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == REQ_PHOTO) {
            try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) {}
            photoUris[photoSlot] = uri.toString();
            if (statusText != null) statusText.setText(photoStatus());
        } else if (requestCode == REQ_EXPORT) {
            writeCSV(uri);
        }
    }

    private void saveRecord() {
        JSONObject r = new JSONObject();
        try {
            for (int i = 0; i < FIELDS.length; i++) {
                if (FIELDS[i].equals("PAYMENT MODE")) r.put(FIELDS[i], paymentSpinner.getSelectedItem().toString());
                else r.put(FIELDS[i], inputs[i].getText().toString().trim());
            }
            for (int i = 0; i < 3; i++) r.put("PHOTO" + (i + 1), photoUris[i] == null ? "" : photoUris[i]);
            if (r.optString("DATE").isEmpty()) {
                Toast.makeText(this, "DATE भरें", Toast.LENGTH_SHORT).show(); return;
            }
            if (editing >= 0) records.put(editing, r); else records.put(r);
            persist();
            Toast.makeText(this, "RECORD SAVED", Toast.LENGTH_SHORT).show();
            showHome();
        } catch (Exception e) {
            Toast.makeText(this, "Save error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showRecords() {
        pageBase("SERVICE RECORDS");
        searchBox = edit("SEARCH: NAME / MOBILE / BILL / PRODUCT");
        root.addView(searchBox);
        LinearLayout filters = horizontal();
        monthSpinner = new Spinner(this);
        yearSpinner = new Spinner(this);
        String[] months = {"ALL MONTHS","01","02","03","04","05","06","07","08","09","10","11","12"};
        ArrayAdapter<String> ma = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, months);
        monthSpinner.setAdapter(ma);
        ArrayList<String> years = new ArrayList<>(); years.add("ALL YEARS");
        int now = Calendar.getInstance().get(Calendar.YEAR);
        for (int y = now + 1; y >= now - 10; y--) years.add(String.valueOf(y));
        yearSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, years));
        filters.addView(monthSpinner, new LinearLayout.LayoutParams(0, dp(48), 1));
        filters.addView(yearSpinner, new LinearLayout.LayoutParams(0, dp(48), 1));
        root.addView(filters);
        recordsLayout = vertical();
        root.addView(recordsLayout);
        searchBox.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int start,int before,int count){ renderRecords(); }
            public void afterTextChanged(android.text.Editable e){}
        });
        monthSpinner.setOnItemSelectedListener(new SimpleSelect(() -> renderRecords()));
        yearSpinner.setOnItemSelectedListener(new SimpleSelect(() -> renderRecords()));
        addButton(root, "BACK TO HOME", gold, this::showHome);
        renderRecords();
    }

    private class SimpleSelect implements AdapterView.OnItemSelectedListener {
        Runnable run;
        SimpleSelect(Runnable r) { run = r; }
        public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { run.run(); }
        public void onNothingSelected(AdapterView<?> p) {}
    }

    private void renderRecords() {
        if (recordsLayout == null) return;
        recordsLayout.removeAllViews();
        String q = searchBox == null ? "" : searchBox.getText().toString().toLowerCase(Locale.ROOT);
        String month = monthSpinner == null ? "ALL MONTHS" : monthSpinner.getSelectedItem().toString();
        String year = yearSpinner == null ? "ALL YEARS" : yearSpinner.getSelectedItem().toString();
        int shown = 0;
        for (int i = records.length() - 1; i >= 0; i--) {
            JSONObject r = records.optJSONObject(i);
            if (r == null) continue;
            String date = r.optString("DATE");
            if (!"ALL MONTHS".equals(month) && !date.matches("\\d{4}-" + month + "-.*")) continue;
            if (!"ALL YEARS".equals(year) && !date.startsWith(year + "-")) continue;
            String hay = (r.optString("CUSTOMER NAME") + " " + r.optString("MOBILE NUMBER") + " " +
                r.optString("BILL NUMBER") + " " + r.optString("PRODUCT NAME")).toLowerCase(Locale.ROOT);
            if (!hay.contains(q)) continue;
            shown++;
            final int index = i;
            LinearLayout card = vertical();
            card.setPadding(dp(12), dp(10), dp(12), dp(10));
            card.setBackground(shape(Color.WHITE, 12, Color.rgb(218, 224, 233)));
            TextView title = text(r.optString("CUSTOMER NAME") + "  |  " + r.optString("DATE"), 15, navy, true);
            card.addView(title);
            card.addView(text("MOBILE: " + r.optString("MOBILE NUMBER") + "   BILL: " + r.optString("BILL NUMBER"), 12, ink, false));
            card.addView(text("PRODUCT: " + r.optString("PRODUCT NAME") + "   NET AMOUNT: ₹" + r.optString("NET AMOUNT"), 12, ink, true));
            LinearLayout actions = horizontal();
            Button edit = smallButton("EDIT");
            Button del = smallButton("DELETE");
            Button photos = smallButton("PHOTOS");
            actions.addView(edit, new LinearLayout.LayoutParams(0, dp(42), 1));
            actions.addView(del, new LinearLayout.LayoutParams(0, dp(42), 1));
            actions.addView(photos, new LinearLayout.LayoutParams(0, dp(42), 1));
            card.addView(actions);
            edit.setOnClickListener(v -> { editing = index; photoUris = new String[3]; showEntry(); });
            del.setOnClickListener(v -> new AlertDialog.Builder(this).setMessage("यह रिकॉर्ड DELETE करें?")
                .setNegativeButton("CANCEL", null).setPositiveButton("DELETE", (d,w) -> {
                    JSONArray next = new JSONArray();
                    for (int k=0;k<records.length();k++) if (k != index) next.put(records.optJSONObject(k));
                    records = next; persist(); renderRecords();
                }).show());
            photos.setOnClickListener(v -> showPhotos(r));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
            cp.topMargin = dp(8);
            recordsLayout.addView(card, cp);
        }
        if (shown == 0) recordsLayout.addView(text("कोई रिकॉर्ड नहीं मिला", 14, Color.GRAY, false));
    }

    private Button smallButton(String s) {
        Button b = new Button(this); b.setText(s); b.setTextSize(11); b.setAllCaps(false);
        b.setPadding(0,0,0,0); b.setBackground(shape(gold, 9, 0)); b.setTextColor(navy); return b;
    }

    private void showPhotos(JSONObject r) {
        StringBuilder b = new StringBuilder();
        for (int i = 1; i <= 3; i++) {
            String uri = r.optString("PHOTO" + i);
            if (!uri.isEmpty()) b.append("PHOTO ").append(i).append(":\n").append(uri).append("\n\n");
        }
        if (b.length() == 0) b.append("इस रिकॉर्ड में फोटो नहीं है।");
        new AlertDialog.Builder(this).setTitle("ATTACHED PHOTOS").setMessage(b.toString()).setPositiveButton("OK", null).show();
    }

    private void exportCSV() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "VIPIN_SERVICE_CRM.csv");
        startActivityForResult(intent, REQ_EXPORT);
    }

    private void writeCSV(Uri uri) {
        try {
            OutputStream out = getContentResolver().openOutputStream(uri);
            OutputStreamWriter w = new OutputStreamWriter(out, "UTF-8");
            w.write("\uFEFF");
            for (int i = 0; i < FIELDS.length; i++) {
                if (i > 0) w.write(",");
                w.write(csv(FIELDS[i]));
            }
            w.write("\n");
            for (int i = 0; i < records.length(); i++) {
                JSONObject r = records.optJSONObject(i);
                for (int j = 0; j < FIELDS.length; j++) {
                    if (j > 0) w.write(",");
                    w.write(csv(r == null ? "" : r.optString(FIELDS[j])));
                }
                w.write("\n");
            }
            w.flush(); w.close();
            Toast.makeText(this, "CSV FILE SAVED", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String csv(String s) {
        if (s == null) s = "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    @Override public void onBackPressed() {
        showHome();
    }
}
