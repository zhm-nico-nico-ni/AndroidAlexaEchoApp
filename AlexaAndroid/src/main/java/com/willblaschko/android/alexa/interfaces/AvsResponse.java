package com.willblaschko.android.alexa.interfaces;

import com.willblaschko.android.alexa.interfaces.audioplayer.AvsAudioItem;

import java.util.ArrayList;

/**
 * Wrapper for the list of {@link AvsItem} {@link com.willblaschko.android.alexa.data.Directive}s returned from a post/get sent to the
 * Alexa server. In the future this will contain other metadata associated with the returned response.
 */
public class AvsResponse extends ArrayList<AvsItem> {
    public boolean continueWakeWordDetect = true;
    public boolean continueAudio = true;
    public int responseCode;

    public void addOtherAvsResponse(AvsResponse other){
        continueWakeWordDetect = other.continueWakeWordDetect;
        continueAudio = other.continueAudio;
        responseCode = other.responseCode;
        addAll(other);
    }

    public boolean hasSpeechItem(){
        if (isEmpty()){
            return false;
        }else{
            for(AvsItem item : this){
                if(item instanceof AvsAudioItem){
                    return true;
                }
            }

            return false;
        }
    }
}
