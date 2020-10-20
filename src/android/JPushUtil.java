package cn.jiguang.cordova.push;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;

import com.chinamobile.gdwy.R;

import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import cn.jpush.android.api.BasicPushNotificationBuilder;
import cn.jpush.android.api.JPushInterface;
import cn.jpush.android.data.JPushLocalNotification;

/**
 * Created by liangzhongtai on 2018/11/22.
 */

public class JPushUtil {
    //检查app是否在前台
    public static  boolean checkTaskIsTop(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = manager.getRunningTasks(100);
        int i=0;
        String packageName = context.getApplicationInfo().packageName;
        for (ActivityManager.RunningTaskInfo info : list) {
            if (info.topActivity.getPackageName().equals(packageName)
                    || info.baseActivity.getPackageName().equals(packageName)) {
                if (i==0) {
                    return true;
                }

            }
            i++;
        }
        return false;
    }

    //检查设置
    public static void checkSetting(Context context,Map<String, Object> extras){
        //判断是否在前台

        //是否为紧急通知
        boolean urgency = false;
        Log.d("JPush",extras.toString());
        String    id   = (String) extras.get("id");
        String title   = (String) extras.get("title");
        String content = (String)extras.get("content");
        if(title=="紧急通知"){
            urgency = true;
        }

        //发送本地通知
        boolean usePush   = JPushSP.getSPBoolean(context,JPushSP.USE_PUSH);
        boolean sound     = JPushSP.getSPBoolean(context,JPushSP.SOUND);
        boolean shake     = JPushSP.getSPBoolean(context,JPushSP.SHAKE);
        int     sWeek     = JPushSP.getSPInt(context,JPushSP.S_SHOWWEEK,1);
        int     eWeek     = JPushSP.getSPInt(context,JPushSP.E_SHOWWEEK,7);
        int     sTime     = JPushSP.getSPInt(context,JPushSP.S_SHOWTIME,8);
        int     eTime     = JPushSP.getSPInt(context,JPushSP.E_SHOWTIME,23);
        int     frequency = JPushSP.getSPInt(context,JPushSP.FREQUENCY,0);
        long  lastNoTime  = JPushSP.getSPLong(context,JPushSP.LAST_NOT_TIME);
        long  nowTime     = System.currentTimeMillis();
        String nowDate    = JPushUtil.formatDate(nowTime,"yyyy-MM-dd HH:mm:ss");
        int   nowWeek     = JPushUtil.formatWeek(nowDate,"yyyy-MM-dd HH:mm:ss");
        long delayTime    = 0;
        if(urgency){
        }else {
            if(usePush) {
                return;
            }
            if(nowTime-lastNoTime<(frequency*24*60*60*1000)) {
                return;
            }
            if(nowWeek<sWeek&&nowWeek>eWeek) {
                return;
            }
        }

        String[] hmDates = nowDate.split(" ")[1].split(":");
        int hh = hmDates[0].startsWith("0")?Integer.valueOf(hmDates[0].substring(1,2)):Integer.valueOf(hmDates[0]);
        int mm = hmDates[1].startsWith("0")?Integer.valueOf(hmDates[1].substring(1,2)):Integer.valueOf(hmDates[1]);
        int ss = hmDates[2].startsWith("0")?Integer.valueOf(hmDates[2].substring(1,2)):Integer.valueOf(hmDates[2]);
        if(hh<sTime){
            delayTime = (sTime-hh)*1000*60*60-mm*1000*60-ss*1000;
        }else if(hh>=eTime){
            delayTime = (24-hh+eTime)*1000*60*60-mm*1000*60-ss*1000;
        }
        setSoundAndShake(context,sound,shake);
        JPushUtil.sendNoti(title,content,Integer.valueOf(id),extras,context,delayTime);
        JPushSP.setSP(context,JPushSP.LAST_NOT_TIME,nowTime);
    }

    //推送通知
    public static void sendNoti(String title, String content, int id, Map<String, Object> extras
            , Context context,long delayTime){
        JPushLocalNotification ln = new JPushLocalNotification();
        ln.setBuilderId(0);
        ln.setTitle(title);
        ln.setContent(content);
        ln.setNotificationId(id);
        ln.setBroadcastTime(System.currentTimeMillis() + delayTime);

        JSONObject json = new JSONObject(extras) ;
        ln.setExtras(json.toString()) ;
        JPushInterface.addLocalNotification(context.getApplicationContext(), ln);
    }

    //日期转成星期
    public static int formatWeek(String date,String format) {
        int week;
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        Date dates = null;
        try {
            dates = dateFormat.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Log.d("dates", (dates == null) + "");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dates);
        week = calendar.get(Calendar.DAY_OF_WEEK);
        return week;
    }

