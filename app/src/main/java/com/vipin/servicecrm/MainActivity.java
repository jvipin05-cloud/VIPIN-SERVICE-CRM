package com.vipin.servicecrm;


import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.*;
import android.view.animation.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;


public class MainActivity extends Activity {

private static final String PREF = "vipin_crm_data";  
private static final String KEY = "records";  
private static final String[] FIELDS = {  
    "DATE", "BILL NUMBER", "PRODUCT NAME", "AMC",  
    "COLLECTION", "CUSTOMER NAME", "MOBILE NUMBER",  
    "ADDRESS", "SPARE PARTS", "AMOUNT", "SPARE AMOUNT",  
    "WARRANTY AMOUNT", "NET AMOUNT", "PAYMENT MODE", "REMARKS"  
};  
private static final String[] PAYMENTS = {  
    "CASH", "UPI", "PS UPI", "RKS UPI"  
};  

private final int navy = Color.rgb(22, 42, 85);  
private final int blue = Color.rgb(35, 105, 210);  
private final int pale = Color.rgb(240, 245, 255);  
private final int white = Color.WHITE;  

private LinearLayout root;  
private JSONArray records = new JSONArray();  
private EditText[] inputs = new EditText[FIELDS.length];  
private String[] photos = new String[3];  
private int editing = -1;  
private int photoIndex = 0;  
private int selectedMonth = -1;  
private int selectedYear = -1;  

private SharedPreferences prefs;  

@Override  
public void onCreate(Bundle b) {  
    super.onCreate(b);  
    prefs = getSharedPreferences(PREF, MODE_PRIVATE);  
    loadRecords();  
    showSplash();  
}  


private void showSplash() {
    LinearLayout box = vertical();
    box.setGravity(Gravity.CENTER);
    box.setPadding(25, 25, 25, 25);
    box.setBackgroundColor(navy);

    ImageView logo = new ImageView(this);
    logo.setImageResource(R.drawable.vipin_logo);
    logo.setScaleType(ImageView.ScaleType.FIT_CENTER);

    LinearLayout.LayoutParams logoParams =
        new LinearLayout.LayoutParams(220, 220);
    logoParams.bottomMargin = 20;
    box.addView(logo, logoParams);

    TextView sub = text("VIPIN SERVICE CRM", 25, white, true);
    box.addView(sub);

    TextView line = text(
        "SERVICE • RECORD • MANAGEMENT", 12, white, false);
    box.addView(line);

    setContentView(box);

    ScaleAnimation anim = new ScaleAnimation(
        0.4f, 1f, 0.4f, 1f,
        Animation.RELATIVE_TO_SELF, 0.5f,
        Animation.RELATIVE_TO_SELF, 0.5f
    );

    anim.setDuration(900);
    logo.startAnimation(anim);

    new Handler().postDelayed(() -> showLogin(), 1300);
}


private void showLogin() {  
    LinearLayout box = vertical();  
    box.setGravity(Gravity.CENTER);  
    box.setPadding(28, 28, 28, 28);  
    box.setBackgroundColor(pale);  

    box.addView(text("VIPIN SERVICE CRM", 27, navy, true));  
    box.addView(text("LOGIN TO CONTINUE", 14, blue, true));  

    EditText pass = new EditText(this);  
    pass.setHint("ENTER PASSWORD");  
    pass.setSingleLine(true);  
    pass.setInputType(129);  
    box.addView(pass);  

    Button login = button("OPEN APP");  
    box.addView(login);  

    login.setOnClickListener(v -> {  
        String saved = prefs.getString("password", "Jvipin");  
        if (pass.getText().toString().equals(saved)) {  
            showHome();  
        } else {  
            msg("WRONG PASSWORD");  
        }  
    });  

    setContentView(box);  
}  

private void showHome() {  
    ScrollView scroll = new ScrollView(this);  
    root = vertical();  
    root.setPadding(16, 18, 16, 25);  
    root.setBackgroundColor(pale);  
    scroll.addView(root);  

    header(root, "VIPIN SERVICE CRM");  

    addHomeButton("ADD NEW RECORD", this::showForm);  
    addHomeButton("SEARCH RECORD", this::searchRecords);  
    addHomeButton("RECORD FOLDER - MONTH / YEAR", this::showFolder);  
    addHomeButton("EXPORT EXCEL / CSV", this::exportCSV);  
    addHomeButton("CHANGE PASSWORD", this::changePassword);  

    addView(root, text("TOTAL RECORDS: " + records.length(),  
        16, navy, true));  
    addView(root, text("LAST MONTH TOTAL", 20, navy, true));
addView(root, text("AMOUNT: ₹" + lastMonthTotal("AMOUNT"), 16, navy, true));
addView(root, text("SPARE AMOUNT: ₹" + lastMonthTotal("SPARE AMOUNT"), 16, navy, true));
addView(root, text("WARRANTY AMOUNT: ₹" + lastMonthTotal("WARRANTY AMOUNT"), 16, navy, true));
addView(root, text("NET AMOUNT: ₹" + lastMonthTotal("NET AMOUNT"), 16, navy, true));

    setContentView(scroll);  
}  
private double lastMonthTotal(String field) {
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.MONTH, -1);

