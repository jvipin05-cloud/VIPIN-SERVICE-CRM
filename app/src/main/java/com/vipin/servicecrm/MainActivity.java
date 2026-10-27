package com.vipin.servicecrm;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.text.*;
import java.util.*;

public class MainActivity extends Activity {
    private static final String PREF = "vipin_crm_data";
    private static final String KEY = "records";
    private static final String[] FIELDS = {
        "DATE", "BILL NUMBER", "PRODUCT NAME", "AMC", "COLLECTION",
        "CUSTOMER NAME", "MOBILE NUMBER", "ADDRESS", "SPARE PARTS",
        "AMOUNT", "SPARE AMOUNT", "WARRANTY AMOUNT", "NET AMOUNT",
        "PAYMENT MODE", "REMARKS"
    };
    private static final String[] PAYMENTS = {"CASH", "UPI", "PS UPI", "RKS UPI"};

    private final int navy = Color.rgb(20, 37, 76);
    private final int blue = Color.rgb(37, 111, 219);
    private final int cyan = Color.rgb(0, 166, 190);
    private final int pale = Color.rgb(242, 246, 252);
    private final int white = Color.WHITE;
    private final int ink = Color.rgb(35, 48, 69);
private final int gold = Color.rgb(190, 125, 20);
private final int lightGold = Color.rgb(239, 181, 66);
private final int paleGold = Color.rgb(255, 239, 190);
    private SharedPreferences prefs;
    private JSONArray records = new JSONArray();
    private EditText[] inputs = new EditText[FIELDS.length];
    private String[] photos = new String[3];
    private LinearLayout root;
    private int editing = -1;
    private int photoIndex = 0;
    private int selectedMonth = -1;
    private int selectedYear = -1;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(navy);
        getWindow().setNavigationBarColor(navy);
        prefs = getSharedPreferences(PREF, MODE_PRIVATE);
        loadRecords();
        showSplash();
    }

    private void showSplash() {
        LinearLayout box = vertical();
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(24), dp(24), dp(24), dp(24));
        box.setBackground(gradient(navy, blue, 28));
        ImageView logo = new ImageView(this);
        int logoId = getResources().getIdentifier("vipin_logo", "drawable", getPackageName());
        if (logoId != 0) logo.setImageResource(logoId);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        box.addView(logo, new LinearLayout.LayoutParams(dp(190), dp(190)));
        TextView title = label("VIPIN SERVICE CRM", 25, white, true);
        title.setGravity(Gravity.CENTER);
        box.addView(title);
        TextView sub = label("SERVICE  •  RECORD  •  MANAGEMENT", 12, white, false);
        sub.setGravity(Gravity.CENTER);
        box.addView(sub);
        setContentView(box);
        new Handler().postDelayed(this::showLogin, 900);
    }

    private void showLogin() {
        LinearLayout box = vertical();
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(24), dp(28), dp(24), dp(28));
        box.setBackgroundColor(pale);
        ImageView logo = new ImageView(this);
        int logoId = getResources().getIdentifier("vipin_logo", "drawable", getPackageName());
        if (logoId != 0) logo.setImageResource(logoId);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        box.addView(logo, new LinearLayout.LayoutParams(dp(110), dp(110)));
        TextView heading = label("VIPIN SERVICE CRM", 24, navy, true);
        heading.setGravity(Gravity.CENTER);
        box.addView(heading);
        TextView hint = label("LOGIN TO CONTINUE", 13, blue, true);
        hint.setGravity(Gravity.CENTER);
        box.addView(hint);
        EditText pass = field("ENTER PASSWORD");
        pass.setInputType(129);
        box.addView(pass, matchWrap());
        Button open = actionButton("UNLOCK CRM");
        box.addView(open, matchWrap());
        open.setOnClickListener(v -> {
            String saved = prefs.getString("password", "Jvipin");
            if (pass.getText().toString().equals(saved)) showHome();
            else toast("WRONG PASSWORD");
        });
        pass.setOnEditorActionListener((v, a, e) -> { open.performClick(); return true; });
        setContentView(box);
    }

    private void showHome() {
        ScrollView scroll = new ScrollView(this);
        root = vertical();
        root.setPadding(dp(16), dp(16), dp(16), dp(24));
        root.setBackgroundColor(pale);
        scroll.addView(root);

        LinearLayout hero = vertical();
        hero.setPadding(dp(18), dp(18), dp(18), dp(18));
        hero.setBackground(gradient(navy, blue, 22));
        TextView title = label("VIPIN SERVICE CRM", 23, white, true);
        hero.addView(title);
        hero.addView(label("OFFLINE SERVICE RECORD MANAGER", 11, white, false));
        hero.addView(label("TOTAL RECORDS  •  " + records.length(), 14, white, true));
        root.addView(hero, matchWrap());

        addHomeButton("＋  ADD NEW RECORD", this::showForm, blue);
        addHomeButton("⌕  SEARCH RECORD", this::searchRecords, cyan);
        addHomeButton("▦  MONTH / YEAR RECORDS", this::showFolder, navy);
        addHomeButton("⇩  EXPORT EXCEL / CSV", this::exportCSV, blue);
        addHomeButton("⚙  CHANGE PASSWORD", this::changePassword, navy);

        TextView totalsTitle = label("LAST MONTH TOTAL", 18, navy, true);
        totalsTitle.setPadding(dp(3), dp(18), dp(3), dp(8));
        root.addView(totalsTitle);
        addTotalCard("AMOUNT", lastMonthTotal("AMOUNT"));
        addTotalCard("SPARE AMOUNT", lastMonthTotal("SPARE AMOUNT"));
        addTotalCard("WARRANTY AMOUNT", lastMonthTotal("WARRANTY AMOUNT"));
        addTotalCard("NET AMOUNT", lastMonthTotal("NET AMOUNT"));
        setContentView(scroll);
    }

    private void addTotalCard(String name, double value) {
        LinearLayout card = horizontal();
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(roundBox(white, 16));
        TextView l = label(name, 13, ink, true);
        TextView r = label("₹ " + money(value), 17, blue, true);
        card.addView(l, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(r);
        LinearLayout.LayoutParams p = matchWrap();
        p.bottomMargin = dp(8);
        root.addView(card, p);
    }

    private double lastMonthTotal(String field) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, -1);
        int month = c.get(Calendar.MONTH) + 1, year = c.get(Calendar.YEAR);
        double total = 0;
        for (int i = 0; i < records.length(); i++) {
            JSONObject o = records.optJSONObject(i);
            if (o == null) continue;
            Calendar d = parseDate(o.optString("DATE", ""));
            if (d != null && d.get(Calendar.MONTH) + 1 == month && d.get(Calendar.YEAR) == year)
                total += number(o.optString(field, "0"));
        }
        return total;
    }

    private void showForm() {
        editing = -1;
        photos = new String[3];
        showFormScreen(null);
    }

    private void showFormScreen(JSONObject old) {
        ScrollView scroll = new ScrollView(this);
        LinearLayout panel = vertical();
        panel.setPadding(dp(14), dp(14), dp(14), dp(26));
        panel.setBackgroundColor(pale);
        scroll.addView(panel);
        addPageHeader(panel, editing < 0 ? "ADD NEW RECORD" : "EDIT RECORD");

        for (int i = 0; i < FIELDS.length; i++) {
            TextView cap = label(FIELDS[i], 13, gold, true);
            cap.setPadding(dp(3), dp(10), dp(3), dp(4));
            panel.addView(cap);
            if (i == 13) {
                Spinner sp = new Spinner(this);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item, PAYMENTS);
                sp.setAdapter(adapter);
                inputs[i] = new EditText(this);
                inputs[i].setVisibility(View.GONE);
                panel.addView(sp, matchWrap());
                panel.addView(inputs[i]);
                final int index = i;
                sp.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                    public void onNothingSelected(android.widget.AdapterView<?> p) {}
                    public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                        inputs[index].setText(PAYMENTS[pos]);
                    }
                });
                if (old != null) {
                    String val = old.optString(FIELDS[i], "CASH");
                    for (int k = 0; k < PAYMENTS.length; k++)
                        if (PAYMENTS[k].equals(val)) sp.setSelection(k);
                }
            } else {
                EditText e = field("ENTER " + FIELDS[i]);
                inputs[i] = e;
                if (i == 0) {
                    e.setFocusable(false);
                    e.setClickable(true);
                    e.setOnClickListener(v -> pickDate(e));
                    e.setText(old == null ? today() : old.optString(FIELDS[i], today()));
                } else if (i == 6) {
                    e.setInputType(3);
                } else if (i == 3 || i == 4 || (i >= 9 && i <= 12)) {
                    e.setInputType(8194);
                } else if (i == 7 || i == 8 || i == 14) {
                    e.setSingleLine(false);
                    e.setMinLines(i == 14 ? 3 : 2);
                    e.setGravity(Gravity.TOP | Gravity.START);
                }
                if (old != null && i != 0) e.setText(old.optString(FIELDS[i], ""));
                panel.addView(e, matchWrap());
            }
        }

        TextView photoTitle = label("ATTACH PHOTOS (UP TO 3)", 13, navy, true);
        photoTitle.setPadding(dp(3), dp(16), dp(3), dp(6));
        panel.addView(photoTitle);
        LinearLayout photoRow = horizontal();
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            Button b = actionButton("PHOTO " + (i + 1));
            photoRow.addView(b, new LinearLayout.LayoutParams(0, dp(48), 1));
            b.setOnClickListener(v -> {
                photoIndex = idx;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("image/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, 101);
            });
        }
        panel.addView(photoRow, matchWrap());
        Button save = actionButton("SAVE RECORD");
        panel.addView(save, buttonParams());
        save.setOnClickListener(v -> saveRecord());
        Button back = secondaryButton("BACK TO HOME");
        panel.addView(back, buttonParams());
        back.setOnClickListener(v -> showHome());
        setContentView(scroll);
    }

    private void pickDate(EditText target) {
        Calendar c = Calendar.getInstance();
        Calendar existing = parseDate(target.getText().toString());
        if (existing != null) c = existing;
        new DatePickerDialog(this, (view, y, m, d) ->
            target.setText(String.format(Locale.getDefault(), "%02d-%02d-%04d", d, m + 1, y)),
            c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void saveRecord() {
        try {
            JSONObject obj = new JSONObject();
            for (int i = 0; i < FIELDS.length; i++) {
                String value = inputs[i] == null ? "" : inputs[i].getText().toString().trim();
                obj.put(FIELDS[i], value);
            }
            JSONArray photoArray = new JSONArray();
            for (String p : photos) photoArray.put(p == null ? "" : p);
            obj.put("PHOTOS", photoArray);
            if (editing >= 0 && editing < records.length()) records.put(editing, obj);
            else records.put(obj);
            saveRecords();
            toast("RECORD SAVED SUCCESSFULLY");
            showHome();
        } catch (Exception e) {
            toast("SAVE ERROR: " + e.getMessage());
        }
    }

    private void searchRecords() {
        EditText q = field("BILL / CUSTOMER / MOBILE / PRODUCT");
        new AlertDialog.Builder(this).setTitle("SEARCH RECORD").setView(q)
            .setPositiveButton("SEARCH", (d, w) -> showResults(q.getText().toString()))
            .setNegativeButton("CANCEL", null).show();
    }

    private void showFolder() {
        Calendar now = Calendar.getInstance();
        int currentYear = now.get(Calendar.YEAR);
        String[] years = new String[16];
        for (int i = 0; i < years.length; i++) years[i] = String.valueOf(currentYear - i);
        new AlertDialog.Builder(this).setTitle("SELECT YEAR").setItems(years, (d, which) -> {
            selectedYear = Integer.parseInt(years[which]);
            String[] months = new DateFormatSymbols().getMonths();
            String[] monthNames = new String[12];
            for (int i = 0; i < 12; i++) monthNames[i] = months[i];
            new AlertDialog.Builder(this).setTitle("SELECT MONTH - " + selectedYear)
                .setItems(monthNames, (dialog, month) -> {
                    selectedMonth = month + 1;
                    showMonthRecords();
                }).setNegativeButton("BACK", null).show();
        }).setNegativeButton("CANCEL", null).show();
    }

    private void showMonthRecords() {
        JSONArray found = new JSONArray();
        for (int i = 0; i < records.length(); i++) {
            JSONObject o = records.optJSONObject(i);
            Calendar d = o == null ? null : parseDate(o.optString("DATE", ""));
            if (d != null && d.get(Calendar.MONTH) + 1 == selectedMonth &&
                d.get(Calendar.YEAR) == selectedYear) found.put(o);
        }
        showArray(found, "RECORDS - " + monthYear(selectedMonth, selectedYear));
    }

    private void showResults(String query) {
        JSONArray found = new JSONArray();
        String q = query.trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < records.length(); i++) {
            JSONObject o = records.optJSONObject(i);
            if (o == null) continue;
            StringBuilder data = new StringBuilder();
            for (String f : FIELDS) data.append(o.optString(f, "")).append(" ");
            if (data.toString().toLowerCase(Locale.ROOT).contains(q)) found.put(o);
        }
        showArray(found, "SEARCH RESULTS");
    }

    private void showArray(JSONArray list, String title) {
        ScrollView scroll = new ScrollView(this);
        LinearLayout panel = vertical();
        panel.setPadding(dp(14), dp(14), dp(14), dp(24));
        panel.setBackgroundColor(pale);
        scroll.addView(panel);
        addPageHeader(panel, title + " (" + list.length() + ")");
        for (int i = 0; i < list.length(); i++) {
            JSONObject o = list.optJSONObject(i);
            if (o == null) continue;
            LinearLayout card = vertical();
            card.setPadding(dp(14), dp(12), dp(14), dp(12));
            card.setBackground(roundBox(white, 17));
            TextView name = label(o.optString("CUSTOMER NAME", "CUSTOMER"), 17, navy, true);
            card.addView(name);
            card.addView(label("BILL: " + o.optString("BILL NUMBER"), 13, blue, true));
            card.addView(label("DATE: " + o.optString("DATE"), 12, ink, false));
            card.addView(label("MOBILE: " + o.optString("MOBILE NUMBER"), 12, ink, false));
            card.addView(label("PRODUCT: " + o.optString("PRODUCT NAME"), 12, ink, false));
            card.addView(label("NET AMOUNT: ₹ " + o.optString("NET AMOUNT"), 14, navy, true));
            final JSONObject selected = o;
            final int recordIndex = findRecord(o);
            LinearLayout actions = horizontal();
            Button edit = actionButton("EDIT");
            Button delete = dangerButton("DELETE");
            actions.addView(edit, new LinearLayout.LayoutParams(0, dp(46), 1));
            actions.addView(delete, new LinearLayout.LayoutParams(0, dp(46), 1));
            card.addView(actions);
            edit.setOnClickListener(v -> {
                editing = recordIndex;
                photos = new String[3];
                JSONArray a = selected.optJSONArray("PHOTOS");
                if (a != null) for (int x = 0; x < 3 && x < a.length(); x++) photos[x] = a.optString(x);
                showFormScreen(selected);
            });
            delete.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setMessage("DELETE THIS RECORD?")
                .setPositiveButton("DELETE", (dialog, which) -> {
                    if (recordIndex >= 0) { records.remove(recordIndex); saveRecords(); }
                    showHome();
                }).setNegativeButton("CANCEL", null).show());
            LinearLayout.LayoutParams cp = matchWrap();
            cp.bottomMargin = dp(10);
            panel.addView(card, cp);
        }
        Button back = secondaryButton("BACK TO HOME");
        panel.addView(back, buttonParams());
        back.setOnClickListener(v -> showHome());
        setContentView(scroll);
    }

    private int findRecord(JSONObject obj) {
        for (int i = 0; i < records.length(); i++)
            if (records.optJSONObject(i) == obj) return i;
        for (int i = 0; i < records.length(); i++)
            if (records.optJSONObject(i) != null &&
                records.optJSONObject(i).toString().equals(obj.toString())) return i;
        return -1;
    }

    private void exportCSV() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("text/csv");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_TITLE, "VIPIN_SERVICE_CRM.csv");
        startActivityForResult(intent, 102);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == 101) {
            try {
                File dir = new File(getFilesDir(), "photos");
                if (!dir.exists()) dir.mkdirs();
                File file = new File(dir, "photo_" + System.currentTimeMillis() + ".jpg");
                try (InputStream in = getContentResolver().openInputStream(uri);
                     FileOutputStream out = new FileOutputStream(file)) {
                    byte[] buffer = new byte[8192];
                    int n;
                    while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
                }
                photos[photoIndex] = file.getAbsolutePath();
                toast("PHOTO SAVED");
            } catch (Exception e) { toast("PHOTO ERROR"); }
        } else if (requestCode == 102) {
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                StringBuilder csv = new StringBuilder();
                for (int i = 0; i < FIELDS.length; i++) {
                    if (i > 0) csv.append(",");
                    csv.append(csvCell(FIELDS[i]));
                }
                csv.append("\r\n");
                for (int i = 0; i < records.length(); i++) {
                    JSONObject o = records.getJSONObject(i);
                    for (int j = 0; j < FIELDS.length; j++) {
                        if (j > 0) csv.append(",");
                        csv.append(csvCell(o.optString(FIELDS[j], "")));
                    }
                    csv.append("\r\n");
                }
                out.write(csv.toString().getBytes("UTF-8"));
                toast("CSV EXPORTED");
            } catch (Exception e) { toast("EXPORT ERROR"); }
        }
    }

    private String csvCell(String s) { return "\"" + (s == null ? "" : s).replace("\"", "\"\"") + "\""; }

    private void changePassword() {
        LinearLayout box = vertical();
        EditText old = field("OLD PASSWORD");
        EditText next = field("NEW PASSWORD");
        old.setInputType(129); next.setInputType(129);
        box.addView(old, matchWrap()); box.addView(next, matchWrap());
        new AlertDialog.Builder(this).setTitle("CHANGE PASSWORD").setView(box)
            .setPositiveButton("SAVE", (d, w) -> {
                String saved = prefs.getString("password", "Jvipin");
                String n = next.getText().toString().trim();
                if (!old.getText().toString().equals(saved)) toast("OLD PASSWORD WRONG");
                else if (n.length() < 4) toast("PASSWORD MUST BE AT LEAST 4 CHARACTERS");
                else { prefs.edit().putString("password", n).apply(); toast("PASSWORD CHANGED"); }
            }).setNegativeButton("CANCEL", null).show();
    }

    private void loadRecords() {
        try { records = new JSONArray(prefs.getString(KEY, "[]")); }
        catch (Exception e) { records = new JSONArray(); }
    }
    private void saveRecords() { prefs.edit().putString(KEY, records.toString()).apply(); }

    private Calendar parseDate(String s) {
        try {
            Date d = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).parse(s);
            Calendar c = Calendar.getInstance(); c.setTime(d); return c;
        } catch (Exception e) { return null; }
    }
    private String today() { return new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date()); }
    private String monthYear(int m, int y) {
        Calendar c = Calendar.getInstance(); c.set(y, m - 1, 1);
        return new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(c.getTime()).toUpperCase(Locale.ROOT);
    }
    private double number(String s) {
        try { return Double.parseDouble(s.replace(",", "").replace("₹", "").trim()); }
        catch (Exception e) { return 0; }
    }
    private String money(double n) { return String.format(Locale.getDefault(), "%.2f", n); }

    private LinearLayout vertical() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout horizontal() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); return l; }
    private TextView label(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD); t.setPadding(dp(4), dp(5), dp(4), dp(5)); return t;
    }
    private EditText field(String hint) {
        EditText e = new EditText(this); e.setHint(hint); e.setTextSize(15);
    e.setTextColor(navy);
e.setHintTextColor(Color.rgb(190, 145, 65));
        e.setPadding(dp(13), dp(10), dp(13), dp(10));
        e.setBackground(roundBox(white, 12)); return e;
    }
    private Button actionButton(String s) {
        Button b = new Button(this); b.setText(s); b.setTextColor(white); b.setTextSize(14);
        b.setTypeface(null, Typeface.BOLD); b.setAllCaps(false); b.setBackground(roundBox(blue, 13));
        return b;
    }
    private Button secondaryButton(String s) {
        Button b = actionButton(s); b.setBackground(roundBox(navy, 13)); return b;
    }
    private Button dangerButton(String s) {
        Button b = actionButton(s); b.setBackground(roundBox(Color.rgb(190, 55, 65), 13)); return b;
    }
    private void addHomeButton(String title, Runnable action, int color) {
        Button b = actionButton(title); b.setBackground(roundBox(color, 15));
        LinearLayout.LayoutParams p = matchWrap(); p.topMargin = dp(10);
        root.addView(b, p); b.setOnClickListener(v -> action.run());
    }
    private void addPageHeader(LinearLayout parent, String title) {
    TextView t = label(title, 21, navy, true);
    t.setGravity(Gravity.CENTER);
    t.setPadding(dp(15), dp(17), dp(15), dp(17));
    t.setBackground(gradient(paleGold, lightGold, 18));
    LinearLayout.LayoutParams hp = matchWrap();
    hp.bottomMargin = dp(10);
    parent.addView(t, hp);
}
    private GradientDrawable roundBox(int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color);
        d.setCornerRadius(dp(radius)); return d;
    }
    private GradientDrawable gradient(int a, int b, int radius) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{a, b});
        d.setCornerRadius(dp(radius)); return d;
    }
    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams p = matchWrap(); p.topMargin = dp(12); return p;
    }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
