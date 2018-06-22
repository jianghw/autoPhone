package com.tzly.phone.call;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

/**
 * 前台服务
 */
public class ForegroundService extends Service {

    public static final String PHONE = "phone";
    //定义onBinder方法所返回的对象
    private ForegroundBinder binder = new ForegroundBinder();

    public class ForegroundBinder extends Binder {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
//        throw new UnsupportedOperationException("Not yet implemented");
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Notification.Builder builder = new Notification.Builder(this);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);
        builder.setContentIntent(contentIntent);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setTicker("Foreground Service Start");
        builder.setContentTitle("Foreground Service");
        builder.setContentText("Make this service run in the foreground.");
        Notification notification = builder.build();
        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle bundle = intent.getExtras();
        /*if (bundle != null) {
            String phone = bundle.getString(PHONE);
            if (!TextUtils.isEmpty(phone)) {
                callPhone(phone);
            }
        }*/

        return super.onStartCommand(intent, flags, startId);
    }

    private void callPhone(String phone) {
        Intent intent = new Intent(); // 意图对象：动作 + 数据
        intent.setAction(Intent.ACTION_CALL); // 设置动作
        Uri data = Uri.parse("tel:" + phone); // 设置数据
        intent.setData(data);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent); // 激活Activity组件

        TelephonyManager systemService = (TelephonyManager) getSystemService(
                Service.TELEPHONY_SERVICE);
        PhoneCallStateListener callStateListener = new PhoneCallStateListener(this);
        if (systemService != null) {
            systemService.listen(callStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
