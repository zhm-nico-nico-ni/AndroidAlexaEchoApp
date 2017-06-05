package com.ggec.voice.assistservice.audio.neartalk;

import android.os.SystemClock;

/**
 * Created by ggec on 2017/6/5.
 *
 */

public class NearTalkState {
    public long initTime;
    public long beginSpeakTime;
    public long lastSilentTime;
    public long lastSilentRecordIndex;

    @Override
    public String toString() {
        long now = SystemClock.elapsedRealtime();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("event duration:").append(now - initTime);
        if (beginSpeakTime > 0) {
            stringBuilder.append(" , speak duration:").append(now - beginSpeakTime);
        }
        return stringBuilder.toString();
    }
}
