package com.tzly.phone.call;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.regex.Matcher;

/**
 * 状态监听
 */
public class PhoneCallStateListener extends PhoneStateListener {

    private static final String TAG = "StateListener";

    private final RadioHandler handler;
    private final HandlerThread thread;

    private Runnable runnable = new Runnable() {
        public void run() {
            rootShell();
        }
    };

    private void rootShell() {
        long threadStart = System.currentTimeMillis();

        try {
            Process process = Runtime.getRuntime().exec("su");
            PrintWriter printWriter = new PrintWriter(process.getOutputStream());
            printWriter.println("logcat -b radio -s GsmCallTracker:I"); //logcat命令, -v 详细时间; -b radio 通信相关日志缓冲区
            printWriter.flush();

            //循环读取通话日志
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String strLine;
            while ((strLine = bufferedReader.readLine()) != null) {

                handlerCallStatus(threadStart, strLine);
            }
            printWriter.close();
            bufferedReader.close();
            process.destroy();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void handlerCallStatus(long threadStart, String strLine) throws ParseException {
        String ACTIVE = "Foreground call: ACTIVE";
        String IDLE = "IDLE";
        String DISCONNECTED = "Background call: DISCONNECTED";

        String date = strLine.substring(1, 15);//06-01 17:39:23
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yy-MM-dd hh:mm:ss", Locale.SIMPLIFIED_CHINESE);
        long linedate = simpleDateFormat.parse("2018-" + date).getTime();
        if (linedate < threadStart) return;

        Log.e("phone==>", strLine);
        if (strLine.contains(IDLE)) {//挂断

        }
        if (strLine.contains(ACTIVE)) {
            ShellUtils.execCommand(new String[]{"input tap 560 800"}, true);
        }

        if (strLine.contains(DISCONNECTED)) {
            if (handler != null) handler.sendEmptyMessage(200);
        }
    }

    public PhoneCallStateListener(Context context) {
        //创建一个线程,线程名字：handler-thread
        thread = new HandlerThread("radio-thread");
        //开启一个线程
        thread.start();
        handler = new RadioHandler(thread.getLooper(), this);
        handler.sendEmptyMessage(100);
        Log.e(TAG, "==>" + "线程开始工作" + Thread.currentThread().getName());
    }

    @Override
    public void onCallStateChanged(int state, String incomingNumber) {
        super.onCallStateChanged(state, incomingNumber);

        switch (state) {
            case TelephonyManager.CALL_STATE_IDLE:
                Log.e(TAG, "==>" + "IDLE");
                break;
            case TelephonyManager.CALL_STATE_RINGING:
                Log.e(TAG, "==>" + "RINGING" + incomingNumber);
                //输出来电号码
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                Log.e(TAG, "==>" + "OFFHOOK");
                break;
        }
    }

    private static class RadioHandler extends Handler {
        private WeakReference<PhoneCallStateListener> weakReference;

        private RadioHandler(Looper looper, PhoneCallStateListener context) {
            super(looper);
            weakReference = new WeakReference<>(context);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            PhoneCallStateListener stateListener = weakReference.get();
            switch (msg.what) {
                case 100:
                    if (stateListener != null) stateListener.rootShell();
                    break;
                case 200:
                    if (stateListener != null) stateListener.closeThread();
                    break;
            }
        }
    }

    private void closeThread() {
        if (thread != null) thread.quit();
        Log.e(TAG, "==>" + "over");
    }

}
