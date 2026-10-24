package com.vipin.servicecrm;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.graphics.Typeface;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;

public class MainActivity extends Activity {
    DB db;
    LinearLayout root, form;
    EditText[] fields = new EditText[14];
    Spinner payment;
    TextView summary, records;
    EditText monthInput;
    final String[] names = {"DATE (YYYY-MM-DD)","BILL NUMBER","PRODUCT NAME","AMC","COLLECTION","CUSTOMER NAME","ADDRESS","CONTACT NUMBER","AMOUNT","SPARE RATE","WARRANTY AMOUNT","REMARKS","SPARE PARTS","NET AMOUNT"};
    final String[] payments = {"RKS CHEQUE","PS CHEQUE","RKS UPI","PS UPI","UPI","CASH"};
    static final int EXPORT_REQUEST = 44;

    @Override public void onCreate(Bundle b) { super.onCreate(b); db = new DB(this); buildUI(); refresh(); }
    int dp(int n){ return (int)(n*getResources().getDisplayMetrics().density+0.5f); }
    TextView label(String s,int size){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(0xff183153); t.setTypeface(null,Typeface.BOLD); t.setPadding(0,dp(8),0,dp(4)); return t; }
    EditText edit(String hint, boolean numeric){ EditText e=new EditText(this); e.setSingleLine(false); e.setMinLines(1); e.setTextSize(15); e.setHint(hint); e.setPadding(dp(12),dp(8),dp(12),dp(8)); e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff8AA2C0)); e.setInputType(numeric?android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL:android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE); return e; }
    Button button(String text){ Button b=new Button(this); b.setText(text); b.setAllCaps(true); return b; }
    void buildUI(){
        ScrollView scroll=new ScrollView(this); root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(16),dp(12),dp(16),dp(24)); root.setBackgroundColor(0xffF4F7FB); scroll.addView(root); setContentView(scroll);
        TextView title=label("VIPIN SERVICE CRM",24); title.setTextColor(0xff123B72); title.setGravity(Gravity.CENTER); root.addView(title);
        TextView sub=label("OFFLINE SERVICE & COLLECTION MANAGER",12); sub.setGravity(Gravity.CENTER); root.addView(sub);
        root.addView(label("NEW SERVICE ENTRY",18));
        form=new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); root.addView(form);
        for(int i=0;i<names.length;i++){ form.addView(label(names[i],12)); fields[i]=edit(names[i], i==4||i==8||i==9||i==10||i==13); form.addView(fields[i]); }
        fields[0].setText(new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date()));
        form.addView(label("PAYMENT MODE",12)); payment=new Spinner(this); payment.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,payments)); form.addView(payment);
        Button save=button("SAVE ENTRY"); form.addView(save); save.setOnClickListener(v->save());
        root.addView(label("MONTHLY REPORT",18));
        monthInput=edit("YYYY-MM",false); monthInput.setText(new SimpleDateFormat("yyyy-MM",Locale.US).format(new Date())); root.addView(monthInput);
        Button report=button("SHOW MONTHLY TOTAL"); root.addView(report); report.setOnClickListener(v->refresh());
        summary=label("",14); root.addView(summary);
        Button export=button("EXPORT ALL DATA TO EXCEL (CSV)"); root.addView(export); export.setOnClickListener(v->exportCsv());
        root.addView(label("SAVED RECORDS",18)); records=label("NO RECORDS YET",13); records.setTypeface(Typeface.DEFAULT); root.addView(records);
        TextView foot=label("DATA IS STORED ON THIS DEVICE. EXPORT BACKUP REGULARLY.",11); foot.setGravity(Gravity.CENTER); root.addView(foot);
    }
    String val(int i){ return fields[i].getText().toString().trim().toUpperCase(Locale.ROOT); }
    void save(){
        if(val(0).isEmpty()||val(1).isEmpty()||val(5).isEmpty()){ Toast.makeText(this,"DATE, BILL NUMBER AND CUSTOMER NAME ARE REQUIRED",Toast.LENGTH_LONG).show(); return; }
        String[] row=new String[15]; for(int i=0;i<14;i++) row[i]=val(i); row[14]=payment.getSelectedItem().toString();
        db.insert(row); for(int i=1;i<14;i++) fields[i].setText(""); fields[0].setText(new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date())); payment.setSelection(0);
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(fields[1].getWindowToken(),0); Toast.makeText(this,"ENTRY SAVED OFFLINE",Toast.LENGTH_SHORT).show(); refresh();
    }
    String money(double d){ return String.format(Locale.US,"₹%,.2f",d); }
    void refresh(){
        String m=monthInput==null?new SimpleDateFormat("yyyy-MM",Locale.US).format(new Date()):monthInput.getText().toString().trim();
        Cursor c=db.month(m); double collection=0,amount=0,spare=0,warranty=0,net=0; int count=0;
        while(c.moveToNext()){ count++; collection+=num(c.getString(4)); amount+=num(c.getString(8)); spare+=num(c.getString(9)); warranty+=num(c.getString(10)); net+=num(c.getString(13)); } c.close();
        if(summary!=null) summary.setText("MONTH: "+m+"\nTOTAL BILLS: "+count+"\nTOTAL COLLECTION: "+money(collection)+"\nTOTAL AMOUNT: "+money(amount)+"\nTOTAL SPARE RATE: "+money(spare)+"\nTOTAL WARRANTY AMOUNT: "+money(warranty)+"\nTOTAL NET AMOUNT: "+money(net));
        Cursor all=db.all(); StringBuilder sb=new StringBuilder(); while(all.moveToNext()){ sb.append("BILL: ").append(all.getString(1)).append(" | ").append(all.getString(5)).append(" | ").append(all.getString(0)).append(" | NET: ").append(money(num(all.getString(13)))).append("\n"); } all.close(); if(records!=null) records.setText(sb.length()==0?"NO RECORDS YET":sb.toString());
    }
    double num(String s){ try{return Double.parseDouble(s==null||s.trim().isEmpty()?"0":s.trim());}catch(Exception e){return 0;} }
    void exportCsv(){ Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("text/csv"); i.putExtra(Intent.EXTRA_TITLE,"VIPIN_SERVICE_CRM_BACKUP.csv"); startActivityForResult(i,EXPORT_REQUEST); }
    @Override protected void onActivityResult(int r,int result,Intent data){ super.onActivityResult(r,result,data); if(r==EXPORT_REQUEST&&result==RESULT_OK&&data!=null){ try(OutputStream os=getContentResolver().openOutputStream(data.getData()); OutputStreamWriter w=new OutputStreamWriter(os,"UTF-8")){ w.write("DATE,BILL NUMBER,PRODUCT NAME,AMC,COLLECTION,CUSTOMER NAME,ADDRESS,CONTACT NUMBER,AMOUNT,SPARE RATE,WARRANTY AMOUNT,REMARKS,SPARE PARTS,NET AMOUNT,PAYMENT MODE\n"); Cursor c=db.all(); while(c.moveToNext()){ for(int j=0;j<15;j++){ if(j>0)w.write(","); String x=c.getString(j)==null?"":c.getString(j).replace("\"","\"\""); w.write("\""+x+"\""); } w.write("\n"); } c.close(); w.flush(); Toast.makeText(this,"EXCEL CSV BACKUP EXPORTED",Toast.LENGTH_LONG).show(); }catch(Exception e){Toast.makeText(this,"EXPORT FAILED: "+e.getMessage(),Toast.LENGTH_LONG).show();} } }

    static class DB extends SQLiteOpenHelper {
        DB(Context c){super(c,"vipin_service_crm.db",null,1);} public void onCreate(SQLiteDatabase d){d.execSQL("CREATE TABLE entries(id INTEGER PRIMARY KEY AUTOINCREMENT,date TEXT,bill TEXT,product TEXT,amc TEXT,collection TEXT,customer TEXT,address TEXT,contact TEXT,amount TEXT,spare_rate TEXT,warranty TEXT,remarks TEXT,spare_parts TEXT,net TEXT,payment TEXT)");}
        public void onUpgrade(SQLiteDatabase d,int o,int n){}
        void insert(String[] a){SQLiteDatabase d=getWritableDatabase(); ContentValues v=new ContentValues(); String[] cols={"date","bill","product","amc","collection","customer","address","contact","amount","spare_rate","warranty","remarks","spare_parts","net","payment"}; for(int i=0;i<15;i++)v.put(cols[i],a[i]); d.insert("entries",null,v);}
        Cursor all(){return getReadableDatabase().rawQuery("SELECT date,bill,product,amc,collection,customer,address,contact,amount,spare_rate,warranty,remarks,spare_parts,net,payment FROM entries ORDER BY id DESC",null);}
        Cursor month(String m){return getReadableDatabase().rawQuery("SELECT date,bill,product,amc,collection,customer,address,contact,amount,spare_rate,warranty,remarks,spare_parts,net,payment FROM entries WHERE date LIKE ?",new String[]{m+"%"});}
    }
}
