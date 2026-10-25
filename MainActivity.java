package com.vipin.servicecrm;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {

    private final int navy = Color.rgb(28,45,95);
    private final int blue = Color.rgb(35,105,220);
    private final int purple = Color.rgb(125,75,205);
    private final int green = Color.rgb(20,155,105);
    private final int orange = Color.rgb(240,140,35);
    private final int red = Color.rgb(210,65,75);
    private final int pale = Color.rgb(242,246,255);

    SharedPreferences prefs;
    JSONArray records;
    LinearLayout root, recordsBox, totalsBox;

    EditText bill, product, amc, collection;
    EditText customer, mobile, address, spareParts;
    EditText amount, spareAmount, warranty, net, remarks;
    EditText search;
    TextView dateText;

    Spinner payment, monthFilter, yearFilter;
    String selectedDate;
    int exportRequest = 701;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        prefs = getSharedPreferences("VIPIN_SERVICE_CRM", MODE_PRIVATE);

        if (!prefs.contains("password")) {
            prefs.edit().putString("password","Jvipin").apply();
        }

        try {
            records = new JSONArray(prefs.getString("records","[]"));
        } catch(Exception e) {
            records = new JSONArray();
        }

        selectedDate = today();
        showLogin();
    }

    String today() {
        return new SimpleDateFormat(
            "dd-MM-yyyy", Locale.getDefault()
        ).format(Calendar.getInstance().getTime());
    }

    int dp(int n) {
        return (int)(n * getResources().getDisplayMetrics().density);
    }

    GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if(bold) t.setTypeface(null, Typeface.BOLD);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    LinearLayout horizontal() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    void pad(View v, int n) {
        v.setPadding(dp(n),dp(n),dp(n),dp(n));
    }

    void gap(LinearLayout l, int h) {
        View v = new View(this);
        l.addView(v,new LinearLayout.LayoutParams(1,dp(h)));
    }

    Button button(String label, int color) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(15);
        b.setAllCaps(false);
        b.setTypeface(null,Typeface.BOLD);
        b.setBackground(shape(color,18));
        return b;
    }

    EditText input(String hint, int type) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(15);
        e.setSingleLine(true);
        e.setInputType(type);
        e.setPadding(dp(14),dp(8),dp(14),dp(8));
        e.setBackground(shape(Color.WHITE,12));
        return e;
    }

    void field(LinearLayout box, String label, EditText e) {
        box.addView(text(label,12,navy,true));
        box.addView(e,new LinearLayout.LayoutParams(-1,dp(49)));
        gap(box,6);
    }

    void showLogin() {
        LinearLayout l = vertical();
        l.setGravity(Gravity.CENTER);
        l.setBackgroundColor(navy);
        pad(l,25);

        TextView logo = text("VIPIN",35,Color.WHITE,true);
        logo.setGravity(Gravity.CENTER);
        l.addView(logo,new LinearLayout.LayoutParams(-1,dp(60)));

        TextView sub = text("SERVICE CRM",22,Color.rgb(255,205,90),true);
        sub.setGravity(Gravity.CENTER);
        l.addView(sub);
        gap(l,25);

        final EditText pass = input("Enter Password",129);
        l.addView(pass,new LinearLayout.LayoutParams(-1,dp(55)));
        gap(l,15);

        Button login = button("OPEN APP",green);
        l.addView(login,new LinearLayout.LayoutParams(-1,dp(55)));

        login.setOnClickListener(v -> {
            if(pass.getText().toString().equals(
                prefs.getString("password","Jvipin"))) {
                showCRM();
            } else {
                pass.setError("Wrong password");
            }
        });

        TextView hint = text("Default Password: Jvipin",13,
            Color.LTGRAY,false);
        hint.setGravity(Gravity.CENTER);
        l.addView(hint);
        setContentView(l);
    }

    void showCRM() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        root = vertical();
        root.setBackgroundColor(pale);
        pad(root,14);
        scroll.addView(root);

        LinearLayout header = vertical();
        header.setBackground(shape(navy,20));
        pad(header,18);
        header.addView(text("VIPIN SERVICE CRM",24,
            Color.WHITE,true));
        header.addView(text("Offline Service & Collection Manager",
            13,Color.LTGRAY,false));
        root.addView(header);
        gap(root,12);

        totalsBox = vertical();
        totalsBox.setBackground(shape(Color.WHITE,16));
        pad(totalsBox,12);
        root.addView(totalsBox);
        updateTotals();

        root.addView(text("ADD NEW RECORD",19,navy,true));
        gap(root,8);

        dateText = text("DATE: "+selectedDate+"  (Tap to change)",
            15,blue,true);
        dateText.setBackground(shape(Color.WHITE,12));
        pad(dateText,14);
        root.addView(dateText,new LinearLayout.LayoutParams(-1,dp(50)));
        dateText.setOnClickListener(v -> pickDate());
        gap(root,8);

        bill = input("Bill Number",2);
        product = input("Product Name",1);
        amc = input("AMC Amount",8194);
        collection = input("Collection Amount",8194);
        customer = input("Customer Name",1);
        mobile = input("Mobile Number",3);
        address = input("Address",1);
        spareParts = input("Spare Parts",1);
        amount = input("Amount",8194);
        spareAmount = input("Spare Amount",8194);
        warranty = input("Warranty Amount",8194);
        net = input("Net Amount",8194);
        remarks = input("Remarks",1);

        field(root,"1. BILL NUMBER",bill);
        field(root,"2. PRODUCT NAME",product);
        field(root,"3. AMC",amc);
        field(root,"4. COLLECTION",collection);
        field(root,"5. CUSTOMER NAME",customer);
        field(root,"6. MOBILE NUMBER",mobile);
        field(root,"7. ADDRESS",address);
        field(root,"8. SPARE PARTS",spareParts);
        field(root,"9. AMOUNT",amount);
        field(root,"10. SPARE AMOUNT",spareAmount);
        field(root,"11. WARRANTY AMOUNT",warranty);
        field(root,"12. NET AMOUNT",net);

        root.addView(text("13. PAYMENT MODE",12,navy,true));
        String[] modes = {"CASH","UPI","PS UPI","RKS UPI"};
        payment = new Spinner(this);
        ArrayAdapter<String> pa = new ArrayAdapter<>(
            this,android.R.layout.simple_spinner_item,modes);
        pa.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item);
        payment.setAdapter(pa);
        root.addView(payment,new LinearLayout.LayoutParams(-1,dp(48)));
        gap(root,6);

        field(root,"14. REMARKS",remarks);

        Button save = button("SAVE RECORD",green);
        root.addView(save,new LinearLayout.LayoutParams(-1,dp(52)));
        save.setOnClickListener(v -> saveRecord());
        gap(root,8);

        Button export = button("EXPORT TO EXCEL CSV",blue);
        root.addView(export,new LinearLayout.LayoutParams(-1,dp(50)));
        export.setOnClickListener(v -> exportCSV());
        gap(root,15);

        root.addView(text("SEARCH RECORDS",18,navy,true));
        search = input("Search Bill / Customer / Mobile / Product",1);
        root.addView(search,new LinearLayout.LayoutParams(-1,dp(50)));

        root.addView(text("MONTH / YEAR FILTER",16,navy,true));
        LinearLayout filters = horizontal();

        monthFilter = new Spinner(this);
        yearFilter = new Spinner(this);

        String[] months = {
            "All Months","January","February","March","April",
            "May","June","July","August","September",
            "October","November","December"
        };

        ArrayAdapter<String> ma = new ArrayAdapter<>(
            this,android.R.layout.simple_spinner_item,months);
        ma.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item);
        monthFilter.setAdapter(ma);

        java.util.ArrayList<String> years = new java.util.ArrayList<>();
        years.add("All Years");
        int current = Calendar.getInstance().get(Calendar.YEAR);
        for(int y=current+1;y>=current-10;y--)
            years.add(String.valueOf(y));

        ArrayAdapter<String> ya = new ArrayAdapter<>(
            this,android.R.layout.simple_spinner_item,years);
        ya.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item);
        yearFilter.setAdapter(ya);

        filters.addView(monthFilter,
            new LinearLayout.LayoutParams(0,dp(48),1));
        filters.addView(yearFilter,
            new LinearLayout.LayoutParams(0,dp(48),1));
        root.addView(filters);

        recordsBox = vertical();
        root.addView(recordsBox);
        gap(root,12);

        Button change = button("CHANGE PASSWORD",purple);
        root.addView(change,new LinearLayout.LayoutParams(-1,dp(48)));
        change.setOnClickListener(v -> changePassword());

        Button logout = button("LOGOUT",red);
        root.addView(logout,new LinearLayout.LayoutParams(-1,dp(48)));
        logout.setOnClickListener(v -> showLogin());

        TextWatcher watcher = new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int before,int count) {
                updateRecords();
            }
            public void afterTextChanged(Editable e){}
        };
        search.addTextChangedListener(watcher);

        AdapterView.OnItemSelectedListener listener =
            new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> p,
                    View v,int pos,long id) { updateRecords(); }
                public void onNothingSelected(AdapterView<?> p){}
            };
        monthFilter.setOnItemSelectedListener(listener);
        yearFilter.setOnItemSelectedListener(listener);

        updateRecords();
        setContentView(scroll);
    }

    void pickDate() {
        Calendar c = Calendar.getInstance();
        try {
            String[] p = selectedDate.split("-");
            c.set(Integer.parseInt(p[2]),
                Integer.parseInt(p[1])-1,
                Integer.parseInt(p[0]));
        } catch(Exception ignored) {}

        new DatePickerDialog(this,(view,y,m,d) -> {
            selectedDate = String.format(Locale.getDefault(),
                "%02d-%02d-%04d",d,m+1,y);
            dateText.setText("DATE: "+selectedDate+"  (Tap to change)");
        },c.get(Calendar.YEAR),c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)).show();
    }

    double num(EditText e) {
        try {
            return Double.parseDouble(
                e.getText().toString().trim().replace(",",""));
        } catch(Exception ex) {
            return 0;
        }
    }

    void saveRecord() {
        if(bill.getText().toString().trim().isEmpty()) {
            bill.setError("Enter Bill Number");
            return;
        }
        if(product.getText().toString().trim().isEmpty()) {
            product.setError("Enter Product Name");
            return;
        }

        try {
            JSONObject r = new JSONObject();
            r.put("date",selectedDate);
            r.put("bill",bill.getText().toString().trim());
            r.put("product",product.getText().toString().trim());
            r.put("amc",num(amc));
            r.put("collection",num(collection));
            r.put("customer",customer.getText().toString().trim());
            r.put("mobile",mobile.getText().toString().trim());
            r.put("address",address.getText().toString().trim());
            r.put("spareParts",spareParts.getText().toString().trim());
            r.put("amount",num(amount));
            r.put("spareAmount",num(spareAmount));
            r.put("warranty",num(warranty));
            r.put("net",num(net));
            r.put("payment",payment.getSelectedItem().toString());
            r.put("remarks",remarks.getText().toString().trim());

            records.put(r);
            prefs.edit().putString("records",records.toString()).apply();

            Toast.makeText(this,"Record Saved Offline",
                Toast.LENGTH_SHORT).show();

            clearForm();
            updateTotals();
            updateRecords();

        } catch(Exception e) {
            Toast.makeText(this,"Save Error",Toast.LENGTH_SHORT).show();
        }
    }

    void clearForm() {
        bill.setText("");
        product.setText("");
        amc.setText("");
        collection.setText("");
        customer.setText("");
        mobile.setText("");
        address.setText("");
        spareParts.setText("");
        amount.setText("");
        spareAmount.setText("");
        warranty.setText("");
        net.setText("");
        remarks.setText("");
        selectedDate = today();
        dateText.setText("DATE: "+selectedDate+"  (Tap to change)");
        payment.setSelection(0);
    }

    void updateTotals() {
        if(totalsBox == null) return;
        totalsBox.removeAllViews();

        double a=0,c=0,amt=0,sa=0,w=0,n=0;

        for(int i=0;i<records.length();i++) {
            JSONObject r = records.optJSONObject(i);
            if(r != null) {
                a += r.optDouble("amc");
                c += r.optDouble("collection");
                amt += r.optDouble("amount");
                sa += r.optDouble("spareAmount");
                w += r.optDouble("warranty");
                n += r.optDouble("net");
            }
        }

        totalsBox.addView(text("ALL-TIME TOTALS",
            16,navy,true));

        addTotal("AMC",a,purple);
        addTotal("COLLECTION",c,green);
        addTotal("AMOUNT",amt,blue);
        addTotal("SPARE AMOUNT",sa,orange);
        addTotal("WARRANTY AMOUNT",w,red);
        addTotal("NET AMOUNT",n,navy);

        totalsBox.addView(text("Total Records: "+records.length(),
            13,Color.DKGRAY,true));
    }

    void addTotal(String label,double value,int color) {
        TextView t = text(label+"    ₹ "+money(value),
            15,color,true);
        t.setPadding(0,dp(5),0,dp(5));
        totalsBox.addView(t);
    }

    String money(double n) {
        return String.format(Locale.getDefault(),"%.2f",n);
    }

    void updateRecords() {
        if(recordsBox == null) return;
        recordsBox.removeAllViews();

        int month = monthFilter == null ? 0 :
            monthFilter.getSelectedItemPosition();
        String year = yearFilter == null ? "All Years" :
            yearFilter.getSelectedItem().toString();
        String q = search == null ? "" :
            search.getText().toString().trim().toLowerCase(Locale.ROOT);

        int shown=0;

        for(int i=records.length()-1;i>=0;i--) {
            JSONObject r = records.optJSONObject(i);
            if(r == null) continue;

            String date = r.optString("date","");
            String[] p = date.split("-");
            if(p.length != 3) continue;

            int m=0;
            try { m=Integer.parseInt(p[1]); }
            catch(Exception ignored){}

            if(month>0 && month!=m) continue;
            if(!year.equals("All Years") && !year.equals(p[2])) continue;

            String searchable = (
                r.optString("bill")+" "+
                r.optString("product")+" "+
                r.optString("customer")+" "+
                r.optString("mobile")+" "+
                r.optString("address")
            ).toLowerCase(Locale.ROOT);

            if(!searchable.contains(q)) continue;

            LinearLayout card = vertical();
            card.setBackground(shape(Color.WHITE,15));
            pad(card,12);

            card.addView(text(
                r.optString("product")+" | Bill: "+r.optString("bill"),
                16,navy,true));

            card.addView(text(
                date+" | "+r.optString("customer"),
                13,Color.DKGRAY,false));

            card.addView(text(
                "AMC: ₹ "+money(r.optDouble("amc"))+
                "   Collection: ₹ "+money(r.optDouble("collection")),
                13,purple,true));

            card.addView(text(
                "Amount: ₹ "+money(r.optDouble("amount"))+
                "   Spare: ₹ "+money(r.optDouble("spareAmount")),
                13,orange,true));

            card.addView(text(
                "Warranty: ₹ "+money(r.optDouble("warranty"))+
                "   Net: ₹ "+money(r.optDouble("net")),
                13,green,true));

            card.addView(text(
                "Payment: "+r.optString("payment"),
                12,blue,true));

            card.addView(text(
                "Mobile: "+r.optString("mobile"),
                12,Color.DKGRAY,false));

            card.addView(text(
                "Address: "+r.optString("address"),
                12,Color.DKGRAY,false));

            card.addView(text(
                "Spare Parts: "+r.optString("spareParts"),
                12,Color.DKGRAY,false));

            card.addView(text(
                "Remarks: "+r.optString("remarks"),
                12,Color.DKGRAY,false));

            recordsBox.addView(card);
            gap(recordsBox,7);
            shown++;
        }

        if(shown==0) {
            recordsBox.addView(text(
                "No Records Found",15,Color.GRAY,false));
        }
    }

    String csv(String s) {
        return "\""+s.replace("\"","\"\"")+"\"";
    }

    void exportCSV() {
        if(records.length()==0) {
            Toast.makeText(this,"No Records to Export",
                Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE,
            "VIPIN_SERVICE_CRM_Records.csv");

        try {
            startActivityForResult(intent,exportRequest);
        } catch(Exception e) {
            Toast.makeText(this,"Export Not Available",
                Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode,
        int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);

        if(requestCode==exportRequest &&
            resultCode==RESULT_OK && data!=null) {
            try {
                OutputStream out =
                    getContentResolver().openOutputStream(data.getData());
                if(out == null) throw new Exception("File unavailable");

                OutputStreamWriter writer =
                    new OutputStreamWriter(out,"UTF-8");

                writer.write("Date,Bill Number,Product,AMC,Collection,Customer,Mobile,Address,Spare Parts,Amount,Spare Amount,Warranty Amount,Net Amount,Payment Mode,Remarks\n");

                for(int i=0;i<records.length();i++) {
                    JSONObject r=records.optJSONObject(i);
                    if(r==null) continue;

                    writer.write(
                        csv(r.optString("date"))+","+
                        csv(r.optString("bill"))+","+
                        csv(r.optString("product"))+","+
                        r.optDouble("amc")+","+
                        r.optDouble("collection")+","+
                        csv(r.optString("customer"))+","+
                        csv(r.optString("mobile"))+","+
                        csv(r.optString("address"))+","+
                        csv(r.optString("spareParts"))+","+
                        r.optDouble("amount")+","+
                        r.optDouble("spareAmount")+","+
                        r.optDouble("warranty")+","+
                        r.optDouble("net")+","+
                        csv(r.optString("payment"))+","+
                        csv(r.optString("remarks"))+"\n"
                    );
                }

                writer.flush();
                writer.close();

                Toast.makeText(this,"Excel CSV Exported",
                    Toast.LENGTH_LONG).show();

            } catch(Exception e) {
                Toast.makeText(this,"Export Failed: "+e.getMessage(),
                    Toast.LENGTH_LONG).show();
            }
        }
    }

    void changePassword() {
        EditText p = input("New Password",129);

        new android.app.AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(p)
            .setPositiveButton("SAVE",(d,w) -> {
                String value=p.getText().toString();
                if(value.trim().length()<4) {
                    Toast.makeText(this,
                        "Minimum 4 characters required",
                        Toast.LENGTH_SHORT).show();
                    return;
                }
                prefs.edit().putString("password",value).apply();
                Toast.makeText(this,"Password Changed",
                    Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("CANCEL",null)
            .show();
    }
}
