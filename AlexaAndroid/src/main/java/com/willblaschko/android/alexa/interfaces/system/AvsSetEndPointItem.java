package com.willblaschko.android.alexa.interfaces.system;

import com.willblaschko.android.alexa.interfaces.AvsItem;

/**
 * Created by ggec on 2017/4/13.
 * The SetEndpoint directive instructs a client to change endpoints when the following conditions are met:
 * 1) A user’s country settings are not supported by the endpoint they have connected to. For example, if a user’s current country is set to the United Kingdom (UK) in Manage Your Content and Devices and the client connects to the United States (US) endpoint, a SetEndpoint directive will be sent instructing the client to connect to the endpoint that supports the UK.
 * 2) A user changes their country settings (or address). For example, if a user connected to the US endpoint changes their current country from the US to the UK, a SetEndpoint directive will be sent instructing the client to connect to the endpoint that supports the UK.
 * {@link "https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/system#setendpoint"}
 */

public class AvsSetEndPointItem extends AvsItem {

    public String endPoint; //For example: https://avs-alexa-na.amazon.com

    public AvsSetEndPointItem(String messageID, String endPoint) {
        super(null, messageID);
        this.endPoint = endPoint;
    }
}
