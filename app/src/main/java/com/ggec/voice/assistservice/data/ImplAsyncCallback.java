package com.ggec.voice.assistservice.data;

import android.os.Handler;
import android.os.Looper;

import com.ggec.voice.assistservice.AvsHandleHelper;
import com.ggec.voice.toollibrary.log.Log;
import com.willblaschko.android.alexa.callbacks.AsyncCallback;
import com.willblaschko.android.alexa.interfaces.AvsResponse;

/**
 * Implemented version of {@link AsyncCallback} generic
 */
public class ImplAsyncCallback implements AsyncCallback<AvsResponse, Exception> {
    private static Handler sMainHandler = new Handler(Looper.getMainLooper());
    private static final String TAG = "ImplAsyncCallback";
    private final String name;
    private long startTime;
    private boolean handled;
    private AvsResponse mHandledResponse;

    public ImplAsyncCallback(String tag) {
        this.name = tag;
    }

    @Override
    public void start() {
        startTime = System.currentTimeMillis();
        Log.i(TAG, "Event " + name + " Start");
    }

    @Override
    public void handle(final AvsResponse result) {
        mHandledResponse = result;
        Log.i(TAG, "Event " + name + " handle");
        handled = true;
        AvsHandleHelper.getAvsHandleHelper().handleResponse(result);
//        sMainHandler.post(new Runnable() {
//            @Override
//            public void run() {
//
//            }
//        });
    }

    @Override
    public void success(final AvsResponse result) {
        Log.i(TAG, "Event " + name + " Success " + result);
        if(!handled)
        sMainHandler.post(new Runnable() {
            @Override
            public void run() {
                AvsHandleHelper.getAvsHandleHelper().handleResponse(result);
            }
        });
    }

    @Override
    public void failure(Exception error) {
        Log.w(TAG, "Event " + name + "  Error", error);
    }

    @Override
    public void complete() {
        long totalTime = System.currentTimeMillis() - startTime;
        Log.i(TAG, "Event " + name + " Complete, " + "Total request time: " + totalTime + " miliseconds");
    }

    public AvsResponse getHandledResponse() {
        return mHandledResponse;
    }
}
