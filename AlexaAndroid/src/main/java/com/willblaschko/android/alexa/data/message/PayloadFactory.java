package com.willblaschko.android.alexa.data.message;


import com.willblaschko.android.alexa.data.message.request.audioplayer.AudioPlayerPayload;
import com.willblaschko.android.alexa.data.message.request.context.AlertsStatePayload;
import com.willblaschko.android.alexa.data.message.request.context.PlaybackStatePayload;
import com.willblaschko.android.alexa.data.message.request.context.SpeechStatePayload;
import com.willblaschko.android.alexa.data.message.request.context.VolumeStatePayload;
import com.willblaschko.android.alexa.data.message.request.speaker.MuteChange;
import com.willblaschko.android.alexa.data.message.request.speaker.VolumeChangedPayload;
import com.willblaschko.android.alexa.data.message.request.speechrecognizer.SpeechRecognizerPayload;

import java.util.ArrayList;

/**
 * Created by ggec on 2017/4/6.
 */

public class PayloadFactory {

    public static Payload createPayload(String token) {
        Payload payload = new Payload();
        payload.token = token;
        return payload;
    }

    /**
     *
     * @param profile Accepted values: "CLOSE_TALK", "NEAR_FIELD", "FAR_FIELD"
     * @param format Accepted value: "AUDIO_L16_RATE_16000_CHANNELS_1"
     * @return
     */
    public static SpeechRecognizerPayload createSpeechRecognizerPayload(String profile, String format) {
        SpeechRecognizerPayload payload = new SpeechRecognizerPayload();
        payload.profile = profile;
        payload.format = format;
        return payload;
    }

    public static AudioPlayerPayload createAudioPlayerPayload(String token, long offsetInMilliseconds) {
        AudioPlayerPayload payload = new AudioPlayerPayload();
        payload.token = token;
        payload.offsetInMilliseconds = offsetInMilliseconds;
        return payload;
    }

    public static VolumeStatePayload createVolumeStatePayload(long volume, boolean muted) {
        VolumeStatePayload payload = new VolumeStatePayload();
        payload.volume = volume;
        payload.muted = muted;
        return payload;
    }

    public static SpeechStatePayload createSpeechStatePayload(String token
            , long offsetInMilliseconds
            , String playerActivity) {
        SpeechStatePayload payload = new SpeechStatePayload();
        payload.token = token;
        payload.offsetInMilliseconds = offsetInMilliseconds;
        payload.playerActivity = playerActivity;
        return payload;
    }

    public static PlaybackStatePayload createPlaybackStatePayload(String token
            , long offsetInMilliseconds
            , String playerActivity) {
        PlaybackStatePayload payload = new PlaybackStatePayload();
        payload.token = token;
        payload.offsetInMilliseconds = offsetInMilliseconds;
        payload.playerActivity = playerActivity;
        return payload;
    }

    public static AlertsStatePayload createAlertsStatePayload() {
        AlertsStatePayload payload = new AlertsStatePayload();
        payload.allAlerts = new ArrayList<>();
        payload.activeAlerts = new ArrayList<>();
        return payload;
    }

    public static VolumeChangedPayload createVolumeChangedPayload(boolean mute, long volume){
        VolumeChangedPayload payload = new VolumeChangedPayload();
        payload.volume = volume;
        payload.muted = mute;
        return payload;
    }

    public static MuteChange createSetMutePayload(boolean mute, long volume){
        MuteChange payload = new MuteChange();
        payload.mute = mute;
        payload.volume = volume;
        return payload;
    }
}
