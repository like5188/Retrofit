package com.like.retrofit.utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * 网络相关的工具类
 */
public class NetWorkUtil {
    private NetWorkUtil() {
        // 不允许直接构造此类，也不允许反射构造此类
        throw new UnsupportedOperationException("cannot be instantiated");
    }

    /**
     * 判断当前连接是否是wifi连接
     *
     * @param context
     * @return
     */
    public static boolean isWifi(Context context) {
        return getNetType(context) == ConnectivityManager.TYPE_WIFI;
    }

    /**
     * 判断网络是否连接.
     * 需要权限<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" /><br/>
     *
     * @param context
     * @return - true 网络连接 - false 网络连接异常
     */
    public static boolean isConnected(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo info = cm.getActiveNetworkInfo();
                if (info != null && info.isAvailable() && info.getState() == NetworkInfo.State.CONNECTED) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 返回当前网络类型连接，如果没有网络，则返回-1
     *
     * @param context
     * @return int -返回网络类型 特殊:-1代表没有网络
     */
    public static int getNetType(Context context) {
        Context applicationContext = context.getApplicationContext();
        if (isConnected(applicationContext)) {
            ConnectivityManager connMgr = (ConnectivityManager) applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connMgr != null) {
                NetworkInfo info = connMgr.getActiveNetworkInfo();
                return info.getType();
            }
        }
        return -1;
    }

    /**
     * 打开网络设置界面
     *
     * @param activity
     */
    public static void openNetworkSettingUi(Activity activity) {
        Intent intent = new Intent("/");
        ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.WirelessSettings");
        intent.setComponent(cn);
        intent.setAction("android.intent.action.VIEW");
        activity.startActivityForResult(intent, 0);
    }

}
