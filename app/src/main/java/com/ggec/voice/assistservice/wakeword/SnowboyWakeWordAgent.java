package com.ggec.voice.assistservice.wakeword;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.ggec.voice.toollibrary.log.Log;

import ai.kitt.snowboy.MsgEnum;
import ai.kitt.snowboy.audio.RecordingThread;

/**
 * Created by ggec on 2017/4/20.
 */

public class SnowboyWakeWordAgent extends WakeWordAgent {
    private final static String TAG = "SnowboyWakeWordAgent";

    private RecordingThread recordingThread;

    public SnowboyWakeWordAgent(Context context, IWakeWordAgentEvent listener) {
        super(context, listener);
    }

    @Override
    protected void init() {
        recordingThread = new RecordingThread(handle, null /*new AudioDataSaver()*/);
    }

    @Override
    public void continueSearch() {
        recordingThread.startRecording();
    }



    public Handler handle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            MsgEnum message = MsgEnum.getMsgEnum(msg.what);
            switch(message) {
                case MSG_ACTIVE:
//                    activeTimes++;
                    Log.d(TAG, " ----> Detected " + "activeTimes" + " times");
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
                    Log.e(TAG, " ----> "+msg.toString());
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    };

}
