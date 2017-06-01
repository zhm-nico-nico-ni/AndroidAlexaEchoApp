package com.willblaschko.android.alexa.interfaces.speechrecognizer;

import com.willblaschko.android.alexa.interfaces.AvsItem;

/**
 * {@link com.willblaschko.android.alexa.data.Directive} to prompt the user for a speech input
 *
 * {@link com.willblaschko.android.alexa.data.Directive} response item type parsed so we can properly
 * deal with the incoming commands from the Alexa server.
 *
 * @author will on 5/21/2016.
 */
public class AvsExpectSpeechItem extends AvsItem {
    long timeoutInMiliseconds;
    public final String initiator;

//    public AvsExpectSpeechItem(){
//        this(null, 2000,);
//    }

    public AvsExpectSpeechItem(String token, long timeoutInMiliseconds, String messageID, String initiator){
        super(token, messageID);
        this.timeoutInMiliseconds = timeoutInMiliseconds;
        this.initiator = initiator;
    }

    public long getTimeoutInMiliseconds() {
        return timeoutInMiliseconds;
    }
}
