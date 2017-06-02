package com.ggec.voice.assistservice.audio.neartalk;

import android.os.SystemClock;
import android.support.annotation.NonNull;

import com.ggec.voice.assistservice.BuildConfig;
import com.ggec.voice.assistservice.MyApplication;
import com.ggec.voice.assistservice.audio.IMyVoiceRecordListener;
import com.ggec.voice.assistservice.audio.MyVoiceRecord;
import com.ggec.voice.assistservice.audio.SingleAudioRecord;
import com.ggec.voice.toollibrary.log.Log;
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
 * Near Talk, use stop capture directive
 */

public class NearTalkVoiceRecord extends Thread {

    public final static float DEFAULT_SILENT_THRESHOLD = -70f;

    private final int MAX_WAIT_TIME = 8 * 1000;
    private final int MAX_RECORD_TIME = 10 * 1000;
    private final int MAX_WAIT_END_TIME = 1000;

    private final static String TAG = "NearTalkVoiceRecord";

    private final TarsosDSPAudioFormat tarsosDSPAudioFormat;
    private final int bufferSizeInBytes;
    private final SilenceDetector continuingSilenceDetector;

    private MyVoiceRecord.State mState;
    private final String mFilePath;
    private volatile RecordState recordState = RecordState.empty;
    private volatile NearTalkRandomAccessFile mShareFile;
    private IMyVoiceRecordListener mListener;
    private final long mBeginPosition;

    public NearTalkVoiceRecord(long beginPosition, String filepath, float silenceThreshold, @NonNull IMyVoiceRecordListener listener) {
        mBeginPosition = beginPosition;
        mListener = listener;

        bufferSizeInBytes = SingleAudioRecord.getInstance().getBufferSizeInBytes();

        tarsosDSPAudioFormat = new TarsosDSPAudioFormat(
                16000
                , 16
                , 1
                , true
                ,false);

        // 这里的最小声音分贝值可以根据先前的输入值来判断
        continuingSilenceDetector = new SilenceDetector(silenceThreshold, false);

        mFilePath = filepath;
        try {
            mShareFile = new NearTalkRandomAccessFile(mFilePath);
        } catch (IOException e) {
            e.printStackTrace();
            doActuallyInterrupt();
        }

        recordState = RecordState.init;
        if(!SingleAudioRecord.getInstance().isRecording()) {
            SingleAudioRecord.getInstance().getAudioRecorder().startRecording();
        }
    }

    @Override
    public void run() {
        int numberOfReadFloat;
        byte audioBuffer[] = new byte[bufferSizeInBytes];
        float tempFloatBuffer[] = new float[bufferSizeInBytes / 2];

        mState = new MyVoiceRecord.State();
        mState.initTime = SystemClock.elapsedRealtime();
        boolean mIsSilent = true;

        long currentDataPointer = mBeginPosition;
        try {
            // While data come from microphone.
            Log.d(TAG, "init file:" + mFilePath);
            while (SingleAudioRecord.getInstance().isRecording()) {
//                numberOfReadFloat = audioRecorder.read(audioBuffer, 0, bufferSizeInBytes, AudioRecord.READ_NON_BLOCKING);
                numberOfReadFloat = SingleAudioRecord.getInstance().getAudioRecorder().read(audioBuffer, 0, bufferSizeInBytes);

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

    private void stopRecord(long actuallyLong) {
        SingleAudioRecord.getInstance().getAudioRecorder().stop();
        Log.i(TAG, " record finish, success:" + (recordState != RecordState.interrupt)
                + " file:" + mFilePath + " state:" + mState.toString());
        boolean success = recordState != RecordState.interrupt;
        if (success) {
//            mListener.recordFinish(true, mFilePath, actuallyLong);
        } else {
            new File(mFilePath).delete();
            mListener.recordFinish(false, "", -1);
        }
    }

    @Override
    public boolean isInterrupted() {
        return recordState == RecordState.interrupt || recordState == RecordState.stop;
    }

    @Override
    @Deprecated
    public void interrupt() {
//        super.interrupt();//warn 这里不能这的调用super的方法，否则只能返回no content
//        Log.d(TAG, "NearTalkVoiceRecord # interrupt");
//        recordState = MyVoiceRecord.RecordState.interrupt;
//        mShareFile.cancel();
    }

    public void interrupt(boolean stopAll) {
//        super.interrupt();//warn 这里不能这的调用super的方法，否则只能返回no content
        SingleAudioRecord.getInstance().stop();
        if(stopAll) {
            Log.d(TAG, "NearTalkVoiceRecord # interrupt");
            recordState = RecordState.interrupt;
        } else {
            Log.d(TAG, "NearTalkVoiceRecord # stop");
            recordState = RecordState.stop;
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

    public void startHttpRequest(long endIndexInSamples, final AsyncCallback<AvsResponse, Exception> callback, IGetContextEventCallBack getContextEventCallBack) {
        AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID);
        alexaManager.sendAudioRequest("NEAR_FIELD", new NearTalkFileDataRequestBody(mShareFile, endIndexInSamples), new AsyncCallback<AvsResponse, Exception>() {
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
        }, getContextEventCallBack);
    }

    private class NearTalkFileDataRequestBody extends RequestBody {
        private NearTalkRandomAccessFile mFile;
        private long pointer;
        private FileOutputStream mRecordOutputStream;
        private ByteArrayOutputStream mByteArrayStream;

        public NearTalkFileDataRequestBody(final NearTalkRandomAccessFile file, long beginPosition) {
            if (file == null) throw new NullPointerException("content == null");
            mFile = file;
            mByteArrayStream = new ByteArrayOutputStream();
            pointer = beginPosition;
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
                //We encourage streaming captured audio to AVS in 10ms chunks at 320 bytes (320 byte DATA frames sent as single units).
                // Larger chunk sizes create unnecessary buffering, which negatively impacts AVS’ ability to process audio and may result in higher latencies.
                byte[] buffer = new byte[320];
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
