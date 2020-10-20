package cn.jiguang.cordova.push;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;



import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.jpush.android.api.JPushInterface;
import cn.jpush.android.service.PushService;

public class JPushReceiver extends BroadcastReceiver {

    private static final List<String> IGNORED_EXTRAS_KEYS = Arrays.asList("cn.jpush.android.TITLE",
            "cn.jpush.android.MESSAGE", "cn.jpush.android.APPKEY", "cn.jpush.android.NOTIFICATION_CONTENT_TITLE");

    @Override
    public void onReceive(Context context, Intent intent) {
        //TODO
        //启动极光推送的服务
        Intent pushintent = new Intent(context, PushService.class);
        context.startService(pushintent);

        String action = intent.getAction();
        if (action.equals(JPushInterface.ACTION_REGISTRATION_ID)) {
            String rId = intent.getStringExtra(JPushInterface.EXTRA_REGISTRATION_ID);
            Log.d("JIGUANG", "JPush 用户注册成功rId=" + rId);
            JPushSP.setSP(context,JPushSP.JPUSH_ID,rId);
            JPushPlugin.transmitReceiveRegistrationId(rId);

        } else if (action.equals(JPushInterface.ACTION_MESSAGE_RECEIVED)) {
            Log.d("JPush", "JPush 接受到推送下来的自定义消息");
            //封装自定义消息推送数据
//            intent.putExtra("id",new Random().nextInt(1000)+"");
//            intent.putExtra("title","站点勘察");
//            intent.putExtra("count",12+"");
//            intent.putExtra("content","您有新的站点勘测任务了,目前未处理工单条数:12");
//            intent.putExtra("platform","android");
            handlingMessageReceive(intent);

        } else if (action.equals(JPushInterface.ACTION_NOTIFICATION_RECEIVED)) {
            Log.d("JIGUANG", "JPush 接受到推送下来的通知");
            handlingNotificationReceive(context, intent);

        } else if (action.equals(JPushInterface.ACTION_NOTIFICATION_OPENED)) {
            Log.d("JIGUANG", "JPush 用户点击打开了通知");
            handlingNotificationOpen(context, intent);

        }
    }

    private void handlingMessageReceive(Intent intent) {
        String msg = intent.getStringExtra(JPushInterface.EXTRA_MESSAGE);
        Map<String, Object> extras = getNotificationExtras(intent);
        JPushPlugin.transmitMessageReceive(msg, extras);
    }

    private void handlingNotificationOpen(Context context, Intent intent) {
        String title = intent.getStringExtra(JPushInterface.EXTRA_NOTIFICATION_TITLE);
        JPushPlugin.openNotificationTitle = title;

        String alert = intent.getStringExtra(JPushInterface.EXTRA_ALERT);
        JPushPlugin.openNotificationAlert = alert;

        Map<String, Object> extras = getNotificationExtras(intent);
        JPushPlugin.openNotificationExtras = extras;

        JPushPlugin.transmitNotificationOpen(title, alert, extras);

        Intent launch = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (launch != null) {
            launch.addCategory(Intent.CATEGORY_LAUNCHER);
            launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(launch);
        }
    }

    private void handlingNotificationReceive(Context context, Intent intent) {
        String title = intent.getStringExtra(JPushInterface.EXTRA_NOTIFICATION_TITLE);
        JPushPlugin.notificationTitle = title;
        Log.d("JIGUANG", "JPush 接受到推送下来的通知title=" + title);

        String alert = intent.getStringExtra(JPushInterface.EXTRA_ALERT);
        JPushPlugin.notificationAlert = alert;

        Map<String, Object> extras = getNotificationExtras(intent);
        JPushPlugin.notificationExtras = extras;

        JPushPlugin.transmitNotificationReceive(title, alert, extras);
    }

    private Map<String, Object> getNotificationExtras(Intent intent) {
        Map<String, Object> extrasMap = new HashMap<String, Object>();
        for (String key : intent.getExtras().keySet()) {
            if (!IGNORED_EXTRAS_KEYS.contains(key)) {
                if (key.equals(JPushInterface.EXTRA_NOTIFICATION_ID)) {
                    extrasMap.put(key, intent.getIntExtra(key, 0));
                } else {
                    extrasMap.put(key, intent.getStringExtra(key));
                }
            }
        }
        return extrasMap;
    }
}
