package com.ggec.voice.assistservice.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.SystemClock;
import android.support.annotation.NonNull;

import com.ggec.voice.assistservice.MyApplication;
import com.ggec.voice.toollibrary.log.Log;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import be.tarsos.dsp.SilenceDetector;
import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;

/**
 * Created by ggec on 2017/4/1.
 */

public class MyVoiceRecord extends Thread {
    public final static float DEFAULT_SILENT_THRESHOLD = -70f;

    private final static String TAG = "MyVoiceRecord";

    public enum RecordState {empty, init, start, stop, interrupt}

    private static final int RECORDER_SAMPLERATE = 16000;
    private static final int RECORDER_BPP = 16;

    private static int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private final int MAX_WAIT_TIME = 10 * 1000;
    private final int MAX_RECORD_TIME = 10 * 1000;
    private final int MAX_WAIT_END_TIME = 1500;

    private final TarsosDSPAudioFormat tarsosDSPAudioFormat;
    private final AudioRecord audioRecorder;
    private final int bufferSizeInBytes;
    private final SilenceDetector continuingSilenceDetector;
    private boolean mIsSilent;
    private State mState;
    private IMyVoiceRecordListener mListener;

    private volatile RecordState recordState = RecordState.empty;
    private String mFilePath;

    public MyVoiceRecord(float silenceThreshold, @NonNull IMyVoiceRecordListener listener) throws IllegalStateException {
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
        mListener = listener;

        String filepath = MyApplication.getContext().getExternalCacheDir().getPath();
        File file = new File(filepath, "AudioRecorder");
        if (!file.exists()) {
            file.mkdirs();
        }

        mFilePath = file.getAbsolutePath() + "/" + System.currentTimeMillis();

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
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(mFilePath);

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
                        Log.d(TAG, "begin "+continuingSilenceDetector.currentSPL());
                        mState.beginSpeakTime = currentTime;
                    } else {
                        if (isSilent != mIsSilent) {

                            if (!mIsSilent) {
                                mState.lastSilentTime = currentTime;
                                mState.lastSilentRecordIndex = currentDataPointer;
                                Log.d(TAG, "record silent time :" + currentTime+ " spl:"+continuingSilenceDetector.currentSPL());
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
                            //write file
                            stream.write(audioBuffer, 0, numberOfReadFloat);
                            currentDataPointer += numberOfReadFloat;

                            if(currentTime - mState.beginSpeakTime > MAX_RECORD_TIME) break;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }

            if(mState.beginSpeakTime > 0){
                stream.close();
////////////part 1  Record pcm
//                    if(BuildConfig.DEBUG) {
//                        byte[] readbuff = new byte[1024];
//                        FileOutputStream stream2 = new FileOutputStream(mFilePath + "-");
//
//                        File res = new File(mFilePath);
//                        FileInputStream inputStream = new FileInputStream(res);
//                        long pointer = 0;
//                        while (inputStream.available() > 0 && pointer <= mState.lastSilentRecordIndex) {
//                            int readCount = inputStream.read(readbuff);
//                            stream2.write(readbuff, 0, readCount);
//                            pointer += readCount;
//                        }
//                        inputStream.close();
//                    }
////////////// end part 1


                ////////////part 2 Record wav
//                    if(BuildConfig.DEBUG) { // Record wav
//                        byte[] readbuff = new byte[1024];
//                        RandomAccessFile stream2 = fopen(mFilePath+".wav");
//
//                        File res = new File(mFilePath);
//                        FileInputStream inputStream = new FileInputStream(res);
//                        long pointer = 0;
//                        while (inputStream.available() > 0 && pointer <= mState.lastSilentRecordIndex) {
//                            int readCount = inputStream.read(readbuff);
//                            fwrite(stream2,readbuff, 0, readCount);
//                            pointer += readCount;
//                        }
//                        fclose(stream2);
//                        inputStream.close();
//                        res.delete();
//                        mFilePath=mFilePath+".wav";
//                        mState.lastSilentRecordIndex = 0;
//                    }
                ////////////end part 2 Record wav
            }

            Log.d("zhm", " important !!!!!!!!!!!!!!!! size:" + currentDataPointer +" act:"+mState.lastSilentRecordIndex);

        } catch (IOException e) {
            e.printStackTrace();
            interrupt();
        } finally {
            if(stream != null){
                IOUtils.closeQuietly(stream);
            }
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
    public boolean isInterrupted() {
        return recordState == MyVoiceRecord.RecordState.interrupt;
    }

    @Override
    public void interrupt() { //warn 这里不能这的调用super的方法，否则只能返回no content
        recordState = RecordState.interrupt;
    }

    public void doActuallyInterrupt(){
        Log.d(TAG, "NearTalkVoiceRecord # doActuallyInterrupt");
//        audioRecorder.stop(); // 这里不应该会到这一步
//        audioRecorder.release();
        interrupt();
        if(!super.isInterrupted()) super.interrupt();
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

    public static class State {
        public long initTime;
        public long beginSpeakTime;
        public long lastSilentTime;
        public long lastSilentRecordIndex;

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

    private RandomAccessFile fopen(String path) throws IOException {
        File f = new File(path);

        if (f.exists()) {
            f.delete();
        } else {
            File parentDir = f.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
        }

        RandomAccessFile file = new RandomAccessFile(f, "rw");
        // 16K、16bit、单声道
    /* RIFF header */
        file.writeBytes("RIFF"); // riff id
        file.writeInt(0); // riff chunk size *PLACEHOLDER*
        file.writeBytes("WAVE"); // wave type

    /* fmt chunk */
        file.writeBytes("fmt "); // fmt id
        file.writeInt(Integer.reverseBytes(16)); // fmt chunk size
        file.writeShort(Short.reverseBytes((short) 1)); // format: 1(PCM)
        file.writeShort(Short.reverseBytes((short) 1)); // channels: 1
        file.writeInt(Integer.reverseBytes(16000)); // samples per second
        file.writeInt(Integer.reverseBytes((int) (1 * 16000 * 16 / 8))); // BPSecond
        file.writeShort(Short.reverseBytes((short) (1 * 16 / 8))); // BPSample
        file.writeShort(Short.reverseBytes((short) (1 * 16))); // bPSample

    /* data chunk */
        file.writeBytes("data"); // data id
        file.writeInt(0); // data chunk size *PLACEHOLDER*

        Log.d(TAG, "wav path: " + path);
        return file;
    }

    private void fwrite(RandomAccessFile file, byte[] data, int offset, int size) throws IOException {
        file.write(data, offset, size);
//        Log.d(TAG, "fwrite: " + size);
    }

    private void fclose(RandomAccessFile file) throws IOException {
        try {
            file.seek(4); // riff chunk size
            file.writeInt(Integer.reverseBytes((int) (file.length() - 8)));

            file.seek(40); // data chunk size
            file.writeInt(Integer.reverseBytes((int) (file.length() - 44)));

            Log.d(TAG, "wav size: " + file.length());

        } finally {
            file.close();
        }
    }
}
