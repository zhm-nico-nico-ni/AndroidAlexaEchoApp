package com.ggec.voice.assistservice.wakeword;

import android.content.Context;

/**
 * Created by ggec on 2017/4/20.
 */

public abstract class WakeWordAgent {
    protected Context mContext;
    protected IWakeWordAgentEvent mListener;

    public WakeWordAgent(Context context, IWakeWordAgentEvent listener) {
        mContext = context;
        mListener = listener;
        init();
    }

    protected abstract void init();

    public abstract void continueSearch();

    public abstract void pauseSearch();
}
