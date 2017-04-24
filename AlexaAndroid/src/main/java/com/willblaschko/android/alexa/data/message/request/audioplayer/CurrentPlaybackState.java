package com.willblaschko.android.alexa.data.message.request.audioplayer;

/**
 * Created by ggec on 2017/4/24.
 */

class CurrentPlaybackState {
    private String token;
    private long offsetInMilliseconds;
    private String playerActivity;

    public CurrentPlaybackState(String directiveToken, long offset, String playerActivity){
        this.token = directiveToken;
        this.offsetInMilliseconds = offset;
        this.playerActivity = playerActivity;
    }
}
