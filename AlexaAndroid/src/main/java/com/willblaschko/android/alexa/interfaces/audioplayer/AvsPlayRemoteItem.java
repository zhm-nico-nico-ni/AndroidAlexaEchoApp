package com.willblaschko.android.alexa.interfaces.audioplayer;

/**
 * Directive to play a remote URL item
 *
 * {@link com.willblaschko.android.alexa.data.Directive} response item type parsed so we can properly
 * deal with the incoming commands from the Alexa server.
 *
 * @author will on 5/21/2016.
 */
public class AvsPlayRemoteItem extends AvsAudioItem {
    private String mUrl, mConvertUrl;
    private String mStreamId;
    private long mStartOffset;

    public AvsPlayRemoteItem(String token, String url, long startOffset,String messageID) {
        super(token,messageID);
        mUrl = url;
        mStartOffset = (startOffset < 0) ? 0 : startOffset;
    }
    public String getUrl() {
        return mUrl;
    }

    public String getConvertUrl() {
        return mConvertUrl;
    }

    public void setConvertUrl(String url){
        mConvertUrl = url;
    }

    public long getStartOffset() {
        return mStartOffset;
    }

}
