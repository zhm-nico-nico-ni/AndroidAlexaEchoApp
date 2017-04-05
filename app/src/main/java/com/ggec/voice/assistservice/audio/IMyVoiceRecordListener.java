package com.ggec.voice.assistservice.audio;

/**
 * Created by ggec on 2017/4/5.
 */

public interface IMyVoiceRecordListener {

    void recordFinish(boolean recordSuccess, String filePath);
}
