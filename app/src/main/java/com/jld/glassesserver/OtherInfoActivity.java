package com.jld.glassesserver;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class OtherInfoActivity extends AppCompatActivity {

    private String s;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_other_info);
        s = getIntent().getStringExtra("scanString");
        initView();
    }

    private void initView() {
        ((TextView)findViewById(R.id.tv_scan_info)).setText(s);
    }
}
