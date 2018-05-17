package com.ggec.voice.assistservice.audio.record;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class GGECAudioRecorder extends Thread {
    public interface AudioRecordRawOnData {
        void onAudioData(byte[] data, int size);

        void onInterrupt();
    }

    public static final int AUDIO_ERROR_SAMPLERATE_NOT_SUPPORT = -1;
    public static final int AUDIO_ERROR_GET_MIN_BUFFER_SIZE_NOT_SUPPORT = -2;
    public static final int AUDIO_ERROR_CREATE_FAILED = -3;
    public static final int AUDIO_ERROR_UNKNOWN = -4;

    private String TAG = "GGECAudioRecorder";
    private AudioRecord mAudioRecord = null;
    private MediaRecordOnInfoError mInfoErrorListener = null;
    private AudioRecordRawOnData mAudioDataListener = null;

    private int mSampleRate = 16000;
    private boolean isRecording = false;
    private boolean isEnd = false;

    public void setSampleRate(int sampleRate) {
        mSampleRate = sampleRate;
    }

    public void setAudioDataListener(AudioRecordRawOnData listener) {
        mAudioDataListener = listener;
    }

    public void setAudioInfoErrorListener(MediaRecordOnInfoError listener) {
        mInfoErrorListener = listener;
    }

    @Override
    public void run() {
        com.ggec.voice.toollibrary.log.Log.d(TAG, "start recording0");
        isRecording = true;
        if (mSampleRate != 8000 && mSampleRate != 16000 && mSampleRate != 22050 && mSampleRate != 44100) {
            if (mInfoErrorListener != null)
                mInfoErrorListener.onInfoError(AUDIO_ERROR_SAMPLERATE_NOT_SUPPORT, null);
            return;
        }

        final int mMinBufferSize = AudioRecord.getMinBufferSize(mSampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

        if (AudioRecord.ERROR_BAD_VALUE == mMinBufferSize) {
            if (mInfoErrorListener != null)
                mInfoErrorListener.onInfoError(AUDIO_ERROR_GET_MIN_BUFFER_SIZE_NOT_SUPPORT, null);
            return;
        }

        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, mSampleRate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, mMinBufferSize);

        if (this.mAudioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
            this.mAudioRecord.release();
            if (mInfoErrorListener != null)
                mInfoErrorListener.onInfoError(AUDIO_ERROR_CREATE_FAILED, null);
            return;
        }
        com.ggec.voice.toollibrary.log.Log.d(TAG, "start recording1");

        try {
            mAudioRecord.startRecording();
        } catch (IllegalStateException e) {
            if (mInfoErrorListener != null)
                mInfoErrorListener.onInfoError(AUDIO_ERROR_UNKNOWN, null);
            return;
        }
        com.ggec.voice.toollibrary.log.Log.d(TAG, "start recording");

        byte[] sampleBuffer = new byte[mMinBufferSize];

        try {
            while (isRecording) {
                int result = mAudioRecord.read(sampleBuffer, 0, mMinBufferSize);
                if (result > 0 && mAudioDataListener != null) {
                    mAudioDataListener.onAudioData(sampleBuffer, result);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            if (mInfoErrorListener != null)
                mInfoErrorListener.onInfoError(AUDIO_ERROR_UNKNOWN, null);
        }

        isRecording = false;
        mAudioRecord.release();
        mAudioRecord = null;
        isEnd = true;
    }

//    @Override
//    public synchronized void start() {
//        super.start();
//    }

    public void interruptAll() {
        isEnd = true;
        isRecording = false;
        if (null != mAudioRecord) {
            mAudioRecord.release();
        }
        if(mAudioDataListener != null){
            mAudioDataListener.onInterrupt();
        }
    }

    public boolean isRecording(){
        return isRecording;
    }

    public boolean isEnd(){
        return isEnd;
    }
}
