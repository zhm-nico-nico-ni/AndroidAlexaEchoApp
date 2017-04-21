package com.willblaschko.android.alexa.interfaces.audioplayer;

import com.willblaschko.android.alexa.interfaces.AvsItem;

/**
 * Created by ggec on 2017/4/21.
 */

public class AvsAudioItem extends AvsItem {
    public long pausePosition;

    public AvsAudioItem(String token, String messageID) {
        super(token, messageID);
    }
}
