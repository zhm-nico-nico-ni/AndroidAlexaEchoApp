package com.ggec.voice.assistservice.audio.neartalk;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;

import com.example.administrator.appled.LedControl;
import com.ggec.voice.assistservice.MyApplication;
import com.ggec.voice.assistservice.audio.IMyVoiceRecordListener;
import com.ggec.voice.assistservice.audio.SingleAudioRecord;
import com.ggec.voice.toollibrary.log.Log;
import com.willblaschko.android.alexa.AlexaManager;
import com.willblaschko.android.alexa.callbacks.AsyncCallback;
import com.willblaschko.android.alexa.callbacks.IGetContextEventCallBack;
import com.willblaschko.android.alexa.interfaces.AvsResponse;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

/**
 * Created by ggec on 2017/4/25.
 * Far Field Talk, 0 - 20 (ft), use stop capture directive
 */

public class FarTalkVoiceRecord extends Thread {


    private final int MAX_WAIT_TIME = 8 * 1000;
    private final int MAX_RECORD_TIME = 10 * 1000;

    private final static String TAG = "FarTalkVoiceRecord";

    private TalkState mState;
    private volatile int recordState = RecordState.EMPTY;
    private volatile int recordHttpState = RecordState.EMPTY;

    private final String mFilePath;

    private volatile TalkDataProvider mShareFile;
    private IMyVoiceRecordListener mListener;

    public FarTalkVoiceRecord(long beginPosition, String filepath, @NonNull IMyVoiceRecordListener listener, int waitTimeOut) throws FileNotFoundException {
        currentDataPointer = beginPosition;
        mListener = listener;

        mFilePath = filepath;
        mShareFile = new TalkDataProvider(mFilePath);

    }

    private boolean handleBreak = false;
    long currentDataPointer;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(handleBreak) return;
            byte[] audioBuffer = (byte[]) msg.obj;
            int numberByteRead = msg.arg1;

            long currentTime = SystemClock.elapsedRealtime();

            if (currentTime - mState.initTime >= MAX_WAIT_TIME) {
                Log.d(TAG, "finish: wait to long ");
                setRecordLocalState(RecordState.FINISH);
                handleBreak = true;
                return;
            }

