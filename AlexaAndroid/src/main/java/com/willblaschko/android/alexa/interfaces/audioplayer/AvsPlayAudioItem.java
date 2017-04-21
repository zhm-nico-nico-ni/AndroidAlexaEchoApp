package com.willblaschko.android.alexa.interfaces.audioplayer;

import com.willblaschko.android.alexa.data.Directive;
import com.willblaschko.android.alexa.interfaces.speechsynthesizer.AvsSpeakItem;

import java.io.IOException;

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
public class AvsPlayAudioItem extends AvsSpeakItem {
    public Directive.Stream mStream;

    public AvsPlayAudioItem(String token, String cid, byte[] audio, String messageID, Directive.Stream stream) throws IOException {
        super(token, cid, audio, messageID, stream.getStreamFormat());
        mStream = stream;
    }
}
