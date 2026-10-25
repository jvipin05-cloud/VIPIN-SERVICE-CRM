package com.vipin.servicecrm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String PREF = "vipin_crm_data";
    private static final String KEY = "records";
    private static final String PASSWORD = "Jvipin";
    private static final int EXPORT_REQUEST = 701;

    private final String[] FIELDS = {
        "DATE", "BILL NUMBER", "PRODUCT NAME", "AMC",
        "COLLECTION", "CUSTOMER NAME", "MOBILE NUMBER",
        "ADDRESS", "SPARE PARTS", "AMOUNT",
        "SPARE AMOUNT", "WARRANTY AMOUNT",
        "NET AMOUNT", "PAYMENT MODE", "REMARKS"
    };

    private final String[] PAYMENTS = {
        "CASH", "UPI", "PS UPI", "RKS UPI"
    };

    private JSONArray records = new JSONArray();

    private LinearLayout page, listArea, totalsArea;
    private EditText searchBox;
    private Spinner monthSpinner, yearSpinner;

    private String selectedMonth = "ALL";
    private String selectedYear = "ALL";

    private final int navy = Color.rgb(25, 42, 86);
    private final int blue = Color.rgb(39, 105, 220);
    private final int teal = Color.rgb(0, 150, 136);
    private final int pale = Color.rgb(243, 247, 255);
    private final int ink = Color.rgb(35, 45, 65);

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        getWindow().setStatusBarColor(navy);
        getWindow().setNavigationBarColor(navy);

        loadRecords();
        showLogin();
    }

    private int dp(int value) {
        return (int) (value *
            getResources().getDisplayMetrics().density);
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private void pad(View v, int all) {
        int p = dp(all);
        v.setPadding(p, p, p, p);
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

    private Button button(String label, int color) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setAllCaps(false);
        b.setBackground(shape(color, 14));
        return b;
    }

    private void showLogin() {
        LinearLayout root = vertical();
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(pale);
        pad(root, 26);

        TextView logo = text(
            "VIPIN\nSERVICE CRM", 29, Color.WHITE, true
        );
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(shape(navy, 24));
        pad(logo, 28);

        root.addView(logo,
            new LinearLayout.LayoutParams(-1, -2));

        TextView caption = text(
            "SECURE LOGIN", 18, navy, true
        );
        caption.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams cp =
            new LinearLayout.LayoutParams(-1, -2);
        cp.topMargin = dp(28);
        root.addView(caption, cp);

        EditText password = new EditText(this);
        password.setSingleLine(true);
        password.setHint("ENTER PASSWORD");
        password.setInputType(
            InputType.TYPE_CLASS_TEXT |
            InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        password.setBackground(shape(Color.WHITE, 14));
        pad(password, 14);

        LinearLayout.LayoutParams pp =
            new LinearLayout.LayoutParams(-1, -2);
        pp.topMargin = dp(16);
        root.addView(password, pp);

        Button login = button("OPEN APP", teal);

        LinearLayout.LayoutParams bp =
            new LinearLayout.LayoutParams(-1, -2);
        bp.topMargin = dp(18);
        root.addView(login, bp);

        login.setOnClickListener(v -> {
            if (PASSWORD.equals(
                password.getText().toString()
            )) {
                showCRM();
            } else {
                password.setError("WRONG PASSWORD");
                Toast.makeText(
                    this, "PASSWORD GALAT HAI",
                    Toast.LENGTH_SHORT
                ).show();
            }
        });

        setContentView(root);
    }

    private void showCRM() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        page = vertical();
        page.setBackgroundColor(pale);
        pad(page, 14);
        scroll.addView(page);

        LinearLayout header = vertical();
        header.setBackground(shape(navy, 20));
        pad(header, 18);

        header.addView(text(
            "VIPIN SERVICE CRM", 23, Color.WHITE, true
        ));

        TextView sub = text(
            "OFFLINE CUSTOMER & SERVICE RECORDS",
            11, Color.rgb(210, 225, 255), true
        );

        LinearLayout.LayoutParams sp =
            new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = dp(5);
        header.addView(sub, sp);
        page.addView(header);

        LinearLayout actions = horizontal();

        Button add = button("+ ADD NEW RECORD", teal);
        Button export = button("EXPORT EXCEL CSV", blue);

        actions.addView(add,
            new LinearLayout.LayoutParams(0, dp(52), 1));

        LinearLayout.LayoutParams ep =
            new LinearLayout.LayoutParams(0, dp(52), 1);
        ep.leftMargin = dp(8);
        actions.addView(export, ep);

        LinearLayout.LayoutParams ap =
            new LinearLayout.LayoutParams(-1, -2);
        ap.topMargin = dp(12);
        page.addView(actions, ap);

        add.setOnClickListener(v -> showRecordForm(-1));
        export.setOnClickListener(v -> exportCsv());

        TextView totalTitle = text(
            "LAST MONTH TOTAL", 17, navy, true
        );

        LinearLayout.LayoutParams tp =
            new LinearLayout.LayoutParams(-1, -2);
        tp.topMargin = dp(18);
        page.addView(totalTitle, tp);

        totalsArea = vertical();
        page.addView(totalsArea);
        buildTotals();

        TextView filterTitle = text(
            "RECORD SEARCH & FILTER", 16, navy, true
        );

        LinearLayout.LayoutParams fp =
            new LinearLayout.LayoutParams(-1, -2);
        fp.topMargin = dp(18);
        page.addView(filterTitle, fp);

        searchBox = new EditText(this);
        searchBox.setSingleLine(true);
        searchBox.setHint(
            "SEARCH BILL / CUSTOMER / MOBILE / PRODUCT"
        );
        searchBox.setTextSize(13);
        searchBox.setBackground(shape(Color.WHITE, 12));
        pad(searchBox, 12);

        LinearLayout.LayoutParams sbp =
            new LinearLayout.LayoutParams(-1, -2);
        sbp.topMargin = dp(8);
        page.addView(searchBox, sbp);

        LinearLayout filters = horizontal();

        monthSpinner = new Spinner(this);
        yearSpinner = new Spinner(this);

        filters.addView(monthSpinner,
            new LinearLayout.LayoutParams(0, dp(48), 1));
        filters.addView(yearSpinner,
            new LinearLayout.LayoutParams(0, dp(48), 1));

        LinearLayout.LayoutParams flp =
            new LinearLayout.LayoutParams(-1, -2);
        flp.topMargin = dp(6);
        page.addView(filters, flp);

        setupFilters();

        TextView recordsTitle = text(
            "SAVED RECORDS", 17, navy, true
        );

        LinearLayout.LayoutParams rtp =
            new LinearLayout.LayoutParams(-1, -2);
        rtp.topMargin = dp(18);
        page.addView(recordsTitle, rtp);

        listArea = vertical();
        page.addView(listArea);

        searchBox.addTextChangedListener(
            new android.text.TextWatcher() {
                public void beforeTextChanged(
                    CharSequence s, int start,
                    int count, int after
                ) {}

                public void onTextChanged(
                    CharSequence s, int start,
                    int before, int count
                ) {
                    refreshList();
                }

                public void afterTextChanged(
                    android.text.Editable e
                ) {}
            }
        );

        refreshList();
        setContentView(scroll);
            }
        private void buildTotals() {
        totalsArea.removeAllViews();

        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, -1);

        String month = new SimpleDateFormat(
            "MM", Locale.US
        ).format(c.getTime());

        String year = new SimpleDateFormat(
            "yyyy", Locale.US
        ).format(c.getTime());

        String[] fields = {
            "AMOUNT", "SPARE AMOUNT",
            "WARRANTY AMOUNT", "NET AMOUNT"
        };

        int[] colors = {
            blue, teal,
            Color.rgb(137, 75, 190),
            Color.rgb(220, 115, 30)
        };

        for (int i = 0; i < fields.length; i += 2) {
            LinearLayout row = horizontal();

            for (int j = i; j < i + 2; j++) {
                double total = 0;

                for (int k = 0; k < records.length(); k++) {
                    JSONObject o = records.optJSONObject(k);
                    if (o == null) continue;

                    String date = o.optString("DATE", "");

                    if (date.length() == 10
                        && date.substring(3, 5).equals(month)
                        && date.substring(6).equals(year)) {
                        total += number(
                            o.optString(fields[j], "0")
                        );
                    }
                }

                LinearLayout card = vertical();
                card.setBackground(shape(colors[j], 14));
                pad(card, 10);

                card.addView(text(
                    fields[j], 11, Color.WHITE, true
                ));

                TextView amount = text(
                    "₹ " + money(total),
                    17, Color.WHITE, true
                );

                LinearLayout.LayoutParams vp =
                    new LinearLayout.LayoutParams(-1, -2);
                vp.topMargin = dp(5);
                card.addView(amount, vp);

                LinearLayout.LayoutParams cp =
                    new LinearLayout.LayoutParams(0, dp(78), 1);
                if (j == i + 1) cp.leftMargin = dp(8);
                row.addView(card, cp);
            }

            LinearLayout.LayoutParams rp =
                new LinearLayout.LayoutParams(-1, -2);
            rp.topMargin = dp(8);
            totalsArea.addView(row, rp);
        }
    }

    private void setupFilters() {
        String[] months = {
            "ALL", "01", "02", "03", "04", "05",
            "06", "07", "08", "09", "10", "11", "12"
        };

        String[] years = new String[12];
        years[0] = "ALL";

        int current = Calendar.getInstance()
            .get(Calendar.YEAR);

        for (int i = 1; i < years.length; i++) {
            years[i] = String.valueOf(current - i + 1);
        }

        ArrayAdapter<String> ma = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            months
        );

        ArrayAdapter<String> ya = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            years
        );

        monthSpinner.setAdapter(ma);
        yearSpinner.setAdapter(ya);

        monthSpinner.setOnItemSelectedListener(
            new SimpleSelect(() -> {
                selectedMonth = String.valueOf(
                    monthSpinner.getSelectedItem()
                );
                refreshList();
            })
        );

        yearSpinner.setOnItemSelectedListener(
            new SimpleSelect(() -> {
                selectedYear = String.valueOf(
                    yearSpinner.getSelectedItem()
                );
                refreshList();
            })
        );
    }

    private class SimpleSelect
        implements AdapterView.OnItemSelectedListener {

        private final Runnable action;

        SimpleSelect(Runnable r) {
            action = r;
        }

        public void onItemSelected(
            AdapterView<?> p, View v,
            int position, long id
        ) {
            action.run();
        }

        public void onNothingSelected(AdapterView<?> p) {}
    }

    private void refreshList() {
        if (listArea == null) return;

        listArea.removeAllViews();

        String query = searchBox == null ? "" :
            searchBox.getText().toString()
                .trim().toLowerCase(Locale.ROOT);

        int shown = 0;

        for (int i = records.length() - 1; i >= 0; i--) {
            JSONObject o = records.optJSONObject(i);
            if (o == null) continue;

            String date = o.optString("DATE", "");

            if (!"ALL".equals(selectedMonth)
                && (date.length() < 5
                || !date.substring(3, 5).equals(selectedMonth))) {
                continue;
            }

            if (!"ALL".equals(selectedYear)
                && (date.length() < 10
                || !date.substring(6).equals(selectedYear))) {
                continue;
            }

            if (!o.toString().toLowerCase(Locale.ROOT)
                .contains(query)) {
                continue;
            }

            listArea.addView(recordCard(o, i));
            shown++;
        }

        if (shown == 0) {
            TextView empty = text(
                "NO RECORD FOUND", 14, ink, true
            );
            empty.setGravity(Gravity.CENTER);
            pad(empty, 24);
            listArea.addView(empty);
        }
    }

    private View recordCard(JSONObject o, int index) {
        LinearLayout card = vertical();
        card.setBackground(shape(Color.WHITE, 16));
        pad(card, 13);

        LinearLayout top = horizontal();

        String customer = o.optString(
            "CUSTOMER NAME", "NO NAME"
        );

        top.addView(text(
            customer, 16, navy, true
        ), new LinearLayout.LayoutParams(0, -2, 1));

        top.addView(text(
            o.optString("DATE", ""),
            12, teal, true
        ));

        card.addView(top);

        String[] important = {
            "BILL NUMBER", "PRODUCT NAME",
            "MOBILE NUMBER", "AMOUNT",
            "SPARE AMOUNT", "WARRANTY AMOUNT",
            "NET AMOUNT", "PAYMENT MODE"
        };

        for (String f : important) {
            String value = o.optString(f, "");

            if (!value.isEmpty()) {
                TextView line = text(
                    f + " : " + value, 12, ink, false
                );

                LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(-1, -2);
                lp.topMargin = dp(4);
                card.addView(line, lp);
            }
        }

        LinearLayout buttons = horizontal();

        Button edit = button("EDIT", blue);
        Button delete = button(
            "DELETE", Color.rgb(190, 55, 65)
        );

        buttons.addView(edit,
            new LinearLayout.LayoutParams(0, dp(42), 1));

        LinearLayout.LayoutParams delp =
            new LinearLayout.LayoutParams(0, dp(42), 1);
        delp.leftMargin = dp(8);
        buttons.addView(delete, delp);

        LinearLayout.LayoutParams bp =
            new LinearLayout.LayoutParams(-1, -2);
        bp.topMargin = dp(9);
        card.addView(buttons, bp);

        edit.setOnClickListener(v -> showRecordForm(index));

        delete.setOnClickListener(v ->
            new AlertDialog.Builder(this)
                .setTitle("DELETE RECORD?")
                .setMessage("THIS RECORD WILL BE DELETED.")
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("DELETE", (d, w) -> {
                    records.remove(index);
                    saveRecords();
                    refreshList();
                    buildTotals();
                })
                .show()
        );

        LinearLayout.LayoutParams cp =
            new LinearLayout.LayoutParams(-1, -2);
        cp.bottomMargin = dp(10);
        card.setLayoutParams(cp);

        return card;
    }
        private void showRecordForm(int editIndex) {
        JSONObject old = editIndex >= 0
            ? records.optJSONObject(editIndex) : null;

        ScrollView scroll = new ScrollView(this);
        LinearLayout form = vertical();
        pad(form, 16);
        form.setBackgroundColor(pale);

        EditText[] inputs = new EditText[FIELDS.length];
        Spinner payment = null;

        for (int i = 0; i < FIELDS.length; i++) {
            String field = FIELDS[i];

            TextView label = text(
                field, 13, navy, true
            );

            LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = dp(9);
            form.addView(label, lp);

            if ("PAYMENT MODE".equals(field)) {
                payment = new Spinner(this);

                ArrayAdapter<String> adapter =
                    new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        PAYMENTS
                    );

                payment.setAdapter(adapter);

                String previous = old == null
                    ? "CASH" : old.optString(field, "CASH");

                for (int k = 0; k < PAYMENTS.length; k++) {
                    if (PAYMENTS[k].equals(previous)) {
                        payment.setSelection(k);
                    }
                }

                form.addView(
                    payment,
                    new LinearLayout.LayoutParams(-1, dp(48))
                );

            } else {
                EditText input = new EditText(this);
                inputs[i] = input;

                input.setTextSize(14);
                input.setHint("ENTER " + field);
                input.setBackground(shape(Color.WHITE, 10));
                pad(input, 10);

                boolean multiline =
                    "ADDRESS".equals(field)
                    || "REMARKS".equals(field)
                    || "SPARE PARTS".equals(field);

                input.setSingleLine(!multiline);

                if (field.contains("AMOUNT")
                    || "AMC".equals(field)
                    || "COLLECTION".equals(field)) {
                    input.setInputType(
                        InputType.TYPE_CLASS_NUMBER
                        | InputType.TYPE_NUMBER_FLAG_DECIMAL
                    );
                }

                if (old != null) {
                    input.setText(old.optString(field, ""));
                }

                if ("DATE".equals(field)) {
                    input.setFocusable(false);
                    input.setClickable(true);

                    if (input.getText().length() == 0) {
                        input.setText(
                            new SimpleDateFormat(
                                "dd-MM-yyyy", Locale.US
                            ).format(new Date())
                        );
                    }

                    input.setOnClickListener(
                        v -> showDatePicker(input)
                    );
                }

                int height = multiline ? 76 : 48;

                form.addView(
                    input,
                    new LinearLayout.LayoutParams(-1, dp(height))
                );
            }
        }

        scroll.addView(form);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(
                editIndex >= 0 ? "EDIT RECORD" : "ADD NEW RECORD"
            )
            .setView(scroll)
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("SAVE", null)
            .create();

        Spinner finalPayment = payment;

        dialog.setOnShowListener(x ->
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    try {
                        JSONObject item = new JSONObject();

                        for (int i = 0; i < FIELDS.length; i++) {
                            String f = FIELDS[i];

                            if ("PAYMENT MODE".equals(f)) {
                                item.put(
                                    f,
                                    finalPayment.getSelectedItem().toString()
                                );
                            } else {
                                item.put(
                                    f,
                                    inputs[i].getText().toString().trim()
                                );
                            }
                        }

                        if (item.optString("DATE").isEmpty()) {
                            inputs[0].setError("DATE REQUIRED");
                            return;
                        }

                        if (editIndex >= 0) {
                            records.put(editIndex, item);
                        } else {
                            records.put(item);
                        }

                        saveRecords();
                        dialog.dismiss();
                        refreshList();
                        buildTotals();

                        Toast.makeText(
                            this, "RECORD SAVED",
                            Toast.LENGTH_SHORT
                        ).show();

                    } catch (Exception e) {
                        Toast.makeText(
                            this, "SAVE ERROR: " + e.getMessage(),
                            Toast.LENGTH_LONG
                        ).show();
                    }
                })
        );

        dialog.show();
    }

    private void showDatePicker(EditText target) {
        Calendar c = Calendar.getInstance();

        try {
            Date d = new SimpleDateFormat(
                "dd-MM-yyyy", Locale.US
            ).parse(target.getText().toString());

            if (d != null) c.setTime(d);

        } catch (Exception ignored) {}

        new DatePickerDialog(
            this,
            (view, year, month, day) ->
                target.setText(
                    String.format(
                        Locale.US,
                        "%02d-%02d-%04d",
                        day, month + 1, year
                    )
                ),
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void exportCsv() {
        Intent intent = new Intent(
            Intent.ACTION_CREATE_DOCUMENT
        );

        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(
            Intent.EXTRA_TITLE,
            "VIPIN_SERVICE_CRM.csv"
        );

        startActivityForResult(intent, EXPORT_REQUEST);
    }

    @Override
    protected void onActivityResult(
        int requestCode, int resultCode, Intent data
    ) {
        super.onActivityResult(
            requestCode, resultCode, data
        );

        if (requestCode == EXPORT_REQUEST
            && resultCode == RESULT_OK
            && data != null) {

            Uri uri = data.getData();

            try (
                OutputStream out =
                    getContentResolver().openOutputStream(uri)
            ) {
                StringBuilder csv = new StringBuilder();

                for (int i = 0; i < FIELDS.length; i++) {
                    if (i > 0) csv.append(",");
                    csv.append(csvCell(FIELDS[i]));
                }

                csv.append("\n");

                for (int r = 0; r < records.length(); r++) {
                    JSONObject o = records.optJSONObject(r);

                    for (int i = 0; i < FIELDS.length; i++) {
                        if (i > 0) csv.append(",");
                        csv.append(
                            csvCell(
                                o == null ? "" :
                                o.optString(FIELDS[i], "")
                            )
                        );
                    }

                    csv.append("\n");
                }

                out.write(
                    csv.toString().getBytes(StandardCharsets.UTF_8)
                );

                Toast.makeText(
                    this, "CSV FILE SAVED",
                    Toast.LENGTH_LONG
                ).show();

            } catch (Exception e) {
                Toast.makeText(
                    this, "EXPORT FAILED: " + e.getMessage(),
                    Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    private String csvCell(String value) {
        if (value == null) value = "";

        return "\"" +
            value.replace("\"", "\"\"") +
            "\"";
    }

    private double number(String value) {
        try {
            return Double.parseDouble(
                value.replace(",", "").trim()
            );
        } catch (Exception e) {
            return 0;
        }
    }

    private String money(double value) {
        return String.format(
            Locale.US, "%.2f", value
        );
    }

    private void loadRecords() {
        SharedPreferences p = getSharedPreferences(
            PREF, MODE_PRIVATE
        );

        try {
            records = new JSONArray(
                p.getString(KEY, "[]")
            );
        } catch (Exception e) {
            records = new JSONArray();
        }
    }

    private void saveRecords() {
        getSharedPreferences(PREF, MODE_PRIVATE)
            .edit()
            .putString(KEY, records.toString())
            .apply();
    }
        }
