package com.willblaschko.android.alexa.audioplayer;

import com.willblaschko.android.alexa.interfaces.AvsItem;

public interface Callback{
        void playerPrepared(AvsItem pendingItem);
        void playerProgress(AvsItem currentItem, long offsetInMilliseconds, float percent, long remain);
        void itemComplete(AvsItem completedItem, boolean error, long offsetInMilliseconds);
        void dataError(AvsItem item, Exception e);
        void onBufferReady(AvsItem item, long offsetInMilliseconds, long stutterDurationInMilliseconds);
        void onBuffering(AvsItem item, long offset);
    }