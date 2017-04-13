package com.willblaschko.android.alexa.data.message.request.system;

import com.willblaschko.android.alexa.data.message.Payload;

/**
 * Created by ggec on 2017/4/13.
 * Your client must send this event hourly to report the amount of time elapsed since the last user activity.
 * {@link "https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/system#userinactivityreport"}
 */

public class UserInactivityReportPayload extends Payload {
    public long inactiveTimeInSeconds;
}
