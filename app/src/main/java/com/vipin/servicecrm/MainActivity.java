package com.vipin.servicecrm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;
import java.util.TreeSet;

public class MainActivity extends Activity {

    private static final String PREF = "vipin_service_crm";
    private static final String RECORDS = "records";
    private static final String PASSWORD = "password";
    private static final int REQ_PHOTO = 701, REQ_EXPORT = 702, REQ_PDF = 703, REQ_REPORT_PDF = 704;

    private final String[] FIELDS = {
        "DATE", "BILL NUMBER", "PRODUCT NAME", "AMC", "COLLECTION",
        "CUSTOMER NAME", "MOBILE NUMBER", "ADDRESS", "SPARE PARTS",
        "AMOUNT", "SPARE AMOUNT", "WARRANTY AMOUNT", "NET AMOUNT",
        "PAYMENT MODE", "REMARKS"
    };
    private final String[] PAYMENTS = {"CASH", "UPI", "PS UPI", "RKS UPI"};
    private final String[] MONEY_FIELDS = {
        "AMC", "COLLECTION", "AMOUNT", "SPARE AMOUNT", "WARRANTY AMOUNT", "NET AMOUNT"
    };

    private final int navy = Color.rgb(31, 48, 79);
    private final int pale = Color.rgb(235, 244, 255);
    private final int ink = Color.rgb(39, 49, 67);
    private final int gold = Color.rgb(246, 218, 153);

