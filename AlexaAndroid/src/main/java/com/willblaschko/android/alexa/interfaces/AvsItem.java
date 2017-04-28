package com.willblaschko.android.alexa.interfaces;

/**
 * @author wblaschko on 8/13/15.
 */
public abstract class AvsItem {
    public transient final String messageID;
    protected String token;
    public AvsItem(String token, String messageID){
        this.token = token;
        this.messageID = messageID;
    }

    public String getToken() {
        return token;
    }
}
