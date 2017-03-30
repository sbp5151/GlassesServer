package com.jld.glassesserver;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.jld.glassesserver.barcode.MipcaActivityCapture;
import com.jld.glassesserver.db.NumberDao;
import com.jld.glassesserver.util.ClsUtils;
import com.jld.glassesserver.util.ConfigInfo;
import com.jld.glassesserver.util.LogUtil;
import com.jld.glassesserver.util.SystemUtil;
import com.jld.glassesserver.util.WifiAdmin;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.UUID;

/**
 * Created by lz on 2016/10/15.
 */

public class BluetoothService extends Service {

    private static final String TAG = "BluetoothService";

    private MyBinder myBinder;

    private BluetoothAdapter mBluetoothAdapter = null;
    private static final int MESSAGE_READ = 0x1;//读
    private static final int MESSAGE_WRITE = 0x2;//写
    public static final int BLUETOOTH_DISCONNECTED = 0x3;//断开连接
    private static final int Initiative_Connecte = 0x4;//主动连接时

    private static final int SettingWifiDelayedTime = 0x5;//当设置wifi连接时需启动的一个判断wifi连接的延时message
    public static final int BREAK_CONNECT = 0x6;//主动断开连接
    public static final int DEVICE_DISCOVER = 0x7;//设备可检测
    private String batteryInfo;
    private boolean isInitiativeConnecte;

