package com.willblaschko.android.alexa.interfaces.audioplayer;

/**
 * Created by ggec on 2017/4/28.
 */

public interface IAvsPlayDirectiveBaseItem  {
    //Note: When adding streams to your playback queue, you must ensure that the token for the
    // active stream matches the expectedPreviousToken in the stream being added to the queue.
    // If the tokens do not match the stream must be ignored. However,
    // if no expectedPreviousToken is returned, the stream must be added to the queue.
    boolean canAddToQueue();

    String getUrl();
}
