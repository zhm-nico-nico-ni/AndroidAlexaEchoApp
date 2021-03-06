package com.ggec.voice.assistservice.wakeword;

import android.content.Context;

import com.example.administrator.appled.LedControl;
import com.example.administrator.appled.Listener;
import com.ggec.voice.toollibrary.log.Log;

/**
 * Created by ggec on 2017/7/4.
 */

public class BlueGeniuneWakeWordAgent extends WakeWordAgent {

    public BlueGeniuneWakeWordAgent(Context context, IWakeWordAgentEvent listener) {
        super(context, listener);
    }

    @Override
    protected void init() {
        Log.w("BlueGeniuneWakeWordAgent", " init" );
        new Thread(){
            @Override
            public void run() {
                Log.w("BlueGeniuneWakeWordAgent", " start" );
                LedControl.getInt();
            }
        }.start();
    }

    @Override
    public void continueSearch() {
        Listener.canDetect.getAndSet(true);
    }

    @Override
    public void pauseSearch() {
        Listener.canDetect.set(false);
    }
}