            try {
                //write file
                mShareFile.write(audioBuffer, 0, numberByteRead);
                currentDataPointer += numberByteRead;

                if (currentTime - mState.initTime > MAX_RECORD_TIME) {
                    handleBreak = true;
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
        LedControl.myLedCtl(2);
        long bt = SystemClock.elapsedRealtime();
        if (!SingleAudioRecord.getInstance().isRecording()) {
            SingleAudioRecord.getInstance().startRecording();
            Log.d(TAG, "startRecording use " +(SystemClock.elapsedRealtime() - bt));

        }

        setRecordLocalState(RecordState.START);
        int numberByteRead;
        int bufferSizeInBytes = 320;// 10ms chunk
        byte audioBuffer[] = new byte[bufferSizeInBytes];

        mState = new TalkState();
        mState.initTime = SystemClock.elapsedRealtime();

        try {
            // While data come from microphone.
            Log.d(TAG, "init file:" + mFilePath);
            while (SingleAudioRecord.getInstance().isRecording() && !handleBreak) {
//                numberOfReadFloat = audioRecorder.read(audioBuffer, 0, bufferSizeInBytes, AudioRecord.READ_NON_BLOCKING);
                numberByteRead = SingleAudioRecord.getInstance().getAudioRecorder().read(audioBuffer, 0, bufferSizeInBytes);

                if (numberByteRead > 0) {
                    long currentTime = SystemClock.elapsedRealtime();
                    handler.obtainMessage(1, numberByteRead, 0, audioBuffer.clone() /*Arrays.copyOf(audioBuffer, numberByteRead)*/).sendToTarget();

                    long diff = SystemClock.elapsedRealtime() - currentTime;
                    if(diff >= 5)
                        Log.w(TAG, "process use to long !!      " + diff);
                }
            }

            if(getRecordLocalState() != RecordState.CANCEL) {
                mShareFile.setEnd(mState.lastSilentRecordIndex);
                Log.d(TAG, " important !!!!!!!!!!!!!!!! size:" + currentDataPointer + " act:" + mState.lastSilentRecordIndex);
            } else {
                Log.d(TAG, " important !!!!!!!!!!!!!!!! cancel");
                mShareFile.cancel();
            }

        } finally {
            stopRecord(mState.lastSilentRecordIndex);
        }
    }

    private void stopRecord(long actuallyLong) {
        SingleAudioRecord.getInstance().stop();
        Log.d(TAG, " record finish, success:" + recordState + " file:" + mFilePath + "\n state:" + mState.toString() + " actuallyLong:" + actuallyLong);
        boolean success = getRecordLocalState() != RecordState.CANCEL;
        if (!success) {
            new File(mFilePath).delete();
            if (getRecordLocalState() == RecordState.ERROR) {
                mListener.failure(null, "", -1, null);
            }
        }
    }

    @Override
    public boolean isInterrupted() { // means local audio record is interrupted
        return getRecordLocalState() == RecordState.CANCEL || getRecordLocalState() == RecordState.FINISH || getRecordLocalState() == RecordState.ERROR || getRecordLocalState() == RecordState.STOP_CAPTURE;
    }

    @Override
    @Deprecated
    public void interrupt() {
//        warn 这里不能这的调用super的方法，否则只能返回no content
    }

    public void stopCapture(boolean stopAll) {
        if(isInterrupted()){
            return;
        }

        if (stopAll) { // stop mic record and http
            Log.d(TAG, "FarTalkVoiceRecord # stopCapture");
            setRecordLocalState(RecordState.CANCEL);
            setRecordHttpState(RecordState.CANCEL);
        } else { // just stop mic
            Log.d(TAG, "FarTalkVoiceRecord # stop capture");
            setRecordLocalState(RecordState.STOP_CAPTURE);
        }

        if(!mShareFile.isEnd()) {
            SingleAudioRecord.getInstance().stop();
        }
        if (stopAll) {
            mShareFile.cancel();
        }
    }

    public void doActuallyInterrupt() {
        Log.d(TAG, "FarTalkVoiceRecord # doActuallyInterrupt");

        stopCapture(true);
        if (!super.isInterrupted()) super.interrupt();
    }

    public void startHttpRequest(long endIndexInSamples, final AsyncCallback<AvsResponse, Exception> callback, IGetContextEventCallBack getContextEventCallBack) {
        AlexaManager.getInstance(MyApplication.getContext()).sendAudioRequest("FAR_FIELD"
                , new NearTalkFileDataRequestBody(mShareFile, endIndexInSamples)
                , new AsyncCallback<AvsResponse, Exception>() {
                    @Override
                    public void start() {
                        setRecordHttpState(RecordState.START);
                        if (callback != null) callback.start();
                    }

                    @Override
                    public void handle(AvsResponse result) {
                        if (callback != null) callback.handle(result);
                    }

                    @Override
                    public void success(AvsResponse result) {
                        setRecordHttpState(RecordState.FINISH);
                        if (callback != null) callback.success(result);
                    }

                    @Override
                    public void failure(Exception error) {
                        setRecordHttpState(RecordState.ERROR);
                        Log.d(TAG, "startHttpRequest#failure");
                        // 这里表示Http已经超时, 或已经失败（没授权）
                        doActuallyInterrupt();
                        if (callback != null) callback.failure(error);
                    }

                    @Override
                    public void complete() {
                        if (callback != null) callback.complete();
                    }
                }, getContextEventCallBack);
    }

    private void setRecordLocalState(int state) {
        recordState = state;
    }

    private int getRecordLocalState() {
        return recordState;
    }

    private void setRecordHttpState(int state) {
        recordHttpState = state;
    }

    private int getRecordHttpState() {
        return recordHttpState;
    }

    private class NearTalkFileDataRequestBody extends RequestBody {
        private TalkDataProvider mFile;
        private long pointer;
        private FileOutputStream mRecordOutputStream;
        private ByteArrayOutputStream mByteArrayStream;

        public NearTalkFileDataRequestBody(final TalkDataProvider file, long beginPosition) {
            if (file == null) throw new NullPointerException("content == null");
            mFile = file;
            mByteArrayStream = new ByteArrayOutputStream();
            pointer = beginPosition;
//            if (BuildConfig.DEBUG) { // Record wav
                try {
                    mRecordOutputStream = new FileOutputStream(mFilePath + ".pcm");
                    Log.w(TAG, "create record file:"+mFilePath + ".pcm");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
//            }
        }

        @Override
        public MediaType contentType() {
            return MediaType.parse("application/octet-stream");
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            if (mFile.isEnd() && !mFile.isCanceled() && mByteArrayStream.size() > 0) {
                Log.w(TAG, "writeTo0000:" + mByteArrayStream.size());
                sink.write(mByteArrayStream.toByteArray());
                sink.flush();
            } else {
                //We encourage streaming captured audio to AVS in 10ms chunks at 320 bytes (320 byte DATA frames sent as single units).
                // Larger chunk sizes create unnecessary buffering, which negatively impacts AVS’ ability to process audio and may result in higher latencies.
                byte[] buffer = new byte[320*5];
                Log.d(TAG, "writeTo0 isClose:" + mFile.isEnd() + " l:" + mFile.getWriteLength());
                try {
                    while (!mFile.isEnd()) {
                        long act = mFile.getActuallyLong();
                        if ((act == 0 && mFile.getWriteLength() > pointer) || pointer < mFile.getActuallyLong()) {
                            if (writeToSink(buffer, sink)) {
                                break;
                            }
                        }
                    }

                    Log.d(TAG, "writeTo1 isClose:" + mFile.isEnd() + "\n cancel:" + mFile.isCanceled()
                            + " interrupted:" + isInterrupted() + " http:"+getRecordHttpState() + "\n pointer:" + pointer + " act_length:" + mFile.getActuallyLong());
                    if (!mFile.isCanceled() && getRecordLocalState() != RecordState.CANCEL && getRecordLocalState() != RecordState.ERROR
                            && getRecordHttpState() != RecordState.ERROR && getRecordHttpState() != RecordState.CANCEL) {
                        while (pointer < mFile.getActuallyLong()) {
                            if (writeToSink(buffer, sink)) {
                                break;
                            }
                        }
                        LedControl.myLedCtl(3);
                        Log.d(TAG, "write remaining end");
                    } else if (mFile.isEnd() && mFile.getWriteLength() <= 0) {
                        //is cancel here
                        Log.d(TAG, "writeTo1 cancel http!");
                        setRecordHttpState(RecordState.CANCEL);
                        AlexaManager.getInstance(MyApplication.getContext()).cancelAudioRequest();
                    }

//                try {
//                    mFile.doActuallyClose(); //这里应该是异步线程调用close，会导致难以恢复的异常,千万别调用
//                } catch (final IOException ioe) {
//                    // ignore
//                    ioe.printStackTrace();
//                }
//                } catch (IOException ioe) {
//                    throw ioe;
                } finally {
                    AlexaManager.getInstance(MyApplication.getContext()).getSpeechSendAudio().recordend = System.currentTimeMillis();
                    IOUtils.closeQuietly(mRecordOutputStream);
                    Log.d(TAG, "writeToSink end, actually_end_point:" + mFile.getActuallyLong() + " p:" + pointer + " diff: " + (mFile.getActuallyLong() - pointer));
                }
            }
        }

        private synchronized boolean writeToSink(byte[] buffer, BufferedSink sink) throws IOException {
            if (mFile.getActuallyLong() > 0 && pointer > mFile.getActuallyLong() || getRecordLocalState() == RecordState.STOP_CAPTURE) {
                return true;
            }

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
