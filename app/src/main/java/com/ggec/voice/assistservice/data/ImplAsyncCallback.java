package com.ggec.voice.assistservice.data;

import android.util.Log;

import com.ggec.voice.assistservice.AvsHandleHelper;
import com.willblaschko.android.alexa.callbacks.AsyncCallback;
import com.willblaschko.android.alexa.interfaces.AvsResponse;

/**
 * Implemented version of {@link AsyncCallback} generic
 */
public class ImplAsyncCallback implements AsyncCallback<AvsResponse, Exception> {
    private static final String TAG = "ImplAsyncCallback";
    private final String name;
    private long startTime;

    public ImplAsyncCallback(String tag) {
        this.name = tag;
    }

    @Override
    public void start() {
        startTime = System.currentTimeMillis();
        Log.i(TAG, "Event " + name + " Start");
    }

    @Override
    public void success(AvsResponse result) {
        Log.i(TAG, "Event " + name + " Success " + result);
        AvsHandleHelper.getAvsHandleHelper().handleResponse(result);
    }

    @Override
    public void failure(Exception error) {
        Log.e(TAG, "Event " + name + "  Error", error);
    }

    @Override
    public void complete() {
        long totalTime = System.currentTimeMillis() - startTime;
        Log.i(TAG, "Event " + name + " Complete, " + "Total request time: " + totalTime + " miliseconds");
    }
}
