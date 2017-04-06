package com.willblaschko.android.alexa.interfaces;

import android.util.Log;

import com.willblaschko.android.alexa.callbacks.AsyncCallback;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author will on 5/21/2016.
 */
public class PingSendEvent extends SendEvent{

    public static final String TAG = "PingSendEvent";
    private final String url;
    private final String accessToken;
    private final AsyncCallback<AvsResponse, Exception> callback;

    public PingSendEvent(String url, String accessToken,
                         final AsyncCallback<AvsResponse, Exception> callback){
        this.url = url;
        this.accessToken = accessToken;
        this.callback = callback;
    }

    public AvsResponse doWork(){
        if (callback != null){
            callback.start();
        }
        AvsResponse response = null;
        try {
            prepareConnection(url, accessToken);
            response = completeGet();
            if (callback != null) {
                callback.success(response);
            }
            Log.i(TAG, "Ping Event sent");
        } catch (IOException | AvsException e) {
            onError(callback, e);
        }

        if (callback != null) {
            callback.complete();
        }
        return response;
    }

    @Override
    protected void prepareConnection(String url, String accessToken) {

        //set the request URL
        mRequestBuilder.url(url);

        //set our authentication access token header
        mRequestBuilder.addHeader("Authorization", "Bearer " + accessToken);
    }

    @NotNull
    @Override
    public String getEvent() {
        return null;
    }


    public void onError(final AsyncCallback<AvsResponse, Exception> callback, Exception e) {
        if (callback != null) {
            callback.failure(e);
        }
    }
}
