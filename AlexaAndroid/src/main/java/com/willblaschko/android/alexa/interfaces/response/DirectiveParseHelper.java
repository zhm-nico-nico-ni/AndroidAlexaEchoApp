package com.willblaschko.android.alexa.interfaces.response;

import android.support.annotation.NonNull;

import com.ggec.voice.toollibrary.log.Log;
import com.willblaschko.android.alexa.data.Directive;
import com.willblaschko.android.alexa.interfaces.AvsItem;
import com.willblaschko.android.alexa.interfaces.AvsResponse;
import com.willblaschko.android.alexa.interfaces.alerts.AvsDeleteAlertItem;
import com.willblaschko.android.alexa.interfaces.alerts.AvsSetAlertItem;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsClearQueueItem;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsPlayAudioItem;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsPlayRemoteItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsReplaceAllItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsReplaceEnqueuedItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsStopItem;
import com.willblaschko.android.alexa.interfaces.speaker.AvsAdjustVolumeItem;
import com.willblaschko.android.alexa.interfaces.speaker.AvsSetMuteItem;
import com.willblaschko.android.alexa.interfaces.speaker.AvsSetVolumeItem;
import com.willblaschko.android.alexa.interfaces.speechrecognizer.AvsExpectSpeechItem;
import com.willblaschko.android.alexa.interfaces.speechrecognizer.AvsStopCaptureItem;
import com.willblaschko.android.alexa.interfaces.speechsynthesizer.AvsSpeakItem;
import com.willblaschko.android.alexa.interfaces.system.AvsResetUserInactivityItem;
import com.willblaschko.android.alexa.interfaces.system.AvsSetEndPointItem;
import com.willblaschko.android.alexa.keep.AVSAPIConstants;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by ggec on 2017/4/10.
 */

public class DirectiveParseHelper {
    private static String TAG = "DirectiveParseHelper";

    public static final AvsItem parseDirective(@NonNull Directive directive, @NonNull HashMap<String, byte[]> audio, AvsResponse response) throws IOException {
        AvsItem item = null;
        final String headNameSpace = directive.getHeaderNameSpace();
        final String headName = directive.getHeaderName();
        if (AVSAPIConstants.SpeechRecognizer.NAMESPACE.equals(headNameSpace)) {
            if (AVSAPIConstants.SpeechRecognizer.Directives.ExpectSpeech.NAME.equals(headName)) {
                return new AvsExpectSpeechItem(directive.getPayload().getToken(), directive.getPayload().getTimeoutInMilliseconds()
                        , directive.getHeaderMessageId(), directive.getPayload().getInitiator());
            } else if (AVSAPIConstants.SpeechRecognizer.Directives.StopCapture.NAME.equals(headName)) {
                return new AvsStopCaptureItem(directive.getHeaderMessageId());
            }
        } else if (AVSAPIConstants.SpeechSynthesizer.NAMESPACE.equals(headNameSpace)) {
            if (AVSAPIConstants.SpeechSynthesizer.Directives.Speak.NAME.equals(headName)) {
                String cid = directive.getPayload().getUrl();
                byte[] sound = audio.get(cid);
                item = new AvsSpeakItem(directive.getPayload().getToken(), cid, sound, directive.getHeaderMessageId(), directive.getPayload().getFormat());
            }
        } else if (AVSAPIConstants.Alerts.NAMESPACE.equals(headNameSpace)) {
            if (AVSAPIConstants.Alerts.Directives.SetAlert.NAME.equals(headName)) {
                item = new AvsSetAlertItem(directive.getPayload().getToken()
                        , directive.getPayload().getType(), directive.getPayload().getScheduledTime()
                        , directive.getHeaderMessageId());
            } else if (AVSAPIConstants.Alerts.Directives.DeleteAlert.NAME.equals(headName)) {
                item = new AvsDeleteAlertItem(directive.getPayload().getToken(), directive.getHeaderMessageId(), directive.getHeaderDialogRequestId());
            }
        } else if (AVSAPIConstants.Speaker.NAMESPACE.equals(headNameSpace)) {
            if (AVSAPIConstants.Speaker.Directives.SetVolume.NAME.equals(headName)) {
                item = new AvsSetVolumeItem(directive.getPayload().getToken(), directive.getPayload().getVolume(), directive.getHeaderMessageId());
            } else if (AVSAPIConstants.Speaker.Directives.AdjustVolume.NAME.equals(headName)) {
                item = new AvsAdjustVolumeItem(directive.getPayload().getToken(), directive.getPayload().getVolume(), directive.getHeaderMessageId());
            } else if (AVSAPIConstants.Speaker.Directives.SetMute.NAME.equals(headName)) {
                item = new AvsSetMuteItem(directive.getPayload().getToken(), directive.getPayload().isMute(), directive.getHeaderMessageId());
            }
        } else if(AVSAPIConstants.System.NAMESPACE.equals(headNameSpace)){
            if (AVSAPIConstants.System.Directives.SetEndpoint.NAME.equals(headName)) {
                item = new AvsSetEndPointItem(directive.getHeaderMessageId(), directive.getPayload().getEndpoint());
            } else if(AVSAPIConstants.System.Directives.ResetUserInactivity.NAME.equals(headName)){
                item = new AvsResetUserInactivityItem(directive.getHeaderMessageId());
            }
        } else if(AVSAPIConstants.AudioPlayer.NAMESPACE.equals(headNameSpace)){
            if (AVSAPIConstants.AudioPlayer.Directives.Play.NAME.equals(headName)) {
                if(directive.isPlayBehaviorReplaceAll()){
                    response.add(0, new AvsReplaceAllItem(directive.getPayload().getToken()));
                }
                if(directive.isPlayBehaviorReplaceEnqueued()){
                    response.add(new AvsReplaceEnqueuedItem(directive.getPayload().getToken()));
                }

                String url = directive.getPayload().getAudioItem().getStream().getUrl();
                if (url.contains("cid:")) {
                    byte[] sound = audio.get(url);
                    item = new AvsPlayAudioItem(directive.getPayload().getToken(), url, sound, directive.getHeaderMessageId()
                            , directive.getPayload().getAudioItem().getStream());
                } else {
                    item = new AvsPlayRemoteItem(directive.getPayload().getToken(), url,
                            directive.getPayload().getAudioItem().getStream().getOffsetInMilliseconds(),
                            directive.getHeaderMessageId(), directive.getPayload().getAudioItem().getStream());
                }
            } else if(AVSAPIConstants.AudioPlayer.Directives.Stop.NAME.equals(headName)){
                response.continueAudio = false;
                item = new AvsStopItem(directive.getPayload().getToken(), directive.getHeaderMessageId());
            } else if(AVSAPIConstants.AudioPlayer.Directives.ClearQueue.NAME.equals(headName)){
                item = new AvsClearQueueItem(directive.getHeaderMessageId(), directive.getPayload().clearBehavior);
            }
        }

        if (item == null) {
            Log.e(TAG, "Unknown type found -> " + headNameSpace + ":" + headName);
        }
        return item;
    }

}
