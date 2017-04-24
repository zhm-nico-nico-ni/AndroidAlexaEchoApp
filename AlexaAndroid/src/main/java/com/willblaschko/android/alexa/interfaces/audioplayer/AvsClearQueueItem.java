package com.willblaschko.android.alexa.interfaces.audioplayer;

import com.willblaschko.android.alexa.interfaces.AvsItem;

/**
 * Created by ggec on 2017/4/24.
 */

public class AvsClearQueueItem extends AvsItem {

    private final String clearBehavior;

    public AvsClearQueueItem(String messageID, String clearBehavior) {
        super(null, messageID);
        this.clearBehavior = clearBehavior;
    }

    public boolean isClearAll(){
        return "CLEAR_ALL".equals(clearBehavior);
    }
}
