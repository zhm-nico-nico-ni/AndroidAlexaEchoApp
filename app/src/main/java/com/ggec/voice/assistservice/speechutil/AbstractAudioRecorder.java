package com.ggec.voice.assistservice.speechutil;

import android.media.AudioFormat;

import com.ggec.voice.assistservice.speechutil.utils.AudioUtils;
import com.ggec.voice.toollibrary.log.Log;

import static com.ggec.voice.toollibrary.log.Log.TAG_APP;

public abstract class AbstractAudioRecorder implements AudioRecorder {
    protected String TAG = "AbstractAudioRecorder";
    private static final int RESOLUTION = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE_MUTLIPLIER = 4; // Buffer size in second

    private SpeechRecord mRecorder = null;

    private double mAvgEnergy = 0;

    private final int mSampleRate;
    private final int mOneSec;

    // Recorder state
    private State mState;

    // The complete space into which the recording in written.
    // Its maximum length is about:
    // 2 (bytes) * 1 (channels) * 30 (max rec time in seconds) * 44100 (times per second) = 2 646 000 bytes
    // but typically is:
    // 2 (bytes) * 1 (channels) * 20 (max rec time in seconds) * 16000 (times per second) = 640 000 bytes
    private final byte[] mRecording;

    // TODO: use: mRecording.length instead
    private int mRecordedLength = 0;

    // The number of bytes the client has already consumed
    private int mConsumedLength = 0;

    // Buffer for output
    private byte[] mBuffer;

    protected AbstractAudioRecorder(int audioSource, int sampleRate) {
        mSampleRate = sampleRate;
        // E.g. 1 second of 16kHz 16-bit mono audio takes 32000 bytes.
        mOneSec = RESOLUTION_IN_BYTES * CHANNELS * mSampleRate;
        // TODO: replace 35 with the max length of the recording
        mRecording = new byte[mOneSec * 35];
    }


    protected void createRecorder(int audioSource, int sampleRate, int bufferSize) {
        mRecorder = new SpeechRecord(audioSource, sampleRate, AudioFormat.CHANNEL_IN_MONO, RESOLUTION, bufferSize, false, false, false);
        if (getSpeechRecordState() != SpeechRecord.STATE_INITIALIZED) {
            throw new IllegalStateException("SpeechRecord initialization failed");
        }
    }

    // TODO: remove
    protected void createBuffer(int framePeriod) {
        mBuffer = new byte[framePeriod * RESOLUTION_IN_BYTES * CHANNELS];
    }

    protected int getBufferSize() {
        return getBufferSize(mSampleRate);
    }

