package com.ggec.voice.assistservice.test;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;

import com.ggec.voice.assistservice.log.Log;
import com.ggec.voice.assistservice.speechutil.AbstractAudioRecorder;
import com.ggec.voice.assistservice.speechutil.SpeechRecord;
import com.ggec.voice.assistservice.speechutil.utils.AudioUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;

public class AudioCapture {
    private static AudioCapture sAudioCapture;

    private int sampleRate;
    private AudioBufferThread thread;

    private static final int BUFFER_SIZE_IN_SECONDS = 6;

    private final int BUFFER_SIZE_IN_BYTES;
    private Context mContext;

    public static AudioCapture getAudioHardware(Context context,  final int sampleRate) {
        if (sAudioCapture == null) {
            sAudioCapture = new AudioCapture(context, sampleRate);
        }
        return sAudioCapture;
    }

    private AudioCapture(Context context, final int sampleRate) {
        super();
        mContext = context;
        this.sampleRate = sampleRate;
        BUFFER_SIZE_IN_BYTES = AbstractAudioRecorder.getBufferSize(sampleRate);
//        BUFFER_SIZE_IN_BYTES = (int) ((audioFormat.getSampleSizeInBits() * audioFormat.getSampleRate()) / 8
//                * BUFFER_SIZE_IN_SECONDS);
    }

    public InputStream getAudioInputStream(final RecordingStateListener stateListener,
                                           final RecordingRMSListener rmsListener) throws IOException {
        try {
            startCapture();
            PipedInputStream inputStream = new PipedInputStream(BUFFER_SIZE_IN_BYTES);
            thread = new AudioBufferThread(inputStream, stateListener, rmsListener);
            thread.start();
            return inputStream;
        } catch (IOException e) {
            stopCapture();
            throw e;
        }
    }

    public void stopCapture() {
        if(thread != null){
            thread.stopThreadAudioRecord();

        }
//        microphoneLine.stop();
//        microphoneLine.close();

    }

    private void startCapture() {
//        microphoneLine.open(audioFormat);
//        microphoneLine.start();
    }

    public int getAudioBufferSizeInBytes() {
        return BUFFER_SIZE_IN_BYTES;
    }

    public boolean isPausing() {
        return thread == null || thread.isInterrupted();
    }

    private class AudioBufferThread extends Thread {

        private final AudioStateOutputStream audioStateOutputStream;
        SpeechRecord record;

        public AudioBufferThread(PipedInputStream inputStream,
                                 RecordingStateListener recordingStateListener, RecordingRMSListener rmsListener)
                throws IOException {
//            state = EnumRecordingState.ready;
            record = new SpeechRecord(sampleRate, BUFFER_SIZE_IN_BYTES);
            audioStateOutputStream =
                    new AudioStateOutputStream(inputStream, recordingStateListener, rmsListener);
        }

        @Override
        public void run() {
            try {
                record.startRecording();

                while (!AudioUtils.getMicroPhoneIsMute(mContext)
                        && record.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {// maybe need  AudioUtils.getMicroPhoneIsMute()
                    copyAudioBytesFromInputToOutput();
//                    Log.d(Log.TAG_APP, "" + record.getRmsdb());
                }
            } catch (IllegalStateException e){

            } finally {
                closePipedOutputStream();
            }
        }

        private void copyAudioBytesFromInputToOutput() {
//            byte[] data = new byte[microphoneLine.getBufferSize() / 5];
            byte[] data = new byte[record.getBufferSizeInFrames() / 5];
            int numBytesRead = record.read(data, 0, data.length);
            try {
                audioStateOutputStream.write(data, 0, numBytesRead);
            } catch (IOException e) {
                stopCapture();
            }
        }

        private void closePipedOutputStream() {
            try {
                audioStateOutputStream.close();
                stopThreadAudioRecord();
            } catch (IOException e) {
                Log.e(Log.TAG_APP, "Failed to close audio stream ", e);
            }
        }

        private void stopThreadAudioRecord(){
            if(record != null && record.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING){
                record.stop();
            }
            if (record != null){
                record = null;
            }
        }
    }

}