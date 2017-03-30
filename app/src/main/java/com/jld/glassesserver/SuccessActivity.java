package com.jld.glassesserver;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.KeyEvent;
import android.widget.TextView;

import com.jld.glassesserver.barcode.MipcaActivityCapture;
import com.jld.glassesserver.util.LogUtil;

import static com.jld.glassesserver.BluetoothService.BREAK_CONNECT;

public class SuccessActivity extends BaseActivity implements BaseActivity.MyKeyListener{

    private static final String TAG = "SuccessActivity";
    private BluetoothService.MyBinder myBinder;
    private MyServiceConn myServiceConn;

    private TextView textView;
    private Handler myBinderHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success);
        initView();
        Intent intent = new Intent(this,BluetoothService.class);
        myServiceConn = new MyServiceConn();
        bindService(intent,myServiceConn,BIND_AUTO_CREATE);
        setKeyListener(this);
    }

    private void initView() {
        textView = (TextView)findViewById(R.id.tv_device_name);
    }

    private boolean isFirst = true;
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && myBinder != null && isFirst){
            BluetoothDevice bluetoothDevice = myBinder.getBluetoothDevice();
            if (bluetoothDevice != null){
                textView.setText(bluetoothDevice.getName());
                isFirst = false;
            }else {
                LogUtil.d(TAG,"onWindowFocusChanged ----> bluetoothDevice = null");
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }



    @Override
    public void singleClick() {
        LogUtil.d(TAG,"singleClick");
        if(myBinderHandler!=null){
            myBinderHandler.sendEmptyMessage(BREAK_CONNECT);
        }
        MipcaActivityCapture.isConnect = false;
        Intent intent = new Intent();
        intent.setClass(SuccessActivity.this, MipcaActivityCapture.class);
        startActivity(intent);
        finish();
    }
    @Override
    public void doubleClick() {
//        Intent intent= new Intent(Intent.ACTION_MAIN);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);// 注意
//        intent.addCategory(Intent.CATEGORY_HOME);
//        startActivity(intent);
        LogUtil.d(TAG,"doubleClick");
        onBackPressed();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtil.d(TAG,"onDestroy");
        unbindService(myServiceConn);
    }



    @Override
    public void leftGlide() {

    }

    @Override
    public void rightGlide() {

    }

    private class MyServiceConn implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            myBinder = (BluetoothService.MyBinder) service;
            myBinder.setHandler(handler);
            myBinderHandler = myBinder.getHandler();
            LogUtil.d(TAG,"---->MyServiceConn.onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            myBinder = null;
            LogUtil.d(TAG,"---->MyServiceConn.onServiceDisconnected");
        }
    }

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case BluetoothService.RESTART_SCAN:
                    break;
                case BluetoothService.BLUETOOTH_CONN_OFF:
                    Intent intent = new Intent(SuccessActivity.this, MipcaActivityCapture.class);
                    startActivity(intent);
                    finish();
                    break;
                case BluetoothService.BLUETOOTH_CONN_SUCCESS:
                    if (myBinder != null){
                        BluetoothDevice bluetoothDevice = myBinder.getBluetoothDevice();
                        if (bluetoothDevice != null){
                            textView.setText(bluetoothDevice.getName());
                        }
                    }
                    break;
                case BluetoothService.BLUETOOTH_CONN_FAIL:
                    break;
                case BluetoothService.BLUETOOTH_DISCONNECTED:
                    textView.setText("未连接");
                    break;
                default:
                    break;
            }
        }
    };
}