    int month = cal.get(Calendar.MONTH) + 1;
    int year = cal.get(Calendar.YEAR);
    double total = 0;

    for (int i = 0; i < records.length(); i++) {
        JSONObject obj = records.optJSONObject(i);
        if (obj == null) continue;

        String date = obj.optString("DATE", "");

        try {
            Date d = new SimpleDateFormat(
                "dd-MM-yyyy", Locale.getDefault()
            ).parse(date);

            Calendar rc = Calendar.getInstance();
            rc.setTime(d);

            if (rc.get(Calendar.MONTH) + 1 == month &&
                rc.get(Calendar.YEAR) == year) {

                String amount = obj.optString(field, "0")
                    .replace(",", "")
                    .replace("₹", "").trim();

                total += Double.parseDouble(
                    amount.isEmpty() ? "0" : amount
                );
            }
        } catch (Exception e) {
        }
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
    panel.setPadding(14, 15, 14, 25);  
    panel.setBackgroundColor(pale);  
    scroll.addView(panel);  

    header(panel, editing < 0 ? "ADD NEW RECORD" : "EDIT RECORD");  

    for (int i = 0; i < FIELDS.length; i++) {  
        panel.addView(text(FIELDS[i], 13, navy, true));  

        if (i == 14) {  
            inputs[i] = new EditText(this);  
            inputs[i].setHint(FIELDS[i]);  
            inputs[i].setMinLines(2);  
            panel.addView(inputs[i]);  
        } else if (i == 13) {  
            Spinner sp = new Spinner(this);  
            ArrayAdapter<String> ad = new ArrayAdapter<>(  
                this, android.R.layout.simple_spinner_dropdown_item, PAYMENTS  
            );  
            sp.setAdapter(ad);  
            panel.addView(sp);  
            inputs[i] = new EditText(this);  
            inputs[i].setVisibility(View.GONE);  
            panel.addView(inputs[i]);  
            sp.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {  
                public void onNothingSelected(android.widget.AdapterView<?> p) {}  
                public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {  
                    inputs[13].setText(PAYMENTS[pos]);  
                }  
            });  
        } else {  
            inputs[i] = new EditText(this);  
            inputs[i].setHint("ENTER " + FIELDS[i]);  
            inputs[i].setSingleLine(i != 7);  
            if (i == 0) {  
                inputs[i].setFocusable(false);  
                  final EditText dateField = inputs[i];

dateField.setOnClickListener(v -> pickDate(dateField));
                inputs[i].setText(new SimpleDateFormat(  
                    "dd-MM-yyyy", Locale.getDefault()  
                ).format(new Date()));  
            }  
            if (i == 3 || i == 4 || (i >= 9 && i <= 12)) {  
                inputs[i].setInputType(8194);  
            }  
            panel.addView(inputs[i]);  
        }  

        if (old != null) {  
            inputs[i].setText(old.optString(FIELDS[i], ""));  
        }  
    }  

