package com.tzly.phone.call;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 匹配电话 拨打
 */
public class PhoneCallService extends IntentService {

    public static final String ACTION = "ringing.to.answer.call";

    public static final String PHONE = "phone";
    public static final String PHONE_CALL = "phone_call";

    public static final String EXTRAS = "extras";
    public static final String WAIT = "wait";

    public static final String VERIFI = "verification";
    public static final String ANSWER = "answer";
    public static final String CALL = "call";
    public static final String REJECT = "Reject";
    public static final String MERGO = "mergo";

    private static final String TAG = "PhoneService";
    private static boolean isInterrupted;

    /**
     * 子线程名字
     */
    public PhoneCallService() {
        super("phone-service");
    }

    public PhoneCallService(String name) {
        super(name);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * 工作线程
     */
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                String extras = bundle.getString(EXTRAS);
                String wait = bundle.getString(WAIT);
                if (!TextUtils.isEmpty(wait)) {
                    stateWait(wait, intent);
                } else if (!TextUtils.isEmpty(extras)) {
                    stateExtras(extras, intent);
                }
            }
        }
    }

    private void stateExtras(String extras, Intent intent) {
        switch (extras) {
            case ANSWER://接听
                String phone = null;
                if (intent.hasExtra(PhoneCallService.PHONE)) {
                    phone = intent.getStringExtra(PhoneCallService.PHONE);
                }
                if (TextUtils.isEmpty(phone)) {
                    rejectPhone();
                } else {
                    requestPhoneNum(phone);
                }
                break;
            case REJECT://拒接
                rejectPhone();
                break;
            default:
                break;
        }
    }

    private void stateWait(String wait, Intent intent) {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        switch (wait) {
            case VERIFI:
                String pe = null;
                if (intent.hasExtra(PhoneCallService.PHONE_CALL)) {
                    pe = intent.getStringExtra(PhoneCallService.PHONE_CALL);
                }
                if (TextUtils.isEmpty(pe)) {
                    rejectPhone();
                } else {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    requestPhoneNum(pe);
                }
                break;
            case ANSWER:
                answerPhone(intent.getStringExtra(PHONE_CALL));
                break;
            case CALL:
                if (intent.hasExtra(PhoneCallService.PHONE_CALL)) {
                    String phone = intent.getStringExtra(PhoneCallService.PHONE_CALL);
                    outCallPhone(phone);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mergoPhone();
                } else {
                    rejectPhone();
                }
                break;
            case MERGO:
                mergoPhone();
                break;
            case REJECT:
                rejectPhone();
                break;
            default:
                break;
        }
    }

    /**
     * -1 50
     */
    private synchronized void combinedDetection() {
        int timeCount = 0;
        isInterrupted = false;
        try {
            while (timeCount <= 50 && timeCount >= 0 && !isInterrupted) {
                Thread.sleep(1000);
                mergoPhone();
                timeCount++;
                Log.e(TAG, "==>" + "timeCount" + timeCount);
            }

            if (timeCount > 50) rejectPhone();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void outCallPhone(String phone) {
        Intent intent = new Intent(); // 意图对象：动作 + 数据
        intent.setAction(Intent.ACTION_CALL); // 设置动作
        Uri data = Uri.parse("tel:" + phone); // 设置数据
        intent.setData(data);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent); // 激活Activity组件
    }

    /**
     * 重新验证
     */
    private void verificationPhone(String phone) {
        Intent intent = new Intent(getApplication(), AnswerAccessibilityService.class);
        intent.setAction(AnswerAccessibilityService.VER_CALL);
        Bundle bundle = new Bundle();
        bundle.putString(PhoneCallService.PHONE, phone);
        intent.putExtras(bundle);
        getApplication().startService(intent);
    }

    /**
     * 接电话
     */
    private void answerPhone(String phone) {
        Intent intent = new Intent(getApplication(), AnswerAccessibilityService.class);
        intent.setAction(AnswerAccessibilityService.ANSWER_CALL);
        Bundle bundle = new Bundle();
        bundle.putString(PhoneCallService.PHONE_CALL, phone);
        intent.putExtras(bundle);
        getApplication().startService(intent);
    }

    /**
     * 挂电话
     */
    private void rejectPhone() {
        Intent intent = new Intent(getApplication(), AnswerAccessibilityService.class);
        intent.setAction(AnswerAccessibilityService.REJECT_CALL);
        getApplication().startService(intent);
    }

    private void mergoPhone() {
        Intent intent = new Intent(getApplication(), AnswerAccessibilityService.class);
        intent.setAction(AnswerAccessibilityService.ADD_CALL);
        getApplication().startService(intent);
    }

    /**
     * 验证号码
     */
    private void requestPhoneNum(final String phone) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://dev.liyingtong.com/moveCarNotice/findByPhone?phone=" + phone)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                verificationPhone(phone);
                call.cancel();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String res = response.body().string();
                    responsePhone(res);
                } else {
                    verificationPhone(phone);
                    call.cancel();
                }
            }
        });
    }

    private void responsePhone(String result) {
        PhoneResponse response = new Gson().fromJson(result, PhoneResponse.class);
        String code = response.getResponseCode();
        if (code != null && code.equals("2000")) {
            if (response.getData() != null) {
                String phone = response.getData().getAcceptPhone();
                if (TextUtils.isEmpty(phone)) {
                    rejectPhone();
                } else {
                    answerPhone(phone);
                }
            } else {
                rejectPhone();
            }
        } else {
            //挂电话
            rejectPhone();
        }
    }

    public byte[] readStream(InputStream inStream) throws Exception {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        while ((len = inStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        inStream.close();
        return outStream.toByteArray();
    }

    private RadioHandler handler;
    private HandlerThread thread;

    private void callPhone(String phone) {
        Intent intent = new Intent(); // 意图对象：动作 + 数据
        intent.setAction(Intent.ACTION_CALL); // 设置动作
        Uri data = Uri.parse("tel:" + phone); // 设置数据
        intent.setData(data);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent); // 激活Activity组件

      /*  TelephonyManager systemService = (TelephonyManager) getSystemService(
                Service.TELEPHONY_SERVICE);
        PhoneCallStateListener callStateListener = new PhoneCallStateListener(this);
        if (systemService != null) {
            systemService.listen(callStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }*/

        //创建一个线程,线程名字：handler-thread
        thread = new HandlerThread("radio-thread");
        if (!thread.isAlive()) {
            thread.start();
        }
        handler = new RadioHandler(thread.getLooper(), PhoneCallService.this);
        if (thread.isAlive()) handler.sendEmptyMessage(100);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    public static void removeCallbacks() {
        isInterrupted = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

//        ShellUtils.execCommand(new String[]{"logcat -c", "logcat -g"}, true);
        Log.e(TAG, "==>" + "onDestroy");
    }


    private static class RadioHandler extends Handler {
        private WeakReference<PhoneCallService> weakReference;

        private RadioHandler(Looper looper, PhoneCallService phoneCallService) {
            super(looper);
            weakReference = new WeakReference<>(phoneCallService);
        }

        @Override
        public void handleMessage(Message msg) {
            PhoneCallService callService = weakReference.get();
            if (callService != null) {
                switch (msg.what) {
                    case 100:
                        Log.e(TAG, "==>" + "线程开始工作" + Thread.currentThread().getName());
                        callService.rootShell();
                        break;
                    case 200:
                        callService.closeThread();
                        break;
                    default:
                        break;
                }
            } else {
                Log.e(TAG, "==>" + "null");
            }
        }
    }

    private void closeThread() {
        if (thread != null) thread.quit();
    }

    private void rootShell() {
        long threadStart = System.currentTimeMillis();

        try {
            Process process = Runtime.getRuntime().exec("su");
            PrintWriter printWriter = new PrintWriter(process.getOutputStream());
            printWriter.println("logcat -b radio -s CAT:D"); //logcat命令, -v 详细时间; -b radio 通信相关日志缓冲区
            printWriter.flush();

            //循环读取通话日志
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String strLine;
            while ((strLine = bufferedReader.readLine()) != null) {

                String date = strLine.substring(1, 15);//06-01 17:39:23
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yy-MM-dd hh:mm:ss", Locale.SIMPLIFIED_CHINESE);
                long linedate = simpleDateFormat.parse("2018-" + date).getTime();
                if (linedate <= threadStart) continue;

                handlerCallStatus(strLine);
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

    private void handlerCallStatus(String strLine) {
        String IDLE_IDLE = "processPhoneStateChanged fg state -> IDLE,IDLE";

        String ACTIVE = "processPhoneStateChanged fg state -> IDLE,ACTIVE";
        String ACTIVE_ACTIVE = "processPhoneStateChanged fg state -> ACTIVE,ACTIVE";
        String ACTIVE_DISCONNECTING = "processPhoneStateChanged fg state -> ACTIVE,DISCONNECTING";

        String DISCONNECTING_IDLE = "processPhoneStateChanged fg state -> DISCONNECTING,IDLE";

        Log.e("phone==>", strLine);
        if (strLine.contains(ACTIVE)) {//第一个接通

        } else if (strLine.contains(ACTIVE_ACTIVE)) {//第二人通

            Intent intent = new Intent(getApplication(), AnswerAccessibilityService.class);
            intent.setAction("intent.add.or.off.phone");
            getApplication().startService(intent);

        } else if (strLine.contains(ACTIVE_DISCONNECTING)) {//一方挂断

        } else if (strLine.contains(DISCONNECTING_IDLE)) {//
        }
    }
}
