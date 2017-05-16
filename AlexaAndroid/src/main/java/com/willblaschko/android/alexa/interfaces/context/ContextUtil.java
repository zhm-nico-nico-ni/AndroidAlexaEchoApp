package com.willblaschko.android.alexa.interfaces.context;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Pair;

import com.willblaschko.android.alexa.data.Event;
import com.willblaschko.android.alexa.data.message.PayloadFactory;
import com.willblaschko.android.alexa.interfaces.speaker.SpeakerUtil;
import com.willblaschko.android.alexa.keep.AVSAPIConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ggec on 2017/4/12.
 */

public class ContextUtil {
    @Deprecated
    public static List<Event> getContextList(Context context){
        List<Event> list = new ArrayList<>();
        String token = "";

        Event.Builder playbackEventBuilder = new Event.Builder()
                .setHeaderNamespace("AudioPlayer")
                .setHeaderName("PlaybackState")
                .setPayload(PayloadFactory.createPlaybackStatePayload(token,0, "FINISHED"))
                ;
        list.add(playbackEventBuilder.build().getEvent());

        Event.Builder speechSynthesizerEventBuilder = new Event.Builder()
                .setHeaderNamespace("SpeechSynthesizer")
                .setHeaderName("SpeechState")
                .setPayload(PayloadFactory.createSpeechStatePayload(token,0, "FINISHED"))
                ;
        list.add(speechSynthesizerEventBuilder.build().getEvent());

        Event.Builder alertsEventBuilder = new Event.Builder()
                .setHeaderNamespace("Alerts")
                .setHeaderName("AlertsState")
                .setPayload(PayloadFactory.createAlertsStatePayload(context));
        list.add(alertsEventBuilder.build().getEvent());

        Pair<Long, Boolean> p = SpeakerUtil.getConvertVolumeState(context);
        Event.Builder speakerEventBuilder = new Event.Builder()
                .setHeaderNamespace("Speaker")
                .setHeaderName("VolumeState")
                .setPayload(PayloadFactory.createVolumeStatePayload(p.first, p.second));
        list.add(speakerEventBuilder.build().getEvent());

        return list;
    }


    public static List<Event> getActuallyContextList(Context context, @NonNull List<Event> audioAndSpeech){
        List<Event> list = new ArrayList<>();

        list.addAll(audioAndSpeech);

        Event.Builder alertsEventBuilder = new Event.Builder()
                .setHeaderNamespace(AVSAPIConstants.Alerts.NAMESPACE)
                .setHeaderName(AVSAPIConstants.Alerts.Events.AlertsState.NAME)
                .setPayload(PayloadFactory.createAlertsStatePayload(context));
        list.add(alertsEventBuilder.build().getEvent());

        Pair<Long, Boolean> p = SpeakerUtil.getConvertVolumeState(context);
        Event.Builder speakerEventBuilder = new Event.Builder()
                .setHeaderNamespace(AVSAPIConstants.Speaker.NAMESPACE)
                .setHeaderName(AVSAPIConstants.Speaker.Events.VolumeState.NAME)
                .setPayload(PayloadFactory.createVolumeStatePayload(p.first, p.second));
        list.add(speakerEventBuilder.build().getEvent());

        return list;
    }

}
