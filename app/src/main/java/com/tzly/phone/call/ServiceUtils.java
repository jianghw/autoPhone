package com.tzly.phone.call;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.view.accessibility.AccessibilityManager;

import java.util.Iterator;
import java.util.List;

public class ServiceUtils {
    /**
     * 某个服务是否还活着
     */
    public static boolean isServiceWork(Context context, Class<?> cls) {

        ActivityManager systemService =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (systemService != null) {
            List<ActivityManager.RunningServiceInfo> runningServices =
                    systemService.getRunningServices(10);

            if (runningServices.size() <= 0) {
                return false;
            }

            for (int i = 0; i < runningServices.size(); i++) {
                ActivityManager.RunningServiceInfo serviceInfo = runningServices.get(i);
                if (serviceInfo == null) return false;
                if (serviceInfo.service.getClass().getSimpleName().equals(cls.getSimpleName())) {
                    return true;
                }
            }
        } else {
            return false;
        }
        return false;
    }


    /**
     * 判断辅助服务是否正在运行
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static boolean isServiceRunning(Context context, Class<?> clz) {
        if (context == null) {
            return false;
        }
        AccessibilityManager accessibilityManager =
                (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (accessibilityManager == null) {
            return false;
        }
        List<AccessibilityServiceInfo> serviceList = accessibilityManager
                .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
        Iterator<AccessibilityServiceInfo> iterator = serviceList.iterator();
        while (iterator.hasNext()) {
            AccessibilityServiceInfo next = iterator.next();
            if (next.getClass().getName().equals(clz.getName())) {
                return true;
            }
        }
        return false;
    }
}
