package com.jld.glassesserver;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.jld.glassesserver.barcode.MipcaActivityCapture;
import com.jld.glassesserver.util.WifiAdmin;


public class MainActivity extends AppCompatActivity {

    private EditText mSSIDEt;
    private EditText mPWDEt;
    private Button mConnBt;
    private WifiAdmin wifiAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setView();
        wifiAdmin = new WifiAdmin(this);
        wifiAdmin.openWifi();
    }

    private void setView() {
        mSSIDEt = (EditText)findViewById(R.id.et_ssid);
        mPWDEt = (EditText)findViewById(R.id.et_pwd);
        mConnBt = (Button)findViewById(R.id.bt_conn);
        mConnBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiAdmin.openWifi();
                final String ssid = mSSIDEt.getText().toString().trim();
                final String pwd = mPWDEt.getText().toString().trim();
                wifiAdmin.addNetwork(wifiAdmin.CreateWifiInfo(ssid,pwd));
            }
        });
        findViewById(R.id.bt_open).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiAdmin.openWifi();
            }
        });
        findViewById(R.id.bt_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiAdmin.closeWifi();
            }
        });
        findViewById(R.id.bt_scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this,MipcaActivityCapture.class);
                startActivity(intent);
            }
        });
    }
}
