package com.vipin.servicecrm;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.text.*;
import java.util.*;

public class MainActivity extends Activity {
    private static final String PREF="vipin_service_crm";
    private static final String RECORDS="records";
    private static final String PASSWORD="app_password";
    private static final String[] FIELDS={
        "DATE","BILL NUMBER","PRODUCT NAME","AMC","COLLECTION","CUSTOMER NAME",
        "MOBILE NUMBER","ADDRESS","SPARE PARTS","AMOUNT","SPARE AMOUNT",
        "WARRANTY AMOUNT","NET AMOUNT","PAYMENT MODE","REMARKS"
    };
    private static final String[] PAYMENTS={"CASH","UPI","PS UPI","RKS UPI"};
    private final int NAVY=Color.rgb(24,35,74), BLUE=Color.rgb(40,92,190),
        BG=Color.rgb(244,247,252), WHITE=Color.WHITE, INK=Color.rgb(35,43,62),
        GREEN=Color.rgb(20,145,105), RED=Color.rgb(190,55,65);
    private SharedPreferences prefs;
    private JSONArray records=new JSONArray();
    private LinearLayout root, body;
    private EditText searchBox;
    private String monthFilter="";
    private int editing=-1;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        prefs=getSharedPreferences(PREF,MODE_PRIVATE);
        load();
        if(prefs.getString(PASSWORD,"").isEmpty()) showSetPassword();
        else showLogin();
    }
    private void load(){
        try { records=new JSONArray(prefs.getString(RECORDS,"[]")); }
        catch(Exception e){ records=new JSONArray(); }
    }
    private void save(){ prefs.edit().putString(RECORDS,records.toString()).apply(); }
    private GradientDrawable shape(int color,int radius){
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); return d;
    }
    private TextView text(String s,int size,int color,boolean bold){
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }
    private LinearLayout vertical(){ LinearLayout l=new LinearLayout(this); l.setOrientation(1); return l; }
    private LinearLayout horizontal(){ LinearLayout l=new LinearLayout(this); l.setOrientation(0); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private void base(String title){
        root=vertical(); root.setBackgroundColor(BG);
        TextView bar=text(title,21,WHITE,true); bar.setPadding(18,20,18,20); bar.setBackgroundColor(NAVY);
        root.addView(bar,new LinearLayout.LayoutParams(-1,-2));
        ScrollView sc=new ScrollView(this); body=vertical(); body.setPadding(14,14,14,24);
        sc.addView(body); root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
    }
    private Button button(String label,int color){
        Button b=new Button(this); b.setText(label); b.setTextColor(WHITE); b.setAllCaps(false);
        b.setTextSize(14); b.setBackground(shape(color,24)); return b;
    }
    private void showSetPassword(){
        base("VIPIN SERVICE CRM");
        body.addView(text("WELCOME",25,NAVY,true));
        body.addView(text("पहली बार उपयोग: अपना ऐप पासवर्ड बनाएँ",15,INK,false));
        EditText p=new EditText(this); p.setHint("नया पासवर्ड (कम से कम 4 अक्षर)"); p.setInputType(129); body.addView(p);
        EditText c=new EditText(this); c.setHint("पासवर्ड दोबारा लिखें"); c.setInputType(129); body.addView(c);
        Button b=button("SAVE PASSWORD",BLUE); body.addView(b);
        b.setOnClickListener(v->{
            String a=p.getText().toString(), z=c.getText().toString();
            if(a.length()<4){msg("कम से कम 4 अक्षर का पासवर्ड रखें");return;}
            if(!a.equals(z)){msg("दोनों पासवर्ड समान नहीं हैं");return;}
            prefs.edit().putString(PASSWORD,a).apply(); showLogin();
        });
    }
    private void showLogin(){
        base("VIPIN SERVICE CRM");
        body.addView(text("SECURE LOGIN",25,NAVY,true));
        body.addView(text("अपना ऐप पासवर्ड डालें",15,INK,false));
        EditText p=new EditText(this); p.setHint("PASSWORD"); p.setInputType(129); body.addView(p);
        Button b=button("LOGIN",BLUE); body.addView(b);
        b.setOnClickListener(v->{ if(p.getText().toString().equals(prefs.getString(PASSWORD,""))){showHome();}
            else msg("गलत पासवर्ड"); });
    }
    private void showHome(){
        base("VIPIN SERVICE CRM");
        TextView sub=text("OFFLINE SERVICE RECORD MANAGER",12,BLUE,true); body.addView(sub);
        LinearLayout actions=horizontal(); actions.setPadding(0,12,0,12);
        Button add=button("+ ADD RECORD",GREEN), export=button("EXPORT CSV",BLUE);
        actions.addView(add,new LinearLayout.LayoutParams(0,-2,1)); actions.addView(export,new LinearLayout.LayoutParams(0,-2,1));
        body.addView(actions);
        Button pass=button("CHANGE PASSWORD",NAVY); body.addView(pass);
        pass.setOnClickListener(v->changePassword());
        add.setOnClickListener(v->{editing=-1;showForm(null);});
        export.setOnClickListener(v->exportCsv());
        addTotals();
        EditText q=new EditText(this); q.setSingleLine(true); q.setHint("SEARCH: NAME / MOBILE / BILL / PRODUCT");
        q.setTextSize(14); body.addView(q); searchBox=q;
        Button filter=button("FILTER MONTH / YEAR",BLUE); body.addView(filter);
        filter.setOnClickListener(v->pickMonth());
        Button clear=button("SHOW ALL RECORDS",NAVY); body.addView(clear);
        clear.setOnClickListener(v->{monthFilter="";renderRecords(q.getText().toString());});
        LinearLayout list=vertical(); body.addView(list);
        q.addTextChangedListener(new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int start,int before,int count){ fillList(list,s.toString());}
            public void afterTextChanged(android.text.Editable e){}
        });
        fillList(list,"");
    }
    private void addTotals(){
        body.addView(text("LAST MONTH TOTAL",18,NAVY,true));
        Calendar cal=Calendar.getInstance(); cal.add(Calendar.MONTH,-1);
        String key=new SimpleDateFormat("yyyy-MM",Locale.US).format(cal.getTime());
        String[] names={"AMOUNT","SPARE AMOUNT","WARRANTY AMOUNT","NET AMOUNT"};
        for(String name:names){
            double sum=0;
            for(int i=0;i<records.length();i++){
                JSONObject o=records.optJSONObject(i);
                if(o!=null && o.optString("DATE","").startsWith(key)) sum+=number(o.optString(name,"0"));
            }
            TextView t=text(name+"   ₹ "+money(sum),15,INK,true);
            t.setPadding(14,11,14,11); t.setBackground(shape(WHITE,14));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,5,0,5); body.addView(t,lp);
        }
    }
    private void pickMonth(){
        Calendar c=Calendar.getInstance();
        new DatePickerDialog(this,(view,y,m,d)->{
            monthFilter=String.format(Locale.US,"%04d-%02d",y,m+1);
            if(searchBox!=null)renderRecords(searchBox.getText().toString());
            msg("FILTER: "+monthFilter);
        },c.get(Calendar.YEAR),c.get(Calendar.MONTH),1).show();
    }
    private void renderRecords(String q){
        if(body==null)return;
        // Home is rebuilt so totals and filtered records stay consistent.
        showHome();
        if(searchBox!=null) searchBox.setText(q);
    }
    private void fillList(LinearLayout list,String query){
        list.removeAllViews();
        int shown=0;
        for(int i=records.length()-1;i>=0;i--){
            JSONObject o=records.optJSONObject(i); if(o==null)continue;
            String date=o.optString("DATE","");
            if(!monthFilter.isEmpty()&&!date.startsWith(monthFilter))continue;
            String hay=(o.optString("CUSTOMER NAME")+" "+o.optString("MOBILE NUMBER")+" "+
                o.optString("BILL NUMBER")+" "+o.optString("PRODUCT NAME")).toLowerCase(Locale.ROOT);
            if(!hay.contains(query.toLowerCase(Locale.ROOT)))continue;
            shown++;
            LinearLayout card=vertical(); card.setPadding(13,12,13,12); card.setBackground(shape(WHITE,16));
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2); cp.setMargins(0,7,0,7); list.addView(card,cp);
            card.addView(text(o.optString("CUSTOMER NAME","CUSTOMER"),17,NAVY,true));
            card.addView(text("DATE: "+date+"   BILL: "+o.optString("BILL NUMBER"),13,BLUE,true));
            card.addView(text("MOBILE: "+o.optString("MOBILE NUMBER"),13,INK,false));
            card.addView(text("NET AMOUNT: ₹ "+o.optString("NET AMOUNT","0"),14,GREEN,true));
            LinearLayout row=horizontal();
            Button edit=button("EDIT",BLUE), del=button("DELETE",RED);
            row.addView(edit,new LinearLayout.LayoutParams(0,-2,1)); row.addView(del,new LinearLayout.LayoutParams(0,-2,1)); card.addView(row);
            final int index=i;
            edit.setOnClickListener(v->{editing=index;showForm(records.optJSONObject(index));});
            del.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("यह रिकॉर्ड DELETE करें?")
                .setNegativeButton("CANCEL",null).setPositiveButton("DELETE",(a,b)->{
                    JSONArray n=new JSONArray(); for(int k=0;k<records.length();k++)if(k!=index)n.put(records.opt(k));
                    records=n;save();showHome();
                }).show());
        }
        if(shown==0)list.addView(text("कोई रिकॉर्ड नहीं मिला",15,INK,false));
    }
    private EditText field(String hint,String value){
        EditText e=new EditText(this); e.setSingleLine(false); e.setTextSize(15); e.setHint(hint);
        if(value!=null)e.setText(value); return e;
    }
    private void showForm(JSONObject old){
        base(editing<0?"ADD NEW RECORD":"EDIT RECORD");
        HashMap<String,EditText> inputs=new HashMap<>();
        for(String f:FIELDS){
            body.addView(text(f,13,NAVY,true));
            if(f.equals("PAYMENT MODE")){
                Spinner sp=new Spinner(this); ArrayAdapter<String> ad=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,PAYMENTS);
                sp.setAdapter(ad); String val=old==null?"CASH":old.optString(f,"CASH");
                for(int j=0;j<PAYMENTS.length;j++)if(PAYMENTS[j].equals(val))sp.setSelection(j);
                body.addView(sp); // Store selected value in a hidden tag.
                EditText holder=field("",sp.getSelectedItem().toString()); holder.setVisibility(View.GONE);
                sp.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
                    public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){holder.setText(PAYMENTS[pos]);}
                    public void onNothingSelected(android.widget.AdapterView<?> p){}
                }); inputs.put(f,holder);
            }else{
                EditText e=field("ENTER "+f,old==null?"":old.optString(f,""));
                if(f.equals("DATE")){
                    e.setFocusable(false); e.setOnClickListener(v->{
                        Calendar c=Calendar.getInstance();
                        try{c.setTime(new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(e.getText().toString()));}catch(Exception ignored){}
                        new DatePickerDialog(this,(dp,y,m,d)->e.setText(String.format(Locale.US,"%04d-%02d-%02d",y,m+1,d)),
                            c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();
                    });
                    if(e.getText().length()==0)e.setText(new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date()));
                }
                if(Arrays.asList("AMC","COLLECTION","AMOUNT","SPARE AMOUNT","WARRANTY AMOUNT","NET AMOUNT").contains(f))
                    e.setInputType(8194);
                body.addView(e); inputs.put(f,e);
            }
        }
        Button saveBtn=button("SAVE RECORD",GREEN); body.addView(saveBtn);
        Button cancel=button("CANCEL",NAVY); body.addView(cancel);
        cancel.setOnClickListener(v->showHome());
        saveBtn.setOnClickListener(v->{
            JSONObject obj=new JSONObject();
            try{
                for(String f:FIELDS)obj.put(f,inputs.get(f).getText().toString().trim());
                if(obj.optString("CUSTOMER NAME").isEmpty()){msg("CUSTOMER NAME जरूरी है");return;}
                if(editing<0)records.put(obj);else records.put(editing,obj);
                save(); hideKeyboard(); showHome(); msg("RECORD SAVED");
            }catch(Exception ex){msg("SAVE नहीं हुआ: "+ex.getMessage());}
        });
    }
    private void changePassword(){
        LinearLayout box=vertical(); EditText old=field("OLD PASSWORD",""), n=field("NEW PASSWORD","");
        old.setInputType(129); n.setInputType(129); box.addView(old);box.addView(n);
        new AlertDialog.Builder(this).setTitle("CHANGE PASSWORD").setView(box)
            .setNegativeButton("CANCEL",null).setPositiveButton("SAVE",(d,w)->{
                if(!old.getText().toString().equals(prefs.getString(PASSWORD,""))){msg("OLD PASSWORD गलत");return;}
                if(n.getText().length()<4){msg("नया पासवर्ड कम से कम 4 अक्षर रखें");return;}
                prefs.edit().putString(PASSWORD,n.getText().toString()).apply();msg("PASSWORD बदल गया");
            }).show();
    }
    private void exportCsv(){
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/csv"); i.putExtra(Intent.EXTRA_TITLE,"VIPIN_SERVICE_CRM.csv");
        startActivityForResult(i,701);
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==701&&result==RESULT_OK&&data!=null){
            try(OutputStream os=getContentResolver().openOutputStream(data.getData());
                OutputStreamWriter w=new OutputStreamWriter(os,"UTF-8")){
                for(int j=0;j<FIELDS.length;j++){if(j>0)w.write(",");w.write(csv(FIELDS[j]));}w.write("\n");
                for(int r=0;r<records.length();r++){
                    JSONObject o=records.optJSONObject(r);
                    for(int j=0;j<FIELDS.length;j++){if(j>0)w.write(",");w.write(csv(o==null?"":o.optString(FIELDS[j],"")));}
                    w.write("\n");
                }
                w.flush();msg("CSV EXPORT सफल");
            }catch(Exception e){msg("EXPORT ERROR: "+e.getMessage());}
        }
    }
    private String csv(String s){return "\""+(s==null?"":s.replace("\"","\"\""))+"\"";}
    private double number(String s){try{return Double.parseDouble(s.replace(",","").trim());}catch(Exception e){return 0;}}
    private String money(double n){return String.format(Locale.US,"%.2f",n);}
    private void hideKeyboard(){try{((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(root.getWindowToken(),0);}catch(Exception ignored){}}
    private void msg(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
