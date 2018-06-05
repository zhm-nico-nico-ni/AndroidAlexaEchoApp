package com.ggec.voice.assistservice.audio.neartalk.bt;

import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;

import com.example.administrator.appled.LedControl;
import com.ggec.voice.assistservice.MyApplication;
import com.ggec.voice.assistservice.audio.IMyVoiceRecordListener;
import com.ggec.voice.assistservice.audio.neartalk.TalkDataProvider;
import com.ggec.voice.assistservice.audio.record.GGECAudioRecorder;
import com.ggec.voice.toollibrary.log.Log;
import com.willblaschko.android.alexa.AlexaManager;
import com.willblaschko.android.alexa.callbacks.AsyncCallback;
import com.willblaschko.android.alexa.callbacks.IGetContextEventCallBack;
import com.willblaschko.android.alexa.interfaces.AvsResponse;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

/**
 * Created by ggec on 2018/5/16.
 */

public class BtVoiceRecord extends Thread implements GGECAudioRecorder.AudioRecordRawOnData{
    private final static String TAG = "BtVoiceRecord";

    private GGECAudioRecorder mRecord;
    private long currentDataPointer;
    private final String mFilePath;
    private volatile TalkDataProvider mShareFile;
    private long lastSilentRecordIndex;

    public BtVoiceRecord(GGECAudioRecorder rec, long beginPosition, String filepath, @NonNull IMyVoiceRecordListener listener, int waitTimeOut) throws FileNotFoundException {
        mRecord = rec;
        mRecord.setAudioDataListener(this);
        currentDataPointer = beginPosition;
//        mListener = listener;

        mFilePath = filepath;
        mShareFile = new TalkDataProvider(mFilePath);

    }

    @Override
    public void run() {
        /////////// start sco //////
        AudioManager audioManager = (AudioManager) MyApplication.getContext().getSystemService(Context.AUDIO_SERVICE);
//        audioManager.stopBluetoothSco();
        //audioManager.setBluetoothScoOn(true);
        audioManager.startBluetoothSco();
        ////////////////////////////
        LedControl.myLedCtl(LedControl.LISTENING);
        Log.d(TAG, "run done");
    }

    public void startHttpRequest(long endIndexInSamples, final AsyncCallback<AvsResponse, Exception> callback, IGetContextEventCallBack getContextEventCallBack) {
        AlexaManager.getInstance(MyApplication.getContext()).sendAudioRequest("FAR_FIELD"
                , new NearTalkFileDataRequestBody(mShareFile, endIndexInSamples)
                , new AsyncCallback<AvsResponse, Exception>() {
                    @Override
                    public void start() {
//                        setRecordHttpState(RecordState.START);
                        if (callback != null) callback.start();
                    }

                    @Override
                    public void handle(AvsResponse result) {
                        if (callback != null) callback.handle(result);
                    }

                    @Override
                    public void success(AvsResponse result) {
//                        setRecordHttpState(RecordState.FINISH);
                        if (callback != null) callback.success(result);
                    }

                    @Override
                    public void failure(Exception error) {
//                        setRecordHttpState(RecordState.ERROR);
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

    @Override
    public void interrupt() {
//        super.interrupt();
        Log.d(TAG, "some one call interrupt");
    }

    @Override
    public boolean isInterrupted() {
        return mShareFile.isEnd();
    }

    public void stopCapture(boolean stopAll) {
        if(isInterrupted()){
            return;
        }

        mRecord.interruptAll();
        AudioManager audioManager = (AudioManager) MyApplication.getContext().getSystemService(Context.AUDIO_SERVICE);
        if (audioManager.isBluetoothScoOn()) {
            audioManager.setBluetoothScoOn(false);
            audioManager.stopBluetoothSco();
            audioManager.setMode(AudioManager.MODE_NORMAL);
        }


//        if (stopAll) { // stop mic record and http
//            Log.d(TAG, "FarTalkVoiceRecord # stopCapture");
//            setRecordLocalState(RecordState.CANCEL);
//            setRecordHttpState(RecordState.CANCEL);
//        } else { // just stop mic
//            Log.d(TAG, "FarTalkVoiceRecord # stop capture");
//            setRecordLocalState(RecordState.STOP_CAPTURE);
//        }

//        if(!mShareFile.isEnd()) {
//            mRecord.interruptAll();
//        }
        if (stopAll) {
            mShareFile.cancel();
        } else {
            LedControl.myLedCtl(LedControl.THINKING);
        }

        Log.d(TAG, "stopCapture "+currentDataPointer);
    }

    public void doActuallyInterrupt() {
        Log.d(TAG, "# doActuallyInterrupt");

        stopCapture(true);
//        if (!super.isInterrupted()) super.interrupt();
    }

    @Override
    public void onAudioData(byte[] data, int size) {
        try {
            mShareFile.write(data, 0, size);
            lastSilentRecordIndex += size;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onInterrupt() {
        mShareFile.setEnd(lastSilentRecordIndex);
        Log.d(TAG, " onInterrupt: "+mShareFile.getWriteLength()+"  "+lastSilentRecordIndex);
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
            if (mFile.isEnd() && !mFile.isCanceled() && mByteArrayStream.size() > 0) {
                Log.w(TAG, "writeTo0000:" + mByteArrayStream.size());
                sink.write(mByteArrayStream.toByteArray());
                sink.flush();
            } else {
                //We encourage streaming captured audio to AVS in 10ms chunks at 320 bytes (320 byte DATA frames sent as single units).
                // Larger chunk sizes create unnecessary buffering, which negatively impacts AVS’ ability to process audio and may result in higher latencies.
                byte[] buffer = new byte[320];
                Log.d(TAG, "writeTo0 isClose:" + mFile.isEnd() + " l:" + mFile.getWriteLength());

                try {
                    while (!mFile.isEnd()) {
                        long act = mFile.getActuallyLong();
                        /*if(handleStopCapture()) {
                            break;
                        } else*/ if ((act == 0 && mFile.getWriteLength() > pointer) || pointer < mFile.getActuallyLong()) {
                            if (writeToSink(buffer, sink)) {
                                Log.i(TAG, "writeToSink break!!");
                                break;
                            }
                        }
                    }

                    if (mFile.isEnd() && mFile.getWriteLength() <= 0) {
                        //is cancel here
                        Log.d(TAG, "writeTo1 cancel http!");
//                        setRecordHttpState(RecordState.CANCEL);
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
//                    AlexaManager.getInstance(MyApplication.getContext()).getSpeechSendAudio().recordend = System.currentTimeMillis();
                    IOUtils.closeQuietly(mRecordOutputStream);
                    Log.d(TAG, "writeToSink end, actually_end_point:" + mFile.getActuallyLong() + " p:" + pointer + " diff: " + (mFile.getActuallyLong() - pointer));
                }

            }
        }

        private boolean handleStopCapture(){
            return mFile.isEnd();
//            if(getRecordLocalState() == RecordState.STOP_CAPTURE){
//                Log.d(TAG, "writeTo# handleStopCapture");
//                LedControl.myLedCtl(3);
//                return true;
//            } else {
//                return false;
//            }
        }

        private synchronized boolean writeToSink(byte[] buffer, BufferedSink sink) throws IOException {
            if (mFile.getActuallyLong() > 0 && pointer > mFile.getActuallyLong() || mFile.isEnd()) {
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
