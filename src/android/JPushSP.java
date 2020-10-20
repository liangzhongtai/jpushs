package cn.jiguang.cordova.push;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by liangzhongtai on 2018/11/22.
 */

public class JPushSP {
    //是否打开推送
    public static final String USE_PUSH      = "usePush";
    //是否打开声音
    public static final String SOUND         = "sound";
    //是否打开震动
    public static final String SHAKE         = "shake";
    //推送的起始星期
    public static final String S_SHOWWEEK    = "sWeek";
    //推送的截止星期
    public static final String E_SHOWWEEK    = "eWeek";
    //推送的开始时间
    public static final String S_SHOWTIME    = "sTime";
    //推送的结束时间
    public static final String E_SHOWTIME    = "eTime";
    //推送频率:0-~天/次
    public static final String FREQUENCY     = "frequency";
    //极光注册id
    public static final String JPUSH_ID      = "jpush_id";
    //上一次显示的通知时间
    public static final String LAST_NOT_TIME = "last_not_time";

    public static void setSP(Context context, String key, Object value) {
        SharedPreferences preferences=context.getSharedPreferences("jpush", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor=preferences.edit();
        if(value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        }else if(value instanceof String){
            editor.putString(key, (String) value);
        }else if(value instanceof Boolean){
            editor.putBoolean(key, (Boolean) value);
        }else if(value instanceof Long){
            editor.putLong(key, (Long) value);
        }else if(value instanceof Float || value instanceof Double){
            editor.putFloat(key, (Float) value);
        }
        editor.commit();
    }

    public static void removeSP(Context context, String key) {
        SharedPreferences preferences=context.getSharedPreferences("jpush", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor=preferences.edit();
        editor.remove(key);
        editor.commit();
    }

    public static int getSPInt(Context context,String key,int defValue) {
        SharedPreferences preferences=context.getSharedPreferences("jpush", Context.MODE_PRIVATE);
        return preferences.getInt(key,defValue);
    }

    public static boolean getSPBoolean(Context context,String key) {
        SharedPreferences preferences=context.getSharedPreferences("jpush", Context.MODE_PRIVATE);
        return preferences.getBoolean(key,false);
    }

    public static String getSPString(Context context,String key) {
        SharedPreferences preferences=context.getSharedPreferences("jpush", Context.MODE_PRIVATE);
        return preferences.getString(key,"");
    }

    public static long getSPLong(Context context,String key) {
        SharedPreferences preferences=context.getSharedPreferences("jpush", Context.MODE_PRIVATE);
        return preferences.getLong(key,0);
    }

    public static float getSPFloat(Context context,String key) {
        SharedPreferences preferences=context.getSharedPreferences("jpush", Context.MODE_PRIVATE);
        return preferences.getFloat(key,0);
    }

}
