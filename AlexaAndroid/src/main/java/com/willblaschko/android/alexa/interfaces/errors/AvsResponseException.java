package com.willblaschko.android.alexa.interfaces.errors;

import com.willblaschko.android.alexa.data.Directive;
import com.willblaschko.android.alexa.interfaces.AvsException;

/**
 * Created by will on 6/26/2016.
 * https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/exception-messages
 */

public class AvsResponseException extends AvsException {
    Directive directive;

    public AvsResponseException(Directive directive) {
        super();
        this.directive = directive;
    }

    public boolean isUnAuthorized() {
        return "UNAUTHORIZED_REQUEST_EXCEPTION".equals(this.directive.getPayload().getCode());
    }

    public String getMessage() {
        String code = this.directive.getPayload().getCode();
        String description;
        if ("INVALID_REQUEST_EXCEPTION".equals(code)) {
            description = "The request was malformed.";
        } else if ("UNAUTHORIZED_REQUEST_EXCEPTION".equals(code)) {
            description = "The request was not authorized.";
        } else if ("THROTTLING_EXCEPTION".equals(code)) {
            description = "Too many requests to the Alexa Voice Service.";
        } else if ("INTERNAL_SERVICE_EXCEPTION".equals(code)) {
            description = "Internal service exception.";
        } else {
            description = "The Alexa Voice Service is unavailable.";
        }

        return " code:" + code + " code desc:" + description + "\n desc:" + directive.getPayload().getDescription();
    }
}
