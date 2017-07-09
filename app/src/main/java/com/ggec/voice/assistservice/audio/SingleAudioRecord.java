package com.ggec.voice.assistservice.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.ggec.voice.toollibrary.log.Log;

/**
 * Created by ggec on 2017/5/24.
 * Only one instance in app, for wake word detect and speech recognize.
 */

public class SingleAudioRecord {
//    private static final float BUFFER_SIZE_SECONDS = 0.4F;
    private static volatile SingleAudioRecord sInstance;
    private final AudioRecord audioRecorder;
    private final int bufferSizeInBytes;

    private SingleAudioRecord(){

        int recorder_sample_rate = 16000;
        int recorder_channels = AudioFormat.CHANNEL_IN_MONO;
        int recorder_audio_encoding = AudioFormat.ENCODING_PCM_16BIT;

//        bufferSizeInBytes = Math.round((float)recorder_sample_rate * BUFFER_SIZE_SECONDS);
        bufferSizeInBytes = 2* AudioRecord.getMinBufferSize(recorder_sample_rate,
                recorder_channels,
                recorder_audio_encoding
        );

        audioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                recorder_sample_rate,
                recorder_channels,
                recorder_audio_encoding,
                bufferSizeInBytes);


        if(this.audioRecorder.getState() == 0) {
            this.audioRecorder.release();
            Log.e("SingleAudioRecord","Failed to initialize recorder. Microphone might be already in use.");
        }

    }

    public static synchronized SingleAudioRecord getInstance(){
        if(sInstance == null){
            sInstance = new SingleAudioRecord();
        }
        return sInstance;
    }

    public void startRecording(){
        isRecording = true;
        if(audioRecorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
            audioRecorder.startRecording();
        }
    }

    public int getState(){
        return audioRecorder.getState();
    }

    public boolean isRecording(){
        return isRecording;
//        return audioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING;
    }

    public AudioRecord getAudioRecorder(){
        return audioRecorder;
    }

    boolean isRecording;
    public void stop(){
        isRecording = false;
        audioRecorder.stop();
        Log.d(Log.TAG_APP, "stop and read " );
    }

    public void release(){
        audioRecorder.release();
    }

    public int getBufferSizeInBytes(){
        return bufferSizeInBytes;
    }


    public static double[] bytesToValues(byte[] byteArray,
                                         int offset,
                                         int length,
                                         int bytesPerValue,
                                         boolean signedData)
            throws ArrayIndexOutOfBoundsException {

        if (0 < length && (offset + length) <= byteArray.length) {
            assert (length % bytesPerValue == 0);
            double[] doubleArray = new double[length / bytesPerValue];

            int i = offset;

            for (int j = 0; j < doubleArray.length; j++) {
                int val = byteArray[i++];
                if (!signedData) {
                    val &= 0xff; // remove the sign extension
                }
                for (int c = 1; c < bytesPerValue; c++) {
                    int temp = byteArray[i++] & 0xff;
                    val = (val << 8) + temp;
                }

                doubleArray[j] = val;
            }

            return doubleArray;
        } else {
            throw new ArrayIndexOutOfBoundsException
                    ("offset: " + offset + ", length: " + length
                            + ", array length: " + byteArray.length);
        }
    }


    /**
     * Converts a little-endian byte array into an array of doubles. Each consecutive bytes of a float are converted
     * into a double, and becomes the next element in the double array. The number of bytes in the double is specified
     * as an argument. The size of the returned array is (data.length/bytesPerValue).
     *
     * @param data          a byte array
     * @param offset        which byte to start from
     * @param length        how many bytes to convert
     * @param bytesPerValue the number of bytes per value
     * @param signedData    whether the data is signed
     * @return a double array, or <code>null</code> if byteArray is of zero length
     * @throws java.lang.ArrayIndexOutOfBoundsException if index goes out of bounds
     *
     */
    public static double[] littleEndianBytesToValues(byte[] data,
                                                     int offset,
                                                     int length,
                                                     int bytesPerValue,
                                                     boolean signedData)
            throws ArrayIndexOutOfBoundsException {

        if (0 < length && (offset + length) <= data.length) {
            assert (length % bytesPerValue == 0);
            double[] doubleArray = new double[length / bytesPerValue];

            int i = offset + bytesPerValue - 1;

            for (int j = 0; j < doubleArray.length; j++) {
                int val = data[i--];
                if (!signedData) {
                    val &= 0xff; // remove the sign extension
                }
                for (int c = 1; c < bytesPerValue; c++) {
                    int temp = data[i--] & 0xff;
                    val = (val << 8) + temp;
                }

                // advance 'i' to the last byte of the next value
                i += (bytesPerValue * 2);

                doubleArray[j] = val;
            }

            return doubleArray;

        } else {
            throw new ArrayIndexOutOfBoundsException
                    ("offset: " + offset + ", length: " + length
                            + ", array length: " + data.length);
        }
    }

    public static byte[] littleEndianBytesToValues(double data)
            throws ArrayIndexOutOfBoundsException {

        byte[] result = new byte[2];
        result[1] = (byte)(( (int) data >> 8) & 0xFF);
        result[0] = (byte) (data);


        return result;
    }

    public static void littleEndianBytesToValues(double data, byte [] out, int index)
            throws ArrayIndexOutOfBoundsException {
        out[index + 1] = (byte)(( (int) data >> 8) & 0xFF);
        out[index] = (byte) (data);
    }
}
