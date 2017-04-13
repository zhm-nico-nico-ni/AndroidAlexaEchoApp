package com.willblaschko.android.alexa.interfaces.system;

import com.willblaschko.android.alexa.interfaces.AvsItem;

/**
 * Created by ggec on 2017/4/13.
 * The ResetUserInactivity directive is sent to your client to reset the inactivity timer used by UserInactivityReport.
 * For example, a user interaction on the Amazon Alexa app would trigger this directive.
 * {@link "https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/system#resetuserinactivity"}
 */

public class AvsResetUserInactivityItem extends AvsItem {

    public AvsResetUserInactivityItem(String messageID) {
        super(null, messageID);
    }
}
