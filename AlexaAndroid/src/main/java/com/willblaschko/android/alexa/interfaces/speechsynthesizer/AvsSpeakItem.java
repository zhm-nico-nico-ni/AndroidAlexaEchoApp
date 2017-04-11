package com.willblaschko.android.alexa.interfaces.speechsynthesizer;

import com.willblaschko.android.alexa.interfaces.AvsItem;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Directive to play a local, returned audio item from the Alexa post/get response
 *
 * {@link com.willblaschko.android.alexa.data.Directive} response item type parsed so we can properly
 * deal with the incoming commands from the Alexa server.
 *
 * @author will on 5/21/2016.
 */
public class AvsSpeakItem extends AvsItem {
    private String mCid;
    private byte[] mAudio;

    public AvsSpeakItem(String token, String cid, ByteArrayInputStream audio,String messageID) throws IOException {
        this(token, cid, IOUtils.toByteArray(audio),messageID);
        audio.close();
    }

    public AvsSpeakItem(String token, String cid, byte[] audio,String messageID){
        super(token,messageID);
        mCid = cid;
        mAudio = audio;
    }

    public String getCid() {
        return mCid;
    }

    public byte[] getAudio() {
        return mAudio;
    }
}
