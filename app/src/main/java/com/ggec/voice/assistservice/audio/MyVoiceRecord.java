package com.ggec.voice.assistservice.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;

import com.ggec.voice.assistservice.MyApplication;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import be.tarsos.dsp.SilenceDetector;
import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;

/**
 * Created by ggec on 2017/4/1.
 */

public class MyVoiceRecord extends Thread {
    public final static float DEFAULT_SILENT_THRESHOLD = -55f;

    private final static String TAG = "MyVoiceRecord";

    private enum RecordState {empty, init, start, stop, interrupt}

    private static final int RECORDER_SAMPLERATE = 16000;
    private static final int RECORDER_BPP = 16;

    private static int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private final int MAX_WAIT_TIME = 10 * 1000;
    private final int MAX_WAIT_END_TIME = 1500; // TODO 这个多余的时间要截掉

    private final TarsosDSPAudioFormat tarsosDSPAudioFormat;
    private final AudioRecord audioRecorder;
    private final int bufferSizeInBytes;
    private final SilenceDetector continuingSilenceDetector;
    private boolean mIsSilent;
    private State mState;
    private IMyVoiceRecordListener mListener;

    private volatile RecordState recordState = RecordState.empty;
    private final String mFilePath;

    public MyVoiceRecord(float silenceThreshold, @NonNull IMyVoiceRecordListener listener) throws IllegalStateException {
        bufferSizeInBytes = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING
        );
        // Initialize Audio Recorder.
        audioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE,
                RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING,
                bufferSizeInBytes);

        tarsosDSPAudioFormat = new TarsosDSPAudioFormat(
                RECORDER_SAMPLERATE
                , RECORDER_BPP
                , 1
                , true,
                false);

        // 这里的最小声音分贝值可以根据先前的输入值来判断
        continuingSilenceDetector = new SilenceDetector(silenceThreshold, false);
        mListener = listener;

        String filepath = MyApplication.getContext().getExternalCacheDir().getPath();
        File file = new File(filepath, "AudioRecorder");
        if (!file.exists()) {
            file.mkdirs();
        }

        mFilePath = file.getAbsolutePath() + "/" + System.currentTimeMillis() + ".pcm";

        recordState = RecordState.init;
        // Start Recording.
        audioRecorder.startRecording();
    }

    @Override
    public void run() {
        int numberOfReadFloat;
        byte audioBuffer[] = new byte[bufferSizeInBytes];
        float tempFloatBuffer[] = new float[bufferSizeInBytes / 2];

        mState = new State();
        mState.initTime = SystemClock.elapsedRealtime();
        mIsSilent = true;

        long currentDataPointer = 0;
        try {
            FileOutputStream stream = new FileOutputStream(mFilePath);

            // While data come from microphone.
            Log.d(TAG, "init file:" + mFilePath);
            while (!isInterrupted()) {
                numberOfReadFloat = audioRecorder.read(audioBuffer, 0, bufferSizeInBytes, AudioRecord.READ_NON_BLOCKING);

                if (numberOfReadFloat > 0) {
                    TarsosDSPAudioFloatConverter
                            .getConverter(tarsosDSPAudioFormat)
                            .toFloatArray(audioBuffer, tempFloatBuffer, numberOfReadFloat / 2);

                    boolean isSilent = continuingSilenceDetector.isSilence(tempFloatBuffer);
                    long currentTime = SystemClock.elapsedRealtime();

                    if (!isSilent && mState.beginSpeakTime == 0) {
                        Log.d(TAG, "begin ");
                        mState.beginSpeakTime = currentTime;
                    } else {
                        if (isSilent != mIsSilent) {

                            if (!mIsSilent) {
                                mState.lastSilentTime = currentTime;
                                mState.lastSilentRecordIndex = currentDataPointer;
                                Log.d(TAG, "record silent time :" + currentTime);
                            } else {
                                Log.d(TAG, "current is " + isSilent);
                            }
                            mIsSilent = isSilent;
                        }
                    }

                    if (mState.beginSpeakTime == 0 &&
                            currentTime - mState.initTime >= MAX_WAIT_TIME) {
                        Log.d(TAG, "finish: wait to long ");
                        recordState = RecordState.interrupt;
                        break;
                    } else if (isSilent &&
                            mState.lastSilentTime != 0 &&
                            currentTime - mState.lastSilentTime >= MAX_WAIT_END_TIME) {
                        Log.d(TAG, "OK: complete after speak ");
                        break;
                    }

                    if (mState.beginSpeakTime > 0) {
                        try {
                            //write file
                            stream.write(audioBuffer, 0, numberOfReadFloat);
                            currentDataPointer += numberOfReadFloat;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }

            Log.d("zhm", " important !!!!!!!!!!!!!!!! size:" + currentDataPointer +" act:"+mState.lastSilentRecordIndex);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            interrupt();
        }
        stopRecord(mState.lastSilentRecordIndex);
    }

    @Override
    public synchronized void start() {
        if (recordState == RecordState.init) {
            super.start();
        } else {
            Log.w(TAG, "start with wrong state");
        }
    }

    private void stopRecord(long actuallyLong) {
        audioRecorder.release();
        Log.i(TAG, " record finish, success:" + (recordState != RecordState.interrupt)
                + " file:" + mFilePath + " state:" + mState.toString());
        boolean success = recordState != RecordState.interrupt;
        if (success) {
            mListener.recordFinish(true, mFilePath, actuallyLong);
        } else {
            new File(mFilePath).delete();
            mListener.recordFinish(false, "", 0);
        }
    }

    @Override
    public void interrupt() {
        recordState = RecordState.interrupt;
        super.interrupt();
    }

//    private int appendByteArray(byte[] left, int appendIndex, byte[] right, int rightOffset, int rightAvail) {
//        if (appendIndex + rightAvail > left.length) {
//            throw new IndexOutOfBoundsException("not enough! left:" + left.length + ", max is " + appendIndex + rightAvail);
//        }
//        int i = appendIndex;
//        if (rightAvail > 0) {
//            int rightIndex = rightOffset;
//            int maxRightIndex = rightOffset + rightAvail;
//
//            for (; i < left.length; i++) {
//                left[i] = right[rightIndex];
//                rightIndex++;
//                if (rightIndex >= maxRightIndex) {
//                    break;
//                }
//            }
//        }
//        return i;
//    }

    class State {
        long initTime;
        long beginSpeakTime;
        long lastSilentTime;
        long lastSilentRecordIndex;

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
}
