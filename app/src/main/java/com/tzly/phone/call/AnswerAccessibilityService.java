package com.tzly.phone.call;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AnswerAccessibilityService extends AccessibilityService {

    private static final String TAG = "AnswerAccessibility";

    public static final String VER_CALL = "com.phone.intent.ver.call";
    public static final String ANSWER_CALL = "com.phone.intent.answer.call";
    public static final String REJECT_CALL = "com.phone.intent.reject.call";
    public static final String ADD_CALL = "com.phone.intent.add.call";
    public static final String REMOVE_CALL = "com.phone.intent.remove.call";

    /**
     * ANSWER
     */
    private Map<String, Integer> statusMap = new ConcurrentHashMap<>();

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        Log.e(TAG, "按钮点击变化");
        //接收按键事件
        return super.onKeyEvent(event);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        //接收事件,如触发了通知栏变化、界面变化等
        int eventType = event.getEventType();
        String className = event.getClassName().toString();
        switch (eventType) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                //来电页面
                if (className.equals("com.android.incallui.InCallActivity")) {
                    callActivity();
                }
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                break;
            default:
                break;

        }
    }

    private void callActivity() {
        //手机号码显示
        AccessibilityNodeInfo nodeInfo = this.getRootInActiveWindow();
        AccessibilityNodeInfo name = findNodeInfoById(nodeInfo,
                "com.android.incallui:id/name");
        if (name == null) {

        } else {
            String phone = name.getText().toString();
            phone = phone.replaceAll(" ", "");
            boolean isPhone = RegexUtils.isNumber(phone);
            if (isPhone) {
                verificationPhone(phone);
            } else {
                rejectPhone();
            }
        }
    }

    @Override
    public void onInterrupt() {
        //服务中断，如授权关闭或者将服务杀死
        Log.e(TAG, "授权中断");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.e(TAG, "service授权成功");
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            Log.e(TAG, "===>" + action);
            switch (action) {
                case VER_CALL:
                    commandVerCall(intent);
                    break;
                case ANSWER_CALL:
                    commandAnswerCall(intent);
                    break;
                case REJECT_CALL:
                    commandRejectCall(intent);
                    break;
                case ADD_CALL:
                    commandMergeCall(intent);
                    break;
                case REMOVE_CALL:
                    commandRemoveCall(intent);
                    break;
                default:
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void commandVerCall(Intent intent) {
        String phone = intent.getStringExtra(PhoneCallService.PHONE);
        waitPhone(phone, PhoneCallService.VERIFI);
    }

    private void commandAnswerCall(Intent intent) {
        AccessibilityNodeInfo nodeInfo = this.getRootInActiveWindow();
        AccessibilityNodeInfo answer = findNodeInfoById(nodeInfo,
                "com.android.incallui:id/incoming_answer");

        if (intent.hasExtra(PhoneCallService.PHONE_CALL)) {
            String phone = intent.getStringExtra(PhoneCallService.PHONE_CALL);
            if (TextUtils.isEmpty(phone)) {
                rejectPhone();
            } else {
                if (answer == null) {//线程等待
                    waitPhone(phone, PhoneCallService.ANSWER);
                } else {
                    answerCall();
                    waitPhone(phone, PhoneCallService.CALL);
                }
            }
        } else {
            rejectPhone();
        }
    }

    private void commandRejectCall(Intent intent) {
        AccessibilityNodeInfo nodeInfo = this.getRootInActiveWindow();
        AccessibilityNodeInfo end = findNodeInfoById(nodeInfo,
                "com.android.incallui:id/endButton");

        AccessibilityNodeInfo reject = findNodeInfoById(nodeInfo,
                "com.android.incallui:id/incoming_reject");
        if (end != null) {
            endCall();
        } else if (reject != null) {
            rejectCall();
        } else {
            waitPhone(PhoneCallService.REJECT);
        }
    }

    private void commandMergeCall(Intent intent) {
        AccessibilityNodeInfo nodeInfo = this.getRootInActiveWindow();
        AccessibilityNodeInfo name = findNodeInfoById(nodeInfo,
                "com.android.incallui:id/name");
        AccessibilityNodeInfo primary = findNodeInfoById(nodeInfo,
                "com.android.incallui:id/primary_call_state");
        if (name != null) {
            rejectPhone();
        } else if (primary == null) {
            waitPhone(PhoneCallService.MERGO);
        } else {
            String callstate = primary.getText().toString();
            if (callstate.equals("通话")) {
                mergeCall();
                silentCall();
            } else {
                waitPhone(PhoneCallService.MERGO);
            }
        }
    }

    private void commandRemoveCall(Intent intent) {
        Intent in = new Intent(getApplication(), PhoneCallService.class);
        in.setAction(PhoneCallService.ACTION);
        getApplication().stopService(in);

        if (!statusMap.isEmpty()) {
            statusMap.clear();
        }
    }

    /**
     * 验证号码
     */
    private void verificationPhone(String phone) {
        Boolean answer = statusMap.containsKey(PhoneCallService.ANSWER);
        if (answer) {
            Log.e(TAG, "正在通话中...");
        } else {
            if (!statusMap.isEmpty()) statusMap.clear();

            Intent intent = new Intent(getApplication(), PhoneCallService.class);
            intent.setAction(PhoneCallService.ACTION);
            Bundle bundle = new Bundle();
            bundle.putString(PhoneCallService.EXTRAS, PhoneCallService.ANSWER);
            bundle.putString(PhoneCallService.PHONE, phone);
            intent.putExtras(bundle);
            getApplication().startService(intent);
        }
    }

    /**
     * PhoneCallService.MERGO
     * PhoneCallService.REJECT
     */
    private void waitPhone(String type) {
        waitPhone(null, type);
    }

    private void waitPhone(String phone, String type) {
        boolean isTypeWait = statusMap.containsKey(type);

        if (isTypeWait) {
            int count = statusMap.get(type);
            if (type.equals(PhoneCallService.MERGO)) {
                if (count <= 100 && statusMap.containsKey(PhoneCallService.ANSWER)) {
                    count++;
                    statusMap.put(type, count);
                    waitToService(phone, type);
                } else {
                    rejectPhone();
                }
            } else if (type.equals(PhoneCallService.REJECT)) {
                if (count <= 5) {
                    count++;
                    statusMap.put(type, count);
                    waitToService(phone, type);
                } else {
                    statusMap.clear();
                }
            } else if (type.equals(PhoneCallService.VERIFI)
                    || type.equals(PhoneCallService.ANSWER)) {
                if (count <= 5) {
                    count++;
                    statusMap.put(type, count);
                    waitToService(phone, type);
                } else {
                    rejectPhone();
                }
            } else {
                if (count <= 5) {
                    count++;
                    statusMap.put(type, count);
                    waitToService(phone, type);
                }
            }
        } else {
            statusMap.put(type, 1);
            waitToService(phone, type);
        }
    }

    private void waitToService(String phone, String type) {
        Intent intent = new Intent(getApplication(), PhoneCallService.class);
        intent.setAction(PhoneCallService.ACTION);
        Bundle bundle = new Bundle();
        bundle.putString(PhoneCallService.WAIT, type);
        if (!TextUtils.isEmpty(phone)) {
            bundle.putString(PhoneCallService.PHONE_CALL, phone);
        }
        intent.putExtras(bundle);
        getApplication().startService(intent);
    }

    private void rejectPhone() {
        Intent intent = new Intent(getApplication(), PhoneCallService.class);
        intent.setAction(PhoneCallService.ACTION);
        Bundle bundle = new Bundle();
        bundle.putString(PhoneCallService.EXTRAS, PhoneCallService.REJECT);
        intent.putExtras(bundle);
        getApplication().startService(intent);
    }

    /**
     * 接电话
     */
    private void answerCall() {
        statusMap.put(PhoneCallService.ANSWER, 0);
        ShellUtils.execCommand(new String[]{"input swipe 370 1100 370 900"}, true);
    }

    /**
     * 挂电话
     */
    private void rejectCall() {
        ShellUtils.execCommand(new String[]{"input swipe 170 1100 170 900"}, true);
    }

    private void endCall() {
        ShellUtils.execCommand(new String[]{"input tap 360 1150"}, true);
    }


    private void mergeCall() {
        AccessibilityNodeInfo nodeInfo = this.getRootInActiveWindow();
        AccessibilityNodeInfo merge = findNodeInfoById(nodeInfo,
                "com.android.incallui:id/mergeButton");
        if (merge != null) {
            ShellUtils.execCommand(new String[]{"input tap 350 920"}, true);
        }
    }

    private void silentCall() {
        AccessibilityNodeInfo nodeInfo = this.getRootInActiveWindow();
        AccessibilityNodeInfo muteButton = findNodeInfoById(nodeInfo,
                "com.android.incallui:id/muteButton");
        if (muteButton != null) {
            ShellUtils.execCommand(new String[]{"input tap 140 720"}, true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    //执行返回
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void performBack() {
        Log.i(TAG, "执行返回");
        this.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
    }

    //执行点击
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void performClick() {
        Log.i(TAG, "点击执行");
        AccessibilityNodeInfo nodeInfo = this.getRootInActiveWindow();
        AccessibilityNodeInfo targetNode = null;
        //通过id获取
        targetNode = findNodeInfoById(nodeInfo, "com.android.incallui:id/glow_pad_view");
        if (targetNode.isClickable()) {
            targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
    }

    //执行点击
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void performClick(String resourceId) {
        Log.i(TAG, "点击执行");
        AccessibilityNodeInfo nodeInfo = this.getRootInActiveWindow();
        AccessibilityNodeInfo info = findNodeInfoById(nodeInfo, "com.android.phone:id/" + resourceId);
        if (info != null && info.isClickable()) {
            info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
    }  //执行点击

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void performScroll(String resourceId) {
        Log.i(TAG, "点击执行");
        AccessibilityNodeInfo nodeInfo = this.getRootInActiveWindow();
        AccessibilityNodeInfo info = findNodeInfoById(nodeInfo, "com.android.phone:id/" + resourceId);
        if (info != null && info.isClickable()) {
            info.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
        }
    }


    //通过id查找
    public static AccessibilityNodeInfo findNodeInfoById(AccessibilityNodeInfo nodeInfo, String resId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (nodeInfo == null) return null;

            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId(resId);
            if (list != null && !list.isEmpty()) {
                return list.get(0);
            }
        }
        return null;
    }

    //通过文本查找
    public static AccessibilityNodeInfo findNodeInfoByText(AccessibilityNodeInfo nodeInfo, String text) {
        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText(text);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

}
