package com.example.administrator.appled;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.ggec.voice.assistservice.MyApplication;

/**
 * Created by ggec on 2017/6/30.
 */

public class LedControl {
    static {
        System.loadLibrary("ledControl");
    }

    public static native int ledCtlInterface(int stauts);

    public static native int add(int a, int b);

//    public static native void myLedCtl(int state);

    public static native void getInt();



    public static final int ON_WAKE_WORD_DETECT = 1;
    public static final int LISTENING = 2;
    public static final int THINKING = 3;
    public static final int IDLE = 4;
    public static final int SPEAK_AND_PLAY = 5;
    public static final int ERROR = 6;

    public static void myLedCtl(int state){
        Intent it = new Intent(com.willblaschko.android.alexa.BroadCast.RECEIVE_LED_STATE_CHANGE);
        it.putExtra("state", state);
        LocalBroadcastManager.getInstance(MyApplication.getContext()).sendBroadcast(it);
    }
}