    private LinearLayout root, recordsLayout;
    private ScrollView pageScroll;
    private EditText[] inputs = new EditText[15];
    private LinearLayout[] fieldGroups = new LinearLayout[15];
    private Spinner paymentSpinner, monthSpinner, yearSpinner;
    private EditText searchBox;
    private TextView statusText;
    private JSONArray records = new JSONArray();
    private int editing = -1, photoSlot = 0;
    private String[] photoUris = new String[3];
    private String pdfUri = "";
    private String currentPage = "LOGIN";
    private boolean entryFromRecords = false;
    private String selectedReportMonth = "ALL MONTHS";
    private String selectedReportYear = "ALL YEARS";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(30, 55, 125));
        getWindow().setNavigationBarColor(Color.rgb(45, 58, 86));
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        loadRecords();
        showLogin();
    }

    private int dp(float n) {
        return (int) (n * getResources().getDisplayMetrics().density + .5f);
    }

    private GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }

    private GradientDrawable gradient(int start, int end, int radius) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{start, end});
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
        // Resolve dynamically so the app still compiles if the optional logo is absent.
        int id = getResources().getIdentifier("vipin_service_crm_icon_512", "drawable", getPackageName());
        if (id == 0) return;
        ImageView logo = new ImageView(this);
        logo.setImageResource(id);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp));
        p.gravity = Gravity.CENTER_HORIZONTAL;
        p.bottomMargin = dp(10);
        parent.addView(logo, p);
        animateIn(logo);
    }

    private void animateIn(View view) {
        if (view == null) return;
        view.setAlpha(0f);
        view.setTranslationY(dp(14));
        view.setScaleX(.98f);
        view.setScaleY(.98f);
        view.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                .setDuration(350).start();
    }

    private void animatePress(View view) {
        view.animate().cancel();
        view.setScaleX(.97f);
        view.setScaleY(.97f);
        view.animate().scaleX(1f).scaleY(1f).setDuration(160).start();
    }

    private void pageBase(String title) {
        root = vertical();
        root.setPadding(dp(16), dp(16), dp(16), dp(30));
        root.setBackgroundColor(pale);

        pageScroll = new ScrollView(this);
        pageScroll.setFillViewport(false);
        pageScroll.setClipToPadding(false);
        pageScroll.setVerticalScrollBarEnabled(true);
        pageScroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        pageScroll.setFocusable(true);
        pageScroll.setFocusableInTouchMode(true);
        pageScroll.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        // Extra scroll space lets the last inputs and SAVE button clear the keyboard.
        pageScroll.setPadding(0, dp(4), 0, dp(180));
        pageScroll.addView(root, new ScrollView.LayoutParams(-1, -2));
        setContentView(pageScroll);

        TextView header = text(title, 21, navy, true);
        header.setGravity(Gravity.CENTER);
        header.setPadding(dp(12), dp(15), dp(12), dp(15));
        header.setBackground(shape(gold, 18, Color.rgb(225, 190, 112)));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(8);
        hp.bottomMargin = dp(16);
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
        e.setPadding(dp(12), dp(9), dp(12), dp(9));
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
        p.topMargin = dp(7);
        p.bottomMargin = dp(4);
        parent.addView(b, p);
        animateIn(b);
        b.setOnClickListener(v -> {
            animatePress(b);
            action.run();
        });
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // OFFLINE STORAGE
    private void loadRecords() {
        String raw = getSharedPreferences(PREF, MODE_PRIVATE).getString(RECORDS, "[]");
        try {
            records = new JSONArray(raw);
        } catch (Exception e) {
            records = new JSONArray();
        }
    }

    private void persist() {
        getSharedPreferences(PREF, MODE_PRIVATE).edit().putString(RECORDS, records.toString()).apply();
    }

    private String getPassword() {
        return getSharedPreferences(PREF, MODE_PRIVATE).getString(PASSWORD, "1234");
    }

    // LOGIN
    private void showLogin() {
        currentPage = "LOGIN";
        pageBase("VIPIN SERVICE CRM");
        addLogo(root, 145);
        TextView welcome = text("OFFLINE SERVICE RECORD MANAGER", 14, navy, true);
        welcome.setGravity(Gravity.CENTER);
        root.addView(welcome);
        TextView label = text("ENTER PASSWORD", 13, ink, true);
        label.setPadding(0, dp(18), 0, dp(4));
        root.addView(label);
        EditText pass = edit("PASSWORD");
        pass.setInputType(129);
        root.addView(pass);
        addButton(root, "LOGIN", navy, () -> {
            if (pass.getText().toString().equals(getPassword())) showHome();
            else toast("गलत पासवर्ड");
        });
        addButton(root, "CHANGE PASSWORD", gold, this::changePassword);
        TextView note = text("पहली बार का पासवर्ड: 1234", 12, Color.GRAY, false);
        note.setGravity(Gravity.CENTER);
        note.setPadding(0, dp(10), 0, 0);
        root.addView(note);
    }

    private void changePassword() {
        LinearLayout box = vertical();
        box.setPadding(dp(8), dp(8), dp(8), dp(8));
        EditText oldP = edit("CURRENT PASSWORD");
        EditText newP = edit("NEW PASSWORD");
        EditText confirm = edit("CONFIRM NEW PASSWORD");
        oldP.setInputType(129);
        newP.setInputType(129);
        confirm.setInputType(129);
        box.addView(oldP);
        box.addView(newP);
        box.addView(confirm);
        new AlertDialog.Builder(this).setTitle("CHANGE PASSWORD").setView(box)
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("SAVE", (d, w) -> {
                    if (!oldP.getText().toString().equals(getPassword())) {
                        toast("Current password गलत है");
                    } else if (newP.length() < 4 || !newP.getText().toString()
                            .equals(confirm.getText().toString())) {
                        toast("नया पासवर्ड जाँचें (कम से कम 4 अक्षर)");
                    } else {
                        getSharedPreferences(PREF, MODE_PRIVATE).edit()
                                .putString(PASSWORD, newP.getText().toString()).apply();
                        toast("पासवर्ड बदल गया");
                    }
                }).show();
    }

    // HOME
    private void homeButton(String label, String icon, int c1, int c2, Runnable action) {
        LinearLayout button = horizontal();
        button.setPadding(dp(15), dp(11), dp(15), dp(11));
        button.setBackground(gradient(c1, c2, 18));
        TextView symbol = text(icon, 25, c1, true);
        symbol.setGravity(Gravity.CENTER);
        symbol.setBackground(shape(Color.WHITE, 50, 0));
        button.addView(symbol, new LinearLayout.LayoutParams(dp(50), dp(50)));
        TextView title = text(label, 15, Color.WHITE, true);
        title.setPadding(dp(14), 0, 0, 0);
        button.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        button.addView(text("›", 30, Color.WHITE, true));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(76));
        p.topMargin = dp(7);
        p.bottomMargin = dp(7);
        root.addView(button, p);
        animateIn(button);
        button.setOnClickListener(v -> {
            animatePress(button);
            action.run();
        });
    }

    private void showHome() {
        currentPage = "HOME";
        editing = -1;
        entryFromRecords = false;
        root = vertical();
        root.setPadding(dp(16), dp(18), dp(16), dp(24));
        root.setBackgroundColor(pale);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(root);
        setContentView(scroll);
        pageScroll = scroll;

        LinearLayout banner = vertical();
        banner.setPadding(dp(18), dp(18), dp(18), dp(20));
        banner.setBackground(gradient(Color.rgb(38, 48, 180), Color.rgb(0, 105, 255), 22));
        addLogo(banner, 115);
        TextView heading = text("VIPIN SERVICE CRM", 22, Color.WHITE, true);
        heading.setGravity(Gravity.CENTER);
        banner.addView(heading);
        TextView sub = text("OFFLINE SERVICE RECORD MANAGER", 12, Color.WHITE, true);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(8), 0, dp(8));
        banner.addView(sub);
        TextView count = text("TOTAL RECORDS  •  " + records.length(), 15, Color.WHITE, true);
        count.setGravity(Gravity.CENTER);
        banner.addView(count);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.bottomMargin = dp(10);
        root.addView(banner, bp);

        homeButton("ADD NEW RECORD", "＋", Color.rgb(0,112,255), Color.rgb(0,145,255), this::showEntry);
        homeButton("SEARCH / MONTH / YEAR RECORDS", "⌕", Color.rgb(0,170,180), Color.rgb(0,180,155), this::showRecords);
        homeButton("MONTHLY / YEARLY REPORTS", "▦", Color.rgb(103,35,235), Color.rgb(153,48,255), this::showReports);
        homeButton("EXPORT EXCEL / CSV", "▣", Color.rgb(255,105,0), Color.rgb(255,175,0), this::exportCSV);
        homeButton("CHANGE PASSWORD", "⚙", Color.rgb(220,0,130), Color.rgb(255,35,105), this::changePassword);
        homeButton("LOG OUT", "↪", Color.rgb(90,100,120), Color.rgb(115,125,145), this::showLogin);

        TextView current = text("CURRENT TOTAL", 19, navy, true);
        current.setPadding(0, dp(16), 0, dp(4));
        root.addView(current);
        addTotalGrid(root, calculateTotals("ALL", "ALL"));

        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, -1);
        String m = String.format(Locale.US, "%02d", c.get(Calendar.MONTH) + 1);
        String y = String.valueOf(c.get(Calendar.YEAR));
        TextView last = text("LAST MONTH TOTAL", 19, navy, true);
        last.setPadding(0, dp(16), 0, dp(4));
        root.addView(last);
        addTotalGrid(root, calculateTotals(m, y));
    }

    // DATE AND AMOUNT HELPERS
    private String normalizeDate(String value) {
        if (value == null) return "";
        String s = value.trim();
        String y = "", m = "", d = "";
        if (s.matches("\\d{4}-\\d{2}-\\d{2}")) {
            String[] p = s.split("-");
            y = p[0]; m = p[1]; d = p[2];
        } else if (s.matches("\\d{2}-\\d{2}-\\d{4}")) {
            String[] p = s.split("-");
            d = p[0]; m = p[1]; y = p[2];
        } else if (s.matches("\\d{2}/\\d{2}/\\d{4}")) {
            String[] p = s.split("/");
            d = p[0]; m = p[1]; y = p[2];
        } else {
            return "";
        }
        try {
            int yi = Integer.parseInt(y), mi = Integer.parseInt(m), di = Integer.parseInt(d);
            Calendar c = Calendar.getInstance();
            c.setLenient(false);
            c.clear();
            c.set(yi, mi - 1, di);
            c.getTime();
            return String.format(Locale.US, "%04d-%02d-%02d", yi, mi, di);
        } catch (Exception e) {
            return "";
        }
    }

    private String displayDate(String value) {
        String iso = normalizeDate(value);
        if (iso.isEmpty()) return value == null ? "" : value;
        String[] p = iso.split("-");
        return p[2] + "-" + p[1] + "-" + p[0];
    }

    private double number(String s) {
        try {
            if (s == null) return 0;
            String cleaned = s.replace(",", "").replace("₹", "")
                    .replace("Rs.", "").replace("RS.", "").trim();
            if (cleaned.isEmpty()) return 0;
            return Double.parseDouble(cleaned);
        } catch (Exception e) {
            return 0;
        }
    }

    private String money(double value) {
        return String.format(Locale.US, "₹ %.2f", value);
    }

    private double[] calculateTotals(String month, String year) {
        double[] totals = new double[MONEY_FIELDS.length];
        for (int i = 0; i < records.length(); i++) {
            JSONObject r = records.optJSONObject(i);
            if (r == null) continue;
            String date = normalizeDate(r.optString("DATE"));
            if (date.isEmpty()) continue;
            if (month != null && !"ALL".equals(month) && !date.substring(5, 7).equals(month)) continue;
            if (year != null && !"ALL".equals(year) && !date.startsWith(year + "-")) continue;
            for (int j = 0; j < MONEY_FIELDS.length; j++)
                totals[j] += number(r.optString(MONEY_FIELDS[j]));
        }
        return totals;
    }

    private void addTotalGrid(LinearLayout parent, double[] totals) {
        int[] c1 = {Color.rgb(0,125,255), Color.rgb(0,170,145), Color.rgb(105,35,230),
                Color.rgb(255,105,0), Color.rgb(210,65,100), Color.rgb(35,105,160)};
        int[] c2 = {Color.rgb(0,180,255), Color.rgb(0,195,160), Color.rgb(150,55,255),
                Color.rgb(255,175,0), Color.rgb(245,100,130), Color.rgb(70,155,210)};
        for (int row = 0; row < 3; row++) {
            LinearLayout line = horizontal();
            for (int col = 0; col < 2; col++) {
                int i = row * 2 + col;
                LinearLayout card = vertical();
                card.setGravity(Gravity.CENTER);
                card.setPadding(dp(5), dp(9), dp(5), dp(9));
                card.setBackground(gradient(c1[i], c2[i], 14));
                TextView name = text(MONEY_FIELDS[i], 10, Color.WHITE, true);
                name.setGravity(Gravity.CENTER);
                TextView value = text(money(totals[i]), 15, Color.WHITE, true);
                value.setGravity(Gravity.CENTER);
                card.addView(name);
                card.addView(value);
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dp(82), 1);
                cp.setMargins(dp(3), dp(3), dp(3), dp(3));
                line.addView(card, cp);
            }
            parent.addView(line);
        }
    }

    // ADD / EDIT RECORD
    private void showEntry() {
        currentPage = "ENTRY";
        if (editing < 0) {
            photoUris = new String[3];
            pdfUri = "";
        }
        inputs = new EditText[15];
        fieldGroups = new LinearLayout[15];
        pageBase(editing >= 0 ? "EDIT SERVICE RECORD" : "NEW SERVICE RECORD");

        for (int i = 0; i < FIELDS.length; i++) {
            LinearLayout group = vertical();
            fieldGroups[i] = group;
            LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-1, -2);
            gp.topMargin = dp(6);
            root.addView(group, gp);
            group.addView(text(FIELDS[i], 12, navy, true));

            if (i == 13) {
                paymentSpinner = new Spinner(this);
                ArrayAdapter<String> a = new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_dropdown_item, PAYMENTS);
                paymentSpinner.setAdapter(a);
                group.addView(paymentSpinner);
            } else {
                EditText e = edit(FIELDS[i]);
                inputs[i] = e;
                if (i == 0) {
                    e.setFocusable(false);
                    e.setHint("SELECT DATE");
                    e.setOnClickListener(v -> pickDate(e));
                } else {
                    e.setFilters(new InputFilter[]{new InputFilter.AllCaps()});
                    if (i == 3 || i == 4 || i == 9 || i == 10 || i == 11 || i == 12)
                        e.setInputType(8194);
                    if (i == 6) e.setInputType(3);
                    if (i == 7 || i == 8 || i == 14) {
                        e.setSingleLine(false);
                        e.setMinLines(1);
                        e.setMaxLines(3);
                        e.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                                | android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                    }
                    e.setImeOptions(i == 14 ? EditorInfo.IME_ACTION_DONE : EditorInfo.IME_ACTION_NEXT);
                }
                group.addView(e);
                animateIn(e);
                if (i != 0) setupKeyboard(e, i);
            }
        }

        if (editing >= 0) fillEditRecord();

        addButton(root, "ATTACH PHOTO (UP TO 3)", Color.rgb(86,105,145), this::choosePhoto);
        addButton(root, "ATTACH PDF", Color.rgb(105,75,175), this::choosePDF);
        statusText = text(attachmentStatus(), 12, Color.GRAY, false);
        statusText.setPadding(0, dp(5), 0, dp(5));
        root.addView(statusText);
        addButton(root, "SAVE RECORD", Color.rgb(42,133,102), this::saveRecord);
        addButton(root, "CANCEL / BACK", gold, this::goBack);
    }
