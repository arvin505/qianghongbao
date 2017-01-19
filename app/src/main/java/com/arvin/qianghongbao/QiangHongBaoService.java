package com.arvin.qianghongbao;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.arvin.qianghongbao.job.AccessbilityJob;
import com.arvin.qianghongbao.job.WechatAccessbilityJob;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created by arvin on 2017/1/19.
 */

public class QiangHongBaoService extends AccessibilityService {
    private final static String TAG = "QiangHongBaoService";

    private static QiangHongBaoService service;
    private List<AccessbilityJob> mAccessbilityJobs;
    private HashMap<String, AccessbilityJob> mPkgAccessbilityJobMap;
    private static final Class[] ACCESSBILITY_JOBS = {
            WechatAccessbilityJob.class,
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mAccessbilityJobs = new ArrayList<>();
        mPkgAccessbilityJobMap = new HashMap<>();
        for (Class clazz : ACCESSBILITY_JOBS) {
            try {
                Object object = clazz.newInstance();
                if (object instanceof AccessbilityJob) {
                    AccessbilityJob job = (AccessbilityJob) object;
                    job.onCreateJob(this);
                    mAccessbilityJobs.add(job);
                    mPkgAccessbilityJobMap.put(job.getTargetPackageName(), job);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "qianghongbao service destory");
        if (mPkgAccessbilityJobMap != null) {
            mPkgAccessbilityJobMap.clear();
        }
        if (mAccessbilityJobs != null && !mAccessbilityJobs.isEmpty()) {
            for (AccessbilityJob job : mAccessbilityJobs) {
                job.onStopJob();
            }
            mAccessbilityJobs.clear();
        }

        service = null;
        mAccessbilityJobs = null;
        mPkgAccessbilityJobMap = null;
        //发送广播，已经断开辅助服务
        /*Intent intent = new Intent(Config.ACTION_QIANGHONGBAO_SERVICE_DISCONNECT);
        sendBroadcast(intent);*/
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        service = this;
        Toast.makeText(this, "已打开抢红包服务", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "事件--->" + event);
        }
        String pkn = String.valueOf(event.getPackageName());
        for (AccessbilityJob job : mAccessbilityJobs) {
            if (pkn.equals(job.getTargetPackageName()) && job.isEnable()) {
                job.onReceiveJob(event);
            }
        }

    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "qianghongbao service interrupt");
        Toast.makeText(this, "中断抢红包服务", Toast.LENGTH_SHORT).show();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static boolean isRunning() {
        if (service == null) return false;
        AccessibilityManager accessibilityManager = (AccessibilityManager) service.getSystemService(ACCESSIBILITY_SERVICE);
        AccessibilityServiceInfo info = service.getServiceInfo();
        if (info == null) return false;
        List<AccessibilityServiceInfo> list = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
        Iterator<AccessibilityServiceInfo> iterator = list.iterator();
        while (iterator.hasNext()) {
            AccessibilityServiceInfo i = iterator.next();
            if (i.getId().equals(info.getId())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNotificationServiceRunning() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return false;
        }
        return QHBNotificationService.isRunning();
    }


    public static void handeNotificationPosted(IStatusBarNotification notificationService) {
        if (notificationService == null) {
            return;
        }
        if (service == null || service.mPkgAccessbilityJobMap == null) {
            return;
        }
        String pack = notificationService.getPackageName();
        AccessbilityJob job = service.mPkgAccessbilityJobMap.get(pack);
        if (job == null) {
            return;
        }
        job.onNotificationPosted(notificationService);
    }
}
