package com.willblaschko.android.alexa.interfaces.speaker;

import com.willblaschko.android.alexa.interfaces.AvsItem;

/**
 * Directive to adjust the device volume
 *
 * {@link com.willblaschko.android.alexa.data.Directive} response item type parsed so we can properly
 * deal with the incoming commands from the Alexa server.
 *
 * @author will on 5/21/2016.
 */
public class AvsAdjustVolumeItem extends AvsItem{
    private final long volumeAdjustment;

    /**
     * Create a new AdjustVolume {@link com.willblaschko.android.alexa.data.Directive}
     * @param adjustment The relative volume adjustment.
     *                   A positive or negative long value used to increase or decrease volume in relation to the current volume setting.
     *                   Accepted values: Any long value between -100 and +100
     */
    public AvsAdjustVolumeItem(String token, long adjustment, String messageID){
        super(token, messageID);
        this.volumeAdjustment = adjustment;
    }

    public long getAdjustment() {
        return volumeAdjustment;
    }
}
