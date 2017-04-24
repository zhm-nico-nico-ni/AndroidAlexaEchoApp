package com.willblaschko.android.alexa.data.message.request.audioplayer;

import com.willblaschko.android.alexa.data.message.Payload;

/**
 * Created by ggec on 2017/4/24.
 */

public class PlaybackFailPayload extends Payload {

    private CurrentPlaybackState currentPlaybackState;
    private PlaybackError error;

    public PlaybackFailPayload(String directiveToken, long offset, String playerActivity, PlaybackError error){
        token = directiveToken;
        currentPlaybackState = new CurrentPlaybackState(directiveToken, offset, playerActivity);
        this.error = error;
    }
}
