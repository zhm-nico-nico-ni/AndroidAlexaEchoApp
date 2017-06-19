package com.willblaschko.android.alexa.interfaces.audioplayer;

import android.text.TextUtils;

import com.willblaschko.android.alexa.data.Directive;
import com.willblaschko.android.alexa.interfaces.speechsynthesizer.AvsSpeakItem;

/**
 * Directive to play a local, returned audio item
 * <p>
 * See: {@link AvsSpeakItem}
 * <p>
 * {@link com.willblaschko.android.alexa.data.Directive} response item type parsed so we can properly
 * deal with the incoming commands from the Alexa server.
 *
 * @author will on 5/21/2016.
 */
public class AvsPlayAudioItem extends AvsSpeakItem implements IAvsPlayDirectiveBaseItem {
    public final Directive.Stream mStream;

    public AvsPlayAudioItem(String token, String cid, String messageID, Directive.Stream stream) {
        super(token, cid, messageID, stream.getStreamFormat());
        mStream = stream;
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
