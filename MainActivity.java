;
package com.vipin.servicecrm;
import android.view.ViewGroup

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
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

    private final String[] FIELDS = {
        "DATE", "BILL NUMBER", "PRODUCT NAME", "AMC", "COLLECTION",
        "CUSTOMER NAME", "MOBILE NUMBER", "ADDRESS", "SPARE PARTS",
        "AMOUNT", "SPARE AMOUNT", "WARRANTY AMOUNT", "NET AMOUNT",
        "PAYMENT MODE", "REMARKS"
    };

    private final String[] PAYMENTS = {
        "CASH", "UPI", "PS UPI", "RKS UPI"
    };

    private final int NAVY = Color.rgb(24,42,85);
    private final int BLUE = Color.rgb(35,105,210);
    private final int TEAL = Color.rgb(0,150,136);
    private final int PALE = Color.rgb(239,245,255);
    private final int RED = Color.rgb(190,55,65);

    private SharedPreferences prefs;
    private JSONArray records = new JSONArray();
    private LinearLayout root, listArea;
    private Spinner monthSpinner, yearSpinner;
    private int exportRequest = 7001;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        );
        prefs = getSharedPreferences("vipin_crm_data", MODE_PRIVATE);
        loadRecords();
        showLogin();
    }

    private void showLogin() {
        LinearLayout box = vertical();
        box.setGravity(Gravity.CENTER);
        box.setPadding(28,32,28,32);
        box.setBackgroundColor(PALE);

        TextView title = label("VIPIN SERVICE CRM",27,Color.WHITE,true);
        title.setGravity(Gravity.CENTER);
        title.setPadding(12,24,12,24);
        title.setBackground(round(NAVY,24));
        box.addView(title,matchWrap());

        TextView sub = label(
            "OFFLINE SERVICE RECORD SYSTEM",14,NAVY,true
        );
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0,24,0,16);
        box.addView(sub);

        EditText pass = new EditText(this);
        pass.setSingleLine(true);
        pass.setHint("Enter App Password");
        pass.setInputType(
            InputType.TYPE_CLASS_TEXT |
            InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        pass.setPadding(18,12,18,12);
        pass.setBackground(round(Color.WHITE,14));
        box.addView(pass,matchWrap());

        Button login = button("OPEN APP",TEAL);
        LinearLayout.LayoutParams lp = matchWrap();
        lp.topMargin = dp(18);
        box.addView(login,lp);

        login.setOnClickListener(v -> {
            if ("Jvipin".equals(pass.getText().toString())) {
                showCRM();
            } else {
                Toast.makeText(this,"Wrong Password",
                    Toast.LENGTH_SHORT).show();
            }
        });

        setContentView(box);
    }

    // MAIN HOME PAGE

    private void showCRM() {
        listArea = null;
        ScrollView scroll = new ScrollView(this);
        root = vertical();
        root.setPadding(14,14,14,20);
        root.setBackgroundColor(PALE);
        scroll.addView(root);

        LinearLayout header = vertical();
        header.setPadding(18,18,18,18);
        header.setBackground(round(NAVY,22));
        header.addView(label(
            "VIPIN SERVICE CRM",23,Color.WHITE,true
        ));
        header.addView(label(
            "OFFLINE • PRIVATE • SERVICE RECORDS",
            13,Color.WHITE,false
        ));
        root.addView(header,matchWrap());

        Button add = button("+ ADD NEW RECORD",TEAL);
        Button search = button("SEARCH RECORD",BLUE);
        Button folder = button("RECORD FOLDER",NAVY);
        Button export = button("EXPORT EXCEL / CSV",TEAL);

        addHomeButton(add);
        addHomeButton(search);
        addHomeButton(folder);
        addHomeButton(export);

        add.setOnClickListener(v -> showRecordForm(-1));
        search.setOnClickListener(v -> showSearch());
        folder.setOnClickListener(v -> showRecordFolder());
        export.setOnClickListener(v -> exportCsv());

        setContentView(scroll);
    }

    private void addHomeButton(Button b) {
        LinearLayout.LayoutParams p = matchWrap();
        p.topMargin = dp(13);
        b.setHeight(dp(55));
        root.addView(b,p);
    }

    // SEARCH

    private void showSearch() {
        LinearLayout box = vertical();
        box.setPadding(12,8,12,8);

        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("BILL NUMBER / CUSTOMER NAME / MOBILE");
        input.setPadding(12,12,12,12);
        input.setBackground(round(PALE,12));
        box.addView(input,matchWrap());

        LinearLayout results = vertical();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(results);
        LinearLayout.LayoutParams rp = matchWrap();
        rp.topMargin = dp(10);
        box.addView(scroll,rp);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("SEARCH RECORD")
            .setView(box)
            .setNegativeButton("CLOSE",null)
            .create();

        Runnable update = () -> {
            results.removeAllViews();
            String q = input.getText().toString()
                .trim().toLowerCase(Locale.ROOT);

            if (q.isEmpty()) {
                results.addView(label(
                    "Enter search details above",14,NAVY,false
                ));
                return;
            }

            int found = 0;
            for (int i=records.length()-1;i>=0;i--) {
                JSONObject r = records.optJSONObject(i);
                if (r == null) continue;

                String bill = r.optString("BILL NUMBER").toLowerCase(Locale.ROOT);
                String customer = r.optString("CUSTOMER NAME").toLowerCase(Locale.ROOT);
                String mobile = r.optString("MOBILE NUMBER").toLowerCase(Locale.ROOT);

                if (bill.contains(q) || customer.contains(q) ||
                    mobile.contains(q)) {
                    results.addView(recordCard(r,i));
                    results.addView(new View(this),lineParams());
                    found++;
                }
            }

            if (found == 0) {
                results.addView(label(
                    "NO RECORD FOUND",15,RED,true
                ));
            }
        };

        input.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a) {}
            public void onTextChanged(CharSequence s,int st,int before,int count) {
                update.run();
            }
            public void afterTextChanged(Editable e) {}
        });

        dialog.setOnShowListener(v -> {
            input.requestFocus();
            dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            );
            update.run();
        });
        dialog.show();
        dialog.getWindow().setLayout(
            (int)(getResources().getDisplayMetrics().widthPixels*.94),
            (int)(getResources().getDisplayMetrics().heightPixels*.80)
        );
    }

    // RECORD FOLDER - SEPARATE PAGE

    private void showRecordFolder() {
        ScrollView page = new ScrollView(this);
        root = vertical();
        root.setPadding(14,14,14,20);
        root.setBackgroundColor(PALE);
        page.addView(root);

        LinearLayout header = vertical();
        header.setPadding(16,16,16,16);
        header.setBackground(round(NAVY,18));
        header.addView(label(
            "RECORD FOLDER",23,Color.WHITE,true
        ));
        root.addView(header,matchWrap());

        Button back = button("← BACK TO HOME",BLUE);
        LinearLayout.LayoutParams bp = matchWrap();
        bp.topMargin = dp(12);
        root.addView(back,bp);
        back.setOnClickListener(v -> showCRM());

        root.addView(label(
            "SELECT MONTH AND YEAR",16,NAVY,true
        ));

        LinearLayout filters = new LinearLayout(this);
        filters.setOrientation(LinearLayout.HORIZONTAL);

        monthSpinner = new Spinner(this);
        yearSpinner = new Spinner(this);

        String[] months = {
            "All Months","January","February","March","April",
            "May","June","July","August","September",
            "October","November","December"
        };

        monthSpinner.setAdapter(new ArrayAdapter<String>(
            this,android.R.layout.simple_spinner_dropdown_item,months
        ));

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        String[] years = new String[32];
        years[0] = "All Years";
        for (int i=1;i<years.length;i++) {
            years[i] = String.valueOf(currentYear-i+1);
        }

        yearSpinner.setAdapter(new ArrayAdapter<String>(
            this,android.R.layout.simple_spinner_dropdown_item,years
        ));

        filters.addView(monthSpinner,
            new LinearLayout.LayoutParams(0,dp(48),1));
        filters.addView(yearSpinner,
            new LinearLayout.LayoutParams(0,dp(48),1));

        LinearLayout.LayoutParams flp = matchWrap();
        flp.topMargin = dp(8);
        root.addView(filters,flp);

        listArea = vertical();
        LinearLayout.LayoutParams lap = matchWrap();
        lap.topMargin = dp(12);
        root.addView(listArea,lap);

        monthSpinner.setOnItemSelectedListener(
            new SimpleSelect(() -> refreshList())
        );
        yearSpinner.setOnItemSelectedListener(
            new SimpleSelect(() -> refreshList())
        );

        setContentView(page);
        refreshList();
    }

    private void refreshList() {
        if (listArea == null) return;

        listArea.removeAllViews();

        int month = monthSpinner == null ? 0 :
            monthSpinner.getSelectedItemPosition();

        String year = yearSpinner == null ? "All Years" :
            String.valueOf(yearSpinner.getSelectedItem());

        double amc = 0, collection = 0, amount = 0;
        double spare = 0, warranty = 0, net = 0;
        int count = 0;

        for (int i=records.length()-1;i>=0;i--) {
            JSONObject r = records.optJSONObject(i);
            if (r == null || !matches(r,month,year)) continue;

            count++;
            amc += number(r.optString("AMC"));
            collection += number(r.optString("COLLECTION"));
            amount += number(r.optString("AMOUNT"));
            spare += number(r.optString("SPARE AMOUNT"));
            warranty += number(r.optString("WARRANTY AMOUNT"));
            net += number(r.optString("NET AMOUNT"));

            listArea.addView(recordCard(r,i));

            // SEPARATE LINE BETWEEN RECORDS
            listArea.addView(new View(this),lineParams());
        }

        if (count == 0) {
            listArea.addView(label(
                "NO RECORD FOUND",16,RED,true
            ));
        }

        // ALL TOTALS AT THE VERY BOTTOM

        LinearLayout summary = vertical();
        summary.setPadding(16,16,16,16);
        summary.setBackground(round(Color.WHITE,16));

        summary.addView(label(
            "TOTAL SUMMARY",19,NAVY,true
        ));
        summary.addView(label(
            "TOTAL RECORDS : "+count,14,BLUE,true
        ));

        addTotal(summary,"AMC TOTAL",amc);
        addTotal(summary,"COLLECTION TOTAL",collection);
        addTotal(summary,"AMOUNT TOTAL",amount);
        addTotal(summary,"SPARE AMOUNT TOTAL",spare);
        addTotal(summary,"WARRANTY AMOUNT TOTAL",warranty);
        addTotal(summary,"NET AMOUNT TOTAL",net);

        LinearLayout.LayoutParams sp = matchWrap();
        sp.topMargin = dp(14);
        listArea.addView(summary,sp);
    }

    private void addTotal(LinearLayout box,String name,double value) {
        TextView t = label(
            name+" : ₹ "+money(value),15,TEAL,true
        );
        box.addView(t,matchWrap());
        box.addView(new View(this),lineParams());
    }

    private boolean matches(JSONObject r,int month,String year) {
        String date = r.optString("DATE");
        if (date.length() < 10)
            return month == 0 && "All Years".equals(year);

        try {
            Calendar c = Calendar.getInstance();
            c.setTime(new SimpleDateFormat(
                "dd-MM-yyyy",Locale.US
            ).parse(date));

            int m = c.get(Calendar.MONTH)+1;
            int y = c.get(Calendar.YEAR);

            return (month == 0 || month == m) &&
                ("All Years".equals(year) ||
                String.valueOf(y).equals(year));

        } catch(Exception e) {
            return false;
        }
    }

    // RECORD CARD

    private View recordCard(JSONObject r,int index) {
        LinearLayout card = vertical();
        card.setPadding(14,12,14,12);
        card.setBackground(round(Color.WHITE,16));

        card.addView(label(
            r.optString("CUSTOMER NAME")+" • "+
            r.optString("BILL NUMBER"),17,NAVY,true
        ));

        card.addView(label(
            "DATE: "+r.optString("DATE")+
            " | PRODUCT: "+r.optString("PRODUCT NAME"),
            13,Color.DKGRAY,false
        ));

        card.addView(label(
            "MOBILE: "+r.optString("MOBILE NUMBER"),
            13,Color.DKGRAY,false
        ));

        card.addView(label(
            "AMC: "+r.optString("AMC")+
            " | COLLECTION: "+r.optString("COLLECTION"),
            13,Color.DKGRAY,false
        ));

        card.addView(label(
            "AMOUNT: "+r.optString("AMOUNT"),
            13,Color.DKGRAY,false
        ));

        card.addView(label(
            "SPARE AMOUNT: "+r.optString("SPARE AMOUNT"),
            13,Color.DKGRAY,false
        ));

        card.addView(label(
            "WARRANTY AMOUNT: "+r.optString("WARRANTY AMOUNT"),
            13,Color.DKGRAY,false
        ));

        card.addView(label(
            "NET AMOUNT: "+r.optString("NET AMOUNT"),
            14,TEAL,true
        ));

        card.addView(label(
            "PAYMENT MODE: "+r.optString("PAYMENT MODE"),
            13,Color.DKGRAY,false
        ));

        LinearLayout row = new LinearLayout(this);

        Button edit = button("EDIT",BLUE);
        Button del = button("DELETE",RED);

        row.addView(edit,new LinearLayout.LayoutParams(0,dp(42),1));

        LinearLayout.LayoutParams dlp =
            new LinearLayout.LayoutParams(0,dp(42),1);
        dlp.leftMargin = dp(8);
        row.addView(del,dlp);

        LinearLayout.LayoutParams rp = matchWrap();
        rp.topMargin = dp(8);
        card.addView(row,rp);

        edit.setOnClickListener(v -> showRecordForm(index));

        del.setOnClickListener(v ->
            new AlertDialog.Builder(this)
                .setTitle("DELETE RECORD?")
                .setMessage("This record will be deleted from this phone.")
                .setNegativeButton("CANCEL",null)
                .setPositiveButton("DELETE",(d,w) -> {
                    JSONArray next = new JSONArray();
                    for (int j=0;j<records.length();j++) {
                        if (j != index) next.put(records.optJSONObject(j));
                    }
                    records = next;
                    saveRecords();
                    refreshList();
                }).show()
        );

        return card;
    }

    // ADD / EDIT FORM

    private void showRecordForm(int editIndex) {
        JSONObject old = editIndex >= 0 ?
            records.optJSONObject(editIndex) : new JSONObject();

        if (old == null) old = new JSONObject();

        LinearLayout form = vertical();
        form.setPadding(12,4,12,12);

        EditText[] inputs = new EditText[FIELDS.length];

        String dateDefault = new SimpleDateFormat(
            "dd-MM-yyyy",Locale.US
        ).format(new Date());

        ScrollView sc = new ScrollView(this);
        sc.setFillViewport(true);
        sc.setDescendantFocusability(
            ViewGroup.FOCUS_AFTER_DESCENDANTS
        );
        sc.addView(form);

        for (int i=0;i<FIELDS.length;i++) {
            final int fieldIndex = i;

            TextView lab = label(FIELDS[i],13,NAVY,true);
            LinearLayout.LayoutParams lp = matchWrap();
            lp.topMargin = dp(7);
            form.addView(lab,lp);

            if ("PAYMENT MODE".equals(FIELDS[i])) {
                Spinner sp = new Spinner(this);
                sp.setAdapter(new ArrayAdapter<String>(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    PAYMENTS
                ));

                String selected = old.optString(FIELDS[i],"CASH");
                for (int j=0;j<PAYMENTS.length;j++) {
                    if (PAYMENTS[j].equals(selected)) sp.setSelection(j);
                }

                form.addView(sp,matchWrap());
                EditText holder = new EditText(this);
                holder.setTag(sp);
                inputs[i] = holder;

            } else {
                EditText e = new EditText(this);
                e.setSingleLine(
                    !"ADDRESS".equals(FIELDS[i]) &&
                    !"REMARKS".equals(FIELDS[i])
                );
                e.setHint("Enter "+FIELDS[i]);

                e.setText(old.optString(
                    FIELDS[i],
                    "DATE".equals(FIELDS[i]) ? dateDefault : ""
                ));

                e.setPadding(12,8,12,8);
                e.setBackground(round(
                    Color.rgb(247,249,253),10
                ));

                if (i == 0) {
                    e.setFocusable(false);
                    e.setOnClickListener(v -> {
                        Calendar c = Calendar.getInstance();
                        try {
                            c.setTime(new SimpleDateFormat(
                                "dd-MM-yyyy",Locale.US
                            ).parse(e.getText().toString()));
                        } catch(Exception ignored) {}

                        new DatePickerDialog(
                            this,
                            (view,y,m,d) -> e.setText(
                                String.format(
                                    Locale.US,"%02d-%02d-%04d",
                                    d,m+1,y
                                )
                            ),
                            c.get(Calendar.YEAR),
                            c.get(Calendar.MONTH),
                            c.get(Calendar.DAY_OF_MONTH)
                        ).show();
                    });

                } else if (i == 6) {
                    e.setInputType(InputType.TYPE_CLASS_PHONE);

                } else if (i==3 || i==4 || i==9 ||
                    i==10 || i==11 || i==12) {

                    e.setInputType(
                        InputType.TYPE_CLASS_NUMBER |
                        InputType.TYPE_NUMBER_FLAG_DECIMAL |
                        InputType.TYPE_NUMBER_FLAG_SIGNED
                    );

                } else if ("ADDRESS".equals(FIELDS[i]) ||
                           "REMARKS".equals(FIELDS[i])) {
                    e.setSingleLine(false);
                    e.setMinLines(2);
                }

                e.setImeOptions(EditorInfo.IME_ACTION_NEXT);

                e.setOnFocusChangeListener((v,hasFocus) -> {
                    if (hasFocus) {
                        sc.postDelayed(() ->
                            sc.smoothScrollTo(0,e.getBottom()),
                            250
                        );
                    }
                });

                form.addView(e,matchWrap());
                inputs[i] = e;
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(editIndex >= 0 ?
                "EDIT RECORD" : "NEW SERVICE RECORD")
            .setView(sc)
            .setNegativeButton("CANCEL",null)
            .setPositiveButton("SAVE",null)
            .create();

        dialog.setOnShowListener(v -> {
            dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            );

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(btn -> {
                    try {
                        JSONObject obj = new JSONObject();

                        for (int i=0;i<FIELDS.length;i++) {
                            String val;

                            if ("PAYMENT MODE".equals(FIELDS[i])) {
                                val = ((Spinner)inputs[i].getTag())
                                    .getSelectedItem().toString();
                            } else {
                                val = inputs[i].getText().toString().trim();
                            }

                            if ("DATE".equals(FIELDS[i]) && val.isEmpty()) {
                                Toast.makeText(this,
                                    "DATE चुनें",Toast.LENGTH_SHORT).show();
                                return;
                            }

                            obj.put(FIELDS[i],val);
                        }

                        if (editIndex >= 0) {
                            records.put(editIndex,obj);
                        } else {
                            records.put(obj);
                        }

                        saveRecords();
                        dialog.dismiss();

                        if (listArea != null) refreshList();

                        Toast.makeText(this,
                            "Record saved offline",
                            Toast.LENGTH_SHORT).show();

                    } catch(Exception ex) {
                        Toast.makeText(this,
                            "Save failed",Toast.LENGTH_SHORT).show();
                    }
                });
        });

        dialog.show();

        dialog.getWindow().setLayout(
            (int)(getResources().getDisplayMetrics().widthPixels*.94),
            (int)(getResources().getDisplayMetrics().heightPixels*.82)
        );
    }

    // EXPORT CSV FOR EXCEL

    private void exportCsv() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");

        intent.putExtra(
            Intent.EXTRA_TITLE,
            "VIPIN_SERVICE_CRM_"+
            new SimpleDateFormat("yyyyMMdd",Locale.US).format(new Date())+
            ".csv"
        );

        startActivityForResult(intent,exportRequest);
    }

    @Override
    protected void onActivityResult(
        int requestCode,int resultCode,Intent data
    ) {
        super.onActivityResult(requestCode,resultCode,data);

        if (requestCode == exportRequest &&
            resultCode == RESULT_OK &&
            data != null && data.getData() != null) {

            try (OutputStream out = getContentResolver()
                .openOutputStream(data.getData())) {

                StringBuilder csv = new StringBuilder("\uFEFF");

                for (int i=0;i<FIELDS.length;i++) {
                    if (i>0) csv.append(',');
                    csv.append(csvCell(FIELDS[i]));
                }
                csv.append("\r\n");

                for (int r=0;r<records.length();r++) {
                    JSONObject obj = records.optJSONObject(r);

                    for (int i=0;i<FIELDS.length;i++) {
                        if (i>0) csv.append(',');
                        csv.append(csvCell(
                            obj == null ? "" :
                            obj.optString(FIELDS[i],"")
                        ));
                    }
                    csv.append("\r\n");
                }

                out.write(csv.toString().getBytes(StandardCharsets.UTF_8));

                Toast.makeText(this,
                    "Excel CSV exported successfully",
                    Toast.LENGTH_LONG).show();

            } catch(Exception e) {
                Toast.makeText(this,
                    "Export failed",Toast.LENGTH_LONG).show();
            }
        }
    }

    private String csvCell(String s) {
        return "\"" + (s == null ? "" :
            s.replace("\"","\"\"")) + "\"";
    }

    // OFFLINE STORAGE

    private void loadRecords() {
        try {
            records = new JSONArray(
                prefs.getString("records","[]")
            );
        } catch(Exception e) {
            records = new JSONArray();
        }
    }

    private void saveRecords() {
        prefs.edit().putString("records",records.toString()).apply();
    }

    private double number(String s) {
        try {
            return Double.parseDouble(
                s.replace(",","").trim()
            );
        } catch(Exception e) {
            return 0;
        }
    }

    private String money(double n) {
        return String.format(Locale.US,"%.2f",n);
    }

    // DESIGN HELPERS

    private LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private TextView label(
        String s,int size,int color,boolean bold
    ) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);

        if (bold) {
            t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        }

        t.setPadding(2,5,2,5);
        return t;
    }

    private Button button(String s,int color) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextColor(Color.WHITE);
        b.setTextSize(12);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setBackground(round(color,14));
        return b;
    }

    private android.graphics.drawable.GradientDrawable round(
        int color,int radius
    ) {
        android.graphics.drawable.GradientDrawable d =
            new android.graphics.drawable.GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private ViewGroup.LayoutParams lineParams() {
        View v = new View(this);
        v.setBackgroundColor(Color.rgb(205,215,230));
        return new LinearLayout.LayoutParams(
            -1,dp(1)
        );
    }

    private int dp(int n) {
        return (int)(
            n*getResources().getDisplayMetrics().density+.5f
        );
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1,-2);
    }

    private class SimpleSelect
        implements AdapterView.OnItemSelectedListener {

        private Runnable action;

        SimpleSelect(Runnable r) {
            action = r;
        }

        public void onItemSelected(
            AdapterView<?> p,View v,int pos,long id
        ) {
            action.run();
        }

        public void onNothingSelected(AdapterView<?> p) {}
    }
}