    //时间戳转成日期
    public static String formatDate(long timeMillis, String format) {
        Date date = null;
        if (timeMillis==0) {
            try {
                date = new Date(timeMillis);
            } catch (NumberFormatException e) {
                date = new Date();
            }
        } else {
            date = new Date();
        }

        final SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(date);
    }


    //设置声音和震动
    public static void setSoundAndShake(Context context,boolean sound,boolean shake){
        if(sound&&shake){
            setNotification1(context);
        }else if(sound){
            setNotification2(context);
        }else if(shake){
            setNotification3(context);
        }else {
            setNotification4(context);
        }
    }

    //自定义报警通知（震动铃声都要）
    public static void setNotification1(Context context){

        BasicPushNotificationBuilder builder = new BasicPushNotificationBuilder(context);

        //builder.statusBarDrawable = R.drawable.logo;//消息栏显示的图标

        builder.notificationFlags = Notification.FLAG_AUTO_CANCEL;  //设置为自动消失

        builder.notificationDefaults = Notification.DEFAULT_SOUND| Notification.DEFAULT_VIBRATE|Notification.DEFAULT_LIGHTS;// 设置为铃声与震动都要

        JPushInterface.setDefaultPushNotificationBuilder(builder);

    }

    //自定义报警通知（铃声）
    public static void setNotification2(Context context){

        BasicPushNotificationBuilder builder = new BasicPushNotificationBuilder(context);

        //builder.statusBarDrawable = R.drawable.logo;//消息栏显示的图标</span>

        builder.notificationFlags = Notification.FLAG_AUTO_CANCEL;  //设置为自动消失

        builder.notificationDefaults = Notification.DEFAULT_SOUND|Notification.DEFAULT_LIGHTS;// 设置为铃声与震动都要

        JPushInterface.setDefaultPushNotificationBuilder(builder);

    }

    //自定义报警通知（震动）
    public static void setNotification3(Context context){

        BasicPushNotificationBuilder builder = new BasicPushNotificationBuilder(context);

        //builder.statusBarDrawable = R.drawable.logo;//消息栏显示的图标</span>

        builder.notificationFlags = Notification.FLAG_AUTO_CANCEL;  //设置为自动消失

        builder.notificationDefaults = Notification.DEFAULT_VIBRATE|Notification.DEFAULT_LIGHTS;// 震动

        JPushInterface.setDefaultPushNotificationBuilder(builder);

    }

    //自定义报警通知（震动铃声都不要）
    public static void setNotification4(Context context){

        BasicPushNotificationBuilder builder = new BasicPushNotificationBuilder(context);

        //builder.statusBarDrawable = R.drawable.logo_app_168;

        builder.notificationFlags = Notification.FLAG_AUTO_CANCEL;  //设置为自动消失

        builder.notificationDefaults = Notification.DEFAULT_LIGHTS;// 设置为铃声与震动都不要

        JPushInterface.setDefaultPushNotificationBuilder(builder);

    }

