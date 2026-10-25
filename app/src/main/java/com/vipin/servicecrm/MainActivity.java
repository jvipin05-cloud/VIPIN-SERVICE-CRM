package com.vipin.servicecrm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

    private LinearLayout listArea;
    private Spinner monthSpinner, yearSpinner;
    private EditText searchBox;

    private final int exportRequest = 7001;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("vipin_crm_data", MODE_PRIVATE);
        loadRecords();
        showLogin();
    }

    private int dp(int n) {
        return (int)(n * getResources().getDisplayMetrics().density + .5f);
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

    private LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w,h);
    }

    private LinearLayout.LayoutParams full() {
        return lp(-1,-2);
    }

    private TextView label(String s,int size,int color,boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        if(bold) t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        t.setPadding(dp(3),dp(5),dp(3),dp(5));
        return t;
    }

    private GradientDrawable shape(int color,int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private Button button(String s,int color) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextColor(Color.WHITE);
        b.setTextSize(12);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setBackground(shape(color,14));
        return b;
    }

    private void showLogin() {
        LinearLayout box = vertical();
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(25),dp(30),dp(25),dp(30));
        box.setBackgroundColor(PALE);

        TextView title = label("VIPIN SERVICE CRM",27,Color.WHITE,true);
        title.setGravity(Gravity.CENTER);
        title.setPadding(dp(12),dp(25),dp(12),dp(25));
        title.setBackground(shape(NAVY,24));
        box.addView(title,full());

        TextView sub = label("OFFLINE SERVICE MANAGEMENT",14,NAVY,true);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0,dp(25),0,dp(15));
        box.addView(sub);

        EditText pass = new EditText(this);
        pass.setSingleLine(true);
        pass.setHint("ENTER APP PASSWORD");
        pass.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD);
        pass.setPadding(dp(15),dp(12),dp(15),dp(12));
        pass.setBackground(shape(Color.WHITE,14));
        box.addView(pass,full());

        Button login = button("OPEN APP",TEAL);
        LinearLayout.LayoutParams p = full();
        p.topMargin = dp(18);
        box.addView(login,p);

        login.setOnClickListener(v -> {
            if("Jvipin".equals(pass.getText().toString())) {
                showCRM();
            } else {
                Toast.makeText(this,"WRONG PASSWORD",
                    Toast.LENGTH_SHORT).show();
            }
        });

        setContentView(box);
    }

    private void showCRM() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = vertical();
        root.setPadding(dp(14),dp(14),dp(14),dp(20));
        root.setBackgroundColor(PALE);
        scroll.addView(root);

        LinearLayout header = vertical();
        header.setPadding(dp(18),dp(18),dp(18),dp(18));
        header.setBackground(shape(NAVY,22));
        header.addView(label("VIPIN SERVICE CRM",24,Color.WHITE,true));
        header.addView(label("OFFLINE • PRIVATE • SERVICE RECORDS",
                12,Color.WHITE,true));
        root.addView(header,full());

        LinearLayout actions = horizontal();

        Button add = button("+ ADD NEW RECORD",TEAL);
        Button export = button("EXPORT EXCEL",BLUE);

        actions.addView(add,lp(0,dp(52)));
        ((LinearLayout.LayoutParams)add.getLayoutParams()).weight=1;

        actions.addView(export,lp(0,dp(52)));
        LinearLayout.LayoutParams ep =
                (LinearLayout.LayoutParams)export.getLayoutParams();
        ep.weight=1;
        ep.leftMargin=dp(8);

        LinearLayout.LayoutParams ap=full();
        ap.topMargin=dp(12);
        root.addView(actions,ap);

        add.setOnClickListener(v -> showRecordForm(-1));
        export.setOnClickListener(v -> exportCsv());

        TextView lastTitle =
                label("LAST MONTH TOTAL",17,NAVY,true);
        LinearLayout.LayoutParams mt=full();
        mt.topMargin=dp(18);
        root.addView(lastTitle,mt);

        LinearLayout cards = vertical();
        cards.addView(totalCard("AMOUNT", "AMOUNT", BLUE));
        cards.addView(totalCard("SPARE AMOUNT", "SPARE AMOUNT", TEAL));
        cards.addView(totalCard("WARRANTY AMOUNT",
                "WARRANTY AMOUNT", RED));
        cards.addView(totalCard("NET AMOUNT", "NET AMOUNT", NAVY));

        root.addView(cards,full());

        TextView recordTitle =
                label("SEARCH RECORDS",17,NAVY,true);
        LinearLayout.LayoutParams st=full();
        st.topMargin=dp(18);
        root.addView(recordTitle,st);

        searchBox = new EditText(this);
        searchBox.setSingleLine(true);
        searchBox.setHint("SEARCH BILL / CUSTOMER / MOBILE / PRODUCT");
        searchBox.setPadding(dp(12),dp(10),dp(12),dp(10));
        searchBox.setBackground(shape(Color.WHITE,12));
        root.addView(searchBox,full());

        TextView filterTitle =
                label("MONTH / YEAR RECORDS",16,NAVY,true);
        LinearLayout.LayoutParams fp=full();
        fp.topMargin=dp(12);
        root.addView(filterTitle,fp);

        LinearLayout filters=horizontal();
        monthSpinner=new Spinner(this);
        yearSpinner=new Spinner(this);

        String[] months={
            "All Months","January","February","March","April",
            "May","June","July","August","September","October",
            "November","December"
        };

        monthSpinner.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item,months));

        int currentYear=Calendar.getInstance().get(Calendar.YEAR);
        String[] years=new String[13];
        years[0]="All Years";
        for(int i=1;i<13;i++)
            years[i]=String.valueOf(currentYear-i+1);

        yearSpinner.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item,years));

        filters.addView(monthSpinner,lp(0,dp(48)));
        filters.addView(yearSpinner,lp(0,dp(48)));

        ((LinearLayout.LayoutParams)monthSpinner.getLayoutParams()).weight=1;
        ((LinearLayout.LayoutParams)yearSpinner.getLayoutParams()).weight=1;

        root.addView(filters,full());

        root.addView(label("RECORD SUMMARY",17,NAVY,true),full());

        listArea=vertical();
        root.addView(listArea,full());

        setContentView(scroll);

        monthSpinner.setOnItemSelectedListener(
                new SimpleSelect(() -> refreshList()));
        yearSpinner.setOnItemSelectedListener(
                new SimpleSelect(() -> refreshList()));

        searchBox.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int a,int c,int f) {}
            public void onTextChanged(CharSequence s,int start,
                                      int before,int count) {
                refreshList();
            }
            public void afterTextChanged(Editable e) {}
        });

        refreshList();
    }

    private View totalCard(String title,String field,int color) {
        LinearLayout card=vertical();
        card.setPadding(dp(14),dp(8),dp(14),dp(8));
        card.setBackground(shape(Color.WHITE,15));

        card.addView(label(title,13,color,true));
        TextView value=label("₹ 0.00",21,NAVY,true);
        card.addView(value);

        card.setTag(field);

        LinearLayout.LayoutParams p=full();
        p.topMargin=dp(7);
        card.setLayoutParams(p);

        card.setOnClickListener(v ->
                Toast.makeText(this,title+" - LAST MONTH",
                        Toast.LENGTH_SHORT).show());

        card.setContentDescription(field);
        card.setId(View.generateViewId());

        return card;
    }

    private double number(String s) {
        try {
            return Double.parseDouble(s.replace(",","").trim());
        } catch(Exception e) {
            return 0;
        }
    }

    private String money(double n) {
        return String.format(Locale.US,"%.2f",n);
    }

    private Calendar parseDate(String date) {
        try {
            Date d=new SimpleDateFormat("dd-MM-yyyy",
                    Locale.US).parse(date);
            Calendar c=Calendar.getInstance();
            c.setTime(d);
            return c;
        } catch(Exception e) {
            return null;
        }
    }

    private boolean isLastMonth(JSONObject r) {
        Calendar previous=Calendar.getInstance();
        previous.add(Calendar.MONTH,-1);

        Calendar date=parseDate(r.optString("DATE"));
        if(date==null) return false;

        return date.get(Calendar.MONTH)==previous.get(Calendar.MONTH)
                && date.get(Calendar.YEAR)==previous.get(Calendar.YEAR);
    }

    private void updateLastMonthCards() {
        double amount=0,spare=0,warranty=0,net=0;

        for(int i=0;i<records.length();i++) {
            JSONObject r=records.optJSONObject(i);
            if(r==null || !isLastMonth(r)) continue;

            amount+=number(r.optString("AMOUNT"));
            spare+=number(r.optString("SPARE AMOUNT"));
            warranty+=number(r.optString("WARRANTY AMOUNT"));
            net+=number(r.optString("NET AMOUNT"));
        }

        setCardValue("AMOUNT",amount);
        setCardValue("SPARE AMOUNT",spare);
        setCardValue("WARRANTY AMOUNT",warranty);
        setCardValue("NET AMOUNT",net);
    }

    private void setCardValue(String field,double value) {
        LinearLayout root=(LinearLayout)listArea.getParent();
        // Card totals are refreshed through the visible card views.
        View decor=findViewById(android.R.id.content);
        updateCardViews(decor,field,value);
    }

    private void updateCardViews(View view,String field,double value) {
    if(view instanceof LinearLayout) {
        LinearLayout l=(LinearLayout)view;

        if(field.equals(String.valueOf(l.getTag()))) {
            if(l.getChildCount()>1 &&
                    l.getChildAt(1) instanceof TextView) {
                ((TextView)l.getChildAt(1))
                    .setText("₹ "+money(value));
            }
        }
    }

    if(view instanceof android.view.ViewGroup) {
        android.view.ViewGroup group =
                (android.view.ViewGroup)view;

        for(int i=0;i<group.getChildCount();i++) {
            updateCardViews(group.getChildAt(i),field,value);
        }
    }
    }
            for(int i=0;i<l.getChildCount();i++)
                updateCardViews(l.getChildAt(i),field,value);
        } else if(view instanceof ScrollView) {
            updateCardViews(((ScrollView)view).getChildAt(0),field,value);
        }
    }

    private boolean matches(JSONObject r,int month,String year) {
        Calendar c=parseDate(r.optString("DATE"));
        if(c==null) return month==0 && "All Years".equals(year);

        int m=c.get(Calendar.MONTH)+1;
        int y=c.get(Calendar.YEAR);

        return (month==0 || month==m) &&
                ("All Years".equals(year) ||
                 String.valueOf(y).equals(year));
    }

    private void refreshList() {
        if(listArea==null) return;

        listArea.removeAllViews();

        int month=monthSpinner.getSelectedItemPosition();
        String year=String.valueOf(yearSpinner.getSelectedItem());

        String search=searchBox.getText().toString()
                .trim().toLowerCase(Locale.ROOT);

        double amc=0,collection=0,amount=0,spare=0,
                warranty=0,net=0;
        int count=0;

        for(int i=records.length()-1;i>=0;i--) {
            JSONObject r=records.optJSONObject(i);
            if(r==null || !matches(r,month,year)) continue;

            if(!r.toString().toLowerCase(Locale.ROOT).contains(search))
                continue;

            count++;
            amc+=number(r.optString("AMC"));
            collection+=number(r.optString("COLLECTION"));
            amount+=number(r.optString("AMOUNT"));
            spare+=number(r.optString("SPARE AMOUNT"));
            warranty+=number(r.optString("WARRANTY AMOUNT"));
            net+=number(r.optString("NET AMOUNT"));

            listArea.addView(recordCard(r,i));
        }

        LinearLayout summary=vertical();
        summary.setPadding(dp(14),dp(14),dp(14),dp(14));
        summary.setBackground(shape(Color.WHITE,16));

        summary.addView(label("RECORDS: "+count,16,NAVY,true));
        summary.addView(label("AMC: "+money(amc),13,Color.DKGRAY,true));
        summary.addView(label("COLLECTION: "+money(collection),
                13,Color.DKGRAY,true));
        summary.addView(label("AMOUNT: "+money(amount),
                13,Color.DKGRAY,true));
        summary.addView(label("SPARE AMOUNT: "+money(spare),
                13,Color.DKGRAY,true));
        summary.addView(label("WARRANTY AMOUNT: "+money(warranty),
                13,Color.DKGRAY,true));
        summary.addView(label("NET AMOUNT: "+money(net),
                17,TEAL,true));

        listArea.addView(summary,0);
        updateLastMonthCards();
    }

    private View recordCard(JSONObject r,int index) {
        LinearLayout card=vertical();
        card.setPadding(dp(14),dp(12),dp(14),dp(12));
        card.setBackground(shape(Color.WHITE,16));

        LinearLayout.LayoutParams cp=full();
        cp.topMargin=dp(9);
        card.setLayoutParams(cp);

        card.addView(label(
                r.optString("CUSTOMER NAME")+" • "+
                r.optString("BILL NUMBER"),17,NAVY,true));

        card.addView(label(
                r.optString("DATE")+" | "+
                r.optString("PRODUCT NAME"),13,Color.DKGRAY,false));

        card.addView(label("MOBILE: "+
                r.optString("MOBILE NUMBER"),13,Color.DKGRAY,false));

        card.addView(label("AMC: "+r.optString("AMC")+
                " | COLLECTION: "+r.optString("COLLECTION"),
                13,Color.DKGRAY,false));

        card.addView(label("AMOUNT: "+r.optString("AMOUNT")+
                " | SPARE AMOUNT: "+r.optString("SPARE AMOUNT"),
                13,Color.DKGRAY,false));

        card.addView(label("WARRANTY: "+
                r.optString("WARRANTY AMOUNT")+
                " | NET: "+r.optString("NET AMOUNT"),
                14,TEAL,true));

        card.addView(label("ADDRESS: "+r.optString("ADDRESS"),
                12,Color.DKGRAY,false));

        card.addView(label("SPARE PARTS: "+
                r.optString("SPARE PARTS"),12,Color.DKGRAY,false));

        card.addView(label("PAYMENT: "+
                r.optString("PAYMENT MODE"),13,Color.DKGRAY,false));

        card.addView(label("REMARKS: "+
                r.optString("REMARKS"),12,Color.DKGRAY,false));

        LinearLayout row=horizontal();
        Button edit=button("EDIT",BLUE);
        Button del=button("DELETE",RED);

        row.addView(edit,lp(0,dp(43)));
        ((LinearLayout.LayoutParams)edit.getLayoutParams()).weight=1;

        row.addView(del,lp(0,dp(43)));
        LinearLayout.LayoutParams dl=
                (LinearLayout.LayoutParams)del.getLayoutParams();
        dl.weight=1;
        dl.leftMargin=dp(8);

        LinearLayout.LayoutParams rp=full();
        rp.topMargin=dp(8);
        card.addView(row,rp);

        edit.setOnClickListener(v -> showRecordForm(index));

        del.setOnClickListener(v ->
            new AlertDialog.Builder(this)
                .setTitle("DELETE RECORD?")
                .setMessage("This record will be deleted from this phone.")
                .setNegativeButton("CANCEL",null)
                .setPositiveButton("DELETE",(d,w) -> {
                    JSONArray next=new JSONArray();
                    for(int j=0;j<records.length();j++) {
                        if(j!=index) next.put(records.optJSONObject(j));
                    }
                    records=next;
                    saveRecords();
                    refreshList();
                }).show());

        return card;
    }

    private void showRecordForm(int editIndex) {
        JSONObject old=editIndex>=0 ?
                records.optJSONObject(editIndex):new JSONObject();

        LinearLayout form=vertical();
        form.setPadding(dp(12),dp(4),dp(12),dp(4));

        EditText[] inputs=new EditText[FIELDS.length];

        String dateDefault=new SimpleDateFormat(
                "dd-MM-yyyy",Locale.US).format(new Date());

        for(int i=0;i<FIELDS.length;i++) {
            String field=FIELDS[i];

            form.addView(label(field,13,NAVY,true),full());

            if("PAYMENT MODE".equals(field)) {
                Spinner sp=new Spinner(this);
                sp.setAdapter(new ArrayAdapter<String>(this,
                        android.R.layout.simple_spinner_dropdown_item,
                        PAYMENTS));

                String selected=old.optString(field,"CASH");
                for(int j=0;j<PAYMENTS.length;j++)
                    if(PAYMENTS[j].equals(selected)) sp.setSelection(j);

                form.addView(sp,full());
                EditText holder=new EditText(this);
                holder.setTag(sp);
                inputs[i]=holder;
                continue;
            }

            EditText e=new EditText(this);
            e.setHint("ENTER "+field);
            e.setText(old.optString(field,
                    "DATE".equals(field)?dateDefault:""));
            e.setPadding(dp(12),dp(9),dp(12),dp(9));
            e.setBackground(shape(Color.rgb(247,249,253),10));

            if("ADDRESS".equals(field) || "REMARKS".equals(field)) {
                e.setSingleLine(false);
                e.setMinLines(2);
            } else {
                e.setSingleLine(true);
            }

            if("DATE".equals(field)) {
                e.setFocusable(false);
                e.setOnClickListener(v -> {
                    Calendar c=Calendar.getInstance();
                    Calendar saved=parseDate(e.getText().toString());
                    if(saved!=null) c=saved;

                    new DatePickerDialog(this,
                        (view,y,m,d) -> e.setText(
                            String.format(Locale.US,
                                "%02d-%02d-%04d",d,m+1,y)),
                        c.get(Calendar.YEAR),
                        c.get(Calendar.MONTH),
                        c.get(Calendar.DAY_OF_MONTH)).show();
                });
            }

            if("MOBILE NUMBER".equals(field)) {
                e.setInputType(InputType.TYPE_CLASS_PHONE);
            }

            if("AMC".equals(field) || "COLLECTION".equals(field) ||
               "AMOUNT".equals(field) || "SPARE AMOUNT".equals(field) ||
               "WARRANTY AMOUNT".equals(field) ||
               "NET AMOUNT".equals(field)) {
                e.setInputType(InputType.TYPE_CLASS_NUMBER |
                        InputType.TYPE_NUMBER_FLAG_DECIMAL);
            }

            form.addView(e,full());
            inputs[i]=e;
        }

        ScrollView sc=new ScrollView(this);
        sc.addView(form);

        AlertDialog dialog=new AlertDialog.Builder(this)
                .setTitle(editIndex>=0?"EDIT RECORD":"NEW SERVICE RECORD")
                .setView(sc)
                .setNegativeButton("CANCEL",null)
                .setPositiveButton("SAVE",null)
                .create();

        dialog.setOnShowListener(v ->
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(btn -> {
                    try {
                        JSONObject obj=new JSONObject();

                        for(int i=0;i<FIELDS.length;i++) {
                            String val;

                            if("PAYMENT MODE".equals(FIELDS[i])) {
                                val=((Spinner)inputs[i].getTag())
                                        .getSelectedItem().toString();
                            } else {
                                val=inputs[i].getText().toString().trim();
                            }

                            if("DATE".equals(FIELDS[i]) && val.isEmpty()) {
                                Toast.makeText(this,"DATE CHOOSE KARE",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }

                            obj.put(FIELDS[i],val);
                        }

                        if(editIndex>=0) records.put(editIndex,obj);
                        else records.put(obj);

                        saveRecords();
                        dialog.dismiss();
                        refreshList();

                        Toast.makeText(this,"RECORD SAVED OFFLINE",
                                Toast.LENGTH_SHORT).show();

                    } catch(Exception ex) {
                        Toast.makeText(this,"SAVE FAILED",
                                Toast.LENGTH_SHORT).show();
                    }
                }));

        dialog.show();

        if(dialog.getWindow()!=null) {
            dialog.getWindow().setLayout(
                (int)(getResources().getDisplayMetrics().widthPixels*.94),
                (int)(getResources().getDisplayMetrics().heightPixels*.88));
        }
    }

    private void exportCsv() {
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE,
            "VIPIN_SERVICE_CRM_"+
            new SimpleDateFormat("yyyyMMdd",Locale.US).format(new Date())
            +".csv");

        startActivityForResult(intent,exportRequest);
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode,resultCode,data);

        if(requestCode==exportRequest && resultCode==RESULT_OK &&
                data!=null && data.getData()!=null) {
            try(OutputStream out=getContentResolver()
                    .openOutputStream(data.getData())) {

                StringBuilder csv=new StringBuilder("\uFEFF");

                for(int i=0;i<FIELDS.length;i++) {
                    if(i>0) csv.append(',');
                    csv.append(csvCell(FIELDS[i]));
                }
                csv.append("\r\n");

                for(int r=0;r<records.length();r++) {
                    JSONObject obj=records.optJSONObject(r);

                    for(int i=0;i<FIELDS.length;i++) {
                        if(i>0) csv.append(',');
                        csv.append(csvCell(
                            obj==null?"":obj.optString(FIELDS[i],"")));
                    }
                    csv.append("\r\n");
                }

                if(out!=null)
                    out.write(csv.toString().getBytes(StandardCharsets.UTF_8));

                Toast.makeText(this,"EXCEL CSV EXPORTED",
                        Toast.LENGTH_LONG).show();

            } catch(Exception e) {
                Toast.makeText(this,"EXPORT FAILED",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private String csvCell(String s) {
        return "\""+(s==null?"":s.replace("\"","\"\""))+"\"";
    }

    private void loadRecords() {
        try {
            records=new JSONArray(
                    prefs.getString("records","[]"));
        } catch(Exception e) {
            records=new JSONArray();
        }
    }

    private void saveRecords() {
        prefs.edit().putString("records",records.toString()).apply();
    }

    private class SimpleSelect
            implements AdapterView.OnItemSelectedListener {

        private Runnable action;

        SimpleSelect(Runnable r) {
            action=r;
        }

        public void onItemSelected(AdapterView<?> p,View v,
                                   int pos,long id) {
            action.run();
        }

        public void onNothingSelected(AdapterView<?> p) {}
    }
}