    panel.addView(text("ATTACH PHOTOS (MAXIMUM 3)", 15, navy, true));  

    LinearLayout photoRow = new LinearLayout(this);  
    for (int i = 0; i < 3; i++) {  
        final int index = i;  
        Button p = button("PHOTO " + (i + 1));  
        photoRow.addView(p, new LinearLayout.LayoutParams(0, -2, 1));  
        p.setOnClickListener(v -> {  
            photoIndex = index;  
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);  
            intent.setType("image/*");  
            intent.addCategory(Intent.CATEGORY_OPENABLE);  
            startActivityForResult(intent, 101);  
        });  
    }  
    panel.addView(photoRow);  

    Button save = button("SAVE RECORD");  
    panel.addView(save);  
    save.setOnClickListener(v -> saveRecord());  

    Button back = button("BACK");  
    panel.addView(back);  
    back.setOnClickListener(v -> showHome());  

    setContentView(scroll);  
}  

private void pickDate(EditText target) {  
    Calendar c = Calendar.getInstance();  
    new DatePickerDialog(this, (view, y, m, d) ->  
        target.setText(String.format(Locale.getDefault(),  
            "%02d-%02d-%04d", d, m + 1, y)),  
        c.get(Calendar.YEAR), c.get(Calendar.MONTH),  
        c.get(Calendar.DAY_OF_MONTH)).show();  
}  

private void saveRecord() {  
    try {  
        JSONObject obj = new JSONObject();  
        for (int i = 0; i < FIELDS.length; i++) {  
            obj.put(FIELDS[i], inputs[i].getText().toString().trim());  
        }  

        JSONArray photoArray = new JSONArray();  
        for (String p : photos) photoArray.put(p == null ? "" : p);  
        obj.put("PHOTOS", photoArray);  

        if (editing >= 0) {  
            records.put(editing, obj);  
        } else {  
            records.put(obj);  
        }  

        saveRecords();  
        msg("RECORD SAVED SUCCESSFULLY");  
        showHome();  
    } catch (Exception e) {  
        msg("SAVE ERROR: " + e.getMessage());  
    }  
}  

private void searchRecords() {  
    EditText q = new EditText(this);  
    q.setHint("BILL NUMBER / CUSTOMER / MOBILE");  

    new AlertDialog.Builder(this)  
        .setTitle("SEARCH RECORD")  
        .setView(q)  
        .setPositiveButton("SEARCH", (d, w) ->  
            showResults(q.getText().toString()))  
        .setNegativeButton("CANCEL", null).show();  
}  

private void showFolder() {  
    Calendar c = Calendar.getInstance();  
    int year = c.get(Calendar.YEAR);  
    String[] months = new String[12];  
    for (int i = 0; i < 12; i++) {  
        months[i] = new java.text.DateFormatSymbols().getMonths()[i];  
    }  

    new AlertDialog.Builder(this)  
        .setTitle("SELECT MONTH")  
        .setItems(months, (d, which) -> {  
            selectedMonth = which + 1;  
            selectedYear = year;  
            showMonthRecords();  
        }).show();  
}  

private void showMonthRecords() {  
    String month = String.format(Locale.getDefault(), "%02d", selectedMonth);  
    JSONArray found = new JSONArray();  

    for (int i = 0; i < records.length(); i++) {  
        JSONObject o = records.optJSONObject(i);  
        String date = o == null ? "" : o.optString("DATE");  
        if (date.endsWith("-" + month + "-" + selectedYear)) {  
            found.put(o);  
        }  
    }  
    showArray(found);  
}  

