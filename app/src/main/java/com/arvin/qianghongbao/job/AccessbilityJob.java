package com.arvin.qianghongbao.job;

import android.view.accessibility.AccessibilityEvent;

import com.arvin.qianghongbao.IStatusBarNotification;
import com.arvin.qianghongbao.QiangHongBaoService;

/**
 * Created by arvin on 2017/1/19.
 */

public interface AccessbilityJob {
    String getTargetPackageName();
    void onCreateJob(QiangHongBaoService service);
    void onReceiveJob(AccessibilityEvent event);
    void onStopJob();
    void onNotificationPosted(IStatusBarNotification service);
    boolean isEnable();
}
