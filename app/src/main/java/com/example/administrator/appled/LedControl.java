package com.example.administrator.appled;

/**
 * Created by ggec on 2017/6/30.
 */

public class LedControl {
    static {
        System.loadLibrary("ledControl");
    }

    public static native int ledCtlInterface(int stauts);

    public static native int add(int a, int b);

    public static native void myLedCtl(int state);

    public static native void getInt();
}
