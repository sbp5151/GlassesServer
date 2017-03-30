package com.jld.glassesserver.barcode;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.jld.glassesserver.BaseActivity;
import com.jld.glassesserver.BluetoothService;
import com.jld.glassesserver.OtherInfoActivity;
import com.jld.glassesserver.R;
import com.jld.glassesserver.SuccessActivity;
import com.jld.glassesserver.barcode.camera.CameraManager;
import com.jld.glassesserver.barcode.decoding.CaptureActivityHandler;
import com.jld.glassesserver.barcode.decoding.InactivityTimer;
import com.jld.glassesserver.barcode.view.ViewfinderView;
import com.jld.glassesserver.util.LogUtil;

import java.io.IOException;
import java.util.Vector;

public class MipcaActivityCapture extends BaseActivity implements Callback, BaseActivity.MyKeyListener {

    private static final String TAG = "MipcaActivityCapture";

    private CaptureActivityHandler handler;
    private ViewfinderView viewfinderView;
    private boolean hasSurface;
    private Vector<BarcodeFormat> decodeFormats;
    private String characterSet;
    private InactivityTimer inactivityTimer;
    private MediaPlayer mediaPlayer;
    private boolean playBeep;
    private static final float BEEP_VOLUME = 1.00f;
    private boolean vibrate;

    private MyServiceConn myServiceConn;
    private BluetoothService.MyBinder myBinder;
    private BluetoothAdapter mBluetoothAdapter = null;
    public static boolean isConnect = false;
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);
        CameraManager.init(this);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }
        viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
        hasSurface = false;
        inactivityTimer = new InactivityTimer(this);
        Intent intent = new Intent(this,BluetoothService.class);
        myServiceConn = new MyServiceConn();
        startService(intent);
        bindService(intent,myServiceConn,BIND_AUTO_CREATE);
        setKeyListener(this);
        Log.d(TAG,"onCreate:");

    }

    private BluetoothAdapter ba;                   //蓝牙适配器
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG,"onStart:"+isConnect);
//        ba = BluetoothAdapter.getDefaultAdapter();
//        Log.d(TAG,"ba.isEnabled():"+ba.isEnabled());
//        if(ba.isEnabled()){
//            Log.d(TAG,"ba.getProfileConnectionState(BluetoothProfile.A2DP):"+ba.getProfileConnectionState(BluetoothProfile.A2DP));
//            if(ba.getProfileConnectionState(BluetoothProfile.A2DP)==BluetoothProfile.STATE_CONNECTED){
//                Intent intent = new Intent();
//                intent.setClass(this, SuccessActivity.class);
//                getApplicationContext().startActivity(intent);
//            }
//        }
        if(isConnect){
            Intent intent = new Intent();
            intent.setClass(this, SuccessActivity.class);
            this.startActivity(intent);
            finish();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"----onDestroy");
        inactivityTimer.shutdown();
        unbindService(myServiceConn);
    }
    @Override
    public void singleClick() {
        Log.d(TAG,"----singleClick");
    }

    @Override
    public void doubleClick() {
        onBackPressed();
    }

    @Override
    public void leftGlide() {

    }

    @Override
    public void rightGlide() {

    }

    public class MyServiceConn implements ServiceConnection{

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            myBinder = (BluetoothService.MyBinder) service;
            myBinder.setHandler(myHandler);
            LogUtil.d(TAG,"---->MyServiceConn.onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            myBinder = null;
            LogUtil.d(TAG,"---->MyServiceConn.onServiceDisconnected");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
        decodeFormats = null;
        characterSet = null;

        playBeep = true;
        AudioManager audioService = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
            playBeep = false;
        }
        initBeepSound();
        vibrate = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        CameraManager.get().closeDriver();
    }



    private Handler myHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case BluetoothService.RESTART_SCAN:
                    Toast.makeText(MipcaActivityCapture.this, R.string.bluetooth_matching_string,Toast.LENGTH_LONG).show();
                    restartScan();
                    break;
                case BluetoothService.BLUETOOTH_CONN_SUCCESS:
                    Log.d(TAG,"BLUETOOTH_CONN_SUCCESS:"+isConnect);
                    if(isConnect){
                        Intent intent = new Intent();
                        intent.setClass(MipcaActivityCapture.this, SuccessActivity.class);
                        startActivity(intent);
                        MipcaActivityCapture.this.finish();
                    }else
                        onBackPressed();
                    isConnect = true;
                    break;
                case BluetoothService.BLUETOOTH_CONN_FAIL:
                    Toast.makeText(MipcaActivityCapture.this, R.string.bluetooth_conn_fail,Toast.LENGTH_LONG).show();
                    restartScan();
                    break;
                case BluetoothService.ON_BACK:
                    onBackPressed();
                    break;
                default:
                    break;
            }
        }
    };

    private void restartScan(){
        //使能多次扫描
        if (handler != null) {
            Message message = Message.obtain();
            message.what = R.id.restart_preview;
            handler.sendMessageDelayed(message, 1000);
            playBeep = true;
            AudioManager audioService = (AudioManager) getSystemService(AUDIO_SERVICE);
            if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
                playBeep = false;
            }
            mediaPlayer = null;
            initBeepSound();
        }
    }

    /**
     * 扫描完成返回结果
     * @param result
     * @param barcode
     */
    public void handleDecode(Result result, Bitmap barcode) {
        inactivityTimer.onActivity();
        playBeepSoundAndVibrate();
        String resultString = result.getText();
        if (myBinder != null && BluetoothAdapter.checkBluetoothAddress(resultString)) {
            LogUtil.d("BluetoothService","---->handleDecode-->result:"+resultString);
            myBinder.conn(resultString);
        }else {
            Intent intent = new Intent();
            intent.putExtra("scanString",resultString);
            intent.setClass(getApplicationContext(), OtherInfoActivity.class);
            MipcaActivityCapture.this.startActivity(intent);
        }
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        try {
            CameraManager.get().openDriver(surfaceHolder);
        } catch (Exception e) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.dialog_title);
            builder.setMessage(R.string.dialog_msg);
            builder.setCancelable(false);
            builder.setPositiveButton(R.string.dialog_sure, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    finish();
                }
            });
            builder.create().show();
            return;
        }
        if (handler == null) {
            handler = new CaptureActivityHandler(this, decodeFormats, characterSet);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    public ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getHandler() {
        return handler;
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }

    private void initBeepSound() {
        if (playBeep && mediaPlayer == null) {
            // The volume on STREAM_SYSTEM is not adjustable, and users found it
            // too loud,
            // so we now play on the music stream.
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnCompletionListener(beepListener);

            AssetFileDescriptor file = getResources().openRawResourceFd(
                    R.raw.qrcode);
            try {
                mediaPlayer.setDataSource(file.getFileDescriptor(),
                        file.getStartOffset(), file.getLength());
                file.close();
                mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
                mediaPlayer.prepare();
            } catch (IOException e) {
                mediaPlayer = null;
            }
        }
    }

    private static final long VIBRATE_DURATION = 200L;

    private void playBeepSoundAndVibrate() {
        if (playBeep && mediaPlayer != null) {
            mediaPlayer.start();
        }
        if (vibrate) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vibrator.vibrate(VIBRATE_DURATION);
        }
    }

    /**
     * When the beep has finished playing, rewind to queue up another one.
     */
    private final OnCompletionListener beepListener = new OnCompletionListener() {
        public void onCompletion(MediaPlayer mediaPlayer) {
            mediaPlayer.seekTo(0);
        }
    };

}