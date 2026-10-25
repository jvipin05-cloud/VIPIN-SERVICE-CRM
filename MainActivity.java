
package com.vipin.servicecrm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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

    private final int NAVY = Color.rgb(25, 45, 90);
    private final int BLUE = Color.rgb(35, 105, 190);
    private final int PALE = Color.rgb(240, 246, 255);
    private final int WHITE = Color.WHITE;
    private final int DARK = Color.rgb(35, 45, 60);

    private final String[] FIELDS = {
        "DATE",
        "BILL NUMBER",
        "PRODUCT NAME",
        "AMC",
        "COLLECTION",
        "CUSTOMER NAME",
        "MOBILE NUMBER",
        "ADDRESS",
        "SPARE PARTS",
        "AMOUNT",
        "SPARE AMOUNT",
        "WARRANTY AMOUNT",
        "NET AMOUNT",
        "PAYMENT MODE",
        "REMARKS"
    };

    private final String[] PAYMENTS = {
        "CASH", "UPI", "PS UPI", "RKS UPI"
    };

    private SharedPreferences prefs;
    private JSONArray records = new JSONArray();

    private LinearLayout root;
    private EditText[] inputs;
    private Spinner paymentSpinner;

    private String selectedMonth = "";
    private String selectedYear = "";

    private static final int EXPORT_REQUEST = 101;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("vipin_crm_data", MODE_PRIVATE);
        loadRecords();

        showLogin();
    }

    private int dp(float value) {
        return (int)(value * getResources().getDisplayMetrics().density);
    }

    private void loadRecords() {
        try {
            records = new JSONArray(
                prefs.getString("records", "[]")
            );
        } catch (Exception e) {
            records = new JSONArray();
        }
    }

    private void saveRecords() {
        prefs.edit().putString("records", records.toString()).apply();
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

    private void pad(View v, int p) {
        v.setPadding(dp(p), dp(p), dp(p), dp(p));
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private Button makeButton(String value, int color) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextColor(WHITE);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTypeface(null, Typeface.BOLD);
        b.setBackground(shape(color, 14));
        return b;
    }

    private void addGap(LinearLayout box, int height) {
        View v = new View(this);
        box.addView(v, new LinearLayout.LayoutParams(-1, dp(height)));
    }

    private View lineView() {
        View v = new View(this);
        v.setBackgroundColor(Color.rgb(205, 215, 230));
        v.setLayoutParams(
            new LinearLayout.LayoutParams(-1, dp(1))
        );
        return v;
    }

    private void setScreen(View view) {
        setContentView(view);
    }

    // LOGIN

    private void showLogin() {
        LinearLayout page = vertical();
        page.setGravity(Gravity.CENTER);
        page.setBackgroundColor(PALE);
        pad(page, 24);

        TextView title = text(
            "VIPIN SERVICE CRM", 27, NAVY, true
        );
        title.setGravity(Gravity.CENTER);
        page.addView(title);

        addGap(page, 12);

        TextView subtitle = text(
            "SECURE LOGIN", 16, BLUE, true
        );
        subtitle.setGravity(Gravity.CENTER);
        page.addView(subtitle);

        addGap(page, 25);

        EditText password = new EditText(this);
        password.setSingleLine(true);
        password.setHint("ENTER PASSWORD");
        password.setInputType(129);
        password.setBackground(shape(WHITE, 12));
        pad(password, 12);
        page.addView(password);

        addGap(page, 18);

        Button login = makeButton("OPEN APP", BLUE);
        page.addView(login, new LinearLayout.LayoutParams(-1, dp(52)));

        login.setOnClickListener(v -> {
            String saved = prefs.getString("app_password", "Jvipin");
            if (password.getText().toString().equals(saved)) {
                showCRM();
            } else {
                Toast.makeText(
                    this, "WRONG PASSWORD", Toast.LENGTH_SHORT
                ).show();
            }
        });

        setScreen(page);
    }

    // MAIN SCREEN

    private void showCRM() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        root = vertical();
        root.setBackgroundColor(PALE);
        pad(root, 14);
        scroll.addView(root);

        LinearLayout header = vertical();
        header.setBackground(shape(NAVY, 20));
        pad(header, 18);

        header.addView(text(
            "VIPIN SERVICE CRM", 23, WHITE, true
        ));

        header.addView(text(
            "OFFLINE SERVICE RECORD MANAGEMENT",
            11, WHITE, false
        ));

        root.addView(header);
        addGap(root, 15);

        addMenuButton("ADD NEW RECORD", BLUE, this::showFormNew);
        addMenuButton("SEARCH RECORD", Color.rgb(0, 145, 125), this::showSearch);
        addMenuButton("RECORD FOLDER", Color.rgb(120, 80, 175), this::showRecordFolder);
        addMenuButton("EXPORT EXCEL / CSV", Color.rgb(210, 115, 35), this::exportCSV);
        addMenuButton("CHANGE PASSWORD", NAVY, this::changePassword);

        addGap(root, 12);

        TextView info = text(
            "TOTAL SAVED RECORDS: " + records.length(),
            15, NAVY, true
        );
        root.addView(info);

        setScreen(scroll);
    }

    private void addMenuButton(
        String label, int color, Runnable action
    ) {
        Button b = makeButton(label, color);
        LinearLayout.LayoutParams lp =
            new LinearLayout.LayoutParams(-1, dp(54));
        lp.bottomMargin = dp(11);
        root.addView(b, lp);

        b.setOnClickListener(v -> action.run());
    }

    // CHANGE PASSWORD

    private void changePassword() {
        LinearLayout box = vertical();
        pad(box, 15);

        EditText oldPass = new EditText(this);
        oldPass.setHint("OLD PASSWORD");
        oldPass.setInputType(129);
        box.addView(oldPass);

        EditText newPass = new EditText(this);
        newPass.setHint("NEW PASSWORD");
        newPass.setInputType(129);
        box.addView(newPass);

        new AlertDialog.Builder(this)
            .setTitle("CHANGE PASSWORD")
            .setView(box)
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("SAVE", (d, w) -> {
                String saved = prefs.getString("app_password", "Jvipin");

                if (!oldPass.getText().toString().equals(saved)) {
                    Toast.makeText(
                        this, "OLD PASSWORD WRONG", Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                String np = newPass.getText().toString().trim();

                if (np.length() < 4) {
                    Toast.makeText(
                        this, "PASSWORD MINIMUM 4 CHARACTERS",
                        Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                prefs.edit().putString("app_password", np).apply();

                Toast.makeText(
                    this, "PASSWORD CHANGED", Toast.LENGTH_SHORT
                ).show();
            }).show();
    }

    // ADD / EDIT FORM

    private void showFormNew() {
        showForm(null);
    }

    private void showForm(JSONObject oldRecord) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout form = vertical();
        form.setBackgroundColor(PALE);
        pad(form, 15);
        scroll.addView(form);

        TextView heading = text(
            oldRecord == null ? "ADD NEW RECORD" : "EDIT RECORD",
            23, NAVY, true
        );
        form.addView(heading);
        addGap(form, 12);

        inputs = new EditText[FIELDS.length];

        for (int i = 0; i < FIELDS.length; i++) {
            final int index = i;

            TextView label = text(
                FIELDS[i], 13, NAVY, true
            );
            form.addView(label);

            if (i == 13) {
                paymentSpinner = new Spinner(this);

                ArrayAdapter<String> adapter =
                    new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        PAYMENTS
                    );

                adapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item
                );

                paymentSpinner.setAdapter(adapter);
                form.addView(paymentSpinner);

                if (oldRecord != null) {
                    String old = oldRecord.optString(FIELDS[i], "CASH");
                    for (int j = 0; j < PAYMENTS.length; j++) {
                        if (PAYMENTS[j].equals(old)) {
                            paymentSpinner.setSelection(j);
                            break;
                        }
                    }
                }
            } else {
                EditText e = new EditText(this);
                inputs[i] = e;
                e.setSingleLine(false);
                e.setTextSize(15);
                e.setTextColor(DARK);
                e.setHint("ENTER " + FIELDS[i]);
                e.setBackground(shape(WHITE, 10));
                pad(e, 10);

                if (i == 0) {
                    e.setFocusable(false);
                    e.setOnClickListener(v -> showDatePicker(e));
                }

                if (i == 9 || i == 10 || i == 11 ||
                    i == 12 || i == 3 || i == 4) {
                    e.setInputType(8194);
                }

                if (oldRecord != null) {
                    e.setText(oldRecord.optString(FIELDS[i], ""));
                }

                LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(-1, -2);
                lp.bottomMargin = dp(10);
                form.addView(e, lp);
            }

            addGap(form, 7);
        }

        Button save = makeButton("SAVE RECORD", BLUE);
        form.addView(save, new LinearLayout.LayoutParams(-1, dp(52)));

        Button back = makeButton("BACK", NAVY);
        LinearLayout.LayoutParams bp =
            new LinearLayout.LayoutParams(-1, dp(48));
        bp.topMargin = dp(10);
        form.addView(back, bp);

        save.setOnClickListener(v -> {
            try {
                JSONObject obj = new JSONObject();

                for (int i = 0; i < FIELDS.length; i++) {
                    if (i == 13) {
                        obj.put(
                            FIELDS[i],
                            paymentSpinner.getSelectedItem().toString()
                        );
                    } else {
                        obj.put(
                            FIELDS[i],
                            inputs[i].getText().toString().trim()
                        );
                    }
                }

                if (obj.optString("DATE").isEmpty()) {
                    Toast.makeText(
                        this, "PLEASE SELECT DATE", Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                if (oldRecord == null) {
                    records.put(obj);
                } else {
                    int position = findRecordIndex(oldRecord);
                    if (position >= 0) {
                        records.put(position, obj);
                    }
                }

                saveRecords();

                Toast.makeText(
                    this, "RECORD SAVED SUCCESSFULLY",
                    Toast.LENGTH_SHORT
                ).show();

                showCRM();

            } catch (Exception ex) {
                Toast.makeText(
                    this, "ERROR SAVING RECORD", Toast.LENGTH_SHORT
                ).show();
            }
        });

        back.setOnClickListener(v -> showCRM());

        setScreen(scroll);
    }

    private int findRecordIndex(JSONObject target) {
        for (int i = 0; i < records.length(); i++) {
            JSONObject item = records.optJSONObject(i);
            if (item == target) return i;
        }

        for (int i = 0; i < records.length(); i++) {
            JSONObject item = records.optJSONObject(i);
            if (item != null &&
                item.optString("BILL NUMBER").equals(
                    target.optString("BILL NUMBER")
                ) &&
                item.optString("CUSTOMER NAME").equals(
                    target.optString("CUSTOMER NAME")
                ) &&
                item.optString("DATE").equals(
                    target.optString("DATE")
                )) {
                return i;
            }
        }
        return -1;
    }

    private void showDatePicker(EditText field) {
        Calendar c = Calendar.getInstance();

        DatePickerDialog picker = new DatePickerDialog(
            this,
            (view, year, month, day) -> {
                field.setText(String.format(
                    Locale.getDefault(),
                    "%02d-%02d-%04d",
                    day, month + 1, year
                ));
            },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        );

        picker.show();
    }

    // SEARCH

    private void showSearch() {
        LinearLayout page = vertical();
        page.setBackgroundColor(PALE);
        pad(page, 14);

        page.addView(text("SEARCH RECORD", 22, NAVY, true));

        EditText search = new EditText(this);
        search.setHint("BILL NUMBER / CUSTOMER NAME / MOBILE NUMBER");
        search.setSingleLine(true);
        search.setBackground(shape(WHITE, 12));
        pad(search, 10);
        page.addView(search);

        ScrollView scroll = new ScrollView(this);
        LinearLayout results = vertical();
        pad(results, 5);
        scroll.addView(results);

        page.addView(scroll, new LinearLayout.LayoutParams(
            -1, 0, 1
        ));

        Button back = makeButton("BACK", NAVY);
        page.addView(back);

        Runnable update = () -> {
            results.removeAllViews();

            String q = search.getText().toString().toLowerCase().trim();

            for (int i = records.length() - 1; i >= 0; i--) {
                JSONObject obj = records.optJSONObject(i);
                if (obj == null) continue;

                String bill = obj.optString("BILL NUMBER").toLowerCase();
                String name = obj.optString("CUSTOMER NAME").toLowerCase();
                String mobile = obj.optString("MOBILE NUMBER").toLowerCase();

                if (q.isEmpty() || bill.contains(q) ||
                    name.contains(q) || mobile.contains(q)) {
                    results.addView(recordCard(obj));
                    results.addView(lineView());
                }
            }
        };

        search.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(
                CharSequence s, int start, int count, int after
            ) {}

            public void onTextChanged(
                CharSequence s, int start, int before, int count
            ) {
                update.run();
            }

            public void afterTextChanged(android.text.Editable e) {}
        });

        update.run();

        back.setOnClickListener(v -> showCRM());
        setScreen(page);
    }

    // RECORD FOLDER

    private void showRecordFolder() {
        LinearLayout page = vertical();
        page.setBackgroundColor(PALE);
        pad(page, 14);

        page.addView(text("RECORD FOLDER", 22, NAVY, true));

        LinearLayout filters = horizontal();

        Spinner month = new Spinner(this);
        String[] months = {
            "ALL MONTHS", "01", "02", "03", "04", "05",
            "06", "07", "08", "09", "10", "11", "12"
        };

        month.setAdapter(new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_dropdown_item, months
        ));

        Spinner year = new Spinner(this);
        String currentYear = new SimpleDateFormat(
            "yyyy", Locale.getDefault()
        ).format(new Date());

        String[] years = new String[16];
        years[0] = "ALL YEARS";
        int y = Integer.parseInt(currentYear);
        for (int i = 1; i < years.length; i++) {
            years[i] = String.valueOf(y - i + 1);
        }

        year.setAdapter(new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_dropdown_item, years
        ));

        filters.addView(month, new LinearLayout.LayoutParams(0, -2, 1));
        filters.addView(year, new LinearLayout.LayoutParams(0, -2, 1));
        page.addView(filters);

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = vertical();
        pad(list, 5);
        scroll.addView(list);

        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout totals = vertical();
        totals.setBackground(shape(WHITE, 12));
        pad(totals, 12);
        page.addView(totals);

        Runnable refresh = () -> {
            list.removeAllViews();
            totals.removeAllViews();

            String m = month.getSelectedItem().toString();
            String yr = year.getSelectedItem().toString();

            JSONArray filtered = new JSONArray();

            for (int i = records.length() - 1; i >= 0; i--) {
                JSONObject obj = records.optJSONObject(i);
                if (obj == null) continue;

                String date = obj.optString("DATE");
                String[] parts = date.split("-");

                boolean match = parts.length == 3;

                if (match && !m.equals("ALL MONTHS")) {
                    match = parts[1].equals(m);
                }

                if (match && !yr.equals("ALL YEARS")) {
                    match = parts[2].equals(yr);
                }

                if (match) {
                    filtered.put(obj);
                    list.addView(recordCard(obj));
                    list.addView(lineView());
                }
            }

            addTotal(totals, "TOTAL RECORDS", filtered.length());

            String[] totalFields = {
                "AMC", "COLLECTION", "AMOUNT",
                "SPARE AMOUNT", "WARRANTY AMOUNT", "NET AMOUNT"
            };

            for (String field : totalFields) {
                double sum = 0;

                for (int i = 0; i < filtered.length(); i++) {
                    JSONObject obj = filtered.optJSONObject(i);
                    if (obj != null) {
                        sum += number(obj.optString(field));
                    }
                }

                addTotal(totals, field, sum);
            }
        };

        month.setOnItemSelectedListener(new SimpleSelectListener(refresh));
        year.setOnItemSelectedListener(new SimpleSelectListener(refresh));

        Button back = makeButton("BACK", NAVY);
        page.addView(back);
        back.setOnClickListener(v -> showCRM());

        refresh.run();
        setScreen(page);
    }

    private static class SimpleSelectListener
        implements AdapterView.OnItemSelectedListener {

        private final Runnable action;

        SimpleSelectListener(Runnable r) {
            action = r;
        }

        public void onItemSelected(
            AdapterView<?> p, View v, int position, long id
        ) {
            action.run();
        }

        public void onNothingSelected(AdapterView<?> p) {}
    }

    private void addTotal(LinearLayout box, String name, double value) {
        TextView t = text(
            name + ": " + formatNumber(value),
            14, NAVY, true
        );
        box.addView(t);
        box.addView(lineView());
    }

    private void addTotal(LinearLayout box, String name, int value) {
        TextView t = text(
            name + ": " + value,
            14, NAVY, true
        );
        box.addView(t);
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

    private String formatNumber(double n) {
        if (n == Math.floor(n)) {
            return String.format(Locale.US, "%.0f", n);
        }
        return String.format(Locale.US, "%.2f", n);
    }

    // RECORD CARD

    private View recordCard(JSONObject obj) {
        LinearLayout card = vertical();
        card.setBackground(shape(WHITE, 12));
        pad(card, 12);

        String[] show = {
            "DATE", "BILL NUMBER", "PRODUCT NAME",
            "CUSTOMER NAME", "MOBILE NUMBER",
            "AMC", "COLLECTION", "AMOUNT",
            "SPARE AMOUNT", "WARRANTY AMOUNT",
            "NET AMOUNT", "PAYMENT MODE",
            "ADDRESS", "SPARE PARTS", "REMARKS"
        };

        for (String key : show) {
            String value = obj.optString(key, "");
            if (!value.isEmpty()) {
                card.addView(text(
                    key + ": " + value, 13, DARK, false
                ));
            }
        }

        addGap(card, 8);

        LinearLayout actions = horizontal();

        Button edit = makeButton("EDIT", BLUE);
        Button delete = makeButton("DELETE", Color.rgb(190, 45, 55));

        actions.addView(edit, new LinearLayout.LayoutParams(0, dp(44), 1));
        actions.addView(delete, new LinearLayout.LayoutParams(0, dp(44), 1));

        card.addView(actions);

        edit.setOnClickListener(v -> showForm(obj));

        delete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("DELETE RECORD?")
                .setMessage("ARE YOU SURE?")
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("DELETE", (d, w) -> {
                    int index = findRecordIndex(obj);
                    if (index >= 0) {
                        records.remove(index);
                        saveRecords();
                        showCRM();
                    }
                }).show();
        });

        return card;
    }

    // CSV EXPORT

    private void exportCSV() {
        if (records.length() == 0) {
            Toast.makeText(
                this, "NO RECORD TO EXPORT", Toast.LENGTH_SHORT
            ).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(
            Intent.EXTRA_TITLE, "VIPIN_SERVICE_CRM.csv"
        );

        startActivityForResult(intent, EXPORT_REQUEST);
    }

    @Override
    protected void onActivityResult(
        int requestCode, int resultCode, Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EXPORT_REQUEST &&
            resultCode == RESULT_OK && data != null) {

            Uri uri = data.getData();

            try {
                OutputStream out = getContentResolver()
                    .openOutputStream(uri);

                if (out == null) throw new Exception("File error");

                StringBuilder csv = new StringBuilder();

                csv.append('\uFEFF');

                for (int i = 0; i < FIELDS.length; i++) {
                    csv.append(csvCell(FIELDS[i]));
                    if (i < FIELDS.length - 1) csv.append(",");
                }
                csv.append("\r\n");

                for (int r = 0; r < records.length(); r++) {
                    JSONObject obj = records.optJSONObject(r);
                    if (obj == null) continue;

                    for (int i = 0; i < FIELDS.length; i++) {
                        csv.append(
                            csvCell(obj.optString(FIELDS[i], ""))
                        );
                        if (i < FIELDS.length - 1) csv.append(",");
                    }
                    csv.append("\r\n");
                }

                out.write(csv.toString().getBytes(StandardCharsets.UTF_8));
                out.close();

                Toast.makeText(
                    this, "CSV EXPORTED SUCCESSFULLY",
                    Toast.LENGTH_LONG
                ).show();

            } catch (Exception e) {
                Toast.makeText(
                    this, "EXPORT FAILED",
                    Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    private String csvCell(String value) {
        if (value == null) value = "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    @Override
    public void onBackPressed() {
        showCRM();
    }
}