    //检查是否已打开通知权限
    public static boolean isPermissionOpen(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return NotificationManagerCompat.from(context).getImportance() != NotificationManager.IMPORTANCE_NONE;
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    //打开通知权限配置页面
    public static void openPermissionSetting(Context context) {
        try {
            Intent localIntent = new Intent();
            localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            //直接跳转到应用通知设置的代码：
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                localIntent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                localIntent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
                context.startActivity(localIntent);
                return;
            }
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                localIntent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                localIntent.putExtra("app_package", context.getPackageName());
                localIntent.putExtra("app_uid", context.getApplicationInfo().uid);
                context.startActivity(localIntent);
                return;
            }
            if (android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                localIntent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                localIntent.addCategory(Intent.CATEGORY_DEFAULT);
                localIntent.setData(Uri.parse("package:" + context.getPackageName()));
                context.startActivity(localIntent);
                return;
            }

            //4.4以下没有从app跳转到应用通知设置页面的Action，可考虑跳转到应用详情页面,

            if (Build.VERSION.SDK_INT >= 9) {
                localIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                localIntent.setData(Uri.fromParts("package", context.getPackageName(), null));
                context.startActivity(localIntent);
                return;
            }

            localIntent.setAction(Intent.ACTION_VIEW);
            localIntent.setClassName("com.android.settings", "com.android.setting.InstalledAppDetails");
            localIntent.putExtra("com.android.settings.ApplicationPkgName", context.getPackageName());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(" cxx   pushPermission 有问题");
        }
    }


    /**
     * 获取自启动管理页面的Intent
     * @param context context
     * @return 返回自启动管理页面的Intent
     * */
    public static Intent getAutostartSettingIntent(Context context) {
        ComponentName componentName = null;
        String brand = Build.MANUFACTURER;
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        switch (brand.toLowerCase()) {
            //三星
            case "samsung":
                componentName = new ComponentName("com.samsung.android.sm", "com.samsung.android.sm.app.dashboard.SmartManagerDashBoardActivity");
                break;
            //华为
            case "huawei":
            //荣耀V8，EMUI 8.0.0，Android 8.0上，以下两者效果一样
                componentName = new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity");
            //  componentName = new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity");//目前看是通用的
                break;
            //小米
            case "xiaomi":
                componentName = new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity");
                break;
            //VIVO
            case "vivo":
            //  componentName = new ComponentName("com.iqoo.secure", "com.iqoo.secure.safaguard.PurviewTabActivity");
                componentName = new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity");
                break;
            //OPPO
            case "oppo":
            //  componentName = new ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity");
                componentName = new ComponentName("com.coloros.oppoguardelf", "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity");
                break;
            //360
            case "yulong":
            case "360":
                componentName = new ComponentName("com.yulong.android.coolsafe", "com.yulong.android.coolsafe.ui.activity.autorun.AutoRunListActivity");
                break;
            //魅族
            case "meizu":
                componentName = new ComponentName("com.meizu.safe", "com.meizu.safe.permission.SmartBGActivity");
                break;
            //一加
            case "oneplus":
                componentName = new ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity");
                break;
            //乐视
            case "letv":
                intent.setAction("com.letv.android.permissionautoboot");
            default://其他
                intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                intent.setData(Uri.fromParts("package", context.getPackageName(), null));
                break;
        }
        intent.setComponent(componentName);
        return intent;
    }

    //获取手机类型
    private static String getMobileType() {
        return Build.MANUFACTURER.toLowerCase();

    }



    //跳转至授权页面

    public static void openStartInterface(Context context) {
        Intent intent = new Intent();
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Log.d("JPush", "******************当前手机型号为：" + getMobileType());
            ComponentName componentName = null;
                // 红米Note4测试通过
            if (getMobileType().equals("xiaomi")) {
                componentName = new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity");
                // 乐视2测试通过
            } else if (getMobileType().equals("letv")) {
                intent.setAction("com.letv.android.permissionautoboot");
                // 三星Note5测试通过
            } else if (getMobileType().equals("samsung")) {
                //componentName = new ComponentName("com.samsung.android.sm_cn", "com.samsung.android.sm.ui.ram.AutoRunActivity");
                //componentName = ComponentName.unflattenFromString("com.samsung.android.sm/.ui.ram.RamActivity");// Permission Denial not exported from uid 1000，不允许被其他程序调用
                componentName = ComponentName.unflattenFromString("com.samsung.android.sm/.app.dashboard.SmartManagerDashBoardActivity");
                // 华为测试通过
            } else if (getMobileType().equals("huawei")) {
                //componentName = new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity");//锁屏清理
                //跳自启动管理
                componentName = ComponentName.unflattenFromString("com.huawei.systemmanager/.startupmgr.ui.StartupNormalAppListActivity");
                //SettingOverlayView.show(context);
                // VIVO测试通过
            } else if (getMobileType().equals("vivo")) {
                componentName = ComponentName.unflattenFromString("com.iqoo.secure/.safeguard.PurviewTabActivity");
                //魅族
            } else if (getMobileType().equals("meizu")) {
                //跳转到手机管家
                //componentName = ComponentName.unflattenFromString("com.meizu.safe/.permission.PermissionMainActivity");
                //跳转到后台管理页面
                componentName = ComponentName.unflattenFromString("com.meizu.safe/.permission.SmartBGActivity");
                // OPPO R8205测试通过
            } else if (getMobileType().equals("oppo")) {
                componentName = ComponentName.unflattenFromString("com.oppo.safe/.permission.startup.StartupAppListActivity");
                // 360手机 未测试
            } else if (getMobileType().equals("ulong")) {
                componentName = new ComponentName("com.yulong.android.coolsafe", ".ui.activity.autorun.AutoRunListActivity");
            } else {
                // 将用户引导到系统设置页面
                if (Build.VERSION.SDK_INT >= 9) {
                    Log.d("JPush", "APPLICATION_DETAILS_SETTINGS");
                    intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                    intent.setData(Uri.fromParts("package", context.getPackageName(), null));
                } else if (Build.VERSION.SDK_INT <= 8) {
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
                    intent.putExtra("com.android.settings.ApplicationPkgName", context.getPackageName());
                }

            }
            intent.setComponent(componentName);
            context.startActivity(intent);
//            if (getMobileType().equals("Xiaomi")) {
//                showtip();//显示弹窗（**特别注意**）
//            }
//            if (getMobileType().equals("samsung")) {
//                //显示悬浮窗
//                new SettingOverlayView().show(context);
//            }
        } catch (Exception e) {
            // 抛出异常就直接打开设置页面
            Log.d("JPush", e.getLocalizedMessage());
            intent = new Intent(Settings.ACTION_SETTINGS);
            context.startActivity(intent);
        }
    }

}
