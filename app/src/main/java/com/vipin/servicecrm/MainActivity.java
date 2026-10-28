
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

    private static final int REQ_PHOTO = 701;
    private static final int REQ_EXPORT = 702;
    private static final int REQ_PDF = 703;

    private final String[] FIELDS = {
        "DATE", "BILL NUMBER", "PRODUCT NAME", "AMC", "COLLECTION",
        "CUSTOMER NAME", "MOBILE NUMBER", "ADDRESS", "SPARE PARTS",
        "AMOUNT", "SPARE AMOUNT", "WARRANTY AMOUNT", "NET AMOUNT",
        "PAYMENT MODE", "REMARKS"
    };

    private final String[] PAYMENTS = {
        "CASH", "UPI", "PS UPI", "RKS UPI"
    };

    private final int navy = Color.rgb(31, 48, 79);
    private final int pale = Color.rgb(235, 244, 255);
    private final int ink = Color.rgb(39, 49, 67);
    private final int gold = Color.rgb(246, 218, 153);

    private LinearLayout root, recordsLayout;
    private ScrollView pageScroll;

    private EditText[] inputs = new EditText[15];
    private Spinner paymentSpinner;

    private JSONArray records = new JSONArray();

    private int editing = -1;
    private int photoSlot = 0;

    private String[] photoUris = new String[3];
    private String pdfUri = "";

    private EditText searchBox;
    private Spinner monthSpinner, yearSpinner;
    private TextView statusText;

    private String currentPage = "LOGIN";
    private boolean entryFromRecords = false;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        getWindow().setStatusBarColor(Color.rgb(30, 55, 125));
        getWindow().setNavigationBarColor(Color.rgb(45, 58, 86));
        getWindow().setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        );

        loadRecords();
        showLogin();
    }

    private int dp(float n) {
        return (int)(n * getResources()
            .getDisplayMetrics().density + .5f);
    }

    private GradientDrawable shape(
        int color, int radius, int stroke
    ) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));

        if (stroke != 0) {
            d.setStroke(dp(1), stroke);
        }
        return d;
    }

    private GradientDrawable gradient(
        int start, int end, int radius
    ) {
        GradientDrawable d = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{start, end}
        );
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

    private TextView text(
        String value, int size, int color, boolean bold
    ) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);

        if (bold) {
            t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return t;
    }

    private void addLogo(LinearLayout parent, int sizeDp) {
        ImageView logo = new ImageView(this);

        logo.setImageResource(
            R.drawable.vipin_service_crm_icon_512
        );

        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setAdjustViewBounds(true);

        LinearLayout.LayoutParams p =
            new LinearLayout.LayoutParams(
                dp(sizeDp), dp(sizeDp)
            );

        p.gravity = Gravity.CENTER_HORIZONTAL;
        p.bottomMargin = dp(10);

        parent.addView(logo, p);
    }

    // PAGE BASE

    private void pageBase(String title) {
        root = vertical();
        root.setPadding(dp(16), dp(20), dp(16), dp(35));
        root.setBackgroundColor(pale);

        pageScroll = new ScrollView(this);
        pageScroll.setFillViewport(true);
        pageScroll.setClipToPadding(false);
        pageScroll.setSmoothScrollingEnabled(true);
        pageScroll.setVerticalScrollBarEnabled(true);

        pageScroll.addView(root);
        setContentView(pageScroll);

        TextView header = text(title, 21, navy, true);
        header.setGravity(Gravity.CENTER);
        header.setPadding(
            dp(12), dp(15), dp(12), dp(15)
        );

        header.setBackground(
            shape(gold, 18, Color.rgb(225, 190, 112))
        );

        LinearLayout.LayoutParams hp =
            new LinearLayout.LayoutParams(-1, -2);

        hp.topMargin = dp(10);
        hp.bottomMargin = dp(18);

        root.addView(header, hp);
    }

    private EditText edit(String hint) {
        EditText e = new EditText(this);

        e.setSingleLine(true);
        e.setTextSize(15);
        e.setHint(hint);
        e.setTextColor(ink);
        e.setHintTextColor(Color.GRAY);

        e.setPadding(
            dp(12), dp(10), dp(12), dp(10)
        );

        e.setBackground(
            shape(Color.WHITE, 10, Color.rgb(213, 220, 231))
        );

        e.setSelectAllOnFocus(false);

        return e;
    }

    private void addButton(
        LinearLayout parent,
        String label,
        int color,
        Runnable action
    ) {
        Button b = new Button(this);

        b.setText(label);
        b.setTextColor(color == gold ? navy : Color.WHITE);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setBackground(shape(color, 12, 0));

        LinearLayout.LayoutParams p =
            new LinearLayout.LayoutParams(-1, dp(48));

        p.topMargin = dp(8);
        p.bottomMargin = dp(4);

        parent.addView(b, p);

        b.setOnClickListener(v -> action.run());
    }

    private void toast(String message) {
        Toast.makeText(
            this, message, Toast.LENGTH_SHORT
        ).show();
    }

    // STORAGE

    private void loadRecords() {
        String raw = getSharedPreferences(
            PREF, MODE_PRIVATE
        ).getString(RECORDS, "[]");

        try {
            records = new JSONArray(raw);
        } catch (Exception e) {
            records = new JSONArray();
        }
    }

    private void persist() {
        getSharedPreferences(PREF, MODE_PRIVATE)
            .edit()
            .putString(RECORDS, records.toString())
            .apply();
    }

    private String getPassword() {
        return getSharedPreferences(PREF, MODE_PRIVATE)
            .getString(PASSWORD, "1234");
    }

    // LOGIN

    private void showLogin() {
        currentPage = "LOGIN";

        pageBase("VIPIN SERVICE CRM");
        addLogo(root, 150);

        TextView welcome = text(
            "OFFLINE SERVICE RECORD MANAGER",
            14, navy, true
        );

        welcome.setGravity(Gravity.CENTER);
        root.addView(welcome);

        root.addView(
            text("ENTER PASSWORD", 13, ink, true)
        );

        EditText pass = edit("Password");
        pass.setInputType(129);

        root.addView(pass);

        addButton(root, "LOGIN", navy, () -> {
            if (pass.getText().toString().equals(getPassword())) {
                showHome();
            } else {
                toast("गलत पासवर्ड");
            }
        });

        addButton(
            root, "CHANGE PASSWORD", gold,
            this::changePassword
        );

        TextView note = text(
            "पहली बार का पासवर्ड: 1234",
            12, Color.GRAY, false
        );

        note.setGravity(Gravity.CENTER);
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

        new AlertDialog.Builder(this)
            .setTitle("CHANGE PASSWORD")
            .setView(box)
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("SAVE", (d, w) -> {

                if (!oldP.getText().toString().equals(getPassword())) {
                    toast("Current password गलत है");
                } else if (
                    newP.length() < 4 ||
                    !newP.getText().toString().equals(
                        confirm.getText().toString()
                    )
                ) {
                    toast("नया पासवर्ड जाँचें");
                } else {
                    getSharedPreferences(PREF, MODE_PRIVATE)
                        .edit()
                        .putString(
                            PASSWORD,
                            newP.getText().toString()
                        )
                        .apply();

                    toast("पासवर्ड बदल गया");
                }
            }).show();
    }

    // HOME

    private void homeButton(
        String label,
        String icon,
        int c1,
        int c2,
        Runnable action
    ) {
        LinearLayout button = horizontal();

        button.setPadding(
            dp(16), dp(12), dp(16), dp(12)
        );

        button.setBackground(gradient(c1, c2, 18));

        TextView symbol = text(icon, 25, c1, true);
        symbol.setGravity(Gravity.CENTER);
        symbol.setBackground(shape(Color.WHITE, 50, 0));

        button.addView(
            symbol,
            new LinearLayout.LayoutParams(dp(52), dp(52))
        );

        TextView title = text(
            label, 16, Color.WHITE, true
        );

        title.setPadding(dp(15), 0, 0, 0);

        button.addView(
            title,
            new LinearLayout.LayoutParams(0, -2, 1)
        );

        TextView arrow = text("›", 32, Color.WHITE, true);
        button.addView(arrow);

        LinearLayout.LayoutParams p =
            new LinearLayout.LayoutParams(-1, dp(80));

        p.topMargin = dp(8);
        p.bottomMargin = dp(8);

        root.addView(button, p);

        button.setOnClickListener(v -> action.run());
    }

    private void showHome() {
        currentPage = "HOME";
        editing = -1;
        entryFromRecords = false;

        root = vertical();

        root.setPadding(
            dp(16), dp(20), dp(16), dp(24)
        );

        root.setBackgroundColor(pale);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(root);

        setContentView(scroll);

        LinearLayout banner = vertical();

        banner.setPadding(
            dp(18), dp(22), dp(18), dp(22)
        );

        banner.setBackground(gradient(
            Color.rgb(38, 48, 180),
            Color.rgb(0, 105, 255), 22
        ));

        addLogo(banner, 125);

        TextView heading = text(
            "🔧 VIPIN SERVICE CRM",
            23, Color.WHITE, true
        );

        heading.setGravity(Gravity.CENTER);
        banner.addView(heading);

        TextView sub = text(
            "OFFLINE SERVICE RECORD MANAGER",
            13, Color.WHITE, true
        );

        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(10), 0, dp(10));

        banner.addView(sub);

        TextView count = text(
            "▤   TOTAL RECORDS   •   " + records.length(),
            16, Color.WHITE, true
        );

        count.setGravity(Gravity.CENTER);
        banner.addView(count);

        LinearLayout.LayoutParams bp =
            new LinearLayout.LayoutParams(-1, -2);

        bp.bottomMargin = dp(12);

        root.addView(banner, bp);

        homeButton(
            "ADD NEW RECORD", "＋",
            Color.rgb(0, 112, 255),
            Color.rgb(0, 145, 255),
            this::showEntry
        );

        homeButton(
            "SEARCH RECORD", "⌕",
            Color.rgb(0, 170, 180),
            Color.rgb(0, 180, 155),
            this::showRecords
        );

        homeButton(
            "MONTH / YEAR RECORDS", "▦",
            Color.rgb(103, 35, 235),
            Color.rgb(153, 48, 255),
            this::showRecords
        );

        homeButton(
            "EXPORT EXCEL / CSV", "▣",
            Color.rgb(255, 105, 0),
            Color.rgb(255, 175, 0),
            this::exportCSV
        );

        homeButton(
            "CHANGE PASSWORD", "⚙",
            Color.rgb(220, 0, 130),
            Color.rgb(255, 35, 105),
            this::changePassword
        );

        homeButton(
            "LOG OUT", "↪",
            Color.rgb(90, 100, 120),
            Color.rgb(115, 125, 145),
            this::showLogin
        );

        addTotalCards();
    }

    // LAST MONTH TOTAL

    private void addTotalCards() {
        TextView label = text(
            "LAST MONTH TOTAL       ▥",
            21, navy, true
        );

        label.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams lp =
            new LinearLayout.LayoutParams(-1, -2);

        lp.topMargin = dp(22);
        lp.bottomMargin = dp(10);

        root.addView(label, lp);

        String[] names = {
            "AMOUNT",
            "SPARE AMOUNT",
            "WARRANTY AMOUNT",
            "NET AMOUNT"
        };

        int[][] colors = {
            {0, Color.rgb(0, 125, 255), Color.rgb(0, 180, 255)},
            {0, Color.rgb(0, 170, 145), Color.rgb(0, 195, 160)},
            {0, Color.rgb(105, 35, 230), Color.rgb(150, 55, 255)},
            {0, Color.rgb(255, 105, 0), Color.rgb(255, 175, 0)}
        };

        double[] totals = lastMonthTotals();

        for (int row = 0; row < 2; row++) {
            LinearLayout line = horizontal();

            for (int col = 0; col < 2; col++) {
                int i = row * 2 + col;

                LinearLayout card = vertical();

                card.setGravity(Gravity.CENTER);
                card.setPadding(
                    dp(8), dp(12), dp(8), dp(12)
                );

                card.setBackground(gradient(
                    colors[i][1], colors[i][2], 17
                ));

                TextView name = text(
                    names[i], 12, Color.WHITE, true
                );

                name.setGravity(Gravity.CENTER);

                TextView value = text(
                    String.format(
                        Locale.US, "₹ %.2f", totals[i]
                    ),
                    19, Color.WHITE, true
                );

                value.setGravity(Gravity.CENTER);

                card.addView(name);
                card.addView(value);

                LinearLayout.LayoutParams cp =
                    new LinearLayout.LayoutParams(0, dp(100), 1);

                cp.setMargins(
                    dp(5), dp(5), dp(5), dp(5)
                );

                line.addView(card, cp);
            }

            root.addView(line);
        }
    }

    private double[] lastMonthTotals() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, -1);

        String month = String.format(
            Locale.US, "%04d-%02d",
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH) + 1
        );

        double[] t = new double[4];

        String[] keys = {
            "AMOUNT", "SPARE AMOUNT",
            "WARRANTY AMOUNT", "NET AMOUNT"
        };

        for (int i = 0; i < records.length(); i++) {
            JSONObject r = records.optJSONObject(i);

            if (r == null ||
                !r.optString("DATE").startsWith(month)) {
                continue;
            }

            for (int j = 0; j < keys.length; j++) {
                t[j] += number(r.optString(keys[j]));
            }
        }

        return t;
    }

    private double number(String s) {
        try {
            return Double.parseDouble(
                s.replace(",", "")
                 .replace("₹", "")
                 .trim()
            );
        } catch (Exception e) {
            return 0;
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

        pageBase(
            editing >= 0
                ? "EDIT SERVICE RECORD"
                : "NEW SERVICE RECORD"
        );

        for (int i = 0; i < FIELDS.length; i++) {

            TextView label = text(
                FIELDS[i], 12, navy, true
            );

            LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(-1, -2);

            p.topMargin = dp(7);

            root.addView(label, p);

            if (FIELDS[i].equals("PAYMENT MODE")) {

                paymentSpinner = new Spinner(this);

                ArrayAdapter<String> a =
                    new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        PAYMENTS
                    );

                paymentSpinner.setAdapter(a);
                root.addView(paymentSpinner);

            } else {

                inputs[i] = edit(FIELDS[i]);

                if (i == 0) {
                    inputs[i].setFocusable(false);

                    inputs[i].setOnClickListener(
                        v -> pickDate(inputs[0])
                    );
                }

                if (i == 9 || i == 10 || i == 11 ||
                    i == 12 || i == 3 || i == 4) {

                    inputs[i].setInputType(
                        8194 | 4096
                    );
                }

                if (i == 6) {
                    inputs[i].setInputType(3);
                }

                if (i == 7 || i == 8 || i == 14) {
                    inputs[i].setSingleLine(false);
                    inputs[i].setMinLines(1);
                    inputs[i].setMaxLines(3);
                }

                if (i == 0) {
                    inputs[i].setHint("SELECT DATE");
                }

                if (i != 0) {
                    inputs[i].setImeOptions(
                        i == 14
                            ? EditorInfo.IME_ACTION_DONE
                            : EditorInfo.IME_ACTION_NEXT
                    );
                }

                root.addView(inputs[i]);

                setupKeyboard(inputs[i], i);
            }
        }

        if (editing >= 0) {
            fillEditRecord();
        }

        addButton(
            root, "ATTACH PHOTO (UP TO 3)",
            Color.rgb(86, 105, 145),
            this::choosePhoto
        );

        addButton(
            root, "ATTACH PDF",
            Color.rgb(105, 75, 175),
            this::choosePDF
        );

        statusText = text(
            attachmentStatus(), 12, Color.GRAY, false
        );

        root.addView(statusText);

        addButton(
            root, "SAVE RECORD",
            Color.rgb(42, 133, 102),
            this::saveRecord
        );

        addButton(
            root, "CANCEL / BACK",
            gold,
            this::goBack
        );
    }

    // KEYBOARD FIX

    private void setupKeyboard(EditText field, int index) {

        field.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                scrollToInput(field);
            }
        });

        field.setOnEditorActionListener((v, action, event) -> {

            if (action == EditorInfo.IME_ACTION_NEXT ||
                action == EditorInfo.IME_ACTION_DONE) {

                int next = index + 1;

                while (next < FIELDS.length &&
                    (next == 13 || next == 0)) {
                    next++;
                }

                if (next < FIELDS.length) {
                    if (inputs[next] != null) {
                        inputs[next].requestFocus();
                        scrollToInput(inputs[next]);
                    }
                } else {
                    InputMethodManager imm =
                        (InputMethodManager)
                        getSystemService(INPUT_METHOD_SERVICE);

                    if (imm != null) {
                        imm.hideSoftInputFromWindow(
                            field.getWindowToken(), 0
                        );
                    }
                }

                return true;
            }

            return false;
        });
    }

    private void scrollToInput(EditText field) {
        if (pageScroll == null) return;

        pageScroll.postDelayed(() -> {
            if (pageScroll == null) return;

            try {
                int y = field.getTop() + root.getTop();

                pageScroll.smoothScrollTo(
                    0, Math.max(0, y - dp(35))
                );

            } catch (Exception ignored) {}
        }, 250);
    }

    private void pickDate(EditText target) {
        Calendar c = Calendar.getInstance();

        new DatePickerDialog(
            this,
            (view, year, month, day) -> {

                target.setText(String.format(
                    Locale.US,
                    "%04d-%02d-%02d",
                    year, month + 1, day
                ));

            },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void fillEditRecord() {
        JSONObject r = records.optJSONObject(editing);

        if (r == null) return;

        for (int i = 0; i < FIELDS.length; i++) {

            if (FIELDS[i].equals("PAYMENT MODE")) {

                String value = r.optString(
                    FIELDS[i], "CASH"
                );

                for (int j = 0; j < PAYMENTS.length; j++) {
                    if (PAYMENTS[j].equals(value)) {
                        paymentSpinner.setSelection(j);
                    }
                }

            } else if (inputs[i] != null) {
                inputs[i].setText(
                    r.optString(FIELDS[i])
                );
            }
        }

        for (int i = 0; i < 3; i++) {
            photoUris[i] = r.optString(
                "PHOTO" + (i + 1), ""
            );
        }

        pdfUri = r.optString("PDF", "");
    }

    // PHOTO ATTACHMENT

    private void choosePhoto() {
        int slot = -1;

        for (int i = 0; i < 3; i++) {
            if (photoUris[i] == null ||
                photoUris[i].isEmpty()) {
                slot = i;
                break;
            }
        }

        if (slot < 0) {

            new AlertDialog.Builder(this)
                .setTitle("SELECT PHOTO SLOT")
                .setItems(
                    new String[]{
                        "PHOTO 1", "PHOTO 2", "PHOTO 3"
                    },
                    (d, which) -> openPhoto(which)
                )
                .show();

        } else {
            openPhoto(slot);
        }
    }

    private void openPhoto(int slot) {
        photoSlot = slot;

        Intent intent = new Intent(
            Intent.ACTION_OPEN_DOCUMENT
        );

        intent.setType("image/*");

        intent.addCategory(
            Intent.CATEGORY_OPENABLE
        );

        intent.addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION |
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        );

        startActivityForResult(intent, REQ_PHOTO);
    }

    // PDF ATTACHMENT

    private void choosePDF() {
        Intent intent = new Intent(
            Intent.ACTION_OPEN_DOCUMENT
        );

        intent.setType("application/pdf");

        intent.addCategory(
            Intent.CATEGORY_OPENABLE
        );

        intent.addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION |
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        );

        startActivityForResult(intent, REQ_PDF);
    }

    private String attachmentStatus() {
        int count = 0;

        for (String s : photoUris) {
            if (s != null && !s.isEmpty()) {
                count++;
            }
        }

        return "PHOTOS: " + count + " / 3"
            + "     PDF: "
            + (pdfUri.isEmpty() ? "NOT ATTACHED" : "ATTACHED");
    }

    @Override
    protected void onActivityResult(
        int requestCode,
        int resultCode,
        Intent data
    ) {
        super.onActivityResult(
            requestCode, resultCode, data
        );

        if (resultCode != RESULT_OK ||
            data == null ||
            data.getData() == null) {
            return;
        }

        Uri uri = data.getData();

        if (requestCode == REQ_EXPORT) {
            writeCSV(uri);
            return;
        }

        try {
            getContentResolver().takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (Exception ignored) {}

        if (requestCode == REQ_PHOTO) {

            photoUris[photoSlot] = uri.toString();

        } else if (requestCode == REQ_PDF) {

            pdfUri = uri.toString();
        }

        if (statusText != null) {
            statusText.setText(attachmentStatus());
        }
    }

    // SAVE RECORD

    private void saveRecord() {
        JSONObject r = new JSONObject();

        try {
            for (int i = 0; i < FIELDS.length; i++) {

                if (FIELDS[i].equals("PAYMENT MODE")) {

                    r.put(
                        FIELDS[i],
                        paymentSpinner.getSelectedItem().toString()
                    );

                } else {

                    r.put(
                        FIELDS[i],
                        inputs[i].getText().toString().trim()
                    );
                }
            }

            for (int i = 0; i < 3; i++) {
                r.put(
                    "PHOTO" + (i + 1),
                    photoUris[i] == null ? "" : photoUris[i]
                );
            }

            r.put("PDF", pdfUri == null ? "" : pdfUri);

            if (r.optString("DATE").isEmpty()) {
                toast("DATE भरें");
                return;
            }

            if (editing >= 0) {
                records.put(editing, r);
            } else {
                records.put(r);
            }

            persist();

            editing = -1;
            entryFromRecords = false;

            toast("RECORD SAVED SUCCESSFULLY");

            showHome();

        } catch (Exception e) {

            Toast.makeText(
                this,
                "Save error: " + e.getMessage(),
                Toast.LENGTH_LONG
            ).show();
        }
    }

    // SEARCH / MONTH / YEAR RECORDS

    private void showRecords() {
        currentPage = "RECORDS";

        pageBase("SERVICE RECORDS");

        searchBox = edit(
            "SEARCH: NAME / MOBILE / BILL / PRODUCT"
        );

        root.addView(searchBox);

        LinearLayout filters = horizontal();

        monthSpinner = new Spinner(this);
        yearSpinner = new Spinner(this);

        String[] months = {
            "ALL MONTHS",
            "01", "02", "03", "04", "05", "06",
            "07", "08", "09", "10", "11", "12"
        };

        ArrayAdapter<String> ma = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            months
        );

        monthSpinner.setAdapter(ma);

        ArrayList<String> years = new ArrayList<>();

        years.add("ALL YEARS");

        int now = Calendar.getInstance().get(Calendar.YEAR);

        for (int y = now + 1; y >= now - 10; y--) {
            years.add(String.valueOf(y));
        }

        yearSpinner.setAdapter(
            new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                years
            )
        );

        filters.addView(
            monthSpinner,
            new LinearLayout.LayoutParams(0, dp(48), 1)
        );

        filters.addView(
            yearSpinner,
            new LinearLayout.LayoutParams(0, dp(48), 1)
        );

        root.addView(filters);

        recordsLayout = vertical();
        root.addView(recordsLayout);

        searchBox.addTextChangedListener(
            new TextWatcher() {

                public void beforeTextChanged(
                    CharSequence s, int st, int c, int a
                ) {}

                public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count
                ) {
                    renderRecords();
                }

                public void afterTextChanged(Editable e) {}
            }
        );

        monthSpinner.setOnItemSelectedListener(
            new SimpleSelect(this::renderRecords)
        );

        yearSpinner.setOnItemSelectedListener(
            new SimpleSelect(this::renderRecords)
        );

        addButton(
            root, "BACK TO HOME",
            gold, this::showHome
        );

        renderRecords();
    }

    private class SimpleSelect
        implements AdapterView.OnItemSelectedListener {

        Runnable run;

        SimpleSelect(Runnable r) {
            run = r;
        }

        public void onItemSelected(
            AdapterView<?> p,
            View v,
            int pos,
            long id
        ) {
            run.run();
        }

        public void onNothingSelected(AdapterView<?> p) {}
    }

    // FULL RECORD DISPLAY

    private void renderRecords() {
        if (recordsLayout == null) return;

        recordsLayout.removeAllViews();

        String q = searchBox == null
            ? ""
            : searchBox.getText().toString()
                .toLowerCase(Locale.ROOT).trim();

        String month = monthSpinner == null
            ? "ALL MONTHS"
            : monthSpinner.getSelectedItem().toString();

        String year = yearSpinner == null
            ? "ALL YEARS"
            : yearSpinner.getSelectedItem().toString();

        int shown = 0;

        for (int i = records.length() - 1; i >= 0; i--) {

            JSONObject r = records.optJSONObject(i);

            if (r == null) continue;

            String date = r.optString("DATE");

            if (!"ALL MONTHS".equals(month) &&
                !date.matches("\\d{4}-" + month + ".*")) {
                continue;
            }

            if (!"ALL YEARS".equals(year) &&
                !date.startsWith(year + "-")) {
                continue;
            }

            StringBuilder all = new StringBuilder();

            for (String field : FIELDS) {
                all.append(r.optString(field))
                   .append(" ");
            }

            String hay = all.toString()
                .toLowerCase(Locale.ROOT);

            if (!hay.contains(q)) continue;

            shown++;

            final int index = i;

            LinearLayout card = vertical();

            card.setPadding(
                dp(12), dp(12), dp(12), dp(12)
            );

            card.setBackground(
                shape(
                    Color.WHITE,
                    14,
                    Color.rgb(218, 224, 233)
                )
            );

            TextView title = text(
                r.optString("CUSTOMER NAME")
                    + " | " + date,
                15, navy, true
            );

            card.addView(title);

            for (String field : FIELDS) {

                if (field.equals("DATE") ||
                    field.equals("CUSTOMER NAME")) {
                    continue;
                }

                String value = r.optString(field);

                TextView line = text(
                    field + ": " + value,
                    12, ink, true
                );

                line.setPadding(0, dp(2), 0, dp(2));

                card.addView(line);
            }

            // ACTION BUTTONS

            LinearLayout row1 = horizontal();

            Button copy = smallButton("COPY TEXT");
            Button edit = smallButton("EDIT");
            Button del = smallButton("DELETE");

            row1.addView(
                copy,
                new LinearLayout.LayoutParams(0, dp(43), 1)
            );

            row1.addView(
                edit,
                new LinearLayout.LayoutParams(0, dp(43), 1)
            );

            row1.addView(
                del,
                new LinearLayout.LayoutParams(0, dp(43), 1)
            );

            card.addView(row1);

            LinearLayout row2 = horizontal();

            Button photos = smallButton("VIEW PHOTOS");
            Button pdf = smallButton("OPEN PDF");

            row2.addView(
                photos,
                new LinearLayout.LayoutParams(0, dp(43), 1)
            );

            row2.addView(
                pdf,
                new LinearLayout.LayoutParams(0, dp(43), 1)
            );

            card.addView(row2);

            copy.setOnClickListener(v ->
                copyRecord(r)
            );

            edit.setOnClickListener(v -> {
                editing = index;
                entryFromRecords = true;
                photoUris = new String[3];
                pdfUri = "";
                showEntry();
            });

            del.setOnClickListener(v ->
                confirmDelete(index)
            );

            photos.setOnClickListener(v ->
                showPhotos(r)
            );

            pdf.setOnClickListener(v ->
                openSavedPDF(r)
            );

            LinearLayout.LayoutParams cp =
                new LinearLayout.LayoutParams(-1, -2);

            cp.topMargin = dp(10);

            recordsLayout.addView(card, cp);
        }

        if (shown == 0) {
            recordsLayout.addView(
                text(
                    "कोई रिकॉर्ड नहीं मिला",
                    14, Color.GRAY, false
                )
            );
        }
    }

    // COPY COMPLETE RECORD

    private void copyRecord(JSONObject r) {
        StringBuilder b = new StringBuilder();

        b.append("VIPIN SERVICE CRM\n");
        b.append("----------------------\n");

        for (String field : FIELDS) {
            b.append(field)
             .append(": ")
             .append(r.optString(field))
             .append("\n");
        }

        ClipboardManager clipboard =
            (ClipboardManager)
            getSystemService(CLIPBOARD_SERVICE);

        if (clipboard != null) {
            clipboard.setPrimaryClip(
                ClipData.newPlainText(
                    "SERVICE RECORD",
                    b.toString()
                )
            );

            toast("पूरा रिकॉर्ड COPY हो गया");
        }
    }

    private Button smallButton(String s) {
        Button b = new Button(this);

        b.setText(s);
        b.setTextSize(10);
        b.setAllCaps(false);
        b.setPadding(0, 0, 0, 0);

        b.setBackground(
            shape(gold, 9, 0)
        );

        b.setTextColor(navy);

        return b;
    }

    private void confirmDelete(int index) {
        new AlertDialog.Builder(this)
            .setMessage("यह रिकॉर्ड DELETE करें?")
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("DELETE", (d, w) -> {

                JSONArray next = new JSONArray();

                for (int k = 0; k < records.length(); k++) {
                    if (k != index) {
                        next.put(records.optJSONObject(k));
                    }
                }

                records = next;
                persist();

                renderRecords();

                toast("RECORD DELETED");
            })
            .show();
    }

    // PHOTO PREVIEW

    private void showPhotos(JSONObject r) {
        LinearLayout content = vertical();

        content.setPadding(
            dp(12), dp(8), dp(12), dp(8)
        );

        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);

        int count = 0;

        for (int i = 1; i <= 3; i++) {

            String value = r.optString("PHOTO" + i);

            if (value.isEmpty()) continue;

            count++;

            TextView label = text(
                "PHOTO " + i,
                14, navy, true
            );

            content.addView(label);

            try {
                ImageView image = new ImageView(this);

                image.setImageURI(Uri.parse(value));

                image.setScaleType(
                    ImageView.ScaleType.FIT_CENTER
                );

                image.setBackground(
                    shape(pale, 10, Color.LTGRAY)
                );

                LinearLayout.LayoutParams ip =
                    new LinearLayout.LayoutParams(-1, dp(220));

                ip.bottomMargin = dp(8);

                content.addView(image, ip);

                image.setOnClickListener(v ->
                    showLargePhoto(value)
                );

            } catch (Exception e) {

                content.addView(
                    text(
                        "PHOTO OPEN ERROR",
                        12, Color.RED, false
                    )
                );
            }
        }

        if (count == 0) {
            content.addView(
                text(
                    "इस रिकॉर्ड में फोटो नहीं है।",
                    14, Color.GRAY, false
                )
            );
        }

        new AlertDialog.Builder(this)
            .setTitle("ATTACHED PHOTOS")
            .setView(scroll)
            .setPositiveButton("CLOSE", null)
            .show();
    }

    private void showLargePhoto(String value) {
        try {
            ImageView image = new ImageView(this);

            image.setImageURI(Uri.parse(value));

            image.setScaleType(
                ImageView.ScaleType.FIT_CENTER
            );

            image.setBackgroundColor(Color.BLACK);

            new AlertDialog.Builder(this)
                .setTitle("PHOTO PREVIEW")
                .setView(image)
                .setPositiveButton("CLOSE", null)
                .show();

        } catch (Exception e) {
            toast("PHOTO OPEN नहीं हो सकी");
        }
    }

    // OPEN SAVED PDF

    private void openSavedPDF(JSONObject r) {
        String value = r.optString("PDF", "");

        if (value.isEmpty()) {
            toast("इस रिकॉर्ड में PDF नहीं है");
            return;
        }

        try {
            Intent intent = new Intent(
                Intent.ACTION_VIEW
            );

            intent.setDataAndType(
                Uri.parse(value),
                "application/pdf"
            );

            intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            startActivity(intent);

        } catch (Exception e) {
            toast("PDF देखने के लिए PDF Viewer इंस्टॉल करें");
        }
    }

    // CSV EXPORT

    private void exportCSV() {
        Intent intent = new Intent(
            Intent.ACTION_CREATE_DOCUMENT
        );

        intent.addCategory(
            Intent.CATEGORY_OPENABLE
        );

        intent.setType("text/csv");

        intent.putExtra(
            Intent.EXTRA_TITLE,
            "VIPIN_SERVICE_CRM.csv"
        );

        startActivityForResult(intent, REQ_EXPORT);
    }

    private void writeCSV(Uri uri) {
        try {
            OutputStream out =
                getContentResolver().openOutputStream(uri);

            if (out == null) {
                throw new IOException("File open failed");
            }

            OutputStreamWriter w =
                new OutputStreamWriter(out, "UTF-8");

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

                    w.write(
                        csv(
                            r == null
                                ? ""
                                : r.optString(FIELDS[j])
                        )
                    );
                }

                w.write("\n");
            }

            w.flush();
            w.close();

            Toast.makeText(
                this,
                "CSV FILE SAVED",
                Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {

            Toast.makeText(
                this,
                "Export error: " + e.getMessage(),
                Toast.LENGTH_LONG
            ).show();
        }
    }

    private String csv(String s) {
        if (s == null) s = "";

        return "\"" +
            s.replace("\"", "\"\"") +
            "\"";
    }

    // NAVIGATION

    private void goBack() {

        if ("ENTRY".equals(currentPage)) {

            if (entryFromRecords) {
                editing = -1;
                entryFromRecords = false;
                showRecords();
            } else {
                showHome();
            }

        } else if ("RECORDS".equals(currentPage)) {

            showHome();

        } else if ("HOME".equals(currentPage)) {

            new AlertDialog.Builder(this)
                .setTitle("EXIT VIPIN SERVICE CRM")
                .setMessage("क्या आप ऐप बंद करना चाहते हैं?")
                .setNegativeButton("CANCEL", null)
                .setPositiveButton(
                    "EXIT", (d, w) -> finish()
                )
                .show();

        } else {

            new AlertDialog.Builder(this)
                .setTitle("EXIT")
                .setMessage("क्या आप ऐप बंद करना चाहते हैं?")
                .setNegativeButton("CANCEL", null)
                .setPositiveButton(
                    "EXIT", (d, w) -> finish()
                )
                .show();
        }
    }

    @Override
    public void onBackPressed() {
        goBack();
    }
}
