package com.ggec.voice.assistservice.audio.neartalk;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;

import com.ggec.voice.assistservice.BuildConfig;
import com.ggec.voice.assistservice.MyApplication;
import com.ggec.voice.assistservice.audio.IMyVoiceRecordListener;
import com.ggec.voice.assistservice.audio.MyVoiceRecord;
import com.willblaschko.android.alexa.AlexaManager;
import com.willblaschko.android.alexa.callbacks.AsyncCallback;
import com.willblaschko.android.alexa.interfaces.AvsResponse;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import be.tarsos.dsp.SilenceDetector;
import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

/**
 * Created by ggec on 2017/4/25.
 */

public class NearTalkVoiceRecord extends Thread {

    public final static float DEFAULT_SILENT_THRESHOLD = -70f;

    private final int MAX_WAIT_TIME = 10 * 1000;
    private final int MAX_RECORD_TIME = 10 * 1000;
    private final int MAX_WAIT_END_TIME = 1500;

    private final static String TAG = "NearTalkVoiceRecord";

    private static final int RECORDER_SAMPLERATE = 16000;
    private static final int RECORDER_BPP = 16;

    private final TarsosDSPAudioFormat tarsosDSPAudioFormat;
    private final AudioRecord audioRecorder;
    private final int bufferSizeInBytes;
    private final SilenceDetector continuingSilenceDetector;
    private boolean mIsSilent;
    private MyVoiceRecord.State mState;
    private String mFilePath;
    private volatile MyVoiceRecord.RecordState recordState = MyVoiceRecord.RecordState.empty;
    private NearTalkRandomAccessFile mShareFile;
    private IMyVoiceRecordListener mListener;

    public NearTalkVoiceRecord(String filepath, float silenceThreshold, @NonNull IMyVoiceRecordListener listener) {
        mListener = listener;
        int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
        int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

        bufferSizeInBytes = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING
        );
        // Initialize Audio Recorder.
        audioRecorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
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

        mFilePath = filepath;
        try {
            mShareFile = new NearTalkRandomAccessFile(mFilePath);
        } catch (IOException e) {
            e.printStackTrace();
            interrupt();
        }

