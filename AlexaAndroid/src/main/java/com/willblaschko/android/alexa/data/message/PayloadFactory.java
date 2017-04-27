package com.willblaschko.android.alexa.data.message;


import android.content.Context;

import com.willblaschko.android.alexa.data.message.request.audioplayer.AudioPlayerPayload;
import com.willblaschko.android.alexa.data.message.request.audioplayer.PlaybackError;
import com.willblaschko.android.alexa.data.message.request.audioplayer.PlaybackFailPayload;
import com.willblaschko.android.alexa.data.message.request.audioplayer.PlaybackStutterFinishedPayload;
import com.willblaschko.android.alexa.data.message.request.context.AlertsStatePayload;
import com.willblaschko.android.alexa.data.message.request.context.PlaybackStatePayload;
import com.willblaschko.android.alexa.data.message.request.context.SpeechStatePayload;
import com.willblaschko.android.alexa.data.message.request.context.VolumeStatePayload;
import com.willblaschko.android.alexa.data.message.request.speaker.MuteChange;
import com.willblaschko.android.alexa.data.message.request.speaker.VolumeChangedPayload;
import com.willblaschko.android.alexa.data.message.request.speechrecognizer.SpeechRecognizerPayload;
import com.willblaschko.android.alexa.data.message.request.system.ExceptionEncounteredPayload;
import com.willblaschko.android.alexa.data.message.request.system.UserInactivityReportPayload;
import com.willblaschko.android.alexa.interfaces.alerts.AvsSetAlertItem;
import com.willblaschko.android.alexa.interfaces.alerts.SetAlertHelper;

import java.util.ArrayList;
import java.util.List;

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

    public static PlaybackFailPayload createPlaybackFailPayload(String directiveToken, long offset, String playerActivity, PlaybackError error) {
        return new PlaybackFailPayload(directiveToken, offset, playerActivity, error);
    }

    public static PlaybackStutterFinishedPayload createPlaybackStutterFinishedPayload(String token
            , long offsetInMilliseconds, long stutterDurationInMilliseconds) {
        PlaybackStutterFinishedPayload payload = new PlaybackStutterFinishedPayload();
        payload.token = token;
        payload.offsetInMilliseconds = offsetInMilliseconds;
        payload.stutterDurationInMilliseconds = stutterDurationInMilliseconds;
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

    public static AlertsStatePayload createAlertsStatePayload(Context context) {
        AlertsStatePayload payload = new AlertsStatePayload();
        payload.allAlerts = new ArrayList<>();
        List<AvsSetAlertItem> list =  SetAlertHelper.getAllAlerts(context);
        payload.allAlerts.addAll(list);
        payload.activeAlerts = new ArrayList<>();
        payload.activeAlerts.addAll(list); //TODO what means active
        return payload;
    }

    public static VolumeChangedPayload createVolumeChangedPayload(boolean mute, long volume){
        VolumeChangedPayload payload = new VolumeChangedPayload();
        payload.volume = volume;
        payload.muted = mute;
        return payload;
    }

    public static MuteChange createSetMutePayload(boolean muted, long volume){
        MuteChange payload = new MuteChange();
        payload.muted = muted;
        payload.volume = volume;
        return payload;
    }

    public static ExceptionEncounteredPayload createExceptionEncounteredPayload(String unparsedDirective, String type, String msg){
        return new ExceptionEncounteredPayload(unparsedDirective, type, msg);
    }

    public static UserInactivityReportPayload createUserInactivityReportPayload(long inactiveTimeInSeconds){
        UserInactivityReportPayload payload = new UserInactivityReportPayload();
        payload.inactiveTimeInSeconds = inactiveTimeInSeconds;
        return payload;
    }
}
