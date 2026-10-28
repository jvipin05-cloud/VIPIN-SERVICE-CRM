package com.vipin.servicecrm;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import android.text.*;
import org.json.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {

    private static final String PREF = "vipin_service_crm";
    private static final String RECORDS = "records";
    private static final String PASSWORD = "password";
    private static final int REQ_PHOTO = 701, REQ_EXPORT = 702, REQ_PDF = 703;

    private final String[] FIELDS = {
        "DATE", "BILL NUMBER", "PRODUCT NAME", "AMC", "COLLECTION",
        "CUSTOMER NAME", "MOBILE NUMBER", "ADDRESS", "SPARE PARTS",
        "AMOUNT", "SPARE AMOUNT", "WARRANTY AMOUNT", "NET AMOUNT",
        "PAYMENT MODE", "REMARKS"
    };
    private final String[] PAYMENTS = {"CASH", "UPI", "PS UPI", "RKS UPI"};
    private final String[] MONEY_FIELDS = {
        "AMC", "COLLECTION", "AMOUNT", "SPARE AMOUNT",
        "WARRANTY AMOUNT", "NET AMOUNT"
    };

    private final int navy = Color.rgb(31, 48, 79);
    private final int pale = Color.rgb(235, 244, 255);
    private final int ink = Color.rgb(39, 49, 67);
    private final int gold = Color.rgb(246, 218, 153);

    private LinearLayout root, recordsLayout;
    private ScrollView pageScroll;
    private EditText[] inputs = new EditText[15];
    private Spinner paymentSpinner, monthSpinner, yearSpinner;
    private EditText searchBox;
    private TextView statusText;
    private JSONArray records = new JSONArray();
    private int editing = -1, photoSlot = 0;
    private String[] photoUris = new String[3];
    private String pdfUri = "";
    private String currentPage = "LOGIN";
    private boolean entryFromRecords = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(30, 55, 125));
        getWindow().setNavigationBarColor(Color.rgb(45, 58, 86));
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        loadRecords();
        showLogin();
    }

    private int dp(float n) {
        return (int)(n * getResources().getDisplayMetrics().density + .5f);
    }

    private GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }

    private GradientDrawable gradient(int start, int end, int radius) {
        GradientDrawable d = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT, new int[]{start, end});
        d.setCornerRadius(dp(radius));
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

    private void addLogo(LinearLayout parent, int sizeDp) {
        try {
            ImageView logo = new ImageView(this);
            logo.setImageResource(R.drawable.vipin_service_crm_icon_512);
            logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp));
            p.gravity = Gravity.CENTER_HORIZONTAL;
            p.bottomMargin = dp(10);
            parent.addView(logo, p);
            animateIn(logo);
        } catch (Exception ignored) { }
    }

    // PREMIUM ENTRANCE AND BUTTON ANIMATIONS
    private void animateIn(View view) {
        if (view == null) return;
        view.setAlpha(0f);
        view.setTranslationY(dp(16));
        view.setScaleX(0.97f);
        view.setScaleY(0.97f);
        view.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
            .setDuration(420)
            .setInterpolator(new android.view.animation.DecelerateInterpolator())
            .start();
    }

    private void animatePress(View view) {
        if (view == null) return;
        view.animate().cancel();
        view.setScaleX(0.97f);
        view.setScaleY(0.97f);
        view.animate().scaleX(1f).scaleY(1f).setDuration(180)
            .setInterpolator(new android.view.animation.OvershootInterpolator())
            .start();
    }

    private void pageBase(String title) {
        root = vertical();
        root.setPadding(dp(16), dp(20), dp(16), dp(35));
        root.setBackgroundColor(pale);
        pageScroll = new ScrollView(this);
        pageScroll.setFillViewport(true);
        pageScroll.setClipToPadding(false);
        pageScroll.setVerticalScrollBarEnabled(true);
        pageScroll.setPadding(0, dp(6), 0, dp(24));
        pageScroll.addView(root);
        setContentView(pageScroll);

        TextView header = text(title, 21, navy, true);
        header.setGravity(Gravity.CENTER);
        header.setPadding(dp(12), dp(15), dp(12), dp(15));
        header.setBackground(shape(gold, 18, Color.rgb(225, 190, 112)));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(10);
        hp.bottomMargin = dp(18);
        root.addView(header, hp);
        animateIn(header);
    }

    private EditText edit(String hint) {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setTextSize(15);
        e.setHint(hint);
        e.setTextColor(ink);
        e.setHintTextColor(Color.GRAY);
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        e.setBackground(shape(Color.WHITE, 10, Color.rgb(213, 220, 231)));
        e.setSelectAllOnFocus(false);
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
        animateIn(b);
        b.setOnClickListener(v -> { animatePress(b); action.run(); });
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // STORAGE
    private void loadRecords() {
        String raw = getSharedPreferences(PREF, MODE_PRIVATE).getString(RECORDS, "[]");
        try { records = new JSONArray(raw); }
        catch (Exception e) { records = new JSONArray(); }
    }

    private void persist() {
        getSharedPreferences(PREF, MODE_PRIVATE).edit()
            .putString(RECORDS, records.toString()).apply();
    }

    private String getPassword() {
        return getSharedPreferences(PREF, MODE_PRIVATE).getString(PASSWORD, "1234");
    }

    // LOGIN
    private void showLogin() {
        currentPage = "LOGIN";
        pageBase("VIPIN SERVICE CRM");
        addLogo(root, 150);
        TextView welcome = text("OFFLINE SERVICE RECORD MANAGER", 14, navy, true);
        welcome.setGravity(Gravity.CENTER);
        root.addView(welcome);
        root.addView(text("ENTER PASSWORD", 13, ink, true));
        EditText pass = edit("Password");
        pass.setInputType(129);
        root.addView(pass);
        addButton(root, "LOGIN", navy, () -> {
            if (pass.getText().toString().equals(getPassword())) showHome();
            else toast("गलत पासवर्ड");
        });
        addButton(root, "CHANGE PASSWORD", gold, this::changePassword);
        TextView note = text("पहली बार का पासवर्ड: 1234", 12, Color.GRAY, false);
        note.setGravity(Gravity.CENTER);
        root.addView(note);
    }

    private void changePassword() {
        LinearLayout box = vertical();
        box.setPadding(dp(8), dp(8), dp(8), dp(8));
        EditText oldP = edit("CURRENT PASSWORD");
        EditText newP = edit("NEW PASSWORD");
        EditText confirm = edit("CONFIRM NEW PASSWORD");
        oldP.setInputType(129); newP.setInputType(129); confirm.setInputType(129);
        box.addView(oldP); box.addView(newP); box.addView(confirm);
        new AlertDialog.Builder(this).setTitle("CHANGE PASSWORD").setView(box)
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("SAVE", (d, w) -> {
                if (!oldP.getText().toString().equals(getPassword())) toast("Current password गलत है");
                else if (newP.length() < 4 || !newP.getText().toString().equals(confirm.getText().toString()))
                    toast("नया पासवर्ड जाँचें");
                else {
                    getSharedPreferences(PREF, MODE_PRIVATE).edit()
                        .putString(PASSWORD, newP.getText().toString()).apply();
                    toast("पासवर्ड बदल गया");
                }
            }).show();
    }

    // HOME
    private void homeButton(String label, String icon, int c1, int c2, Runnable action) {
        LinearLayout button = horizontal();
        button.setPadding(dp(16), dp(12), dp(16), dp(12));
        button.setBackground(gradient(c1, c2, 18));
        TextView symbol = text(icon, 25, c1, true);
        symbol.setGravity(Gravity.CENTER);
        symbol.setBackground(shape(Color.WHITE, 50, 0));
        button.addView(symbol, new LinearLayout.LayoutParams(dp(52), dp(52)));
        TextView title = text(label, 16, Color.WHITE, true);
        title.setPadding(dp(15), 0, 0, 0);
        button.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        button.addView(text("›", 32, Color.WHITE, true));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(80));
        p.topMargin = dp(8); p.bottomMargin = dp(8);
        root.addView(button, p);
        animateIn(button);
        button.setOnClickListener(v -> { animatePress(button); action.run(); });
    }

    private void showHome() {
        currentPage = "HOME";
        editing = -1;
        entryFromRecords = false;
        root = vertical();
        root.setPadding(dp(16), dp(20), dp(16), dp(24));
        root.setBackgroundColor(pale);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(root);
        setContentView(scroll);

        LinearLayout banner = vertical();
        banner.setPadding(dp(18), dp(22), dp(18), dp(22));
        banner.setBackground(gradient(Color.rgb(38, 48, 180), Color.rgb(0, 105, 255), 22));
        addLogo(banner, 125);
        TextView heading = text("VIPIN SERVICE CRM", 23, Color.WHITE, true);
        heading.setGravity(Gravity.CENTER); banner.addView(heading);
        TextView sub = text("OFFLINE SERVICE RECORD MANAGER", 13, Color.WHITE, true);
        sub.setGravity(Gravity.CENTER); sub.setPadding(0, dp(10), 0, dp(10));
        banner.addView(sub);
        TextView count = text("TOTAL RECORDS  •  " + records.length(), 16, Color.WHITE, true);
        count.setGravity(Gravity.CENTER); banner.addView(count);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.bottomMargin = dp(12); root.addView(banner, bp);

        homeButton("ADD NEW RECORD", "＋", Color.rgb(0,112,255), Color.rgb(0,145,255), this::showEntry);
        homeButton("SEARCH / MONTH / YEAR RECORDS", "⌕", Color.rgb(0,170,180), Color.rgb(0,180,155), this::showRecords);
        homeButton("MONTHLY / YEARLY REPORTS", "▦", Color.rgb(103,35,235), Color.rgb(153,48,255), this::showReports);
        homeButton("EXPORT EXCEL / CSV", "▣", Color.rgb(255,105,0), Color.rgb(255,175,0), this::exportCSV);
        homeButton("CHANGE PASSWORD", "⚙", Color.rgb(220,0,130), Color.rgb(255,35,105), this::changePassword);
        homeButton("LOG OUT", "↪", Color.rgb(90,100,120), Color.rgb(115,125,145), this::showLogin);
        addDashboardTotals();
    }

    // TOTAL CALCULATION - one common function for dashboard, records and reports
    private double[] calculateTotals(String month, String year) {
        double[] totals = new double[MONEY_FIELDS.length];
        for (int i = 0; i < records.length(); i++) {
            JSONObject r = records.optJSONObject(i);
            if (r == null) continue;
            String date = normalizeDate(r.optString("DATE"));
            if (date.length() < 7) continue;
            if (month != null && !"ALL".equals(month) && !date.substring(5,7).equals(month)) continue;
            if (year != null && !"ALL".equals(year) && !date.startsWith(year + "-")) continue;
            for (int j = 0; j < MONEY_FIELDS.length; j++)
                totals[j] += number(r.optString(MONEY_FIELDS[j]));
        }
        return totals;
    }

    private String normalizeDate(String value) {
        if (value == null) return "";
        String s = value.trim();
        if (s.matches("\\d{4}-\\d{2}-\\d{2}.*")) return s;
        if (s.matches("\\d{2}-\\d{2}-\\d{4}")) {
            String[] p = s.split("-");
            return p[2] + "-" + p[1] + "-" + p[0];
        }
        if (s.matches("\\d{2}/\\d{2}/\\d{4}")) {
            String[] p = s.split("/");
            return p[2] + "-" + p[1] + "-" + p[0];
        }
        return "";
    }

    private double number(String s) {
        try {
            if (s == null) return 0;
            String cleaned = s.replace(",", "").replace("₹", "").trim();
            if (cleaned.isEmpty()) return 0;
            return Double.parseDouble(cleaned);
        } catch (Exception e) { return 0; }
    }

    private String money(double value) {
        return String.format(Locale.US, "₹ %.2f", value);
    }

    private void addDashboardTotals() {
        TextView title = text("CURRENT TOTAL", 20, navy, true);
        title.setPadding(0, dp(18), 0, dp(5));
        root.addView(title);
        addTotalGrid(calculateTotals("ALL", "ALL"));

        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, -1);
        String m = String.format(Locale.US, "%02d", c.get(Calendar.MONTH) + 1);
        String y = String.valueOf(c.get(Calendar.YEAR));
        TextView last = text("LAST MONTH TOTAL", 20, navy, true);
        last.setPadding(0, dp(18), 0, dp(5));
        root.addView(last);
        addTotalGrid(calculateTotals(m, y));
    }

    private void addTotalGrid(double[] totals) {
        for (int row = 0; row < 3; row++) {
            LinearLayout line = horizontal();
            for (int col = 0; col < 2; col++) {
                int i = row * 2 + col;
                LinearLayout card = vertical();
                card.setGravity(Gravity.CENTER);
                card.setPadding(dp(6), dp(10), dp(6), dp(10));
                int c1 = new int[]{Color.rgb(0,125,255),Color.rgb(0,170,145),Color.rgb(105,35,230),
                    Color.rgb(255,105,0),Color.rgb(210,65,100),Color.rgb(35,105,160)}[i];
                int c2 = new int[]{Color.rgb(0,180,255),Color.rgb(0,195,160),Color.rgb(150,55,255),
                    Color.rgb(255,175,0),Color.rgb(245,100,130),Color.rgb(70,155,210)}[i];
                card.setBackground(gradient(c1, c2, 15));
                TextView name = text(MONEY_FIELDS[i], 11, Color.WHITE, true);
                name.setGravity(Gravity.CENTER);
                TextView value = text(money(totals[i]), 16, Color.WHITE, true);
                value.setGravity(Gravity.CENTER);
                card.addView(name); card.addView(value);
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dp(88), 1);
                cp.setMargins(dp(4), dp(4), dp(4), dp(4));
                line.addView(card, cp);
            }
            root.addView(line);
        }
    }

    // ADD / EDIT RECORD
    private void showEntry() {
        currentPage = "ENTRY";
        if (editing < 0) { photoUris = new String[3]; pdfUri = ""; }
        inputs = new EditText[15];
        pageBase(editing >= 0 ? "EDIT SERVICE RECORD" : "NEW SERVICE RECORD");

        for (int i = 0; i < FIELDS.length; i++) {
            TextView label = text(FIELDS[i], 12, navy, true);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
            p.topMargin = dp(7); root.addView(label, p);

            if ("PAYMENT MODE".equals(FIELDS[i])) {
                paymentSpinner = new Spinner(this);
                ArrayAdapter<String> a = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item, PAYMENTS);
                paymentSpinner.setAdapter(a);
                root.addView(paymentSpinner);
            } else {
                inputs[i] = edit(FIELDS[i]);
                if (i == 0) {
                    inputs[i].setFocusable(false);
                    inputs[i].setHint("SELECT DATE");
                    inputs[i].setOnClickListener(v -> pickDate(inputs[0]));
                }
                if (i == 3 || i == 4 || i == 9 || i == 10 || i == 11 || i == 12)
                    inputs[i].setInputType(8194 | 4096);
                if (i == 6) inputs[i].setInputType(3);
                if (i == 7 || i == 8 || i == 14) {
                    inputs[i].setSingleLine(false);
                    inputs[i].setMinLines(1); inputs[i].setMaxLines(3);
                }
                if (i != 0) inputs[i].setImeOptions(i == 14 ?
                    EditorInfo.IME_ACTION_DONE : EditorInfo.IME_ACTION_NEXT);
                root.addView(inputs[i]);
                animateIn(inputs[i]);
                setupKeyboard(inputs[i], i);
            }
        }
        if (editing >= 0) fillEditRecord();

        addButton(root, "ATTACH PHOTO (UP TO 3)", Color.rgb(86,105,145), this::choosePhoto);
        addButton(root, "ATTACH PDF", Color.rgb(105,75,175), this::choosePDF);
        statusText = text(attachmentStatus(), 12, Color.GRAY, false);
        root.addView(statusText);
        addButton(root, "SAVE RECORD", Color.rgb(42,133,102), this::saveRecord);
        addButton(root, "CANCEL / BACK", gold, this::goBack);
    }

    private void setupKeyboard(EditText field, int index) {
        field.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) scrollToInput(field);
        });
        field.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_NEXT || action == EditorInfo.IME_ACTION_DONE) {
                int next = index + 1;
                while (next < FIELDS.length && (next == 13 || next == 0)) next++;
                if (next < FIELDS.length && inputs[next] != null) {
                    inputs[next].requestFocus();
                    scrollToInput(inputs[next]);
                } else {
                    InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(field.getWindowToken(), 0);
                }
                return true;
            }
            return false;
        });
    }

    // KEYBOARD AUTO-SCROLL FIX: keep the field label visible above the input.
    private void scrollToInput(EditText field) {
        if (pageScroll == null || field == null) return;

        pageScroll.postDelayed(() -> {
            if (pageScroll == null || field.getWindowToken() == null) return;

            try {
                int[] fieldLocation = new int[2];
                int[] scrollLocation = new int[2];

                field.getLocationOnScreen(fieldLocation);
                pageScroll.getLocationOnScreen(scrollLocation);

                // Leave enough room above the input for its field label.
                int desiredTop = dp(105);
                int delta = fieldLocation[1] - scrollLocation[1] - desiredTop;

                if (delta > 0) {
                    pageScroll.smoothScrollBy(0, delta);
                } else if (delta < -dp(20)) {
                    pageScroll.smoothScrollBy(0, delta);
                }
            } catch (Exception ignored) {
                // Keep the form usable even if the keyboard changes window size.
            }
        }, 300);
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
            if ("PAYMENT MODE".equals(FIELDS[i])) {
                String value = r.optString(FIELDS[i], "CASH");
                for (int j = 0; j < PAYMENTS.length; j++)
                    if (PAYMENTS[j].equals(value)) paymentSpinner.setSelection(j);
            } else if (inputs[i] != null) inputs[i].setText(r.optString(FIELDS[i]));
        }
        for (int i = 0; i < 3; i++) photoUris[i] = r.optString("PHOTO" + (i + 1), "");
        pdfUri = r.optString("PDF", "");
    }

    // PHOTO / PDF ATTACHMENTS
    private void choosePhoto() {
        int slot = -1;
        for (int i = 0; i < 3; i++) {
            if (photoUris[i] == null || photoUris[i].isEmpty()) { slot = i; break; }
        }
        if (slot < 0) {
            new AlertDialog.Builder(this).setTitle("SELECT PHOTO SLOT")
                .setItems(new String[]{"PHOTO 1", "PHOTO 2", "PHOTO 3"},
                    (d, which) -> openPhoto(which)).show();
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

    private void choosePDF() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQ_PDF);
    }

    private String attachmentStatus() {
        int count = 0;
        for (String s : photoUris) if (s != null && !s.isEmpty()) count++;
        return "PHOTOS: " + count + " / 3     PDF: " +
            ((pdfUri == null || pdfUri.isEmpty()) ? "NOT ATTACHED" : "ATTACHED");
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == REQ_EXPORT) { writeCSV(uri); return; }
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) { }
        if (requestCode == REQ_PHOTO) photoUris[photoSlot] = uri.toString();
        else if (requestCode == REQ_PDF) pdfUri = uri.toString();
        if (statusText != null) statusText.setText(attachmentStatus());
    }

    private void saveRecord() {
        JSONObject r = new JSONObject();
        try {
            for (int i = 0; i < FIELDS.length; i++) {
                if ("PAYMENT MODE".equals(FIELDS[i]))
                    r.put(FIELDS[i], paymentSpinner.getSelectedItem().toString());
                else r.put(FIELDS[i], inputs[i].getText().toString().trim());
            }
            for (int i = 0; i < 3; i++) r.put("PHOTO" + (i + 1), photoUris[i] == null ? "" : photoUris[i]);
            r.put("PDF", pdfUri == null ? "" : pdfUri);

            String date = normalizeDate(r.optString("DATE"));
            if (date.isEmpty()) { toast("DATE सही तरीके से चुनें"); return; }
            r.put("DATE", date);

            if (editing >= 0) records.put(editing, r);
            else records.put(r);
            persist();
            editing = -1; entryFromRecords = false;
            toast("RECORD SAVED SUCCESSFULLY");
            showHome();
        } catch (Exception e) {
            Toast.makeText(this, "Save error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // RECORDS / FILTERS
    private ArrayList<String> getYears() {
        TreeSet<String> found = new TreeSet<>(Collections.reverseOrder());
        for (int i = 0; i < records.length(); i++) {
            JSONObject r = records.optJSONObject(i);
            if (r == null) continue;
            String d = normalizeDate(r.optString("DATE"));
            if (d.length() >= 4) found.add(d.substring(0,4));
        }
        int now = Calendar.getInstance().get(Calendar.YEAR);
        found.add(String.valueOf(now));
        found.add(String.valueOf(now - 1));
        ArrayList<String> list = new ArrayList<>();
        list.add("ALL YEARS"); list.addAll(found);
        return list;
    }

    private void setupMonthYearFilters(LinearLayout parent) {
        LinearLayout filters = horizontal();
        monthSpinner = new Spinner(this);
        yearSpinner = new Spinner(this);
        String[] months = {"ALL MONTHS","01","02","03","04","05","06","07","08","09","10","11","12"};
        monthSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, months));
        yearSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, getYears()));
        filters.addView(monthSpinner, new LinearLayout.LayoutParams(0, dp(48), 1));
        filters.addView(yearSpinner, new LinearLayout.LayoutParams(0, dp(48), 1));
        parent.addView(filters);
    }

    private void showRecords() {
        currentPage = "RECORDS";
        pageBase("SERVICE RECORDS");
        searchBox = edit("SEARCH: NAME / MOBILE / BILL / PRODUCT");
        root.addView(searchBox);
        setupMonthYearFilters(root);
        recordsLayout = vertical();
        root.addView(recordsLayout);

        searchBox.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { renderRecords(); }
            public void afterTextChanged(Editable e) {}
        });
        monthSpinner.setOnItemSelectedListener(new SimpleSelect(this::renderRecords));
        yearSpinner.setOnItemSelectedListener(new SimpleSelect(this::renderRecords));
        addButton(root, "BACK TO HOME", gold, this::showHome);
        renderRecords();
    }

    private class SimpleSelect implements AdapterView.OnItemSelectedListener {
        Runnable run;
        SimpleSelect(Runnable r) { run = r; }
        public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { run.run(); }
        public void onNothingSelected(AdapterView<?> p) {}
    }

    private boolean matchesDate(String date, String month, String year) {
        String d = normalizeDate(date);
        if (d.length() < 7) return false;
        if (!"ALL MONTHS".equals(month) && !d.substring(5,7).equals(month)) return false;
        if (!"ALL YEARS".equals(year) && !d.startsWith(year + "-")) return false;
        return true;
    }

    private void renderRecords() {
        if (recordsLayout == null) return;
        recordsLayout.removeAllViews();
        String q = searchBox == null ? "" : searchBox.getText().toString().toLowerCase(Locale.ROOT).trim();
        String month = monthSpinner == null ? "ALL MONTHS" : monthSpinner.getSelectedItem().toString();
        String year = yearSpinner == null ? "ALL YEARS" : yearSpinner.getSelectedItem().toString();
        double[] totals = new double[MONEY_FIELDS.length];
        int shown = 0;

        for (int i = records.length() - 1; i >= 0; i--) {
            JSONObject r = records.optJSONObject(i);
            if (r == null || !matchesDate(r.optString("DATE"), month, year)) continue;
            StringBuilder all = new StringBuilder();
            for (String field : FIELDS) all.append(r.optString(field)).append(' ');
            if (!all.toString().toLowerCase(Locale.ROOT).contains(q)) continue;

            shown++;
            for (int j = 0; j < MONEY_FIELDS.length; j++)
                totals[j] += number(r.optString(MONEY_FIELDS[j]));

            final int index = i;
            LinearLayout card = vertical();
            card.setPadding(dp(12), dp(12), dp(12), dp(12));
            card.setBackground(shape(Color.WHITE, 14, Color.rgb(218,224,233)));
            TextView title = text(r.optString("CUSTOMER NAME") + " | " + r.optString("DATE"), 15, navy, true);
            card.addView(title);
            for (String field : FIELDS) {
                if ("DATE".equals(field) || "CUSTOMER NAME".equals(field)) continue;
                TextView line = text(field + ": " + r.optString(field), 12, ink, true);
                line.setPadding(0, dp(2), 0, dp(2));
                card.addView(line);
            }
            LinearLayout row1 = horizontal();
            Button copy = smallButton("COPY TEXT"), edit = smallButton("EDIT"), del = smallButton("DELETE");
            row1.addView(copy, new LinearLayout.LayoutParams(0, dp(43), 1));
            row1.addView(edit, new LinearLayout.LayoutParams(0, dp(43), 1));
            row1.addView(del, new LinearLayout.LayoutParams(0, dp(43), 1));
            card.addView(row1);
            LinearLayout row2 = horizontal();
            Button photos = smallButton("VIEW PHOTOS"), pdf = smallButton("OPEN PDF");
            row2.addView(photos, new LinearLayout.LayoutParams(0, dp(43), 1));
            row2.addView(pdf, new LinearLayout.LayoutParams(0, dp(43), 1));
            card.addView(row2);

            copy.setOnClickListener(v -> copyRecord(r));
            edit.setOnClickListener(v -> {
                editing = index; entryFromRecords = true;
                photoUris = new String[3]; pdfUri = "";
                showEntry();
            });
            del.setOnClickListener(v -> confirmDelete(index));
            photos.setOnClickListener(v -> showPhotos(r));
            pdf.setOnClickListener(v -> openSavedPDF(r));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
            cp.topMargin = dp(10); recordsLayout.addView(card, cp);
        }

        TextView count = text("MATCHING RECORDS: " + shown, 14, navy, true);
        count.setPadding(0, dp(10), 0, dp(5));
        recordsLayout.addView(count);
        TextView totalTitle = text("FILTERED TOTALS", 18, navy, true);
        totalTitle.setPadding(0, dp(8), 0, dp(4));
        recordsLayout.addView(totalTitle);
        addTotalsText(recordsLayout, totals);

        if (shown == 0) recordsLayout.addView(text("कोई रिकॉर्ड नहीं मिला", 14, Color.GRAY, false));
    }

    private void addTotalsText(LinearLayout parent, double[] totals) {
        for (int i = 0; i < MONEY_FIELDS.length; i++) {
            TextView t = text(MONEY_FIELDS[i] + " : " + money(totals[i]), 14, ink, true);
            t.setPadding(dp(10), dp(6), dp(10), dp(6));
            t.setBackground(shape(Color.WHITE, 8, Color.rgb(220,225,235)));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
            p.topMargin = dp(3); parent.addView(t, p);
        }
    }

    private void copyRecord(JSONObject r) {
        StringBuilder b = new StringBuilder("VIPIN SERVICE CRM\n----------------------\n");
        for (String field : FIELDS) b.append(field).append(": ").append(r.optString(field)).append("\n");
        android.content.ClipboardManager clipboard =
            (android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("SERVICE RECORD", b.toString()));
            toast("पूरा रिकॉर्ड COPY हो गया");
        }
    }

    private Button smallButton(String s) {
        Button b = new Button(this);
        b.setText(s); b.setTextSize(10); b.setAllCaps(false);
        b.setPadding(0,0,0,0); b.setBackground(shape(gold,9,0)); b.setTextColor(navy);
        return b;
    }

    private void confirmDelete(int index) {
        new AlertDialog.Builder(this).setMessage("यह रिकॉर्ड DELETE करें?")
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("DELETE", (d,w) -> {
                JSONArray next = new JSONArray();
                for (int k = 0; k < records.length(); k++)
                    if (k != index) next.put(records.opt(k));
                records = next; persist(); renderRecords(); toast("RECORD DELETED");
            }).show();
    }

    // MONTHLY / YEARLY REPORTS
    private void showReports() {
        currentPage = "REPORTS";
        pageBase("MONTHLY / YEARLY REPORTS");
        TextView hint = text("MONTH AND YEAR SELECT करें", 13, navy, true);
        root.addView(hint);
        setupMonthYearFilters(root);
        LinearLayout reportArea = vertical();
        root.addView(reportArea);

        Runnable update = () -> {
            reportArea.removeAllViews();
            String m = monthSpinner.getSelectedItem().toString();
            String y = yearSpinner.getSelectedItem().toString();
            String month = "ALL MONTHS".equals(m) ? "ALL" : m;
            String year = "ALL YEARS".equals(y) ? "ALL" : y;
            double[] sums = calculateTotals(month, year);
            String period = ("ALL YEARS".equals(y) ? "ALL YEARS" : y) +
                " / " + ("ALL MONTHS".equals(m) ? "ALL MONTHS" : m);
            TextView selected = text("REPORT PERIOD: " + period, 14, navy, true);
            selected.setPadding(0, dp(12), 0, dp(8));
            reportArea.addView(selected);
            addTotalsText(reportArea, sums);
            int count = 0;
            for (int i = 0; i < records.length(); i++) {
                JSONObject r = records.optJSONObject(i);
                if (r != null && matchesDate(r.optString("DATE"), m, y)) count++;
            }
            TextView recordsCount = text("TOTAL RECORDS: " + count, 15, navy, true);
            recordsCount.setPadding(0, dp(12), 0, dp(8));
            reportArea.addView(recordsCount);
        };
        monthSpinner.setOnItemSelectedListener(new SimpleSelect(update));
        yearSpinner.setOnItemSelectedListener(new SimpleSelect(update));
        addButton(root, "BACK TO HOME", gold, this::showHome);
        update.run();
    }

    // PHOTO PREVIEW
    private void showPhotos(JSONObject r) {
        LinearLayout content = vertical();
        content.setPadding(dp(12), dp(8), dp(12), dp(8));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        int count = 0;
        for (int i = 1; i <= 3; i++) {
            String value = r.optString("PHOTO" + i);
            if (value.isEmpty()) continue;
            count++;
            content.addView(text("PHOTO " + i, 14, navy, true));
            try {
                ImageView image = new ImageView(this);
                image.setImageURI(Uri.parse(value));
                image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                image.setBackground(shape(pale,10,Color.LTGRAY));
                LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(-1, dp(220));
                ip.bottomMargin = dp(8); content.addView(image, ip);
                image.setOnClickListener(v -> showLargePhoto(value));
            } catch (Exception e) {
                content.addView(text("PHOTO OPEN ERROR", 12, Color.RED, false));
            }
        }
        if (count == 0) content.addView(text("इस रिकॉर्ड में फोटो नहीं है।",14,Color.GRAY,false));
        new AlertDialog.Builder(this).setTitle("ATTACHED PHOTOS").setView(scroll)
            .setPositiveButton("CLOSE", null).show();
    }

    private void showLargePhoto(String value) {
        try {
            ImageView image = new ImageView(this);
            image.setImageURI(Uri.parse(value));
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setBackgroundColor(Color.BLACK);
            new AlertDialog.Builder(this).setTitle("PHOTO PREVIEW").setView(image)
                .setPositiveButton("CLOSE", null).show();
        } catch (Exception e) { toast("PHOTO OPEN नहीं हो सकी"); }
    }

    private void openSavedPDF(JSONObject r) {
        String value = r.optString("PDF", "");
        if (value.isEmpty()) { toast("इस रिकॉर्ड में PDF नहीं है"); return; }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(value), "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) { toast("PDF देखने के लिए PDF Viewer इंस्टॉल करें"); }
    }

    // CSV EXPORT
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
            if (out == null) throw new IOException("File open failed");
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

    // NAVIGATION
    private void goBack() {
        if ("ENTRY".equals(currentPage)) {
            if (entryFromRecords) {
                editing = -1; entryFromRecords = false; showRecords();
            } else showHome();
        } else if ("RECORDS".equals(currentPage) || "REPORTS".equals(currentPage)) showHome();
        else if ("HOME".equals(currentPage)) {
            new AlertDialog.Builder(this).setTitle("EXIT VIPIN SERVICE CRM")
                .setMessage("क्या आप ऐप बंद करना चाहते हैं?")
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("EXIT", (d,w) -> finish()).show();
        } else {
            new AlertDialog.Builder(this).setTitle("EXIT")
                .setMessage("क्या आप ऐप बंद करना चाहते हैं?")
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("EXIT", (d,w) -> finish()).show();
        }
    }

    @Override public void onBackPressed() { goBack(); }
}
