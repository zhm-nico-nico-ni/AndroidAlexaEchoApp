package com.willblaschko.android.alexa.data.message.request.audioplayer;

import com.google.android.exoplayer2.ExoPlaybackException;

/**
 * Created by ggec on 2017/4/24.
 */

public class PlaybackError {
    public final String type;
    public final String message;

    public PlaybackError(Exception ex){
        if(ex instanceof ExoPlaybackException){
            type  = "MEDIA_ERROR_UNKNOWN";
        } else { // IOException
            type = "MEDIA_ERROR_INTERNAL_DEVICE_ERROR";
        }

        message = ex.getMessage();
    }
}
