package com.ggec.voice.assistservice.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.ggec.voice.toollibrary.log.Log;

/**
 * Created by ggec on 2017/5/24.
 * Only one instance in app, for wake word detect and speech recognize.
 */

public class SingleAudioRecord {
//    private static final float BUFFER_SIZE_SECONDS = 0.4F;
    private static volatile SingleAudioRecord sInstance;
    private final AudioRecord audioRecorder;
    private final int bufferSizeInBytes;

    private SingleAudioRecord(){

        int recorder_sample_rate = 16000;
        int recorder_channels = AudioFormat.CHANNEL_IN_MONO;
        int recorder_audio_encoding = AudioFormat.ENCODING_PCM_16BIT;

//        bufferSizeInBytes = Math.round((float)recorder_sample_rate * BUFFER_SIZE_SECONDS);
        bufferSizeInBytes = AudioRecord.getMinBufferSize(recorder_sample_rate,
                recorder_channels,
                recorder_audio_encoding
        );

        audioRecorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                recorder_sample_rate,
                recorder_channels,
                recorder_audio_encoding,
                bufferSizeInBytes);


        if(this.audioRecorder.getState() == 0) {
            this.audioRecorder.release();
            Log.e("SingleAudioRecord","Failed to initialize recorder. Microphone might be already in use.");
        }

    }

    public static synchronized SingleAudioRecord getInstance(){
        if(sInstance == null){
            sInstance = new SingleAudioRecord();
        }
        return sInstance;
    }

    public void startRecording(){
        audioRecorder.startRecording();
    }

    public int getState(){
        return audioRecorder.getState();
    }

    public boolean isRecording(){
        return audioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING;
    }

    public AudioRecord getAudioRecorder(){
        return audioRecorder;
    }

    public void stop(){
        audioRecorder.stop();
    }

    public void release(){
        audioRecorder.release();
    }

    public int getBufferSizeInBytes(){
        return bufferSizeInBytes;
    }
}