    public static int getBufferSize(int sampleRate) {
        int minBufferSizeInBytes = SpeechRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, RESOLUTION);
        if (minBufferSizeInBytes == SpeechRecord.ERROR_BAD_VALUE) {
            throw new IllegalArgumentException("SpeechRecord.getMinBufferSize: parameters not supported by hardware");
        } else if (minBufferSizeInBytes == SpeechRecord.ERROR) {
            Log.e(TAG_APP, "SpeechRecord.getMinBufferSize: unable to query hardware for output properties");
            minBufferSizeInBytes = sampleRate * (120 / 1000) * RESOLUTION_IN_BYTES * CHANNELS;
        }
        int bufferSize = BUFFER_SIZE_MUTLIPLIER * minBufferSizeInBytes;
        Log.i(TAG_APP, "SpeechRecord buffer size: " + bufferSize + ", min size = " + minBufferSizeInBytes);
        return bufferSize;
    }

    /**
     * Returns the recorded bytes since the last call, and resets the recording.
     *
     * @return bytes that have been recorded since this method was last called
     */
    public synchronized byte[] consumeRecordingAndTruncate() {
        int len = getConsumedLength();
        byte[] bytes = getCurrentRecording(len);
        setRecordedLength(0);
        setConsumedLength(0);
        return bytes;
    }

    protected int getSampleRate() {
        return mSampleRate;
    }


    /**
     * Checking of the read status.
     * The total recording array has been pre-allocated (e.g. for 35 seconds of audio).
     * If it gets full (status == -5) then the recording is stopped.
     */
    protected int getStatus(int numOfBytes, int len) {
        Log.i(TAG, "Read bytes: request/actual: " + len + "/" + numOfBytes);
        if (numOfBytes < 0) {
            Log.e(TAG, "AudioRecord error: " + numOfBytes);
            return numOfBytes;
        }
        if (numOfBytes > len) {
            Log.e(TAG, "Read more bytes than is buffer length:" + numOfBytes + ": " + len);
            return -100;
        } else if (numOfBytes == 0) {
            Log.e(TAG, "Read zero bytes");
            return -200;
        } else if (mRecording.length < mRecordedLength + numOfBytes) {
            Log.e(TAG, "Recorder buffer overflow: " + mRecordedLength);
            return -300;
        }
        return 0;
    }

    float tempFloatBuffer[] = new float[3];
    int tempIndex = 0;
    int totalReadBytes = 0;
    byte totalByteBuffer[] = new byte[60 * 44100 * 2];
    /**
     * Copy data from the given recorder into the given buffer, and append to the complete recording.
     * public int read (byte[] audioData, int offsetInBytes, int sizeInBytes)
     */
    protected int read(SpeechRecord recorder, byte[] buffer) {
        int len = buffer.length;
        int numOfReadBytes = recorder.read(buffer, 0, len);
        int status = getStatus(numOfReadBytes, len);
        if (status == 0 && numOfReadBytes >= 0) {
            // arraycopy(Object src, int srcPos, Object dest, int destPos, int length)
            // numOfBytes <= len, typically == len, but at the end of the recording can be < len.
            System.arraycopy(buffer, 0, mRecording, mRecordedLength, numOfReadBytes);
            mRecordedLength += len;
        }



        //////////////

        /////////////
        return status;
    }


    /**
     * @return recorder state
     */
    public State getState() {
        return mState;
    }

    protected void setState(State state) {
        mState = state;
    }


    /**
     * @return bytes that have been recorded since the beginning
     */
    public byte[] getCompleteRecording() {
        return getCurrentRecording(0);
    }


    /**
     * @return bytes that have been recorded since the beginning, with wav-header
     */
    public byte[] getCompleteRecordingAsWav() {
        return getRecordingAsWav(getCompleteRecording(), mSampleRate);
    }


    public static byte[] getRecordingAsWav(byte[] pcm, int sampleRate) {
        return AudioUtils.getRecordingAsWav(pcm, sampleRate, RESOLUTION_IN_BYTES, CHANNELS);
    }

    /**
     * @return bytes that have been recorded since this method was last called
     */
    public synchronized byte[] consumeRecording() {
        byte[] bytes = getCurrentRecording(mConsumedLength);
        mConsumedLength = mRecordedLength;
        return bytes;
    }

    protected byte[] getCurrentRecording(int startPos) {
        int len = getLength() - startPos;
        byte[] bytes = new byte[len];
        System.arraycopy(mRecording, startPos, bytes, 0, len);
        Log.i(TAG, "Copied from: " + startPos + ": " + bytes.length + " bytes");
        return bytes;
    }

    protected int getConsumedLength() {
        return mConsumedLength;
    }

    protected void setConsumedLength(int len) {
        mConsumedLength = len;
    }

    protected void setRecordedLength(int len) {
        mRecordedLength = len;
    }

    public int getLength() {
        return mRecordedLength;
    }

    public boolean isAllStop(){
        return getState() == State.ERROR
                || getState() == State.STOPPED && mConsumedLength >= mRecordedLength;
    }

    /**
     * @return <code>true</code> iff a speech-ending pause has occurred at the end of the recorded data
     */
    public boolean isPausing() {
        double pauseScore = getPauseScore();
//        Log.i(TAG, "Pause score: " + pauseScore);
        return pauseScore > 7;
    }


    /**
     * @return volume indicator that shows the average volume of the last read buffer
     */
    public float getRmsdb() {
        long sumOfSquares = getRms(mRecordedLength, mBuffer.length);
        double rootMeanSquare = Math.sqrt(sumOfSquares / (mBuffer.length / 2));
        if (rootMeanSquare > 1) {
            // TODO: why 10?
            return (float) (10 * Math.log10(rootMeanSquare));
        }
        return 0;
    }


    /**
     * <p>In order to calculate if the user has stopped speaking we take the
     * data from the last second of the recording, map it to a number
     * and compare this number to the numbers obtained previously. We
     * return a confidence score (0-INF) of a longer pause having occurred in the
     * speech input.</p>
     * <p/>
     * <p>TODO: base the implementation on some well-known technique.</p>
     *
     * @return positive value which the caller can use to determine if there is a pause
     */
    private double getPauseScore() {
        long t2 = getRms(mRecordedLength, mOneSec);
        if (t2 == 0) {
            return 0;
        }
        double t = mAvgEnergy / t2;
        mAvgEnergy = (2 * mAvgEnergy + t2) / 3;
        return t;
    }


    /**
     * <p>Stops the recording (if needed) and releases the resources.
     * The object can no longer be used and the reference should be
     * set to null after a call to release().</p>
     */
    public synchronized void release() {
        if (mRecorder != null) {
            if (mRecorder.getRecordingState() == SpeechRecord.RECORDSTATE_RECORDING) {
                stop();
            }
            mRecorder.release();
            mRecorder = null;
        }
    }

    /**
     * <p>Starts the recording, and sets the state to RECORDING.</p>
     */
    public void start() {
        if (getSpeechRecordState() == SpeechRecord.STATE_INITIALIZED) {
            mRecorder.startRecording();
            if (mRecorder.getRecordingState() == SpeechRecord.RECORDSTATE_RECORDING) {
                setState(State.RECORDING);
                new Thread() {
                    public void run() {
                        recorderLoop(mRecorder);
                    }
                }.start();
            } else {
                handleError("startRecording() failed");
            }
        } else {
            handleError("start() called on illegal state");
        }
    }


    /**
     * <p>Stops the recording, and sets the state to STOPPED.
     * If stopping fails then sets the state to ERROR.</p>
     */
    public void stop() {
        // We check the underlying SpeechRecord state trying to avoid IllegalStateException.
        // If it still occurs then we catch it.
        if (getSpeechRecordState() == SpeechRecord.STATE_INITIALIZED &&
                mRecorder.getRecordingState() == SpeechRecord.RECORDSTATE_RECORDING) {
            try {
                mRecorder.stop();
                setState(State.STOPPED);
            } catch (IllegalStateException e) {
                handleError("native stop() called in illegal state: " + e.getMessage());
            }
        } else {
            handleError("stop() called in illegal state");
        }
    }

    protected void recorderLoop(SpeechRecord recorder) {
        while (recorder.getRecordingState() == SpeechRecord.RECORDSTATE_RECORDING) {
            int status = read(recorder, mBuffer);
            if (status < 0) {
                handleError("status = " + status);
                break;
            }
        }
    }


    private long getRms(int end, int span) {
        int begin = end - span;
        if (begin < 0) {
            begin = 0;
        }
        // make sure begin is even
        if (0 != (begin % 2)) {
            begin++;
        }

        long sum = 0;
        for (int i = begin; i < end; i += 2) {
            short curSample = getShort(mRecording[i], mRecording[i + 1]);
            sum += curSample * curSample;
        }
        return sum;
    }


    /*
     * Converts two bytes to a short (assuming little endian).
     * TODO: We don't need the whole short, just take the 2nd byte (the more significant one)
     * TODO: Most Android devices are little endian?
     */
    private static short getShort(byte argB1, byte argB2) {
        //if (ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN)) {
        //    return (short) ((argB1 << 8) | argB2);
        //}
        return (short) (argB1 | (argB2 << 8));
    }


    protected void handleError(String msg) {
        release();
        setState(State.ERROR);
        Log.e(TAG, msg);
    }

    public int getSpeechRecordState() {
        if (mRecorder == null) {
            return SpeechRecord.STATE_UNINITIALIZED;
        }
        return mRecorder.getState();
    }
}