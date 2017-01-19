package com.arvin.qianghongbao.job;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.arvin.qianghongbao.BuildConfig;
import com.arvin.qianghongbao.IStatusBarNotification;
import com.arvin.qianghongbao.QiangHongBaoService;
import com.arvin.qianghongbao.util.AccessibilityHelper;
import com.arvin.qianghongbao.util.NotifyHelper;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by arvin on 2017/1/19.
 */

public class WechatAccessbilityJob extends BaseAccessbilityJob {
    private static final String TAG = "WechatAccessbilityJob";
    /**
     * 微信的包名
     */
    public static final String WECHAT_PACKAGENAME = "com.tencent.mm";
    private static final String WECHAT_HONGBAO_UI = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI";
    private static final String WECHAT_LAUNCHERUI = "com.tencent.mm.ui.LauncherUI";
    private static final String WECHAT_HONGBAO_DETAIL = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI";

    private static final String KAI_BUTTON_ID = "com.tencent.mm:id/be_";
    //nodeInfo.findFocus(1).findAccessibilityNodeInfosByViewId("com.tencent.mm:id/adu")
    private static final String LIST_ITEM_ID = "com.tencent.mm:id/adu";
    private static final long DELAY_MILLS = 300;
    /**
     * 红包消息的关键字
     */
    private static final String HONGBAO_TEXT_KEY = "[微信红包]";

    private static final String BUTTON_CLASS_NAME = "android.widget.Button";
    /**
     * 不能再使用文字匹配的最小版本号
     */
    private static final int USE_ID_MIN_VERSION = 700;// 6.3.8 对应code为680,6.3.9对应code为700

    private static final int WINDOW_NONE = 0;
    private static final int WINDOW_LUCKYMONEY_RECEIVEUI = 1;
    private static final int WINDOW_LUCKYMONEY_DETAIL = 2;
    private static final int WINDOW_LAUNCHER = 3;
    private static final int WINDOW_OTHER = -1;

    private int mCurrentWindow = WINDOW_NONE;
    private boolean isReceivingHongbao;
    private List<Integer> hashCodeList = new ArrayList<>();

    @Override
    public String getTargetPackageName() {
        return WECHAT_PACKAGENAME;
    }

