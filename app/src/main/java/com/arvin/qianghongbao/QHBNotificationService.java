package com.arvin.qianghongbao;

import android.annotation.TargetApi;
import android.app.Notification;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by arvin on 2017/1/19.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class QHBNotificationService extends NotificationListenerService {
    private static QHBNotificationService service;
    private static final String TAG = "QHBNotificationService";
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onNotificationPosted(final StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        if(BuildConfig.DEBUG) {
            Log.i(TAG, "onNotificationRemoved");
        }
        QiangHongBaoService.handeNotificationPosted(new IStatusBarNotification() {
            @Override
            public String getPackageName() {
                return sbn.getPackageName();
            }

            @Override
            public Notification getNotification() {
                return sbn.getNotification();
            }
        });
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        service = this;
        Toast.makeText(this, "已开启监听状态栏服务，不错过每一个红包", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    public static boolean isRunning() {
        if (service == null) return false;
        return true;
    }
}
