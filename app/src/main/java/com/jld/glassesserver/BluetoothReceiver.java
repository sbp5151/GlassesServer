package com.jld.glassesserver;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.jld.glassesserver.util.ClsUtils;
import com.jld.glassesserver.util.LogUtil;

/**
 * Created by lz on 2016/10/17.
 */

public class BluetoothReceiver extends BroadcastReceiver {

    private String pin = "1234"; //此处为你要连接的蓝牙设备的初始密钥，一般为1234或0000
    private static final String TAG = "BluetoothReceiver";
    public BluetoothReceiver(){}

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        LogUtil.d(TAG,"onReceive---action--->"+ action);
        BluetoothDevice bluetoothDevice = null;
        bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (BluetoothDevice.ACTION_FOUND.equals(action)){//发现设备
            LogUtil.d(TAG,"BluetoothReceiver ----> 发现设备");
            try {
                ClsUtils.createBond(bluetoothDevice.getClass(),bluetoothDevice);
            }catch (Exception e){
                e.printStackTrace();
            }
        }else
        if (action.equals("android.bluetooth.device.action.PAIRING_REQUEST")){//再次得到的action，会等于PAIRING_REQUEST
            LogUtil.d(TAG,"BluetoothReceiver ----> 配对设备");
            try {
                //1.确认配对
                ClsUtils.setPairingConfirmation(bluetoothDevice.getClass(),bluetoothDevice,true);
                //2.终止有序广播
                abortBroadcast();//如果没有将广播终止，则会出现一个一闪而过的配对框。
                //3.调用setPin方法进行配对...
                boolean ret = ClsUtils.setPin(bluetoothDevice.getClass(),bluetoothDevice,pin);
            }catch (Exception e){
                e.printStackTrace();
            }
        }else {
            LogUtil.d(TAG,"这个设备不是目标蓝牙设备");
        }
    }
}
