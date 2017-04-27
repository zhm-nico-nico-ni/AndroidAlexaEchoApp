package com.willblaschko.android.alexa.data.message.request.speaker;

import com.willblaschko.android.alexa.data.message.Payload;

public class MuteChange extends Payload {
    public boolean muted;
    public long volume;

//    public final void setMute(boolean mute) {
//        this.mute = mute;
//    }
//
//    public final boolean getMute() {
//        return mute;
//    }
}