private void setupKeyboard(EditText field, int index) {
        field.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) scrollToInput(field);
        });
        field.setOnClickListener(v -> scrollToInput(field));
        field.setOnEditorActionListener((v, action, event) -> {
            boolean enter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;
            if (action == EditorInfo.IME_ACTION_NEXT || action == EditorInfo.IME_ACTION_DONE || enter) {
                int next = index + 1;
                while (next < FIELDS.length && (next == 0 || next == 13)) next++;
                if (next < FIELDS.length && inputs[next] != null) {
                    EditText nextField = inputs[next];
                    nextField.requestFocus();
                    nextField.postDelayed(() -> {
                        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        if (imm != null) imm.showSoftInput(nextField, InputMethodManager.SHOW_IMPLICIT);
                        scrollToInput(nextField);
                    }, 150);
                } else {
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(field.getWindowToken(), 0);
                    field.clearFocus();
                }
                return true;
            }
            return false;
        });
    }

    private void scrollToInput(EditText field) {
        if (pageScroll == null || field == null) return;

        // Move the whole active field group above the keyboard.
        int index = findInputIndex(field);
        final View target = index >= 0 && fieldGroups != null && fieldGroups[index] != null
                ? fieldGroups[index] : field;

        Runnable reveal = () -> {
            if (pageScroll == null || target.getWindowToken() == null) return;
            try {
                Rect visible = new Rect();
                getWindow().getDecorView().getWindowVisibleDisplayFrame(visible);

                int[] location = new int[2];
                target.getLocationOnScreen(location);
                int targetTop = location[1];
                int targetBottom = targetTop + target.getHeight();

                // Keep a safe gap above the keyboard, including Samsung IME.
                int safeTop = visible.top + dp(12);
                int safeBottom = visible.bottom - dp(115);
                int delta = 0;

                if (targetBottom > safeBottom) {
                    delta = targetBottom - safeBottom;
                } else if (targetTop < safeTop) {
                    delta = targetTop - safeTop;
                }

                if (delta != 0) pageScroll.smoothScrollBy(0, delta);
                field.requestRectangleOnScreen(
                        new Rect(0, 0, field.getWidth(), field.getHeight()), true);
            } catch (Exception ignored) { }
        };

        // Recheck after focus and after the keyboard has resized the window.
        pageScroll.post(reveal);
        pageScroll.postDelayed(reveal, 180);
        pageScroll.postDelayed(reveal, 420);
        pageScroll.postDelayed(reveal, 750);
    }

    private int findInputIndex(EditText field) {
        if (inputs == null) return -1;
        for (int i = 0; i < inputs.length; i++) if (inputs[i] == field) return i;
        return -1;
    }

    private void pickDate(EditText target) {
        Calendar c = Calendar.getInstance();
        String existing = normalizeDate(target.getText().toString());
        if (!existing.isEmpty()) {
            String[] p = existing.split("-");
            c.set(Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2]));
        }
        new DatePickerDialog(this, (view, year, month, day) ->
                target.setText(String.format(Locale.US, "%02d-%02d-%04d", day, month + 1, year)),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void fillEditRecord() {
        JSONObject r = records.optJSONObject(editing);
        if (r == null) return;
        for (int i = 0; i < FIELDS.length; i++) {
            if (i == 13) {
                String value = r.optString(FIELDS[i], "CASH");
                for (int j = 0; j < PAYMENTS.length; j++)
                    if (PAYMENTS[j].equals(value)) paymentSpinner.setSelection(j);
            } else if (inputs[i] != null) {
                String value = r.optString(FIELDS[i]);
                inputs[i].setText(i == 0 ? displayDate(value) : value);
            }
        }
        for (int i = 0; i < 3; i++) photoUris[i] = r.optString("PHOTO" + (i + 1), "");
        pdfUri = r.optString("PDF", "");
    }

    // PHOTO / PDF ATTACHMENTS
    private void choosePhoto() {
        int slot = -1;
        for (int i = 0; i < 3; i++) {
            if (photoUris[i] == null || photoUris[i].isEmpty()) {
                slot = i;
                break;
            }
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
        return "PHOTOS: " + count + " / 3     PDF: "
                + ((pdfUri == null || pdfUri.isEmpty()) ? "NOT ATTACHED" : "ATTACHED");
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == REQ_EXPORT) {
            writeCSV(uri);
            return;
        }
        if (requestCode == REQ_REPORT_PDF) {
            writeMonthlyPDF(uri);
            return;
        }
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) { }
        if (requestCode == REQ_PHOTO) photoUris[photoSlot] = uri.toString();
        else if (requestCode == REQ_PDF) pdfUri = uri.toString();
        if (statusText != null) statusText.setText(attachmentStatus());
    }

    private boolean validateAmounts() {
        int[] indexes = {3, 4, 9, 10, 11, 12};
        for (int i : indexes) {
            String value = inputs[i].getText().toString().trim();
            if (!value.isEmpty()) {
                String cleaned = value.replace(",", "").replace("₹", "")
                        .replace("Rs.", "").replace("RS.", "").trim();
                try {
                    Double.parseDouble(cleaned);
                } catch (Exception e) {
                    inputs[i].setError("केवल सही नंबर लिखें");
                    inputs[i].requestFocus();
                    scrollToInput(inputs[i]);
                    return false;
                }
            }
        }
        return true;
    }

    private void saveRecord() {
        if (inputs[0] == null || normalizeDate(inputs[0].getText().toString()).isEmpty()) {
            toast("DATE सही तरीके से चुनें");
            if (inputs[0] != null) inputs[0].performClick();
            return;
        }
        if (!validateAmounts()) return;

        JSONObject r = new JSONObject();
        try {
            for (int i = 0; i < FIELDS.length; i++) {
                if (i == 13) {
                    Object selected = paymentSpinner.getSelectedItem();
                    r.put(FIELDS[i], selected == null ? "CASH" : selected.toString().toUpperCase(Locale.ROOT));
                } else {
                    String value = inputs[i].getText().toString().trim();
                    if (i == 0) value = normalizeDate(value);
                    else value = value.toUpperCase(Locale.ROOT);
                    r.put(FIELDS[i], value);
                }
            }
            for (int i = 0; i < 3; i++) r.put("PHOTO" + (i + 1), photoUris[i] == null ? "" : photoUris[i]);
            r.put("PDF", pdfUri == null ? "" : pdfUri);

            if (editing >= 0 && editing < records.length()) records.put(editing, r);
            else records.put(r);
            persist();
            editing = -1;
            entryFromRecords = false;
            hideKeyboard();
            toast("RECORD SAVED SUCCESSFULLY");
            showHome();
        } catch (Exception e) {
            Toast.makeText(this, "Save error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null && getCurrentFocus() != null)
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        } catch (Exception ignored) { }
    }

    // RECORD FILTERS
    private ArrayList<String> getYears() {
        TreeSet<String> found = new TreeSet<>(Collections.reverseOrder());
        for (int i = 0; i < records.length(); i++) {
            JSONObject r = records.optJSONObject(i);
            if (r == null) continue;
            String d = normalizeDate(r.optString("DATE"));
            if (!d.isEmpty()) found.add(d.substring(0, 4));
        }
        int now = Calendar.getInstance().get(Calendar.YEAR);
        found.add(String.valueOf(now));
        found.add(String.valueOf(now - 1));
        ArrayList<String> list = new ArrayList<>();
        list.add("ALL YEARS");
        list.addAll(found);
        return list;
    }

    private void setupMonthYearFilters(LinearLayout parent) {
        LinearLayout filters = horizontal();
        monthSpinner = new Spinner(this);
        yearSpinner = new Spinner(this);
        String[] months = {"ALL MONTHS", "01", "02", "03", "04", "05", "06",
                "07", "08", "09", "10", "11", "12"};
        monthSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, months));
        yearSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, getYears()));
        filters.addView(monthSpinner, new LinearLayout.LayoutParams(0, dp(48), 1));
        filters.addView(yearSpinner, new LinearLayout.LayoutParams(0, dp(48), 1));
        parent.addView(filters);
    }

    private boolean matchesDate(String value, String month, String year) {
        String d = normalizeDate(value);
        if (d.isEmpty()) return false;
        if (!"ALL MONTHS".equals(month) && !d.substring(5, 7).equals(month)) return false;
        if (!"ALL YEARS".equals(year) && !d.startsWith(year + "-")) return false;
        return true;
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
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { renderRecords(); }
            public void afterTextChanged(Editable e) { }
        });
        monthSpinner.setOnItemSelectedListener(new SimpleSelect(this::renderRecords));
        yearSpinner.setOnItemSelectedListener(new SimpleSelect(this::renderRecords));
        addButton(root, "BACK TO HOME", gold, this::showHome);
        renderRecords();
    }

    private class SimpleSelect implements AdapterView.OnItemSelectedListener {
        private final Runnable action;
        SimpleSelect(Runnable r) { action = r; }
        public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { action.run(); }
        public void onNothingSelected(AdapterView<?> p) { }
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
            card.setPadding(dp(12), dp(11), dp(12), dp(11));
            card.setBackground(shape(Color.WHITE, 14, Color.rgb(218,224,233)));
            TextView title = text(r.optString("CUSTOMER NAME") + " | "
                    + displayDate(r.optString("DATE")), 15, navy, true);
            card.addView(title);
            for (String field : FIELDS) {
                if ("DATE".equals(field) || "CUSTOMER NAME".equals(field)) continue;
                TextView line = text(field + ": " + r.optString(field), 12, ink, true);
                line.setPadding(0, dp(2), 0, dp(2));
                card.addView(line);
            }

            LinearLayout row1 = horizontal();
            Button copy = smallButton("COPY TEXT");
            Button edit = smallButton("EDIT");
            Button del = smallButton("DELETE");
            row1.addView(copy, new LinearLayout.LayoutParams(0, dp(42), 1));
            row1.addView(edit, new LinearLayout.LayoutParams(0, dp(42), 1));
            row1.addView(del, new LinearLayout.LayoutParams(0, dp(42), 1));
            card.addView(row1);

            LinearLayout row2 = horizontal();
            Button photos = smallButton("VIEW PHOTOS");
            Button pdf = smallButton("OPEN PDF");
            row2.addView(photos, new LinearLayout.LayoutParams(0, dp(42), 1));
            row2.addView(pdf, new LinearLayout.LayoutParams(0, dp(42), 1));
            card.addView(row2);

            copy.setOnClickListener(v -> copyRecord(r));
            edit.setOnClickListener(v -> {
                editing = index;
                entryFromRecords = true;
                showEntry();
            });
            del.setOnClickListener(v -> confirmDelete(index));
            photos.setOnClickListener(v -> showPhotos(r));
            pdf.setOnClickListener(v -> openSavedPDF(r));

            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
            cp.topMargin = dp(9);
            recordsLayout.addView(card, cp);
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
            p.topMargin = dp(3);
            parent.addView(t, p);
        }
    }

    private void copyRecord(JSONObject r) {
        StringBuilder b = new StringBuilder("VIPIN SERVICE CRM\n----------------------\n");
        for (String field : FIELDS) b.append(field).append(": ").append(r.optString(field)).append("\n");
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("SERVICE RECORD", b.toString()));
            toast("पूरा रिकॉर्ड COPY हो गया");
        }
    }

    private Button smallButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(10);
        b.setAllCaps(false);
        b.setPadding(0, 0, 0, 0);
        b.setBackground(shape(gold, 9, 0));
        b.setTextColor(navy);
        return b;
    }

    private void confirmDelete(int index) {
        new AlertDialog.Builder(this).setMessage("यह रिकॉर्ड DELETE करें?")
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("DELETE", (d, w) -> {
                    JSONArray next = new JSONArray();
                    for (int k = 0; k < records.length(); k++)
                        if (k != index) next.put(records.opt(k));
                    records = next;
                    persist();
                    renderRecords();
                    toast("RECORD DELETED");
                }).show();
    }

    // REPORTS
    private void showReports() {
        currentPage = "REPORTS";
        pageBase("MONTHLY / YEARLY REPORTS");
        root.addView(text("MONTH AND YEAR SELECT करें", 13, navy, true));
        setupMonthYearFilters(root);
        LinearLayout reportArea = vertical();
        root.addView(reportArea);

        Runnable update = () -> {
            if (monthSpinner.getSelectedItem() == null || yearSpinner.getSelectedItem() == null) return;
            reportArea.removeAllViews();
            String m = monthSpinner.getSelectedItem().toString();
            String y = yearSpinner.getSelectedItem().toString();
            selectedReportMonth = m;
            selectedReportYear = y;
            String month = "ALL MONTHS".equals(m) ? "ALL" : m;
            String year = "ALL YEARS".equals(y) ? "ALL" : y;
            addTotalsText(reportArea, calculateTotals(month, year));
            String period = y + " / " + m;
            TextView selected = text("REPORT PERIOD: " + period, 14, navy, true);
            selected.setPadding(0, dp(12), 0, dp(8));
            reportArea.addView(selected, 0);
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
        addButton(root, "DOWNLOAD MONTHLY / YEARLY REPORT PDF",
                Color.rgb(190, 35, 75), this::exportMonthlyPDF);
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
            ImageView image = new ImageView(this);
            image.setImageURI(Uri.parse(value));
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setBackground(shape(pale, 10, Color.LTGRAY));
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(-1, dp(220));
            ip.bottomMargin = dp(8);
            content.addView(image, ip);
            image.setOnClickListener(v -> showLargePhoto(value));
        }
        if (count == 0) content.addView(text("इस रिकॉर्ड में फोटो नहीं है।", 14, Color.GRAY, false));
        new AlertDialog.Builder(this).setTitle("ATTACHED PHOTOS").setView(scroll)
                .setPositiveButton("CLOSE", null).show();
    }

    private void showLargePhoto(String value) {
        try {
            ImageView image = new ImageView(this);
            image.setImageURI(Uri.parse(value));
            image.setAdjustViewBounds(true);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setBackgroundColor(Color.BLACK);
            new AlertDialog.Builder(this).setTitle("PHOTO PREVIEW").setView(image)
                    .setPositiveButton("CLOSE", null).show();
        } catch (Exception e) {
            toast("PHOTO OPEN नहीं हो सकी");
        }
            }
    private void openSavedPDF(JSONObject r) {
        String value = r.optString("PDF", "");
        if (value.isEmpty()) {
            toast("इस रिकॉर्ड में PDF नहीं है");
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(value), "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            toast("PDF देखने के लिए PDF Viewer इंस्टॉल करें");
        }
    }

    // OFFLINE PDF EXPORT
    private void exportMonthlyPDF() {
        if (monthSpinner != null && monthSpinner.getSelectedItem() != null)
            selectedReportMonth = monthSpinner.getSelectedItem().toString();
        if (yearSpinner != null && yearSpinner.getSelectedItem() != null)
            selectedReportYear = yearSpinner.getSelectedItem().toString();

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, "VIPIN_SERVICE_REPORT_"
                + selectedReportYear.replace(" ", "_") + "_"
                + selectedReportMonth.replace(" ", "_") + ".pdf");
        startActivityForResult(intent, REQ_REPORT_PDF);
    }

    private void writeMonthlyPDF(Uri uri) {
        PdfDocument document = new PdfDocument();
        OutputStream output = null;
        PdfDocument.Page page = null;
        try {
            ArrayList<String> lines = new ArrayList<>();
            lines.add("VIPIN SERVICE CRM");
            lines.add("MONTHLY / YEARLY SERVICE REPORT");
            lines.add("PERIOD: " + selectedReportMonth + " / " + selectedReportYear);
            lines.add("============================================================");
            lines.add("");
            int count = 0;
            for (int i = 0; i < records.length(); i++) {
                JSONObject r = records.optJSONObject(i);
                if (r == null || !matchesDate(r.optString("DATE"),
                        selectedReportMonth, selectedReportYear)) continue;
                count++;
                lines.add("RECORD " + count);
                lines.add("------------------------------------------------------------");
                for (String field : FIELDS) {
                    String value = r.optString(field, "");
                    if ("DATE".equals(field)) value = displayDate(value);
                    lines.add(field + ": " + (value.isEmpty() ? "-" : value));
                }
                lines.add("");
            }

            String monthArg = "ALL MONTHS".equals(selectedReportMonth) ? "ALL" : selectedReportMonth;
            String yearArg = "ALL YEARS".equals(selectedReportYear) ? "ALL" : selectedReportYear;
            double[] totals = calculateTotals(monthArg, yearArg);
            lines.add("============================================================");
            lines.add("MONTHLY / YEARLY TOTAL SUMMARY");
            lines.add("============================================================");
            for (int i = 0; i < MONEY_FIELDS.length; i++)
                lines.add(MONEY_FIELDS[i] + " : " + money(totals[i]));
            lines.add("------------------------------------------------------------");
            lines.add("TOTAL RECORDS: " + count);
            lines.add("============================================================");
            lines.add("Generated by VIPIN SERVICE CRM - OFFLINE REPORT");

            final int pageWidth = 595, pageHeight = 842, left = 38, right = 38;
            final int top = 48, bottom = 48;
            android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Color.BLACK);
            paint.setTextSize(9.5f);

            ArrayList<String> wrapped = new ArrayList<>();
            float maxWidth = pageWidth - left - right;
            for (String line : lines) {
                if (line == null || line.isEmpty()) {
                    wrapped.add("");
                    continue;
                }
                String remaining = line;
                while (paint.measureText(remaining) > maxWidth) {
                    int cut = remaining.length();
                    while (cut > 1 && paint.measureText(remaining.substring(0, cut)) > maxWidth) cut--;
                    wrapped.add(remaining.substring(0, cut));
                    remaining = remaining.substring(cut);
                }
                wrapped.add(remaining);
            }

            int pageNumber = 1;
            int y = top;
            for (String line : wrapped) {
                boolean heading = line.equals("VIPIN SERVICE CRM")
                        || line.equals("MONTHLY / YEARLY SERVICE REPORT")
                        || line.equals("MONTHLY / YEARLY TOTAL SUMMARY");
                int lineHeight = heading ? 22 : 15;
                if (page == null || y + lineHeight > pageHeight - bottom) {
                    if (page != null) document.finishPage(page);
                    PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(
                            pageWidth, pageHeight, pageNumber).create();
                    page = document.startPage(info);
                    pageNumber++;
                    android.graphics.Canvas canvas = page.getCanvas();
                    paint.setTypeface(Typeface.DEFAULT_BOLD);
                    paint.setTextSize(8);
                    canvas.drawText("VIPIN SERVICE CRM", left, 25, paint);
                    canvas.drawText("PAGE " + (pageNumber - 1), pageWidth - 85, 25, paint);
                    paint.setTypeface(Typeface.DEFAULT);
                    paint.setTextSize(9.5f);
                    y = top;
                }
                Typeface oldTypeface = paint.getTypeface();
                float oldSize = paint.getTextSize();
                if (heading) {
                    paint.setTypeface(Typeface.DEFAULT_BOLD);
                    paint.setTextSize(line.equals("VIPIN SERVICE CRM") ? 15f : 11f);
                }
                page.getCanvas().drawText(line, left, y, paint);
                paint.setTypeface(oldTypeface);
                paint.setTextSize(oldSize);
                y += lineHeight;
            }
            if (page != null) document.finishPage(page);
            output = getContentResolver().openOutputStream(uri);
            if (output == null) throw new IOException("PDF FILE OPEN ERROR");
            document.writeTo(output);
            output.flush();
            toast("REPORT PDF SAVED SUCCESSFULLY");
        } catch (Exception e) {
            Toast.makeText(this, "PDF ERROR: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            try {
                if (output != null) output.close();
            } catch (Exception ignored) { }
            document.close();
        }
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
            OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");
            writer.write("\uFEFF");
            for (int i = 0; i < FIELDS.length; i++) {
                if (i > 0) writer.write(",");
                writer.write(csv(FIELDS[i]));
            }
            writer.write("\n");
            for (int i = 0; i < records.length(); i++) {
                JSONObject r = records.optJSONObject(i);
                for (int j = 0; j < FIELDS.length; j++) {
                    if (j > 0) writer.write(",");
                    String value = r == null ? "" : r.optString(FIELDS[j]);
                    if (j == 0) value = displayDate(value);
                    writer.write(csv(value));
                }
                writer.write("\n");
            }
            writer.flush();
            writer.close();
            Toast.makeText(this, "CSV FILE SAVED", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String csv(String value) {
        if (value == null) value = "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    // NAVIGATION
    private void goBack() {
        if ("ENTRY".equals(currentPage)) {
            hideKeyboard();
            if (entryFromRecords) {
                editing = -1;
                entryFromRecords = false;
                showRecords();
            } else {
                editing = -1;
                showHome();
            }
        } else if ("RECORDS".equals(currentPage) || "REPORTS".equals(currentPage)) {
            showHome();
        } else if ("HOME".equals(currentPage)) {
            new AlertDialog.Builder(this).setTitle("EXIT VIPIN SERVICE CRM")
                    .setMessage("क्या आप ऐप बंद करना चाहते हैं?")
                    .setNegativeButton("CANCEL", null)
                    .setPositiveButton("EXIT", (d, w) -> finish()).show();
        } else {
            new AlertDialog.Builder(this).setTitle("EXIT")
                    .setMessage("क्या आप ऐप बंद करना चाहते हैं?")
                    .setNegativeButton("CANCEL", null)
                    .setPositiveButton("EXIT", (d, w) -> finish()).show();
        }
    }

    @Override public void onBackPressed() {
        goBack();
    }
}
