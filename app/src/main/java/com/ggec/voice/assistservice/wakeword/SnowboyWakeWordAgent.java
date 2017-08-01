package com.ggec.voice.assistservice.wakeword;

import android.content.Context;

import com.ggec.voice.toollibrary.log.Log;

import ai.kitt.snowboy.MsgEnum;
import ai.kitt.snowboy.audio.ISnowEventCallback;
import ai.kitt.snowboy.audio.RecordingThread;

/**
 * Created by ggec on 2017/4/20.
 */

public class SnowboyWakeWordAgent extends WakeWordAgent implements ISnowEventCallback {
    private final static String TAG = "SnowboyWakeWordAgent";

    private RecordingThread recordingThread;

    public SnowboyWakeWordAgent(Context context, IWakeWordAgentEvent listener) {
        super(context, listener);
    }

    @Override
    protected void init() {
        Log.d(TAG, "initd");
        recordingThread = new RecordingThread(this, null /*new AudioDataSaver()*/);
    }

    @Override
    public void continueSearch() {
        recordingThread.startRecording();
    }

    @Override
    public void pauseSearch() {
        recordingThread.stopRecording();
    }


    @Override
    public void onEvent(MsgEnum message, Object obj) {
        switch(message) {
            case MSG_ACTIVE:
//                    activeTimes++;
                Log.d(TAG, " ----> Detected");
                // Toast.makeText(Demo.this, "Active "+activeTimes, Toast.LENGTH_SHORT).show();
//                    showToast("Active "+activeTimes);
                recordingThread.stopRecording();
                mListener.onDetectWakeWord(null, 0, 0);
                break;
            case MSG_INFO:
                Log.d(TAG, " ----> "+message);
                break;
            case MSG_VAD_SPEECH:
                Log.d(TAG, " ----> normal voice");
                break;
            case MSG_VAD_NOSPEECH:
//                    updateLog(" ----> no speech", "blue");
                break;
            case MSG_ERROR:
                Log.e(TAG, " ----> "+obj.toString());
                break;
            default:
                break;
        }
    }
}
