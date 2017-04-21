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
    private byte[] mAudio;
    private final String format;

    public AvsSpeakItem(String token, String cid, byte[] audio,String messageID, String format){
        super(token,messageID);
        mCid = cid;
        mAudio = audio;
        this.format = format;
    }

    public String getCid() {
        return mCid;
    }

    public byte[] getAudio() {
        return mAudio;
    }

    public String getFormat(){
        return format;
    }
}