    @Override
    public void onReceiveJob(AccessibilityEvent event) {
        final int eventType = event.getEventType();
        switch (eventType) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                Parcelable data = event.getParcelableData();
                if (data == null || !(data instanceof Notification)) return;
                if (QiangHongBaoService.isNotificationServiceRunning()) return;
                List<CharSequence> texts = event.getText();
                if (!texts.isEmpty()) {
                    String text = String.valueOf(texts.get(0));
                    notificationEvent(text, (Notification) data);
                }
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                openHongBao(event);
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                if (mCurrentWindow != WINDOW_LAUNCHER) { //不在聊天界面或聊天列表，不处理
                    //  return;
                }
                // if (isReceivingHongbao) {
                handleChatListHongBao();
                //}
                break;
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void handleChatListHongBao() {
        AccessibilityNodeInfo nodeInfo = getService().getRootInActiveWindow();
        if (nodeInfo == null) {
            Log.w(TAG, "rootWindow为空");
            return;
        }
        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("领取红包");
        if (list != null && list.isEmpty()) {
            // 从消息列表查找红包
            AccessibilityNodeInfo node = AccessibilityHelper.findNodeInfosByText(nodeInfo, "[微信红包]");
            if (node != null) {
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "-->微信红包:" + node);
                }
                isReceivingHongbao = true;
                AccessibilityHelper.performClick(nodeInfo);
            } else {
                AccessibilityNodeInfo focus = nodeInfo.findFocus(1);
                if (focus!=null) {
                    List<AccessibilityNodeInfo> chatList = focus.findAccessibilityNodeInfosByViewId(LIST_ITEM_ID);
                    if (chatList != null && !chatList.isEmpty()) {
                        for (AccessibilityNodeInfo info : chatList) {
                            if (String.valueOf(info.getText()).contains(HONGBAO_TEXT_KEY)) {
                                AccessibilityHelper.performClick(info);
                            }
                        }
                    }
                }
            }
        } else if (list != null) {
            handleHongbao(list);
        }
    }

    private void handleHongbao(List<AccessibilityNodeInfo> nodes) {
        int index = nodes.size() - 1;
        AccessibilityNodeInfo newNode = null;
        for (int i = index; i >= 0; i--) {
            AccessibilityNodeInfo node = nodes.get(i);
            if (hashCodeList.contains(node.hashCode()))
                continue;
            else {
                newNode = node;
                hashCodeList.add(newNode.hashCode());
                break;
            }
        }
        if (newNode != null) {
            AccessibilityHelper.performClick(newNode);
        }

    }

    private void notificationEvent(String ticker, Notification no) {
        String text = ticker;
        int index = text.indexOf(":");
        if (index != -1) {
            text = text.substring(index + 1);
        }
        text = text.trim();
        if (text.contains(HONGBAO_TEXT_KEY)) { //红包消息
            newHongBaoNotification(no);
        }
    }

    /**
     * 打开通知栏消息
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void newHongBaoNotification(Notification notification) {
        isReceivingHongbao = true;
        //以下是精华，将微信的通知栏消息打开
        PendingIntent pendingIntent = notification.contentIntent;
        boolean lock = NotifyHelper.isLockScreen(getContext());

        if (!lock) {
            NotifyHelper.send(pendingIntent);
        } else {
            NotifyHelper.showNotify(getContext(), String.valueOf(notification.tickerText), pendingIntent);
        }
        NotifyHelper.playEffect(getContext());
    }


    private void openHongBao(AccessibilityEvent event) {
        switch (String.valueOf(event.getClassName())) {
            case WECHAT_HONGBAO_UI:
                mCurrentWindow = WINDOW_LUCKYMONEY_RECEIVEUI;
                //点中了红包，下一步就是去拆红包
                handleLuckyMoneyReceive();
                break;
            case WECHAT_LAUNCHERUI:
                mCurrentWindow = WINDOW_LAUNCHER;
                //在聊天界面,去点中红包
                handleChatListHongBao();
                break;
            case WECHAT_HONGBAO_DETAIL:
                mCurrentWindow = WINDOW_LUCKYMONEY_DETAIL;
                AccessibilityHelper.performBack(getService());
            default:
                mCurrentWindow = WINDOW_OTHER;
                break;
        }
    }

    /**
     * 拆红包
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void handleLuckyMoneyReceive() {
        AccessibilityNodeInfo root = getService().getRootInActiveWindow();
        if (root == null) {
            Log.w(TAG, "rootWindow为空");
            return;
        }
        AccessibilityNodeInfo targetNode = null;
        targetNode = AccessibilityHelper.findNodeInfosById(root, KAI_BUTTON_ID);
        if (targetNode == null) {
            //分别对应固定金额的红包 拼手气红包
            AccessibilityNodeInfo textNode = AccessibilityHelper.findNodeInfosByTexts(root, "发了一个红包", "给你发了一个红包", "发了一个红包，金额随机");
            if (textNode != null) {
                for (int i = 0; i < textNode.getChildCount(); i++) {
                    AccessibilityNodeInfo node = textNode.getChild(i);
                    if (BUTTON_CLASS_NAME.equals(node.getClassName())) {
                        targetNode = node;
                        break;
                    }
                }
            }
        }
        if (targetNode == null) { //通过组件查找
            targetNode = AccessibilityHelper.findNodeInfosByClassName(root, BUTTON_CLASS_NAME);
        }

        if (targetNode != null) {
            final AccessibilityNodeInfo finalTargetNode = targetNode;
            getHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    AccessibilityHelper.performClick(finalTargetNode);
                }
            }, DELAY_MILLS);
        }
    }

    private Handler mHandler = null;

    private Handler getHandler() {
        if (mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper());
        }
        return mHandler;
    }

    @Override
    public void onStopJob() {

    }

    @Override
    public void onNotificationPosted(IStatusBarNotification service) {
        Notification nf = service.getNotification();
        String text = String.valueOf(service.getNotification().tickerText);
        notificationEvent(text, nf);
    }

    @Override
    public boolean isEnable() {
        return true;
    }
}
