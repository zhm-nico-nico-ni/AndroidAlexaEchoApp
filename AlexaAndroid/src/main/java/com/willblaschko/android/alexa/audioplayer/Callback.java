package com.willblaschko.android.alexa.audioplayer;

import com.willblaschko.android.alexa.interfaces.AvsItem;

public interface Callback{
        void playerPrepared(AvsItem pendingItem);
        void playerProgress(AvsItem currentItem, long offsetInMilliseconds, float percent);
        void itemComplete(AvsItem completedItem, boolean error, long offsetInMilliseconds);
        boolean playerError(AvsItem item, int what, int extra);
        void dataError(AvsItem item, Exception e);
    }