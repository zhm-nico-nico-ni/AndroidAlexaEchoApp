package com.willblaschko.android.alexa.interfaces.audioplayer;

import android.text.TextUtils;

import com.willblaschko.android.alexa.data.Directive;

/**
 * Directive to play a remote URL item
 *
 * {@link com.willblaschko.android.alexa.data.Directive} response item type parsed so we can properly
 * deal with the incoming commands from the Alexa server.
 *
 * @author will on 5/21/2016.
 */
public class AvsPlayRemoteItem extends AvsAudioItem implements IAvsPlayDirectiveBaseItem{
    private String mUrl, mConvertUrl;
//    private String mStreamId;
    private long mStartOffset;
    public final Directive.Stream mStream;

    public AvsPlayRemoteItem(String token, String url, long startOffset,String messageID, Directive.Stream stream) {
        super(token,messageID);
        mUrl = url;
        mStartOffset = (startOffset < 0) ? 0 : startOffset;
        mStream = stream;
    }
    public String getUrl() {
        return mUrl;
    }

    public String getConvertUrl() {
        return mConvertUrl;
//        return "http://pd.npr.org/anon.npr-mp3/npr/news/newscast.mp3";
    }

    public void setConvertUrl(String url){
        mConvertUrl = url;
    }

    public long getStartOffset() {
        return mStartOffset;
    }

    @Override
    public boolean canAddToQueue() {
        if (TextUtils.isEmpty(mStream.getExpectedPreviousToken())) {
            return true;
        } else {
            return TextUtils.equals(mStream.getExpectedPreviousToken(), token);
        }
    }
}
