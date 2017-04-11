package com.willblaschko.android.alexa.interfaces.speechrecognizer;

import com.willblaschko.android.alexa.data.Directive;
import com.willblaschko.android.alexa.interfaces.AvsItem;

import static com.willblaschko.android.alexa.interfaces.response.DirectiveParseHelper.throwUnSupportType;

/**
 * Created by ggec on 2017/4/10.
 */

public class SpeechRecognizerHelper {
    public static AvsItem handleSpeechRecognizerDirective(Directive directive) {
        if (directive.isTypeExpectSpeech()) {
            return new AvsExpectSpeechItem(directive.getPayload().getToken(), directive.getPayload().getTimeoutInMilliseconds(), directive.getHeaderMessageId());
        } else if (directive.isTypeStopCapture()) {
            //TODO
//            {
//                "directive": {
//                "header": {
//                    "namespace": "SpeechRecognizer",
//                            "name": "StopCapture",
//                            "messageId": "{{STRING}}",
//                            "dialogRequestId": "{{STRING}}"
//                },
//                "payload": {
//                }
//            }
//            }

        } else {
            throwUnSupportType(directive);
        }
        return null;
    }

//    public static void ackExpectSpeechTimedOutEvent(@NonNull AlexaManager manager) {
//
//        Event.Builder builder = new Event.Builder();
//        builder.setHeaderNamespace(AVSAPIConstants.SpeechRecognizer.NAMESPACE)
//                .setHeaderName(AVSAPIConstants.SpeechRecognizer.Events.ExpectSpeechTimedOut.NAME)
//                .setHeaderMessageId(getUuid())
//        ;
//
//        manager.sendEvent(builder.toJson(), null);
//    }
}
