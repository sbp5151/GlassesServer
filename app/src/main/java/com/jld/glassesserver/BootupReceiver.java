package com.jld.glassesserver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by lz on 2016/10/14.
 */

public class BootupReceiver extends BroadcastReceiver {

    private final String ACTION_BOOT = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (ACTION_BOOT.equals(action)){
//            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            intent.setClass(context,MipcaActivityCapture.class);
//            context.startActivity(intent);
        }
    }
}
