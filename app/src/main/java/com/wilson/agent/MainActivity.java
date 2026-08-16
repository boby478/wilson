package com.wilson.agent;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setText("Wilson is running.\nEnable Accessibility Service in Settings.");
        tv.setTextSize(18);
        tv.setPadding(40, 100, 40, 40);
        setContentView(tv);
    }
}
