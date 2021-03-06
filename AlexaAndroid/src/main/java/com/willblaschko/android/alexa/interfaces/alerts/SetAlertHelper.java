package com.willblaschko.android.alexa.interfaces.alerts;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.ggec.voice.toollibrary.log.Log;
import com.willblaschko.android.alexa.AlexaManager;
import com.willblaschko.android.alexa.callbacks.AsyncCallback;
import com.willblaschko.android.alexa.data.Event;
import com.willblaschko.android.alexa.datepersisted.MultiprocessSharedPreferences;
import com.willblaschko.android.alexa.interfaces.AvsResponse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by ggec on 2017/4/10.
 */

public class SetAlertHelper {
    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());

    public static void sendSetAlertSucceeded(@NonNull AlexaManager manager, String token, AsyncCallback<AvsResponse, Exception> callback) {
        manager.sendEvent(Event.getSetAlertSucceededEvent(token), callback);
    }

    public static void sendSetAlertFailed(@NonNull AlexaManager manager, String token, AsyncCallback<AvsResponse, Exception> callback) {
        manager.sendEvent(Event.getSetAlertFailedEvent(token), callback);
    }

    public static void sendDeleteAlertSucceeded(@NonNull AlexaManager manager, String token, AsyncCallback<AvsResponse, Exception> callback) {
        manager.sendEvent(Event.getDeleteAlertSucceededEvent(token), callback);
    }

    public static void sendDeleteAlertFail(@NonNull AlexaManager manager, String token, AsyncCallback<AvsResponse, Exception> callback) {
        manager.sendEvent(Event.getDeleteAlertFailedEvent(token), callback);
    }

    public static void sendAlertStarted(@NonNull AlexaManager manager, String token, AsyncCallback<AvsResponse, Exception> callback) {
        manager.sendEvent(Event.getAlertStartedEvent(token), callback);
    }

    public static void sendAlertStopped(@NonNull AlexaManager manager, String token, AsyncCallback<AvsResponse, Exception> callback) {
        manager.sendEvent(Event.getAlertStoppedEvent(token), callback);
    }

    public static void sendAlertEnteredForeground(@NonNull AlexaManager manager, String token, AsyncCallback<AvsResponse, Exception> callback) {
        manager.sendEvent(Event.getAlertEnteredForegroundEvent(token), callback);
    }

    public static void sendAlertEnteredBackground(@NonNull AlexaManager manager, String token, AsyncCallback<AvsResponse, Exception> callback) {
        manager.sendEvent(Event.getAlertEnteredBackgroundEvent(token), callback);
    }

//////////////////////////////////////////////////////////////////////////////

    private final static String PREF = "AlertManagerPref";

    public static void putAlert(Context context, AvsSetAlertItem setAlertItem) {
        SharedPreferences.Editor ed = MultiprocessSharedPreferences.getSharedPreferences(context, PREF, Context.MODE_PRIVATE).edit();
        ed.putString(setAlertItem.getToken(), setAlertItem.toJsonString());
        ed.apply();
    }

    public static void deleteAlertSP(Context context, String tokenID) {
        SharedPreferences.Editor ed = MultiprocessSharedPreferences.getSharedPreferences(context, PREF, Context.MODE_PRIVATE).edit();
        ed.remove(tokenID);
        ed.apply();
    }

    public static AvsSetAlertItem getAlertItemByToken(Context context, String token) {
        SharedPreferences sp = MultiprocessSharedPreferences.getSharedPreferences(context, PREF, Context.MODE_PRIVATE);
        String json = sp.getString(token, null);
        if (!TextUtils.isEmpty(json)) {
            return AvsSetAlertItem.create(json);
        }
        return null;
    }

    public static List<AvsSetAlertItem> getAllAlerts(Context context) {
        ArrayList<AvsSetAlertItem> list = new ArrayList<>();
        SharedPreferences sp = MultiprocessSharedPreferences.getSharedPreferences(context, PREF, Context.MODE_PRIVATE);
        for (Object entry : sp.getAll().values()) {
            if (entry instanceof String) {
                AvsSetAlertItem item = AvsSetAlertItem.create((String) entry);
                if (item != null)
                    list.add(item);//FIXME 这里应该先验证下这个item是否合法, 不合法要删除
            }
        }
        return list;
    }

    ///////////////////////////////////////////////////////

    public static boolean setAlert(Context context, AvsSetAlertItem setAlertItem, Intent it) {
        try {
            Date date = simpleDateFormat.parse(setAlertItem.getScheduledTime());
            Date local = new Date(System.currentTimeMillis());
            Log.d("SetAlertHelper", "setAlert date:" + date.getTime() + " off:" + local.getTime()
                    + " diff:" + (date.getTime() - System.currentTimeMillis()) + " \nsc:" + setAlertItem.getScheduledTime()+ " now:"+simpleDateFormat.format(local)
                + " timezoneOffset:"+date.getTimezoneOffset()+ " time:"+(local.getTime() - System.currentTimeMillis()));
            it.putExtra("token", setAlertItem.getToken());
            it.putExtra("messageId", setAlertItem.getMessageId());
            PendingIntent intent = PendingIntent.getService(context, setAlertItem.getTimeId(), it, 0);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, date.getTime(), intent);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean cancelOrDeleteAlert(Context context, String tokenID, Intent it) {
        AvsSetAlertItem setAlertItem = getAlertItemByToken(context, tokenID);
        if (setAlertItem == null) return false;

        it.putExtra("token", setAlertItem.getToken());
        it.putExtra("messageId", setAlertItem.getMessageId());

        deleteAlertSP(context, tokenID);
        PendingIntent intent = PendingIntent.getService(context, setAlertItem.getTimeId(), it, PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(intent);

        return true;
    }

    public static boolean isScheduledTimeAvailable(String scheduledTime, long compareTime) {
        try {
            Date date = simpleDateFormat.parse(scheduledTime);
            return date.getTime() > compareTime;
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }
}