private void showResults(String query) {  
    JSONArray found = new JSONArray();  
    String q = query.toLowerCase(Locale.ROOT);  

    for (int i = 0; i < records.length(); i++) {  
        JSONObject o = records.optJSONObject(i);  
        if (o == null) continue;  
        String data = o.optString("BILL NUMBER") + " " +  
            o.optString("CUSTOMER NAME") + " " +  
            o.optString("MOBILE NUMBER");  
        if (data.toLowerCase(Locale.ROOT).contains(q)) found.put(o);  
    }  
    showArray(found);  
}  

private void showArray(JSONArray list) {  
    ScrollView scroll = new ScrollView(this);  
    LinearLayout panel = vertical();  
    panel.setPadding(14, 14, 14, 20);  
    panel.setBackgroundColor(pale);  
    scroll.addView(panel);  

    header(panel, "RECORDS: " + list.length());  

    for (int i = 0; i < list.length(); i++) {  
        JSONObject o = list.optJSONObject(i);  
        if (o == null) continue;  

        LinearLayout card = vertical();  
        card.setPadding(12, 12, 12, 12);  
        card.setBackground(shape(white, 14));  
        card.addView(text(o.optString("CUSTOMER NAME"), 18, navy, true));  
        card.addView(text("BILL: " + o.optString("BILL NUMBER"), 14, blue, true));  
        card.addView(text("DATE: " + o.optString("DATE"), 13, navy, false));  
        card.addView(text("MOBILE: " + o.optString("MOBILE NUMBER"), 13, navy, false));  
        card.addView(text("NET AMOUNT: " + o.optString("NET AMOUNT"), 14, navy, true));  

        final JSONObject selected = o;  
        final int recordIndex = findRecord(o);  

        Button edit = button("EDIT");  
        Button delete = button("DELETE");  
        card.addView(edit);  
        card.addView(delete);  

        edit.setOnClickListener(v -> {  
            editing = recordIndex;  
            photos = new String[3];  
            JSONArray a = selected.optJSONArray("PHOTOS");  
            if (a != null) for (int x = 0; x < 3 && x < a.length(); x++)  
                photos[x] = a.optString(x);  
            showFormScreen(selected);  
        });  

        delete.setOnClickListener(v ->  
            new AlertDialog.Builder(this)  
                .setMessage("DELETE THIS RECORD?")  
                .setPositiveButton("YES", (d, w) -> {  
                    if (recordIndex >= 0) {  
                        records.remove(recordIndex);  
                        saveRecords();  
                    }  
                    showHome();  
                }).setNegativeButton("NO", null).show());  

        panel.addView(card);  
    }  

    panel.addView(button("BACK"));  
    setContentView(scroll);  
}  

private int findRecord(JSONObject obj) {  
    for (int i = 0; i < records.length(); i++) {  
        if (records.optJSONObject(i) == obj) return i;  
    }  
    for (int i = 0; i < records.length(); i++) {  
        if (records.optJSONObject(i) != null &&  
            records.optJSONObject(i).toString().equals(obj.toString())) return i;  
    }  
    return -1;  
}  

private void exportCSV() {  
    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);  
    intent.setType("text/csv");  
    intent.addCategory(Intent.CATEGORY_OPENABLE);  
    intent.putExtra(Intent.EXTRA_TITLE, "VIPIN_SERVICE_CRM.csv");  
    startActivityForResult(intent, 102);  
}  

