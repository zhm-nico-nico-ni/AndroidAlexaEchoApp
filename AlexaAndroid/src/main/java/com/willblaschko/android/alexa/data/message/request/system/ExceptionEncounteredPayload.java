package com.willblaschko.android.alexa.data.message.request.system;

import com.willblaschko.android.alexa.data.message.Payload;

/**
 * Created by ggec on 2017/4/13.
 * ExceptionEncountered Event
 * Your client must send this event when it is unable to execute a directive from AVS.
 * https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/system#exceptionencountered
 */

public class ExceptionEncounteredPayload extends Payload {
    private String unparsedDirective;
    private Error error;

    public ExceptionEncounteredPayload(String unparsedDirective, String type, String msg) {
        this.unparsedDirective = unparsedDirective;
        error = new Error();
        error.type = type;
        error.message = msg;
    }

    private class Error {
        String type;
        String message;
    }
}
