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
import com.willblaschko.android.alexa.callbacks.IGetContextEventCallBack;
import com.willblaschko.android.alexa.interfaces.AvsResponse;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
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

    private final int MAX_WAIT_TIME = 8 * 1000;
    private final int MAX_RECORD_TIME = 10 * 1000;
    private final int MAX_WAIT_END_TIME = 1000;

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
    private volatile NearTalkRandomAccessFile mShareFile;
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
            doActuallyInterrupt();
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
            mListener.recordFinish(false, "", -1);
        }
    }

    @Override
    public boolean isInterrupted() {
        return recordState == MyVoiceRecord.RecordState.interrupt || recordState == MyVoiceRecord.RecordState.stop;
    }

    @Override
    public void interrupt() {
//        super.interrupt();//warn 这里不能这的调用super的方法，否则只能返回no content
//        Log.d(TAG, "NearTalkVoiceRecord # interrupt");
//        recordState = MyVoiceRecord.RecordState.interrupt;
//        mShareFile.cancel();
    }

    public void interrupt(boolean stopAll) {
//        super.interrupt();//warn 这里不能这的调用super的方法，否则只能返回no content

        if(stopAll) {
            Log.d(TAG, "NearTalkVoiceRecord # interrupt");
            recordState = MyVoiceRecord.RecordState.interrupt;
        } else {
            Log.d(TAG, "NearTalkVoiceRecord # stop");
            recordState = MyVoiceRecord.RecordState.stop;
        }
        mShareFile.cancel();
    }

    public void doActuallyInterrupt(){
        Log.d(TAG, "NearTalkVoiceRecord # doActuallyInterrupt");
//        audioRecorder.stop(); // 这里不应该会到这一步
//        audioRecorder.release();
        interrupt(true);
        if(!super.isInterrupted()) super.interrupt();
    }

    public void startHttpRequest(final AsyncCallback<AvsResponse, Exception> callback, IGetContextEventCallBack getContextEventCallBack) {
        AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID);
        alexaManager.sendAudioRequest("NEAR_FIELD", new NearTalkFileDataRequestBody(mShareFile), new AsyncCallback<AvsResponse, Exception>() {
            @Override
            public void start() {
                if (callback != null) callback.start();
            }

            @Override
            public void success(AvsResponse result) {
                if (callback != null) callback.success(result);
            }

            @Override
            public void failure(Exception error) {
                Log.d(TAG, "startHttpRequest#failure");
                // 这里表示Http已经超时了
                doActuallyInterrupt();
                if (callback != null) callback.failure(error);
            }

            @Override
            public void complete() {
                if (callback != null) callback.complete();
            }
        },getContextEventCallBack);
    }

    class NearTalkFileDataRequestBody extends RequestBody {
        private NearTalkRandomAccessFile mFile;
        private int pointer = 0;
        private FileOutputStream mRecordOutputStream;
        ByteArrayOutputStream mByteArrayStream;

        public NearTalkFileDataRequestBody(final NearTalkRandomAccessFile file) {
            if (file == null) throw new NullPointerException("content == null");
            mFile = file;
            mByteArrayStream = new ByteArrayOutputStream();
//            if (BuildConfig.DEBUG) { // Record wav
//                try {
//                    mRecordOutputStream = new FileOutputStream(mFilePath + ".pcm");
//                    Log.w(TAG, "create record file:"+mFilePath + ".pcm");
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                }
//            }
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
            if(mFile.isClose() && mByteArrayStream.size()>0){
                Log.w(TAG, "writeTo0000:"+mByteArrayStream.size());
                sink.write(mByteArrayStream.toByteArray());
                sink.flush();
            } else {
                byte[] buffer = new byte[256];
                Log.d(TAG, "writeTo0 isClose:" + mFile.isClose() + " l:" + mFile.length());
                try {
                    while (!mFile.isClose()) {
                        long act = mFile.getActuallyLong();
                        if ((act == 0 && mFile.length() > pointer) || pointer < mFile.getActuallyLong()) {
                            if (writeToSink(buffer, sink)) {
                                break;
                            }

                        }
                    }

                    Log.d(TAG, "writeTo1 isClose:" + mFile.isClose() + "\n cancel:" + mFile.isCanceled()
                            + " interrupted:" + isInterrupted() + "\n pointer:"+pointer + " act_length:"+mFile.getActuallyLong() );
                    if (!mFile.isCanceled() && !isInterrupted()) {
                        while (pointer < mFile.getActuallyLong()) {
                            if (writeToSink(buffer, sink)) {
                                break;
                            }
                        }
                        mListener.recordFinish(true, mFilePath, pointer);
                    }else if(mFile.isClose() && mFile.length() == 0){
                        //is cancel here
                        Log.d(TAG, "writeTo1 cancel http!");
                        AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID).cancelAudioRequest();
                    } else {
//                    mListener.recordFinish(false, mFilePath, 0);
//                    Log.w(TAG, "it should cancel http request here");
                    }
                    int actl = (int) mFile.getActuallyLong();
                    if(actl>0 && actl<mByteArrayStream.size()){
                        Log.d(TAG, "trying change ByteArrayStream size to "+ actl);
                        byte[] raw = mByteArrayStream.toByteArray();
                        mByteArrayStream.reset();
                        mByteArrayStream.write(raw, 0, actl);
                        Log.d(TAG, "change ByteArrayStream size to "+ mByteArrayStream.size());
                    }
//                try {
//                    mFile.doActuallyClose(); //这里应该是异步线程调用close，会导致难以恢复的异常,千万别调用
//                } catch (final IOException ioe) {
//                    // ignore
//                    ioe.printStackTrace();
//                }
                } catch (IOException ioe) {
                    throw ioe;
                } finally {
                    IOUtils.closeQuietly(mRecordOutputStream);
                    Log.d(TAG, "writeToSink end, actually_end_point:" + mFile.getActuallyLong() + " p:" + pointer + " diff: " + (mFile.getActuallyLong() - pointer));
                }
            }
        }

        private synchronized boolean writeToSink(byte[] buffer, BufferedSink sink) throws IOException {
            if (mFile.getActuallyLong() > 0 && pointer > mFile.getActuallyLong()) {
                return true;
            }
            mFile.seek(pointer);
            int readCount = mFile.read(buffer);

            if (readCount > 0) {
                sink.write(buffer, 0, readCount);
                mByteArrayStream.write(buffer, 0, readCount);
                if (mRecordOutputStream != null) {
                    mRecordOutputStream.write(buffer, 0, readCount);
                }
                pointer += readCount;
                sink.flush();
            }
            return false;
        }

    }
}
