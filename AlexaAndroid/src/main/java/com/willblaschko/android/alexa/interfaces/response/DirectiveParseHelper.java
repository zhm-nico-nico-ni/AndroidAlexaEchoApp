package com.willblaschko.android.alexa.interfaces.response;

import android.support.annotation.NonNull;
import android.util.Log;

import com.willblaschko.android.alexa.AVSAPIConstants;
import com.willblaschko.android.alexa.data.Directive;
import com.willblaschko.android.alexa.interfaces.AvsItem;
import com.willblaschko.android.alexa.interfaces.alerts.AvsDeleteAlertItem;
import com.willblaschko.android.alexa.interfaces.alerts.AvsSetAlertItem;
import com.willblaschko.android.alexa.interfaces.speechsynthesizer.AvsSpeakItem;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;

import static com.willblaschko.android.alexa.interfaces.speechrecognizer.SpeechRecognizerHelper.handleSpeechRecognizerDirective;

/**
 * Created by ggec on 2017/4/10.
 */

public class DirectiveParseHelper {
    private static String TAG = "DirectiveParseHelper";

    public static final AvsItem parseDirective(@NonNull Directive directive, @NonNull HashMap<String, ByteArrayInputStream> audio) throws IOException {
        AvsItem item = null;
        if (AVSAPIConstants.SpeechRecognizer.NAMESPACE.equals(directive.getHeaderNameSpace())){
            item = handleSpeechRecognizerDirective(directive);
        } else if(AVSAPIConstants.SpeechSynthesizer.NAMESPACE.equals(directive.getHeaderNameSpace())) {
            if(directive.isTypeSpeak()){
                String cid = directive.getPayload().getUrl();
                ByteArrayInputStream sound = audio.get(cid);
                item = new AvsSpeakItem(directive.getPayload().getToken(), cid, sound, directive.getHeaderMessageId());
            }
        } else if(AVSAPIConstants.Alerts.NAMESPACE.equals(directive.getHeaderNameSpace())){
            if (directive.isTypeSetAlert()) {
                item = new AvsSetAlertItem(directive.getPayload().getToken()
                        , directive.getPayload().getType(), directive.getPayload().getScheduledTime()
                        , directive.getHeaderMessageId());
            } else if (directive.isTypeDeleteAlert()) {
                item = new AvsDeleteAlertItem(directive.getPayload().getToken(), directive.getHeaderMessageId());
            }
        }

        if(item==null){
            throwUnSupportType(directive);
        }
        return item;
    }



    public static void throwUnSupportType(Directive directive){
//        throw new UnsupportedOperationException("Unknown type found -> "+directive.getHeaderNameSpace()+":"+directive.getHeaderName());
        Log.e(TAG, "Unknown type found -> "+directive.getHeaderNameSpace()+":"+directive.getHeaderName());
    }
}