    private Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MESSAGE_READ:
                    byte[] readBuff = (byte[]) msg.obj;
                    String readString = new String(readBuff, 0, msg.arg1);
                    LogUtil.d(TAG, "bluetooth read message : " + readString);
                    feedBack(readString);
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuff = (byte[]) msg.obj;
                    myBinder.write(writeBuff);
                    break;
                case BLUETOOTH_DISCONNECTED:
                    Log.d(TAG,"BLUETOOTH_DISCONNECTED");
                    MipcaActivityCapture.isConnect = false;
                    Toast.makeText(getApplicationContext(), R.string.bluetooth_disconnected, Toast.LENGTH_SHORT).show();
                    mMipCaHandler.sendEmptyMessage(BLUETOOTH_CONN_OFF);
                    break;
                case Initiative_Connecte://主动连接蓝牙，给对方发送连接消息
                    HashMap<Object, Object> params = new HashMap<>();
                    params.put(ConfigInfo.code, ConfigInfo.send_conn_2);
                    params.put("bt_name", mBluetoothAdapter.getName());
                    params.put("state", "1");
                    sendJsonInfo(params);
                    break;
                case SettingWifiDelayedTime:
                    isWifiChangeConnected = false;
                    needConnWifiName = "";
                    HashMap<Object, Object> wifiParams = new HashMap<>();
                    wifiParams.put(ConfigInfo.code, ConfigInfo.send_wifi_setting);
                    wifiParams.put("state", "0");
                    wifiParams.put("result", "0");//0表示wifi设置连接失败，1是成功
                    sendJsonInfo(wifiParams);
                    break;
                case DEVICE_DISCOVER:
                    Log.d(TAG, "-----------> DEVICE_DISCOVER");
                    Intent discoverIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    //设置蓝牙可见性，500表示可见时间（单位：秒），当值大于300时默认为300
                    discoverIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                    startActivity(discoverIntent);
                    if (!isRunning) {
                        myHandler.sendEmptyMessageDelayed(DEVICE_DISCOVER, 300 * 1000);
                    }
                    break;
            }
        }
    };

    private boolean isWifiChangeConnected = false;
    private String needConnWifiName = "";
    private SettingCameraReceiver mCameraReceiver;

    private void feedBack(String readString) {
        try {
            JSONObject jsonObject = new JSONObject(readString);
            String code = jsonObject.getString(ConfigInfo.code);
            switch (code) {
                case ConfigInfo.receive_conn_1://蓝牙被连接
                    String stateString = jsonObject.getString("state"); // 1”连接“0”断开
                    if ("1".equals(stateString)) {
                        HashMap<Object, Object> params1 = new HashMap<>();
                        params1.put(ConfigInfo.code, ConfigInfo.send_conn_1);
                        params1.put("bt_name", mBluetoothAdapter.getName());
                        sendJsonInfo(params1);
                        mMipCaHandler.sendEmptyMessage(BLUETOOTH_CONN_SUCCESS);
                    } else if ("0".equals(stateString)) {
                        LogUtil.d(TAG, "执行断开蓝牙后的一系列操作");
                    }
                    break;
                case ConfigInfo.receive_conn_2://主动连接后，对方返回的信息
                    LogUtil.d(TAG, "主动连接后，对方返回的信息");
                    mMipCaHandler.sendEmptyMessage(BLUETOOTH_CONN_SUCCESS);
                    break;
                case ConfigInfo.receive_give_info://请求系统消息
                    HashMap<Object, Object> params3 = new HashMap<>();
                    params3.put(ConfigInfo.code, ConfigInfo.send_give_info);
                    params3.put("dev_name", mBluetoothAdapter.getName());
                    params3.put("dev_sn", SystemUtil.getSerialNumber());
                    params3.put("hw_version", Build.HARDWARE);
                    params3.put("ad_version", SystemUtil.getSystemVersion());
                    params3.put("memory_size", SystemUtil.formatFileSize(SystemUtil.getTotalInternalMemorySize(), false));
                    params3.put("usable_mem", SystemUtil.formatFileSize(SystemUtil.getAvailableInternalMemorySize(), false));
                    params3.put("usable_ele", batteryInfo);
                    sendJsonInfo(params3);
                    break;
                case ConfigInfo.receive_modify_name://修改蓝牙名称
                    String dev_name = jsonObject.getString("dev_name");
                    boolean result = false;
                    if (dev_name != null && !dev_name.isEmpty()) {
                        result = mBluetoothAdapter.setName(dev_name);
                    }
                    HashMap<Object, Object> params4 = new HashMap<>();
                    params4.put(ConfigInfo.code, ConfigInfo.send_modify_name);
                    if (result) {
                        params4.put("result", "1");
                    } else {
                        params4.put("result", "0");
                    }
                    sendJsonInfo(params4);
                    break;
                case ConfigInfo.receive_wifi_setting://修改wifi信息
                    String state = jsonObject.getString("state"); // 1”请求“0”设置
                    final WifiAdmin wifiAdmin = new WifiAdmin(getApplicationContext());
                    wifiAdmin.openWifi();

                    if ("1".equals(state)) {
                        LogUtil.d(TAG, "请求wifi信息");
                        HashMap<Object, Object> params5 = new HashMap<>();
                        params5.put(ConfigInfo.code, ConfigInfo.send_wifi_setting);
                        if (wifiAdmin.isNetworkConnected(getApplicationContext())) {
                            params5.put("result", wifiAdmin.getSSID());//当是请求时,该值表示返回去的wifi名称
                        } else {
                            params5.put("result", "");//当是请求时,该值表示返回去的wifi名称
                        }
                        params5.put("state", "1");
                        sendJsonInfo(params5);
                    } else if ("0".equals(state)) {
                        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                        wifiManager.disconnect();

                        String ssid = jsonObject.getString("ssid");
                        needConnWifiName = "\"" + ssid + "\"";
                        String password = jsonObject.getString("password");
                        wifiAdmin.addNetwork(wifiAdmin.CreateWifiInfo(ssid, password));
                        if (!isWifiChangeConnected) {
                            isWifiChangeConnected = true;
                        }
                        myHandler.sendEmptyMessageDelayed(SettingWifiDelayedTime, 15000);
                    }
                    break;
                case ConfigInfo.receive_call_setting://呼叫设置
                    String callState = jsonObject.getString("state"); // 1”请求“0”设置
                    HashMap<Object, Object> params6 = new HashMap<>();
                    params6.put(ConfigInfo.code, ConfigInfo.send_call_setting);
                    if ("1".equals(callState)) {// 请求呼叫号码的信息
                        params6.put("state", "1");
                        params6.put("result", numberDao.queryNumber());//请求时该值表示为查到的号码
                    } else if ("0".equals(callState)) {// 设置呼叫号码的信息
                        final String call_number = jsonObject.getString("call_number");
                        params6.put("state", "0");
                        if (numberDao.addOrUpdateNumber(call_number)) {
                            params6.put("result", "1");
                        } else {
                            params6.put("result", "0");
                        }
                    }
                    sendJsonInfo(params6);
                    break;
                case ConfigInfo.setting_camera://收到设置相机参数以广播形式发送
                    String pictureSize = jsonObject.getString("picturesize"); // 1”请求“0”设置
                    String videoSize = jsonObject.getString("videosize"); // 1”请求“0”设置
                    Log.d(TAG, "pictureSize:" + pictureSize);
                    Log.d(TAG, "videoSize:" + videoSize);
                    Intent intent = new Intent(ConfigInfo.send_setting_camera_broadcast_action);
                    intent.putExtra("pictureSize", pictureSize);
                    intent.putExtra("videoSize", videoSize);
                    sendBroadcast(intent);
                    if (mCameraReceiver == null) {
                        //相机接到参数反馈
                        mCameraReceiver = new SettingCameraReceiver();
                        IntentFilter intentFilter = new IntentFilter("setting_camera_receive");
                        registerReceiver(mCameraReceiver, intentFilter);
                    }
                    break;
            }
        } catch (JSONException e) {
            LogUtil.d(TAG, "" + e);
        }
    }

    class SettingCameraReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //参数设置成功反馈
            Log.d(TAG, "参数设置成功反馈");
            HashMap<Object, Object> hashMap = new HashMap<>();
            hashMap.put("number", ConfigInfo.send_setting_camera);
            sendJsonInfo(hashMap);
        }
    }

    //发送信息
    private void sendJsonInfo(HashMap<Object, Object> params) {
        final Gson gson = new Gson();
        String string = gson.toJson(params);
        myHandler.obtainMessage(MESSAGE_WRITE, string.getBytes()).sendToTarget();
    }

    private NumberDao numberDao;

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.d(TAG, "BluetoothService---->onCreate");
        myBinder = new MyBinder();

        numberDao = NumberDao.getInstance(getApplicationContext());
        //获得本地的蓝牙适配器
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //如果为null,说明没有蓝牙设备
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "没有蓝牙设备", Toast.LENGTH_LONG).show();
        }
        if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);//网络wifi改变
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);//电量变化
        registerReceiver(mBroadcastReceiver, intentFilter);
        //myHandler.sendEmptyMessage(DEVICE_DISCOVER);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        LogUtil.d(TAG, "BluetoothService---->onBind");
        if (myBinder == null) {
            myBinder = new MyBinder();
            LogUtil.d(TAG, "---->new MyBinder()");
        }
        if (mBluetoothAdapter == null) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }
        return myBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        LogUtil.d(TAG, "BluetoothService---->onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        LogUtil.d(TAG, "BluetoothService---->unbindService");
        super.unbindService(conn);
    }


    public static final int RESTART_SCAN = 0x11;//蓝牙与目标在配对状态
    public static final int BLUETOOTH_CONN_SUCCESS = 0x12;//蓝牙连接成功
    public static final int BLUETOOTH_CONN_FAIL = 0x13;//蓝牙连接失败
    public static final int BLUETOOTH_CONN_OFF = 0x14;//断开连接
    public static final int ON_BACK = 0x14;//返回桌面
    private BluetoothSocket mSocket;
    private InputStream mInputStream;
    private OutputStream mOutputStream;
    private MyBinder.ConnectedThread mConnectedThread;
    private boolean isRunning;
    MyBinder.ServiceSocketThread mServiceSocketThread;
    private boolean isServiceRunning = true;
    private MyBinder.ConnectThread mConnectThread;
    private Handler mMipCaHandler;

    public class MyBinder extends Binder {

        private BluetoothDevice mBluetoothDevice;
        private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        private final String NAME = "ServiceBluetooth";
        private ConnectedThread mConnectedThread;

        private Handler uiHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case RESTART_SCAN:
                        if (mMipCaHandler != null) {
                            mMipCaHandler.sendEmptyMessage(RESTART_SCAN);
                        }
                        break;
                    case BLUETOOTH_CONN_SUCCESS:
                        Log.d(TAG,"BLUETOOTH_CONN_SUCCESS:"+mMipCaHandler);
                        if (mMipCaHandler != null) {
                            mMipCaHandler.sendEmptyMessage(BLUETOOTH_CONN_SUCCESS);
                        } else {
                            Intent intent = new Intent();
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.setClass(getApplicationContext(), SuccessActivity.class);
                            getApplicationContext().startActivity(intent);
                        }
                        Toast.makeText(getApplicationContext(), "蓝牙设备已连接：" + mBluetoothDevice.getName(), Toast.LENGTH_SHORT).show();
                        break;
                    case BLUETOOTH_CONN_FAIL:
                        if (mMipCaHandler != null) {
                            mMipCaHandler.sendEmptyMessage(BLUETOOTH_CONN_FAIL);
                        }
                        break;
                    case BLUETOOTH_DISCONNECTED:
                        if (mMipCaHandler != null) {
                            mMipCaHandler.sendEmptyMessage(BLUETOOTH_DISCONNECTED);
                        }
                        break;
                    case BREAK_CONNECT:
                        HashMap<Object, Object> params2 = new HashMap<>();
                        params2.put(ConfigInfo.code, ConfigInfo.send_conn_2);
                        params2.put("bt_name", mBluetoothAdapter.getName());
                        params2.put("state", "0");
                        sendJsonInfo(params2);
                        stopSelf();
                        LogUtil.d(TAG, "BREAK_CONNECT ------------>");
                        break;
                    default:
                        break;
                }
            }
        };


        public void setHandler(Handler handlers) {
            mMipCaHandler = handlers;
        }

        public Handler getHandler() {
            return uiHandler;
        }

        public MyBinder() {
            mServiceSocketThread = new ServiceSocketThread();
            mServiceSocketThread.start();
        }

        public BluetoothDevice getBluetoothDevice() {
            if (mBluetoothDevice != null) {
                return mBluetoothDevice;
            }
            return null;
        }

        public boolean write(byte[] buffer) {
            if (mConnectedThread == null) {
                return false;
            }
            return mConnectedThread.write(buffer);
        }

        public void conn(String macAddress) {
            if (mBluetoothAdapter == null) {
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            }
            if (!mBluetoothAdapter.isEnabled()) {
                mBluetoothAdapter.enable();
            }
            if (uiHandler == null) {
                LogUtil.d(TAG, "conn ---> mMipCaHandler is null");
                return;
            }
            mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(macAddress);
            mBluetoothAdapter.cancelDiscovery();
            try {
                if (mBluetoothDevice.getBondState() == BluetoothDevice.BOND_NONE) {//目标蓝牙没匹配时先进行配对
                    Log.d(TAG, "未配对");
                    ClsUtils.createBond(mBluetoothDevice.getClass(), mBluetoothDevice);
                    uiHandler.sendEmptyMessage(RESTART_SCAN);
                } else if (mBluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED) {//已配对直接连接
                    Log.d(TAG, "已配对");
                    connect(mBluetoothDevice);
                }
            } catch (Exception e) {
                LogUtil.d(TAG, "conn---->" + e);
                uiHandler.sendEmptyMessage(BLUETOOTH_CONN_FAIL);
            }
        }

        private void connect(BluetoothDevice bluetoothDevice) {
            mConnectThread = new ConnectThread(bluetoothDevice);
            mConnectThread.start();
        }


        /**
         * 对连接成功了的BluetoothSocket进行处理
         * 在一个单独的线程中进行读写操作
         *
         * @param socket
         */
        private void manageConnectedSocket(BluetoothSocket socket) {
            if (mConnectedThread == null) {
                mConnectedThread = new ConnectedThread(socket);
                mConnectedThread.start();
            }
        }

        public void bluetoothDisconnect() {
            if (mConnectedThread != null) {
                mConnectedThread.cancel();
                mConnectedThread = null;
            }
        }

        /**
         * 当做蓝牙服务端被连接的线程
         */
        public class ServiceSocketThread extends Thread {

            private BluetoothServerSocket serverSocket;

            public ServiceSocketThread() {
            }

            @Override
            public void run() {
                super.run();
                if (mBluetoothAdapter == null) {
                    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                }
                if (!mBluetoothAdapter.isEnabled()) {
                    mBluetoothAdapter.enable();
                }
                boolean isConnFail = true;
                do {
                    try {
                        serverSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
                        if (serverSocket != null) {
                            BluetoothSocket socket = null;
                            while (isServiceRunning) {
                                try {
                                    LogUtil.d(TAG, "wait socket connect...");
                                    socket = serverSocket.accept();
                                    if (socket != null) {
                                        mBluetoothDevice = socket.getRemoteDevice();
                                        LogUtil.d(TAG, "have socket connecting..." + mBluetoothDevice.getName());
                                        isInitiativeConnecte = false;
                                        manageConnectedSocket(socket);
                                        serverSocket.close();
                                        isConnFail = false;
                                        break;
                                    }
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                    isConnFail = true;
                                    break;
                                }
                            }
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            LogUtil.e(TAG, "线程睡眠--err-->" + e);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        isConnFail = true;
                    }
                } while (isConnFail);
            }
        }

        /**
         * 主动执行蓝牙连接的线程
         * 需传一个要连接的目标 BluetoothDevice
         */
        private class ConnectThread extends Thread {
            private final BluetoothDevice mDevice;
            private final BluetoothSocket mSocket;


            public ConnectThread(BluetoothDevice device) {
                mDevice = device;
                BluetoothSocket tmp = null;
                try {
                    tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                } catch (IOException e) {
                    LogUtil.e(TAG, "create() failed  --> " + e);
                    uiHandler.sendEmptyMessage(BLUETOOTH_CONN_FAIL);
                }
                mSocket = tmp;
            }

            @Override
            public void run() {
                super.run();
                if (mSocket != null) {
                    try {
                        mBluetoothAdapter.cancelDiscovery();
                        mSocket.connect();
                        isInitiativeConnecte = true;
                        manageConnectedSocket(mSocket);
                    } catch (IOException e) {
                        uiHandler.sendEmptyMessage(BLUETOOTH_CONN_FAIL);
                        LogUtil.e(TAG, "ConnectThread:mSocket.connect()-->" + e);
                        try {
                            mSocket.close();
                        } catch (IOException e1) {
                        }
                    }
                } else {
                    uiHandler.sendEmptyMessage(BLUETOOTH_CONN_FAIL);
                }
            }

            public void cancel() {
                try {
                    if (mSocket != null) {
                        mSocket.close();
                    }
                } catch (IOException e) {
                    LogUtil.e(TAG, "" + e);
                }
            }
        }

        /**
         * 单独执行 BluetoothSocket 读写操作的线程
         * 需传一个已经连接了得 BluetoothSocket 进来
         */
        private class ConnectedThread extends Thread {

            private final BluetoothSocket mSocket;
            private final InputStream mInputStream;
            private final OutputStream mOutputStream;
            private boolean isRunning;

            public ConnectedThread(BluetoothSocket socket) {
                mSocket = socket;
                InputStream in = null;
                OutputStream out = null;
                try {
                    in = socket.getInputStream();
                    out = socket.getOutputStream();
                } catch (IOException e) {
                    LogUtil.e(TAG, "" + e);
                }
                mInputStream = in;
                mOutputStream = out;
            }

            @Override
            public void run() {
                super.run();
                LogUtil.d(TAG, "isInitiativeConnecte--->" + isInitiativeConnecte);
                isRunning = true;
                LogUtil.d(TAG, "ConnectedThread ----> read run ");
                if (isInitiativeConnecte) {//主动连接蓝牙设备
                    myHandler.sendEmptyMessage(Initiative_Connecte);
                }
                try {
                    byte[] buffer = new byte[1024];
                    int bytes;
                    while (isRunning) {
                        while (mInputStream != null && isRunning) {
                            bytes = mInputStream.read(buffer);
                            if (bytes > 0) {
                                myHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                            }
                        }
                    }
                } catch (IOException e) {
                    if (mBluetoothAdapter == null) {
                        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    }
                    if (!mBluetoothAdapter.isEnabled()) {
                        mBluetoothAdapter.enable();
                    }
                    myHandler.sendEmptyMessage(BLUETOOTH_DISCONNECTED);
                    uiHandler.sendEmptyMessage(BLUETOOTH_DISCONNECTED);
                    bluetoothDisconnect();
                    if (!isInitiativeConnecte && myBinder != null) {
                        new ServiceSocketThread().start();
                    }
                    LogUtil.e(TAG, "disconnected -->" + e);
//                    try {
//                        if (mInputStream != null) {
//                            mInputStream.close();
//                        }
//                        if (mOutputStream != null) {
//                            mOutputStream.close();
//                        }
//                        if (mSocket != null) {
//                            mSocket.close();
//                        }
//                    } catch (Exception e1) {
//                        LogUtil.e(TAG, "close fail -->" + e1);
//                    }
                }
            }

            public boolean write(byte[] buffer) {
                if (mOutputStream == null)
                    return false;
                if (!isRunning)
                    return false;
                try {
                    mOutputStream.write(buffer);
                    mOutputStream.flush();
                    LogUtil.d(TAG, "write --->" + new String(buffer));
                    return true;
                } catch (IOException e) {
                    LogUtil.e(TAG, "" + e);
                    return false;
                }
            }

            public void cancel() {
                isRunning = false;
                try {
                    if (mInputStream != null) {
                        mInputStream.close();
                    }
                    if (mOutputStream != null) {
                        mOutputStream.close();
                    }
                    if (mSocket != null) {
                        mSocket.close();
                    }
                } catch (Exception e) {
                    LogUtil.e(TAG, "close fail -->" + e);
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        LogUtil.d(TAG, "BluetoothService---->onDestroy");
        unregisterReceiver(mBroadcastReceiver);
        myBinder = null;
        isRunning = false;
        isServiceRunning = false;
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
            mBluetoothAdapter = null;
        }
        if (mInputStream != null && mOutputStream != null) {
            try {
                mInputStream.close();
                mOutputStream.close();
                mInputStream = null;
                mOutputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mSocket != null) {
            try {
                mSocket.close();
                mSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mServiceSocketThread != null) {
            mServiceSocketThread.interrupt();
            mServiceSocketThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.interrupt();
            mConnectedThread = null;
        }
        if (mConnectThread != null) {
            mConnectThread.interrupt();
            mConnectThread = null;
        }
        super.onDestroy();
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            LogUtil.d(TAG, "" + action);
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {//电量广播
                int level = intent.getIntExtra("level", 0);
                int scale = intent.getIntExtra("scale", 100); //电池最大值。通常为100
                batteryInfo = level * 100 / scale + "%";
            } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {//网络改变广播
                if (isWifiChangeConnected) {
                    WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                    NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    //LogUtil.d(TAG, "2连接的wifi为：" + wifiInfo.getSSID());
                    if (networkInfo != null && networkInfo.isConnected() && needConnWifiName.equals(wifiInfo.getSSID())) {
                        myHandler.removeMessages(SettingWifiDelayedTime);
                        HashMap<Object, Object> params = new HashMap<>();
                        params.put(ConfigInfo.code, ConfigInfo.send_wifi_setting);
                        params.put("state", "0");
                        params.put("result", "1");
                        sendJsonInfo(params);
                        isWifiChangeConnected = false;
                        needConnWifiName = "";
                    }
                }
            }
        }
    };

}
