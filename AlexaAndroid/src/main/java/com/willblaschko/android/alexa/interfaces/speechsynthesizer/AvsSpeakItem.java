package com.willblaschko.android.alexa.interfaces.speechsynthesizer;

import com.willblaschko.android.alexa.interfaces.audioplayer.AvsAudioItem;

/**
 * Directive to play a local, returned audio item from the Alexa post/get response
 *
 * {@link com.willblaschko.android.alexa.data.Directive} response item type parsed so we can properly
 * deal with the incoming commands from the Alexa server.
 *
 * @author will on 5/21/2016.
 */
public class AvsSpeakItem extends AvsAudioItem {
    private final String mCid;
    private final String format;

    public AvsSpeakItem(String token, String cid, String messageID, String format){
        super(token,messageID);
        mCid = cid;
        this.format = format;
    }

    public String getUrl() {
        return mCid;
    }

    public String getFormat(){
        return format;
    }

}
