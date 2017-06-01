package com.ggec.voice.assistservice.wakeword;

/**
 * Created by ggec on 2017/4/20.
 */

public interface IWakeWordAgentEvent {
    void onDetectWakeWord(String rawPath, long startIndexInSamples, long endIndexInSamples);
}
