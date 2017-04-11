package com.willblaschko.android.alexa.interfaces;

import com.google.gson.annotations.Expose;

/**
 * @author wblaschko on 8/13/15.
 */
public abstract class AvsItem {
    @Expose
    public final String messageID;
    String token;
    public AvsItem(String token, String messageID){
        this.token = token;
        this.messageID = messageID;
    }

    public String getToken() {
        return token;
    }
}