        recordState = MyVoiceRecord.RecordState.init;
        // Start Recording.
        audioRecorder.startRecording();
    }

    @Override
    public void run() {
        int numberOfReadFloat;
        byte audioBuffer[] = new byte[bufferSizeInBytes];
        float tempFloatBuffer[] = new float[bufferSizeInBytes / 2];

        mState = new MyVoiceRecord.State();
        mState.initTime = SystemClock.elapsedRealtime();
        mIsSilent = true;

        long currentDataPointer = 0;
        try {
            // While data come from microphone.
            Log.d(TAG, "init file:" + mFilePath);
            while (!isInterrupted()) {
//                numberOfReadFloat = audioRecorder.read(audioBuffer, 0, bufferSizeInBytes, AudioRecord.READ_NON_BLOCKING);
                numberOfReadFloat = audioRecorder.read(audioBuffer, 0, bufferSizeInBytes);

                if (numberOfReadFloat > 0) {
                    TarsosDSPAudioFloatConverter
                            .getConverter(tarsosDSPAudioFormat)
                            .toFloatArray(audioBuffer, tempFloatBuffer, numberOfReadFloat / 2);

                    boolean isSilent = continuingSilenceDetector.isSilence(tempFloatBuffer);
                    long currentTime = SystemClock.elapsedRealtime();

                    if (!isSilent && mState.beginSpeakTime == 0) {
                        Log.d(TAG, "begin " + continuingSilenceDetector.currentSPL());
                        mState.beginSpeakTime = currentTime;
                    } else {
                        if (isSilent != mIsSilent) {

                            if (!mIsSilent) {
                                mState.lastSilentTime = currentTime;
                                mState.lastSilentRecordIndex = currentDataPointer;
                                Log.d(TAG, "record silent time :" + currentTime + " spl:" + continuingSilenceDetector.currentSPL());
                            } else {
                                Log.d(TAG, "current is slient:" + continuingSilenceDetector.currentSPL());
                            }
                            mIsSilent = isSilent;
                        }
                    }

                    if (mState.beginSpeakTime == 0 &&
                            currentTime - mState.initTime >= MAX_WAIT_TIME) {
                        Log.d(TAG, "finish: wait to long ");
                        recordState = MyVoiceRecord.RecordState.interrupt;
                        break;
                    } else if (isSilent &&
                            mState.lastSilentTime != 0 &&
                            currentTime - mState.lastSilentTime >= MAX_WAIT_END_TIME) {
                        Log.d(TAG, "OK: complete after speak ");
                        break;
                    }

                    if (mState.beginSpeakTime > 0) {
                        try {
                            mShareFile.seek(currentDataPointer);
                            //write file
                            mShareFile.write(audioBuffer, 0, numberOfReadFloat);
                            currentDataPointer += numberOfReadFloat;

                            if (currentTime - mState.beginSpeakTime > MAX_RECORD_TIME) break;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }

            if (mState.beginSpeakTime > 0) {
                mShareFile.close();
                mShareFile.setActuallyLong(mState.lastSilentRecordIndex);
            } else {
                mShareFile.cancel();
            }

            Log.d(TAG, " important !!!!!!!!!!!!!!!! size:" + currentDataPointer + " act:" + mState.lastSilentRecordIndex);

//        } catch (IOException e) {
//            e.printStackTrace();
//            interrupt();
        } finally {
            if (mShareFile != null) {
                IOUtils.closeQuietly(mShareFile);
            }
        }
        stopRecord(mState.lastSilentRecordIndex);
    }

    @Override
    public synchronized void start() {
        if (recordState == MyVoiceRecord.RecordState.init) {
            super.start();
        } else {
            Log.w(TAG, "start with wrong state");
        }
    }

    private void stopRecord(long actuallyLong) {
        audioRecorder.release();
        Log.i(TAG, " record finish, success:" + (recordState != MyVoiceRecord.RecordState.interrupt)
                + " file:" + mFilePath + " state:" + mState.toString());
        boolean success = recordState != MyVoiceRecord.RecordState.interrupt;
        if (success) {
//            mListener.recordFinish(true, mFilePath, actuallyLong);
        } else {
            new File(mFilePath).delete();
            mListener.recordFinish(false, "", 0);
        }
    }

    @Override
    public void interrupt() {
        Log.d(TAG, "NearTalkVoiceRecord # interrupt");
        recordState = MyVoiceRecord.RecordState.interrupt;
        mShareFile.cancel();
        super.interrupt();
    }

    public void startHttpRequest(AsyncCallback<AvsResponse, Exception> callback) {
        AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID);
        alexaManager.sendAudioRequest("NEAR_FIELD", new NearTalkFileDataRequestBody(mShareFile), callback);
    }

    class NearTalkFileDataRequestBody extends RequestBody {
        private NearTalkRandomAccessFile mFile;
        int pointer = 0;
        FileOutputStream mRecordOutputStream;

        public NearTalkFileDataRequestBody(final NearTalkRandomAccessFile file) {
            if (file == null) throw new NullPointerException("content == null");
            mFile = file;
            if (BuildConfig.DEBUG) { // Record wav
                try {
                    mRecordOutputStream = new FileOutputStream(mFilePath + ".pcm");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public long contentLength() throws IOException {
            return super.contentLength();
        }

        @Override
        public MediaType contentType() {
            return MediaType.parse("application/octet-stream");
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {

            byte[] buffer = new byte[1024];
            Log.w(TAG, "writeTo0 isClose:" + mFile.isClose() + " l:" + mFile.length());
            while (!mFile.isClose()) {
                if (mFile.length() > pointer) {

                    if(writeToSink(buffer, sink)){
                        break;
                    }
//                    Log.v(TAG, "writeToSink p:"+pointer+" l:"+mFile.length());
                }
            }
            Log.w(TAG, "writeTo2 isClose:" + mFile.isClose() + " l:" + mFile.length() + " cancel:"+mFile.isCanceled());
            if (!mFile.isCanceled()) {
                while (pointer < mFile.length()) {
                    if(writeToSink(buffer, sink)){
                        break;
                    }
                    //Log.v(TAG, "writeToSink p:"+pointer+" r:"+readCount+" l:"+mFile.length());
                }
                mListener.recordFinish(true, mFilePath, pointer);
            } else {
                mListener.recordFinish(false, mFilePath, 0);
                Log.w(TAG, "it should cancel http request here");
            }

            IOUtils.closeQuietly(mRecordOutputStream);

            Log.w(TAG, "writeToSink end l:" + mFile.length() + " actually_end_point:"+ mFile.getActuallyLong()+ " p:" + pointer+" diff: "+(mFile.getActuallyLong() - pointer));
        }

        private synchronized boolean writeToSink(byte[] buffer, BufferedSink sink) throws IOException {
            if(mFile.getActuallyLong()>0 && pointer>mFile.getActuallyLong()){
                return true;
            }
            mFile.seek(pointer);
            int readCount = mFile.read(buffer);

            if (readCount > 0) {
                sink.write(buffer, 0, readCount);
                if(mRecordOutputStream!=null){
                    mRecordOutputStream.write(buffer, 0, readCount);
                }
                pointer += readCount;
                sink.flush();
            }
            return false;
        }

    }
}
