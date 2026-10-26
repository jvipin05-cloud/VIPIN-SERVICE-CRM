package com.vipin.servicecrm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String PREF = "VIPIN_SERVICE_CRM_DATA";
    private static final String RECORDS_KEY = "records";
    private static final String PASS_KEY = "app_password";

    private static final int EXPORT_REQUEST = 701;
    private static final int PHOTO_GALLERY_REQUEST = 702;
    private static final int PDF_REQUEST = 703;
    private static final int CAMERA_REQUEST = 704;

    private final String[] FIELDS = {
            "DATE", "BILL NUMBER", "PRODUCT NAME", "AMC", "COLLECTION",
            "CUSTOMER NAME", "MOBILE NUMBER", "ADDRESS", "SPARE PARTS",
            "AMOUNT", "SPARE AMOUNT", "WARRANTY AMOUNT", "NET AMOUNT",
            "PAYMENT MODE", "REMARKS"
    };
    private final String[] PAYMENTS = {"CASH", "UPI", "PS UPI", "RKS UPI"};

    private final int NAVY = Color.rgb(8, 22, 55);
    private final int BLUE = Color.rgb(28, 91, 235);
    private final int CYAN = Color.rgb(48, 190, 255);
    private final int PALE = Color.rgb(239, 245, 255);
    private final int WHITE = Color.WHITE;

    private SharedPreferences prefs;
    private JSONArray records;
    private LinearLayout base, content, nav;
    private String currentPage = "DASHBOARD";
    private EditText searchBox;
    private int editingIndex = -1;
    private String selectedPhoto = "";
    private String selectedPdf = "";
    private TextView photoStatus, pdfStatus;
    private AlertDialog recordDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREF, MODE_PRIVATE);
        records = loadRecords();
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(Color.rgb(245, 247, 252));
        getWindow().getDecorView().setSystemUiVisibility(0);
        showLogin();
    }

    private JSONArray loadRecords() {
        try {
            return new JSONArray(prefs.getString(RECORDS_KEY, "[]"));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private void saveRecords() {
        prefs.edit().putString(RECORDS_KEY, records.toString()).apply();
    }

    private String password() {
        return prefs.getString(PASS_KEY, "1234");
    }

    private void showLogin() {
    LinearLayout page = vertical();
    page.setGravity(Gravity.CENTER);
    page.setPadding(dp(28), dp(24), dp(28), dp(24));
    page.setBackground(gradient(
            new int[]{NAVY, BLUE, Color.rgb(9, 36, 94)}, 0));

    ImageView logo = new ImageView(this);
    logo.setImageResource(R.drawable.vipin_logo);
    logo.setScaleType(ImageView.ScaleType.FIT_CENTER);

    LinearLayout.LayoutParams lp =
            new LinearLayout.LayoutParams(dp(180), dp(180));
    lp.gravity = Gravity.CENTER;
    page.addView(logo, lp);

    logo.setScaleX(0.7f);
    logo.setScaleY(0.7f);
    logo.animate().scaleX(1f).scaleY(1f)
            .setDuration(850)
            .setInterpolator(new DecelerateInterpolator())
            .start();

    TextView title = text("VIPIN SERVICE CRM", 25, WHITE, true);
    title.setGravity(Gravity.CENTER);
    title.setPadding(0, dp(22), 0, dp(8));
    page.addView(title);

    TextView sub = text(
            "SERVICE  •  COLLECTION  •  RECORDS",
            12, Color.rgb(205, 225, 255), true);
    sub.setGravity(Gravity.CENTER);
    page.addView(sub);

    EditText pin = new EditText(this);
    pin.setSingleLine(true);
    pin.setInputType(
            InputType.TYPE_CLASS_TEXT |
            InputType.TYPE_TEXT_VARIATION_PASSWORD);
    pin.setHint("ENTER PASSWORD");
    pin.setHintTextColor(Color.LTGRAY);
    pin.setTextColor(WHITE);
    pin.setPadding(dp(18), dp(14), dp(18), dp(14));
    pin.setBackground(round(
            Color.rgb(34, 65, 125), dp(16),
            Color.rgb(100, 155, 255), dp(1)));

    LinearLayout.LayoutParams ep =
            new LinearLayout.LayoutParams(-1, dp(56));
    ep.topMargin = dp(32);
    page.addView(pin, ep);

    Button login = button("UNLOCK CRM");
    LinearLayout.LayoutParams bp =
            new LinearLayout.LayoutParams(-1, dp(56));
    bp.topMargin = dp(16);
    page.addView(login, bp);

    login.setOnClickListener(v -> {
        if (pin.getText().toString().equals(password())) {
            tap(v);
            showCRM();
        } else {
            pin.setError("WRONG PASSWORD");
            shake(pin);
        }
    });

    TextView hint = text(
            "DEFAULT PASSWORD: 1234",
            11, Color.rgb(190, 210, 245), false);
    hint.setGravity(Gravity.CENTER);
    hint.setPadding(0, dp(18), 0, 0);
    page.addView(hint);

    setContentView(page);
    enter(page);
}
private void showCRM() {
    base = vertical();
    base.setBackgroundColor(PALE);

    LinearLayout header = vertical();
    header.setGravity(Gravity.CENTER);
    header.setPadding(dp(20), dp(22), dp(20), dp(20));
    header.setBackground(gradient(
            new int[]{NAVY, Color.rgb(18, 54, 125), BLUE}, dp(26)));

    ImageView homeLogo = new ImageView(this);
    homeLogo.setImageResource(R.drawable.vipin_logo);
    homeLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);

    header.addView(homeLogo,
            new LinearLayout.LayoutParams(dp(90), dp(90)));

    header.addView(text("VIPIN SERVICE CRM", 24, WHITE, true));

    TextView subtitle = text(
            "✦  SERVICE  •  COLLECTION  •  RECORDS",
            12, Color.rgb(205, 225, 255), true);
    subtitle.setPadding(0, dp(7), 0, 0);
    header.addView(subtitle);

    LinearLayout.LayoutParams hp =
            new LinearLayout.LayoutParams(-1, -2);
    hp.setMargins(dp(14), dp(12), dp(14), dp(10));

    header.setElevation(dp(18));
    header.setTranslationZ(dp(8));
    header.setRotationX(2.5f);
    base.addView(header, hp);

    header.animate().rotationX(0f).translationZ(dp(4))
            .setDuration(650)
            .setInterpolator(new DecelerateInterpolator())
            .start();

    content = vertical();

    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    scroll.setClipToPadding(false);
    scroll.addView(content);

    base.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

    nav = new LinearLayout(this);
    nav.setGravity(Gravity.CENTER);
    nav.setPadding(dp(5), dp(8), dp(5), dp(8));
    nav.setBackgroundColor(WHITE);

    base.addView(nav, new LinearLayout.LayoutParams(-1, dp(68)));

    setContentView(base);
    renderPage("DASHBOARD");
}
    

    private void renderPage(String page) {
        currentPage = page;
        content.removeAllViews();
        nav.removeAllViews();

        if ("DASHBOARD".equals(page)) dashboard();
        else if ("RECORDS".equals(page)) recordsPage();
        else if ("REPORTS".equals(page)) reportsPage();
        else settingsPage();

        String[] names = {"DASHBOARD", "RECORDS", "REPORTS", "SETTINGS"};
        String[] icons = {"⌂", "▤", "▥", "⚙"};
        for (int i = 0; i < names.length; i++) {
            final String n = names[i];
            LinearLayout item = vertical();
            item.setGravity(Gravity.CENTER);
            TextView ico = text(icons[i], 23, n.equals(page) ? BLUE : Color.GRAY, true);
            ico.setShadowLayer(n.equals(page) ? dp(8) : 0, 0, dp(2), n.equals(page) ? CYAN : Color.TRANSPARENT);
            TextView label = text(n, 9, n.equals(page) ? BLUE : Color.GRAY, true);
            item.addView(ico);
            item.addView(label);
            item.setPadding(dp(8), 0, dp(8), 0);
            item.setBackground(round(n.equals(page) ? Color.rgb(235, 244, 255) : WHITE, dp(16), Color.TRANSPARENT, 0));
            item.setElevation(n.equals(page) ? dp(7) : 0);
            nav.addView(item, new LinearLayout.LayoutParams(0, -1, 1));
            item.setOnClickListener(v -> {
                tap(v);
                renderPage(n);
            });
        }
        enter(content);
    }

    private void dashboard() {
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(dp(14), dp(5), dp(14), dp(8));
        actions.addView(actionCard("+ NEW RECORD", BLUE, () -> {
            editingIndex = -1;
            recordForm(null);
        }), new LinearLayout.LayoutParams(0, dp(58), 1));
        actions.addView(actionCard("PASSWORD", Color.rgb(30, 119, 220), this::changePassword),
                new LinearLayout.LayoutParams(0, dp(58), 1));
        actions.addView(actionCard("EXPORT CSV", Color.rgb(13, 151, 177), this::exportCsv),
                new LinearLayout.LayoutParams(0, dp(58), 1));
        content.addView(actions);

        searchBox = new EditText(this);
        searchBox.setSingleLine(true);
        searchBox.setTextSize(14);
        searchBox.setHint("⌕  SEARCH BILL / CUSTOMER / MOBILE / PRODUCT");
        searchBox.setPadding(dp(16), dp(12), dp(16), dp(12));
        searchBox.setBackground(round(WHITE, dp(18), Color.rgb(220, 230, 245), dp(1)));
        content.addView(searchBox, margins(-1, -2, dp(14), dp(6), dp(14), dp(12)));
        searchBox.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int before, int count) {
                showRecordCards(s.toString(), content);
            }
            public void afterTextChanged(android.text.Editable e) {}
        });

        LinearLayout totals = card(NAVY);
        totals.setPadding(dp(20), dp(18), dp(20), dp(18));
        totals.addView(text("LAST MONTH TOTAL", 19, WHITE, true));
        String[] amountFields = {"AMOUNT", "SPARE AMOUNT", "WARRANTY AMOUNT", "NET AMOUNT"};
        for (String f : amountFields) {
            TextView tv = text(f + "  :  ₹" + money(lastMonthTotal(f)), 15, WHITE, true);
            tv.setPadding(0, dp(12), 0, 0);
            totals.addView(tv);
            countUp(tv, lastMonthTotal(f));
        }
        content.addView(totals, margins(-1, -2, dp(14), dp(4), dp(14), dp(16)));

        TextView recent = text("RECENT RECORDS", 17, NAVY, true);
        recent.setPadding(dp(18), dp(8), 0, dp(8));
        content.addView(recent);
        showRecordCards("", content);
    }

    private void showRecordCards(String query, LinearLayout parent) {
        int start = parent == content ? Math.min(parent.getChildCount(), 4) : 0;
        if (parent == content) {
            while (parent.getChildCount() > start) parent.removeViewAt(start);
        } else {
            parent.removeAllViews();
        }

        int found = 0;
        for (int i = records.length() - 1; i >= 0; i--) {
            JSONObject o = records.optJSONObject(i);
            if (o == null || !matches(o, query)) continue;
            found++;
            final int index = i;

            LinearLayout c = card(WHITE);
            c.setPadding(dp(15), dp(14), dp(15), dp(14));
            c.addView(text(safe(o, "CUSTOMER NAME", "CUSTOMER"), 16, NAVY, true));

            TextView detail = text("BILL: " + safe(o, "BILL NUMBER", "-") + "   •   " + safe(o, "DATE", "-"),
                    12, Color.GRAY, true);
            detail.setPadding(0, dp(5), 0, 0);
            c.addView(detail);

            TextView amt = text("NET AMOUNT  ₹" + safe(o, "NET AMOUNT", "0"), 14, BLUE, true);
            amt.setPadding(0, dp(7), 0, 0);
            c.addView(amt);

            String attach = "";
            if (!o.optString("PHOTO_URI", "").isEmpty()) attach += "  📷 PHOTO";
            if (!o.optString("PDF_URI", "").isEmpty()) attach += "  📄 PDF";
            if (!attach.isEmpty()) {
                TextView at = text("ATTACHED:" + attach, 11, Color.rgb(20, 130, 100), true);
                at.setPadding(0, dp(6), 0, 0);
                c.addView(at);
            }

            LinearLayout row = new LinearLayout(this);
            Button edit = smallButton("EDIT");
            Button del = smallButton("DELETE");
            row.addView(edit);
            row.addView(del);
            c.addView(row);

            edit.setOnClickListener(v -> {
                tap(v);
                editingIndex = index;
                recordForm(o);
            });
            del.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle("DELETE RECORD?")
                    .setMessage("This record will be deleted from this phone.")
                    .setNegativeButton("CANCEL", null)
                    .setPositiveButton("DELETE", (d, w) -> {
                        JSONArray next = new JSONArray();
                        for (int k = 0; k < records.length(); k++) {
                            if (k != index) next.put(records.optJSONObject(k));
                        }
                        records = next;
                        saveRecords();
                        renderPage(currentPage);
                    }).show());

            parent.addView(c, margins(-1, -2, dp(14), dp(5), dp(14), dp(7)));
            enter(c);
        }

        if (found == 0) {
            TextView empty = text("NO RECORD FOUND", 15, Color.GRAY, true);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(24), 0, dp(24));
            parent.addView(empty);
        }
    }

    private boolean matches(JSONObject o, String q) {
        if (q == null || q.trim().isEmpty()) return true;
        return o.toString().toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT));
    }

    private void recordsPage() {
        TextView h = text("ALL SERVICE RECORDS", 19, NAVY, true);
        h.setPadding(dp(18), dp(10), 0, dp(8));
        content.addView(h);

        EditText s = new EditText(this);
        s.setSingleLine(true);
        s.setHint("SEARCH RECORDS...");
        s.setPadding(dp(15), dp(10), dp(15), dp(10));
        s.setBackground(round(WHITE, dp(15), Color.LTGRAY, dp(1)));
        content.addView(s, margins(-1, -2, dp(14), 0, dp(14), dp(8)));

        LinearLayout list = vertical();
        content.addView(list);
        showRecordCards("", list);
        s.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence x, int a, int c, int f) {}
            public void onTextChanged(CharSequence x, int a, int b, int c) {
                showRecordCards(x.toString(), list);
            }
            public void afterTextChanged(android.text.Editable e) {}
        });
    }

    private void reportsPage() {
        TextView h = text("MONTHLY REPORTS", 20, NAVY, true);
        h.setPadding(dp(18), dp(10), 0, dp(10));
        content.addView(h);

        Calendar cal = Calendar.getInstance();
        Spinner month = new Spinner(this);
        Spinner year = new Spinner(this);
        String[] months = new String[12];
        for (int i = 0; i < 12; i++) months[i] = new java.text.DateFormatSymbols().getMonths()[i];
        String[] years = new String[10];
        for (int i = 0; i < 10; i++) years[i] = "" + (cal.get(Calendar.YEAR) - 5 + i);

        month.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, months));
        year.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, years));
        month.setSelection(cal.get(Calendar.MONTH));
        year.setSelection(5);

        LinearLayout selectors = new LinearLayout(this);
        selectors.addView(month, new LinearLayout.LayoutParams(0, dp(52), 1));
        selectors.addView(year, new LinearLayout.LayoutParams(0, dp(52), 1));
        content.addView(selectors, margins(-1, -2, dp(14), 0, dp(14), dp(12)));

        LinearLayout result = card(WHITE);
        result.setPadding(dp(18), dp(16), dp(18), dp(16));
        content.addView(result, margins(-1, -2, dp(14), 0, dp(14), dp(12)));

        Runnable update = () -> {
            result.removeAllViews();
            int m = month.getSelectedItemPosition() + 1;
            String y = year.getSelectedItem().toString();
            result.addView(text(months[m - 1].toUpperCase() + " " + y, 17, NAVY, true));
            String[] fs = {"AMOUNT", "SPARE AMOUNT", "WARRANTY AMOUNT", "NET AMOUNT", "AMC", "COLLECTION"};
            for (String f : fs) {
                TextView t = text(f + "  :  ₹" + money(monthTotal(f, m, y)), 14, BLUE, true);
                t.setPadding(0, dp(13), 0, 0);
                result.addView(t);
            }
            TextView count = text("TOTAL RECORDS  :  " + countMonth(m, y), 14, NAVY, true);
            count.setPadding(0, dp(14), 0, 0);
            result.addView(count);
        };
        month.setOnItemSelectedListener(new SimpleSelect(update));
        year.setOnItemSelectedListener(new SimpleSelect(update));
        update.run();
    }

    private class SimpleSelect implements AdapterView.OnItemSelectedListener {
        Runnable r;
        SimpleSelect(Runnable x) { r = x; }
        public void onItemSelected(AdapterView<?> a, View v, int p, long id) { r.run(); }
        public void onNothingSelected(AdapterView<?> a) {}
    }

    private void settingsPage() {
        TextView h = text("SETTINGS", 20, NAVY, true);
        h.setPadding(dp(18), dp(12), 0, dp(10));
        content.addView(h);
        settingButton("CHANGE APP PASSWORD", this::changePassword);
        settingButton("EXPORT ALL RECORDS (CSV)", this::exportCsv);
        settingButton("ABOUT VIPIN SERVICE CRM", () ->
                new AlertDialog.Builder(this).setTitle("VIPIN SERVICE CRM")
                        .setMessage("Offline service and collection record manager. Your records are stored on this phone.")
                        .setPositiveButton("OK", null).show());
    }

    private void settingButton(String label, Runnable r) {
        Button b = button(label);
        content.addView(b, margins(-1, dp(54), dp(16), dp(7), dp(16), dp(7)));
        b.setOnClickListener(v -> { tap(v); r.run(); });
    }

    private void recordForm(JSONObject existing) {
        selectedPhoto = existing == null ? "" : existing.optString("PHOTO_URI", "");
        selectedPdf = existing == null ? "" : existing.optString("PDF_URI", "");

        ScrollView sc = new ScrollView(this);
        LinearLayout form = vertical();
        form.setPadding(dp(18), dp(18), dp(18), dp(24));
        form.setBackgroundColor(PALE);
        sc.addView(form);

        TextView heading = text(existing == null ? "ADD NEW RECORD" : "EDIT RECORD", 22, NAVY, true);
        heading.setPadding(0, 0, 0, dp(14));
        form.addView(heading);

        EditText[] inputs = new EditText[FIELDS.length];
        Spinner payment = null;

        for (int i = 0; i < FIELDS.length; i++) {
            String field = FIELDS[i];
            TextView label = text(field, 12, NAVY, true);
            label.setPadding(dp(3), dp(9), 0, dp(5));
            form.addView(label);

            if ("PAYMENT MODE".equals(field)) {
                payment = new Spinner(this);
                payment.setAdapter(new ArrayAdapter<String>(this,
                        android.R.layout.simple_spinner_dropdown_item, PAYMENTS));
                if (existing != null) {
                    String val = safe(existing, field, "CASH");
                    for (int j = 0; j < PAYMENTS.length; j++) {
                        if (PAYMENTS[j].equals(val)) payment.setSelection(j);
                    }
                }
                form.addView(payment, new LinearLayout.LayoutParams(-1, dp(52)));
            } else {
                EditText e = new EditText(this);
                inputs[i] = e;
                e.setSingleLine(!"ADDRESS".equals(field) && !"REMARKS".equals(field) && !"SPARE PARTS".equals(field));
                e.setTextSize(14);
                e.setPadding(dp(14), dp(10), dp(14), dp(10));
                e.setTextColor(NAVY);
                e.setBackground(round(WHITE, dp(14), Color.rgb(210, 225, 245), dp(1)));
                e.setHint("ENTER " + field);

                if (field.contains("AMOUNT") || "AMC".equals(field) || "COLLECTION".equals(field)) {
                    e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                }
                if ("MOBILE NUMBER".equals(field)) e.setInputType(InputType.TYPE_CLASS_PHONE);
                if ("DATE".equals(field)) {
                    e.setFocusable(false);
                    e.setClickable(true);
                    e.setHint("SELECT DATE");
                    e.setOnClickListener(v -> showDatePicker(e));
                    if (existing == null) {
                        e.setText(new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date()));
                    }
                }
                if (existing != null) e.setText(safe(existing, field, ""));
                int height = ("ADDRESS".equals(field) || "REMARKS".equals(field) || "SPARE PARTS".equals(field)) ? 86 : 52;
                form.addView(e, new LinearLayout.LayoutParams(-1, dp(height)));
            }
        }

        TextView attachHeading = text("CUSTOMER PHOTO & PDF", 18, BLUE, true);
        attachHeading.setPadding(0, dp(22), 0, dp(8));
        form.addView(attachHeading);

        Button cameraButton = button("📸 TAKE PHOTO");
        form.addView(cameraButton, margins(-1, dp(52), 0, dp(5), 0, dp(5)));
        cameraButton.setOnClickListener(v -> {
            tap(v);
            Intent camera = new Intent("android.media.action.IMAGE_CAPTURE");
            if (camera.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(camera, CAMERA_REQUEST);
            } else {
                Toast.makeText(this, "CAMERA APP NOT FOUND", Toast.LENGTH_SHORT).show();
            }
        });

        Button galleryButton = button("🖼 SELECT PHOTO FROM GALLERY");
        form.addView(galleryButton, margins(-1, dp(52), 0, dp(5), 0, dp(5)));
        galleryButton.setOnClickListener(v -> {
            tap(v);
            Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            pick.addCategory(Intent.CATEGORY_OPENABLE);
            pick.setType("image/*");
            startActivityForResult(pick, PHOTO_GALLERY_REQUEST);
        });

        photoStatus = text(photoLabel(), 12, NAVY, true);
        photoStatus.setPadding(dp(3), dp(5), 0, dp(5));
        form.addView(photoStatus);

        Button viewPhotoButton = button("👁 VIEW CUSTOMER PHOTO");
        form.addView(viewPhotoButton, margins(-1, dp(48), 0, dp(4), 0, dp(8)));
        viewPhotoButton.setOnClickListener(v -> viewPhoto());

        Button pdfButton = button("📄 ATTACH CUSTOMER PDF");
        form.addView(pdfButton, margins(-1, dp(52), 0, dp(5), 0, dp(5)));
        pdfButton.setOnClickListener(v -> {
            tap(v);
            Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            pick.addCategory(Intent.CATEGORY_OPENABLE);
            pick.setType("application/pdf");
            startActivityForResult(pick, PDF_REQUEST);
        });

        pdfStatus = text(pdfLabel(), 12, NAVY, true);
        pdfStatus.setPadding(dp(3), dp(5), 0, dp(5));
        form.addView(pdfStatus);

        Button openPdfButton = button("OPEN CUSTOMER PDF");
        form.addView(openPdfButton, margins(-1, dp(48), 0, dp(4), 0, dp(8)));
        openPdfButton.setOnClickListener(v -> openPdf());

        Spinner finalPayment = payment;
        Button save = button(existing == null ? "SAVE RECORD" : "UPDATE RECORD");
        form.addView(save, margins(-1, dp(58), 0, dp(24), 0, 0));
        save.setOnClickListener(v -> {
            tap(v);
            try {
                JSONObject obj = existing == null ? new JSONObject() : new JSONObject(existing.toString());
                for (int i = 0; i < FIELDS.length; i++) {
                    if ("PAYMENT MODE".equals(FIELDS[i])) {
                        obj.put(FIELDS[i], finalPayment.getSelectedItem().toString());
                    } else {
                        obj.put(FIELDS[i], inputs[i].getText().toString().trim());
                    }
                }
                if (obj.optString("CUSTOMER NAME").trim().isEmpty()) {
                    inputs[5].setError("CUSTOMER NAME REQUIRED");
                    return;
                }
                obj.put("PHOTO_URI", selectedPhoto);
                obj.put("PDF_URI", selectedPdf);

                if (existing == null) records.put(obj);
                else records.put(editingIndex, obj);
saveRecords();
if (recordDialog != null) recordDialog.dismiss();
Toast.makeText(this, "RECORD SAVED SUCCESSFULLY", Toast.LENGTH_SHORT).show();
                
                renderPage("RECORDS");
            } catch (Exception ex) {
                Toast.makeText(this, "SAVE FAILED: " + ex.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        recordDialog = new AlertDialog.Builder(this)
        .setView(sc)
        .setNegativeButton("CLOSE", null)
        .create();
recordDialog.show();
        enter(form);
    }

    private String photoLabel() {
        return selectedPhoto == null || selectedPhoto.isEmpty() ? "NO PHOTO SELECTED" : "PHOTO ATTACHED ✓";
    }

    private String pdfLabel() {
        return selectedPdf == null || selectedPdf.isEmpty() ? "NO PDF SELECTED" : "PDF ATTACHED ✓";
    }

    private void refreshAttachmentLabels() {
        if (photoStatus != null) photoStatus.setText(photoLabel());
        if (pdfStatus != null) pdfStatus.setText(pdfLabel());
    }

    private void viewPhoto() {
        if (selectedPhoto == null || selectedPhoto.isEmpty()) {
            Toast.makeText(this, "PLEASE ATTACH PHOTO FIRST", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            ImageView image = new ImageView(this);
            image.setAdjustViewBounds(true);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            Uri uri = Uri.parse(selectedPhoto);
            if ("file".equals(uri.getScheme())) {
                Bitmap bitmap = BitmapFactory.decodeFile(uri.getPath());
                image.setImageBitmap(bitmap);
            } else {
                image.setImageURI(uri);
            }
            new AlertDialog.Builder(this).setTitle("CUSTOMER PHOTO").setView(image)
                    .setPositiveButton("CLOSE", null).show();
        } catch (Exception e) {
            Toast.makeText(this, "PHOTO COULD NOT BE OPENED", Toast.LENGTH_LONG).show();
        }
    }

    private void openPdf() {
        if (selectedPdf == null || selectedPdf.isEmpty()) {
            Toast.makeText(this, "PLEASE ATTACH PDF FIRST", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Uri uri = Uri.parse(selectedPdf);
            Intent open = new Intent(Intent.ACTION_VIEW);
            open.setDataAndType(uri, "application/pdf");
            open.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(open, "OPEN PDF"));
        } catch (Exception e) {
            Toast.makeText(this, "PDF VIEWER NOT FOUND OR FILE UNAVAILABLE", Toast.LENGTH_LONG).show();
        }
    }

    private void showDatePicker(EditText target) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (v, y, m, d) ->
                target.setText(String.format(Locale.getDefault(), "%02d-%02d-%04d", d, m + 1, y)),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void changePassword() {
        LinearLayout box = vertical();
        box.setPadding(dp(18), dp(10), dp(18), 0);

        EditText old = new EditText(this);
        old.setHint("CURRENT PASSWORD");
        old.setInputType(129);
        box.addView(old);

        EditText n = new EditText(this);
        n.setHint("NEW PASSWORD");
        n.setInputType(129);
        box.addView(n);

        EditText confirm = new EditText(this);
        confirm.setHint("CONFIRM NEW PASSWORD");
        confirm.setInputType(129);
        box.addView(confirm);

        new AlertDialog.Builder(this).setTitle("CHANGE PASSWORD").setView(box)
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("SAVE", (d, w) -> {
                    if (!old.getText().toString().equals(password())) {
                        Toast.makeText(this, "CURRENT PASSWORD INCORRECT", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (n.getText().length() < 4 || !n.getText().toString().equals(confirm.getText().toString())) {
                        Toast.makeText(this, "PASSWORD MUST MATCH AND BE 4+ CHARACTERS", Toast.LENGTH_LONG).show();
                        return;
                    }
                    prefs.edit().putString(PASS_KEY, n.getText().toString()).apply();
                    Toast.makeText(this, "PASSWORD UPDATED", Toast.LENGTH_SHORT).show();
                }).show();
    }

    private void exportCsv() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "VIPIN_SERVICE_CRM.csv");
        startActivityForResult(intent, EXPORT_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;

        if (requestCode == PHOTO_GALLERY_REQUEST || requestCode == PDF_REQUEST) {
            Uri uri = data.getData();
            if (uri == null) return;
            try {
                int flags = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
                getContentResolver().takePersistableUriPermission(uri, flags);
            } catch (Exception ignored) {}

            if (requestCode == PHOTO_GALLERY_REQUEST) {
                selectedPhoto = uri.toString();
            } else {
                selectedPdf = uri.toString();
            }
            refreshAttachmentLabels();
            return;
        }

        if (requestCode == CAMERA_REQUEST) {
            try {
                Bundle extras = data.getExtras();
                Bitmap photo = extras == null ? null : (Bitmap) extras.get("data");
                if (photo == null) {
                    Toast.makeText(this, "CAMERA DID NOT RETURN PHOTO", Toast.LENGTH_LONG).show();
                    return;
                }
                File file = new File(getFilesDir(), "customer_photo_" + System.currentTimeMillis() + ".jpg");
                try (FileOutputStream out = new FileOutputStream(file)) {
                    photo.compress(Bitmap.CompressFormat.JPEG, 92, out);
                }
                selectedPhoto = Uri.fromFile(file).toString();
                refreshAttachmentLabels();
                Toast.makeText(this, "PHOTO CAPTURED", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "PHOTO SAVE FAILED", Toast.LENGTH_LONG).show();
            }
            return;
        }

        if (requestCode == EXPORT_REQUEST) {
            Uri uri = data.getData();
            try (OutputStream os = getContentResolver().openOutputStream(uri);
                 OutputStreamWriter w = new OutputStreamWriter(os)) {
                for (int i = 0; i < FIELDS.length; i++) {
                    if (i > 0) w.write(",");
                    w.write(csv(FIELDS[i]));
                }
                w.write("\n");

                for (int r = 0; r < records.length(); r++) {
                    JSONObject o = records.optJSONObject(r);
                    for (int i = 0; i < FIELDS.length; i++) {
                        if (i > 0) w.write(",");
                        w.write(csv(o == null ? "" : o.optString(FIELDS[i], "")));
                    }
                    w.write("\n");
                }
                w.flush();
                Toast.makeText(this, "CSV EXPORTED", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "EXPORT FAILED", Toast.LENGTH_LONG).show();
            }
        }
    }

    private String csv(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private double lastMonthTotal(String field) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, -1);
        return monthTotal(field, c.get(Calendar.MONTH) + 1, "" + c.get(Calendar.YEAR));
    }

    private double monthTotal(String field, int month, String year) {
        double sum = 0;
        for (int i = 0; i < records.length(); i++) {
            JSONObject o = records.optJSONObject(i);
            if (o == null) continue;
            try {
                Date d = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                        .parse(safe(o, "DATE", ""));
                Calendar c = Calendar.getInstance();
                c.setTime(d);
                if (c.get(Calendar.MONTH) + 1 == month && ("" + c.get(Calendar.YEAR)).equals(year)) {
                    sum += Double.parseDouble(safe(o, field, "0").replace(",", ""));
                }
            } catch (Exception ignored) {}
        }
        return sum;
    }

    private int countMonth(int month, String year) {
        int count = 0;
        for (int i = 0; i < records.length(); i++) {
            JSONObject o = records.optJSONObject(i);
            if (o == null) continue;
            try {
                Date d = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                        .parse(safe(o, "DATE", ""));
                Calendar c = Calendar.getInstance();
                c.setTime(d);
                if (c.get(Calendar.MONTH) + 1 == month && ("" + c.get(Calendar.YEAR)).equals(year)) count++;
            } catch (Exception ignored) {}
        }
        return count;
    }

    private String money(double v) {
        return String.format(Locale.getDefault(), "%,.2f", v);
    }

    private String safe(JSONObject o, String k, String def) {
        return o == null ? def : o.optString(k, def);
    }

    private LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif-condensed", bold ? Typeface.BOLD : Typeface.NORMAL));
        return t;
    }

    private LinearLayout card(int color) {
        LinearLayout l = vertical();
        l.setBackground(round(color, dp(22), Color.rgb(225, 233, 247), dp(1)));
        l.setElevation(dp(10));
        l.setTranslationZ(dp(2));
        l.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        return l;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(WHITE);
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setBackground(gradient(new int[]{BLUE, CYAN}, dp(16)));
        b.setElevation(dp(5));
        return b;
    }

    private Button smallButton(String label) {
        Button b = button(label);
        b.setTextSize(10);
        b.setPadding(dp(5), 0, dp(5), 0);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(95), dp(38));
        p.setMargins(0, dp(10), dp(8), 0);
        b.setLayoutParams(p);
        return b;
    }

    private View actionCard(String label, int color, Runnable action) {
        TextView t = text(label, 12, WHITE, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(5), dp(8), dp(5), dp(8));
        t.setBackground(gradient(new int[]{color, CYAN, BLUE}, dp(17)));
        t.setElevation(dp(12));
        t.setTranslationZ(dp(3));
        t.setOnClickListener(v -> { tap(v); action.run(); });
        return t;
    }

    private void tap(View v) {
        v.animate().cancel();
        v.setCameraDistance(dp(900));
        v.animate().scaleX(0.94f).scaleY(0.94f).rotationX(7f).translationZ(dp(1))
                .setDuration(95).withEndAction(() ->
                        v.animate().scaleX(1f).scaleY(1f).rotationX(0f).translationZ(dp(4))
                                .setDuration(220).setInterpolator(new DecelerateInterpolator()).start()).start();
    }

    private void enter(View v) {
        v.setCameraDistance(dp(1200));
        v.setAlpha(0f);
        v.setTranslationY(dp(26));
        v.setRotationX(9f);
        v.setScaleX(0.97f);
        v.setScaleY(0.97f);
        v.animate().alpha(1f).translationY(0).rotationX(0f).scaleX(1f).scaleY(1f)
                .setDuration(520).setInterpolator(new DecelerateInterpolator()).start();
    }

    private void shake(View v) {
        TranslateAnimation a = new TranslateAnimation(-dp(8), dp(8), 0, 0);
        a.setDuration(55);
        a.setRepeatCount(3);
        a.setRepeatMode(Animation.REVERSE);
        v.startAnimation(a);
    }

    private void countUp(TextView tv, double value) {
        if (value <= 0) return;
        final long start = System.currentTimeMillis();
        final String prefix = tv.getText().toString().split("₹")[0];
        tv.post(new Runnable() {
            public void run() {
                float f = Math.min(1f, (System.currentTimeMillis() - start) / 650f);
                tv.setText(prefix + "₹" + money(value * f));
                if (f < 1f) tv.postDelayed(this, 16);
            }
        });
    }

    private GradientDrawable round(int color, int radius, int stroke, int sw) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        d.setStroke(sw, stroke);
        return d;
    }

    private GradientDrawable gradient(int[] colors, int radius) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        d.setCornerRadius(radius);
        return d;
    }

    private GradientDrawable circle(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        return d;
    }

    private int dp(int n) {
        return (int) (n * getResources().getDisplayMetrics().density + .5f);
    }

    private LinearLayout.LayoutParams margins(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(l, t, r, b);
        return p;
    }
}
