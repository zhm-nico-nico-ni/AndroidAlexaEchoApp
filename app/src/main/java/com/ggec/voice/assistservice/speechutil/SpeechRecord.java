package com.ggec.voice.assistservice.speechutil;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;

import com.ggec.voice.assistservice.log.Log;

/**
 * The following takes effect only on Jelly Bean and higher.
 *
 * @author Kaarel Kaljurand
 */
public class SpeechRecord extends AudioRecord {
    private String TAG = "SpeechRecord";

    public SpeechRecord(int sampleRateInHz, int bufferSizeInBytes)
            throws IllegalArgumentException {

        this(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRateInHz,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSizeInBytes,
                false,
                false,
                false
        );
    }


    public SpeechRecord(int sampleRateInHz, int bufferSizeInBytes, boolean noise, boolean gain, boolean echo)
            throws IllegalArgumentException {

        this(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRateInHz,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSizeInBytes,
                noise,
                gain,
                echo
        );
    }


    // This is a copy of the AudioRecord constructor
    public SpeechRecord(int audioSource, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes)
            throws IllegalArgumentException {

        this(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes, false, false, false);
    }


    public SpeechRecord(int audioSource, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes,
                        boolean noise, boolean gain, boolean echo)
            throws IllegalArgumentException {

        super(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            Log.i(TAG, "Trying to enhance audio because running on SDK " + Build.VERSION.SDK_INT);

            int audioSessionId = getAudioSessionId();

            if (noise) {
                if (NoiseSuppressor.create(audioSessionId) == null) {
                    Log.i(TAG, "NoiseSuppressor: failed");
                } else {
                    Log.i(TAG, "NoiseSuppressor: ON");
                }
            } else {
                Log.i(TAG, "NoiseSuppressor: OFF");
            }

            if (gain) {
                if (AutomaticGainControl.create(audioSessionId) == null) {
                    Log.i(TAG, "AutomaticGainControl: failed");
                } else {
                    Log.i(TAG, "AutomaticGainControl: ON");
                }
            } else {
                Log.i(TAG, "AutomaticGainControl: OFF");
            }

            if (echo) {
                if (AcousticEchoCanceler.create(audioSessionId) == null) {
                    Log.i(TAG, "AcousticEchoCanceler: failed");
                } else {
                    Log.i(TAG, "AcousticEchoCanceler: ON");
                }
            } else {
                Log.i(TAG, "AcousticEchoCanceler: OFF");
            }
        }
    }


    public static boolean isNoiseSuppressorAvailable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return NoiseSuppressor.isAvailable();
        }
        return false;
    }
}