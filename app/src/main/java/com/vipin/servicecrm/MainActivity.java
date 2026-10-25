
package com.vipin.servicecrm;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("VIPIN SERVICE CRM");
        title.setTextSize(24);
        title.setTextColor(Color.BLACK);
        title.setPadding(20, 30, 20, 30);
        layout.addView(title);

        TextView message = new TextView(this);
        message.setText("Welcome to VIPIN SERVICE CRM");
        message.setTextSize(18);
        message.setTextColor(Color.DKGRAY);
        message.setPadding(20, 30, 20, 30);
        layout.addView(message);

        setContentView(layout);
    }
}
