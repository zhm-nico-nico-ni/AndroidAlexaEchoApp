

package com.ggec.voice.assistservice.audio.neartalk;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;

import com.example.administrator.appled.LedControl;
import com.ggec.voice.assistservice.MyApplication;
import com.ggec.voice.assistservice.audio.IMyVoiceRecordListener;
import com.ggec.voice.assistservice.audio.SingleAudioRecord;
import com.ggec.voice.assistservice.wakeword.sphinx.frontend.Data;
import com.ggec.voice.assistservice.wakeword.sphinx.frontend.DataEndSignal;
import com.ggec.voice.assistservice.wakeword.sphinx.frontend.DataProcessingException;
import com.ggec.voice.assistservice.wakeword.sphinx.frontend.DataProcessor;
import com.ggec.voice.assistservice.wakeword.sphinx.frontend.DataStartSignal;
import com.ggec.voice.assistservice.wakeword.sphinx.frontend.DoubleData;
import com.ggec.voice.assistservice.wakeword.sphinx.frontend.PropertyException;
import com.ggec.voice.assistservice.wakeword.sphinx.frontend.PropertySheet;
import com.ggec.voice.assistservice.wakeword.sphinx.frontend.endpoint.SpeechClassifier;
import com.ggec.voice.assistservice.wakeword.sphinx.frontend.endpoint.SpeechEndSignal;
import com.ggec.voice.assistservice.wakeword.sphinx.frontend.endpoint.SpeechMarker;
import com.ggec.voice.assistservice.wakeword.sphinx.frontend.endpoint.SpeechStartSignal;
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
import java.util.Arrays;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

/**
 * Created by ggec on 2017/4/25.
 * Near Talk, use stop capture directive
 */

public class NearTalkVoiceRecord2 extends Thread implements DataProcessor {


    private final int MAX_WAIT_TIME = 8 * 1000;
    private final int MAX_RECORD_TIME = 10 * 1000;
    private int MAX_WAIT_END_TIME = 500;

    private final static String TAG = "NearTalkVoiceRecord";

    private NearTalkState mState;
    private final String mFilePath;
    private volatile int recordState = RecordState.EMPTY;
    private volatile int recordHttpState = RecordState.EMPTY;
    private volatile NearTalkRandomAccessFile2 mShareFile;
    private IMyVoiceRecordListener mListener;
    private final long mBeginPosition;

    SpeechClassifier classifier;
    SpeechMarker speechMarker;

    public NearTalkVoiceRecord2(long beginPosition, String filepath, float silenceThreshold, @NonNull IMyVoiceRecordListener listener, int waitTimeOut) throws FileNotFoundException {
        mBeginPosition = beginPosition;
        mListener = listener;
        MAX_WAIT_END_TIME = 0;// waitTimeOut;

        // 这里的最小声音分贝值可以根据先前的输入值来判断
//        continuingSilenceDetector = new SilenceDetector(silenceThreshold, false);

        mFilePath = filepath;
        mShareFile = new NearTalkRandomAccessFile2(mFilePath);

        classifier = new SpeechClassifier(10, 0.003, 13, 0);
        classifier.setPredecessor(this);
         speechMarker = new SpeechMarker(150, 1000, 50);
        speechMarker.setPredecessor(classifier);
        speechMarker.reset();
    }

    boolean handleBreak = false;
    boolean mIsSilent = true;
    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(handleBreak) return;

            Data raw = new DoubleData(SingleAudioRecord.littleEndianBytesToValues((byte[]) msg.obj, 0, msg.arg1, 2, true));
            Data data = classifier.getConvertData(raw);
            speechMarker.handle(data);

            boolean isSilent = !speechMarker.inSpeech();
            long currentTime = SystemClock.elapsedRealtime();

            if (!isSilent && mState.beginSpeakTime == 0) {
                Log.d(TAG, "begin "+ " snr:" + classifier.getSNR());
                mState.beginSpeakTime = currentTime;
            } else {
                if (isSilent != mIsSilent) {

                    if (!mIsSilent) {
                        Log.d(TAG, "record silent time :" + (currentTime - mState.lastSilentTime) + " snr:" + classifier.getSNR());
                        mState.lastSilentTime = currentTime;
                        mState.lastSilentRecordIndex = currentDataPointer;

                    } else {
                        Log.d(TAG, "current is slient, snr:" + classifier.getSNR());
                    }
                    mIsSilent = isSilent;
                }
            }

            if (mState.beginSpeakTime == 0 &&
                    currentTime - mState.initTime >= MAX_WAIT_TIME) {
                Log.d(TAG, "finish: wait to long ");
                setRecordLocalState(RecordState.ERROR);
                handleBreak = true;
                return;
            } else if (isSilent &&
                    mState.lastSilentTime != 0 &&
                    currentTime - mState.lastSilentTime >= MAX_WAIT_END_TIME) {
                setRecordLocalState(RecordState.FINISH);
                Log.d(TAG, "OK: complete after speak ");
                handleBreak = true;
                return;
            }

