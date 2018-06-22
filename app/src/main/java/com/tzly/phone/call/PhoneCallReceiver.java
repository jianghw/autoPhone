package com.tzly.phone.call;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

/**
 * 电话监听
 */
public class PhoneCallReceiver extends BroadcastReceiver {
    private static final String TAG = "PhoneCallReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            Log.e(TAG, "==>" + "拨出");
        } else {
            //设置一个监听器
            TelephonyManager telephonyManager = (TelephonyManager)
                    context.getSystemService(Service.TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                int callState = telephonyManager.getCallState();
                Log.e(TAG, "==>=======" + callState);
                //处理各种状态
                handlingPhoneStates(context, callState, intent);
            }
            //接电话
        }
    }

    /**
     * 电话状态处理
     */
    private void handlingPhoneStates(Context context, int callState, Intent i) {
        String inPhoneNum = i.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
        //来电处理
        if (!TextUtils.isEmpty(inPhoneNum) && callState == TelephonyManager.CALL_STATE_RINGING) {

        } else if (callState == TelephonyManager.CALL_STATE_IDLE) {
            Intent intent = new Intent(context, AnswerAccessibilityService.class);
            intent.setAction(AnswerAccessibilityService.REMOVE_CALL);
            context.startService(intent);
        }
    }
}