@Override  
protected void onActivityResult(int requestCode, int resultCode, Intent data) {  
    super.onActivityResult(requestCode, resultCode, data);  
    if (resultCode != RESULT_OK || data == null) return;  

    Uri uri = data.getData();  
    if (requestCode == 101) {  
        try {  
            File dir = new File(getFilesDir(), "photos");  
            if (!dir.exists()) dir.mkdirs();  

            File file = new File(dir, "photo_" + System.currentTimeMillis() + ".jpg");  
            InputStream in = getContentResolver().openInputStream(uri);  
            FileOutputStream out = new FileOutputStream(file);  
            byte[] buffer = new byte[8192];  
            int n;  
            while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);  
            in.close();  
            out.close();  
            photos[photoIndex] = file.getAbsolutePath();  
            msg("PHOTO SAVED");  
        } catch (Exception e) {  
            msg("PHOTO ERROR");  
        }  
    }  

    if (requestCode == 102) {  
        try {  
            OutputStream out = getContentResolver().openOutputStream(uri);  
            StringBuilder csv = new StringBuilder();  
            for (String f : FIELDS) csv.append("\"").append(f).append("\",");  
            csv.append("\n");  

            for (int i = 0; i < records.length(); i++) {  
                JSONObject o = records.getJSONObject(i);  
                for (String f : FIELDS) {  
                    String value = o.optString(f, "").replace("\"", "\"\"");  
                    csv.append("\"").append(value).append("\",");  
                }  
                csv.append("\n");  
            }  
            out.write(csv.toString().getBytes("UTF-8"));  
            out.close();  
            msg("CSV EXPORTED");  
        } catch (Exception e) {  
            msg("EXPORT ERROR");  
        }  
    }  
}  

private void changePassword() {  
    EditText old = new EditText(this);  
    old.setHint("OLD PASSWORD");  
    EditText next = new EditText(this);  
    next.setHint("NEW PASSWORD");  

    LinearLayout box = vertical();  
    box.addView(old);  
    box.addView(next);  

    new AlertDialog.Builder(this)  
        .setTitle("CHANGE PASSWORD")  
        .setView(box)  
        .setPositiveButton("SAVE", (d, w) -> {  
            String saved = prefs.getString("password", "Jvipin");  
            if (!old.getText().toString().equals(saved)) {  
                msg("OLD PASSWORD WRONG");  
            } else if (next.getText().toString().trim().isEmpty()) {  
                msg("ENTER NEW PASSWORD");  
            } else {  
                prefs.edit().putString("password",  
                    next.getText().toString()).apply();  
                msg("PASSWORD CHANGED");  
            }  
        }).setNegativeButton("CANCEL", null).show();  
}  

private void loadRecords() {  
    try {  
        records = new JSONArray(prefs.getString(KEY, "[]"));  
    } catch (Exception e) {  
        records = new JSONArray();  
    }  
}  

private void saveRecords() {  
    prefs.edit().putString(KEY, records.toString()).apply();  
}  

private LinearLayout vertical() {  
    LinearLayout l = new LinearLayout(this);  
    l.setOrientation(LinearLayout.VERTICAL);  
    return l;  
}  

private void header(LinearLayout box, String title) {  
    TextView t = text(title, 23, white, true);  
    t.setPadding(16, 20, 16, 20);  
    t.setBackground(shape(navy, 18));  
    box.addView(t);  
}  

private TextView text(String s, int size, int color, boolean bold) {  
    TextView t = new TextView(this);  
    t.setText(s);  
    t.setTextSize(size);  
    t.setTextColor(color);  
    if (bold) t.setTypeface(null, Typeface.BOLD);  
    t.setPadding(5, 7, 5, 7);  
    return t;  
}  

private Button button(String s) {  
    Button b = new Button(this);  
    b.setText(s);  
    b.setTextColor(white);  
    b.setTextSize(14);  
    b.setAllCaps(false);  
    b.setBackground(shape(blue, 14));  
    b.setPadding(12, 8, 12, 8);  
    return b;  
}  

private void addHomeButton(String label, Runnable action) {  
    Button b = button(label);  
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(  
        -1, -2);  
    lp.setMargins(0, 10, 0, 4);  
    root.addView(b, lp);  
    b.setOnClickListener(v -> action.run());  
}  

private void addView(LinearLayout box, View v) {  
    box.addView(v);  
}  

private GradientDrawable shape(int color, int radius) {  
    GradientDrawable d = new GradientDrawable();  
    d.setColor(color);  
    d.setCornerRadius(radius * getResources().getDisplayMetrics().density);  
    return d;  
}  

private void msg(String s) {  
    Toast.makeText(this, s, Toast.LENGTH_SHORT).show();  
}

            }