            if (!mIsSilent && mState.beginSpeakTime > 0) {

                writeDoubleToFile(speechMarker.getOutPutData());
                if (currentTime - mState.beginSpeakTime > MAX_RECORD_TIME) {
                    handleBreak = true;
                    return;
                }

            }
        }
    };

    @Override
    public void run() {
        LedControl.myLedCtl(2);
        long bt = SystemClock.elapsedRealtime();
        if (!SingleAudioRecord.getInstance().isRecording()) {
            SingleAudioRecord.getInstance().startRecording();
            Log.d(TAG, "startRecording use " + (SystemClock.elapsedRealtime() - bt));

        }

        setRecordLocalState(RecordState.START);
        int numberByteRead;
        int bufferSizeInBytes = 320;// 10ms chunk
        byte audioBuffer[] = new byte[bufferSizeInBytes];

        mState = new NearTalkState();
        mState.initTime = SystemClock.elapsedRealtime();


        currentDataPointer = mBeginPosition;
        classifier.getConvertData(new DataStartSignal(16000));
        try {
            // While data come from microphone.
            Log.d(TAG, "init file:" + mFilePath);
            while (SingleAudioRecord.getInstance().isRecording() && !handleBreak) {
//                numberOfReadFloat = audioRecorder.read(audioBuffer, 0, bufferSizeInBytes, AudioRecord.READ_NON_BLOCKING);
                numberByteRead = SingleAudioRecord.getInstance().getAudioRecorder().read(audioBuffer, 0, bufferSizeInBytes);

                if (numberByteRead > 0) {
                    long currentTime = SystemClock.elapsedRealtime();
                    handler.obtainMessage(1, numberByteRead, 0, Arrays.copyOf(audioBuffer, numberByteRead)).sendToTarget();
                    long diff = SystemClock.elapsedRealtime() - currentTime;
                    if(diff > 5)
                    Log.w(TAG, "process use to long !!      " + diff);
                }
            }

            if (mState.beginSpeakTime > 0) {

                speechMarker.handle(new DataEndSignal(SystemClock.elapsedRealtime()-mState.initTime));
                mShareFile.close();
                mShareFile.setActuallyLong(mState.lastSilentRecordIndex);
            } else {
                mShareFile.cancel();
            }

            Log.d(TAG, " important !!!!!!!!!!!!!!!! size:" + currentDataPointer + " act:" + mState.lastSilentRecordIndex);

        } finally {
//            if (mShareFile != null) {
//                IOUtils.closeQuietly(mShareFile);
//            }
        }
        stopRecord(mState.lastSilentRecordIndex);
    }

    long currentDataPointer;
    boolean beginSpeech;
    private void writeDoubleToFile(Data d){
        if(d instanceof SpeechStartSignal){
            beginSpeech = true;
        }else if(d instanceof DoubleData){
            mShareFile.write((DoubleData) d);
            currentDataPointer++;
        } else if(d instanceof SpeechEndSignal || d == null){
            beginSpeech = false;
        }
    }

    private void writeRemain(){
        Log.d(TAG, "write Remain");
        Data data;
        do {
            data = speechMarker.getOutPutData();
            writeDoubleToFile(data);
        }while(beginSpeech && data !=null);
    }

    private void stopRecord(long actuallyLong) {
        SingleAudioRecord.getInstance().stop();
        Log.d(TAG, " record finish, success:" + recordState + " file:" + mFilePath + "\n state:" + mState.toString() + " actuallyLong:" + actuallyLong);
        boolean success = getRecordLocalState() != RecordState.CANCEL;
        if (!success) {
            new File(mFilePath).delete();
            if (getRecordLocalState() == RecordState.ERROR)
                mListener.failure(null, "", -1, null);
        }
    }

    @Override
    public boolean isInterrupted() { // means local audio record is interrupted
        return getRecordLocalState() == RecordState.CANCEL || getRecordLocalState() == RecordState.FINISH || getRecordLocalState() == RecordState.ERROR || getRecordLocalState() == RecordState.STOP_CAPTURE;
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
        if (isInterrupted()) {
            return;
        }

        if (stopAll) { // stop mic record and http
            Log.d(TAG, "NearTalkVoiceRecord # interrupt");
            setRecordLocalState(RecordState.CANCEL);
            setRecordHttpState(RecordState.CANCEL);
        } else { // just stop mic
            Log.d(TAG, "NearTalkVoiceRecord # stop capture");
            setRecordHttpState(RecordState.CANCEL);
            setRecordLocalState(RecordState.STOP_CAPTURE);
        }

        if (!mShareFile.isCanceled() && !mShareFile.isClose()) {
            SingleAudioRecord.getInstance().stop();
        }
        mShareFile.cancel();
    }

    public void doActuallyInterrupt() {
        Log.d(TAG, "NearTalkVoiceRecord # doActuallyInterrupt");

        interrupt(true);
        if (!super.isInterrupted()) super.interrupt();
    }

    public void startHttpRequest(long endIndexInSamples, final AsyncCallback<AvsResponse, Exception> callback, IGetContextEventCallBack getContextEventCallBack) {
        AlexaManager.getInstance(MyApplication.getContext()).sendAudioRequest("NEAR_FIELD"
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

    @Override
    public void initialize() {

    }

    @Override
    public Data getData() throws DataProcessingException {
            throw new DataProcessingException("cannot take Data from audioList");
    }

    @Override
    public DataProcessor getPredecessor() {
        return null;
    }

    @Override
    public void setPredecessor(DataProcessor predecessor) {

    }

    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {

    }

    private class NearTalkFileDataRequestBody extends RequestBody {
        private NearTalkRandomAccessFile2 mFile;
        private long pointer;
        private FileOutputStream mRecordOutputStream;
        private ByteArrayOutputStream mByteArrayStream;

        public NearTalkFileDataRequestBody(final NearTalkRandomAccessFile2 file, long beginPosition) {
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
            if (mFile.isClose() && mByteArrayStream.size() > 0) {
                Log.w(TAG, "writeTo0000:" + mByteArrayStream.size());
                sink.write(mByteArrayStream.toByteArray());
                sink.flush();
            } else {
                //We encourage streaming captured audio to AVS in 10ms chunks at 320 bytes (320 byte DATA frames sent as single units).
                // Larger chunk sizes create unnecessary buffering, which negatively impacts AVS’ ability to process audio and may result in higher latencies.

                Log.d(TAG, "writeTo0 isClose:" + mFile.isClose() + " l:" + mFile.getWriteLength());
                try {
                    while (!mFile.isClose()) {
                        long act = mFile.getActuallyLong();
                        if ((act == 0 && mFile.getWriteLength() > pointer) || pointer < mFile.getActuallyLong()) {
                            if (writeToSink(sink)) {
                                break;
                            }

                        }
                    }

                    Log.d(TAG, "writeTo1 isClose:" + mFile.isClose() + "\n cancel:" + mFile.isCanceled()
                            + " interrupted:" + isInterrupted() + " http:" + getRecordHttpState() + "\n pointer:" + pointer + " act_length:" + mFile.getActuallyLong());
                    if (!mFile.isCanceled() && getRecordLocalState() != RecordState.CANCEL && getRecordLocalState() != RecordState.ERROR
                            && getRecordHttpState() != RecordState.ERROR && getRecordHttpState() != RecordState.CANCEL) {
                        while (pointer < mFile.getActuallyLong()) {
                            if (writeToSink(sink)) {
                                break;
                            }
                        }
                        LedControl.myLedCtl(3);
                        Log.d(TAG, "write remaining end");
                    } else if (mFile.isClose() && mFile.getWriteLength() == 0) {
                        //is cancel here
                        Log.d(TAG, "writeTo1 cancel http!");
                        setRecordHttpState(RecordState.CANCEL);
                        AlexaManager.getInstance(MyApplication.getContext()).cancelAudioRequest();
                    }
                    if (getRecordHttpState() != RecordState.CANCEL && getRecordLocalState() != RecordState.STOP_CAPTURE) {
                        int actl = (int) mFile.getActuallyLong();
                        if (actl > 0 && actl < mByteArrayStream.size()) {
                            Log.d(TAG, "trying change ByteArrayStream size to " + actl);
                            byte[] raw = mByteArrayStream.toByteArray();
                            mByteArrayStream.reset();
                            mByteArrayStream.write(raw, 0, actl);
                            Log.d(TAG, "change ByteArrayStream size to " + mByteArrayStream.size());
                        }
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

        private synchronized boolean writeToSink(BufferedSink sink) throws IOException {
            if (mFile.getActuallyLong() > 0 && pointer > mFile.getActuallyLong() || getRecordLocalState() == RecordState.STOP_CAPTURE) {
                return true;
            }

            DoubleData doubleData = mFile.take();

            if (doubleData != null) {
                double[] array = doubleData.getValues();
                byte[] output = new byte[array.length * 2];
                int y=0;
                for(int i = 0; i < array.length; i++){
                    SingleAudioRecord.littleEndianBytesToValues(array[i], output, y);
                    y+=2;
                }

                sink.write(output);
                mByteArrayStream.write(output);
                if (mRecordOutputStream != null) {
                    mRecordOutputStream.write(output);
                }
                pointer += 1;
                sink.flush();
            }
            return false;
        }

    }
}


