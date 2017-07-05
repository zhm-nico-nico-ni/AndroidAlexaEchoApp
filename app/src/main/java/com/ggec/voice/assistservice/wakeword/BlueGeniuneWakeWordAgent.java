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
        new Thread(){
            @Override
            public void run() {
                Log.d("BlueGeniuneWakeWordAgent", " start" );
                LedControl.getInt();
            }
        }.start();
    }

    @Override
    public void continueSearch() {
        Listener.canDetect.getAndSet(true);
    }
}
