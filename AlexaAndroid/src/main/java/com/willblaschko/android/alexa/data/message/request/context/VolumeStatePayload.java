package com.willblaschko.android.alexa.data.message.request.context;

import com.willblaschko.android.alexa.data.message.Payload;

public class VolumeStatePayload extends Payload {
    public long volume;
    public boolean muted;

//    public VolumeStatePayload(long volume, boolean muted) {
//        this.volume = volume;
//        this.muted = muted;
//    }
//
//    public long getVolume() {
//        return volume;
//    }
//
//    public boolean getMuted() {
//        return muted;
//    }
}