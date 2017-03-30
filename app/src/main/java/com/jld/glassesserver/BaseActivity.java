package com.jld.glassesserver;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;

public class BaseActivity extends AppCompatActivity {
    public static final String TAG = "BaseActivity";
    public int firstKeyDown = -1;
    public int curKeyDown = -1;
    public int curKeyUp = -1;
    public static final int SINGLE_KEY = 1;
    public static final int GLIDE_UP = 2;
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            int what = msg.what;
            switch (what) {
                case SINGLE_KEY://单击
                    Log.d(TAG, "------onKeyUp:" + "单击");
                    listener.singleClick();
                    break;
                case GLIDE_UP://滑动抬起
                    glideEnd();
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        // 隐藏标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 隐藏状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "------onKeyDown:" + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_F1:
                return true;
            case KeyEvent.KEYCODE_F2:
            case KeyEvent.KEYCODE_F3:
            case KeyEvent.KEYCODE_F4:
            case KeyEvent.KEYCODE_F5:
            case KeyEvent.KEYCODE_F6:
                if (curKeyDown != keyCode) {
                    curKeyDown = keyCode;
                    if (firstKeyDown < 0) {//触摸开始
                        firstKeyDown = keyCode;
                    }else {
                        mHandler.removeMessages(GLIDE_UP);
                    }
                }
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    long curTime;
    int doubleDelay = 400;
    int glideDelay = 120;

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG, "------onKeyUp:" + keyCode);
        Log.d(TAG, "------System.currentTimeMillis():" + System.currentTimeMillis());
        switch (keyCode) {
            case KeyEvent.KEYCODE_F1:
                if (System.currentTimeMillis() - curTime < doubleDelay) {//双击
                    Log.d(TAG, "------onKeyUp:" + "双击");
                    mHandler.removeMessages(SINGLE_KEY);
                    listener.doubleClick();
                } else {//单击
                    mHandler.sendEmptyMessageDelayed(SINGLE_KEY, doubleDelay);
                }
                curTime = System.currentTimeMillis();
                return true;
            case KeyEvent.KEYCODE_F2:
            case KeyEvent.KEYCODE_F3:
            case KeyEvent.KEYCODE_F4:
            case KeyEvent.KEYCODE_F5:
            case KeyEvent.KEYCODE_F6:
                if (curKeyUp != keyCode)
                    curKeyUp = keyCode;
                mHandler.sendEmptyMessageDelayed(GLIDE_UP, glideDelay);
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    public void glideEnd() {
        int touchNum = curKeyDown - firstKeyDown;
        if (Math.abs(touchNum) > 1) {//触摸有效
            if (touchNum > 0) {//右滑
                listener.rightGlide();
            } else {//左滑
                listener.leftGlide();
            }
        }
        firstKeyDown = -1;
        curKeyUp = -1;
        curKeyDown = -1;
    }

    MyKeyListener listener;

    public void setKeyListener(MyKeyListener listener) {
        this.listener = listener;
    }

    public interface MyKeyListener {
        //单击
        public void singleClick();

        //双击
        public void doubleClick();

        //左滑
        public void leftGlide();

        //右滑
        public void rightGlide();
    }
}
