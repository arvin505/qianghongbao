package com.arvin.qianghongbao;

import android.app.Notification;

/**
 * Created by arvin on 2017/1/19.
 */
public interface IStatusBarNotification {
    String getPackageName();
    Notification getNotification();
}
