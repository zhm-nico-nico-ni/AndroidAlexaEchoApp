package com.willblaschko.android.alexa.interfaces.context;

import android.content.Context;
import android.util.Pair;

import com.willblaschko.android.alexa.data.Event;
import com.willblaschko.android.alexa.data.message.PayloadFactory;
import com.willblaschko.android.alexa.interfaces.speaker.SpeakerUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ggec on 2017/4/12.
 */

public class ContextUtil {
    public static List<Event> getContextList(Context context){// TODO need send actually PlaybackState SpeechSynthesizer context
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


}
