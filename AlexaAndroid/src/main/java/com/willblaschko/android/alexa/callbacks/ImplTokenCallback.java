package com.willblaschko.android.alexa.callbacks;

import android.content.Context;

import com.willblaschko.android.alexa.TokenManager;
import com.willblaschko.android.alexa.interfaces.AvsResponse;

/**
 * Created by ggec on 2017/4/19.
 */

public class ImplTokenCallback implements TokenManager.TokenCallback {
//    private static Handler sHandler = new Handler(Looper.getMainLooper());
//    private static RunRun sRunnable = new RunRun();
    private AsyncCallback<AvsResponse, Exception> mCallback;

    public ImplTokenCallback(AsyncCallback<AvsResponse, Exception> callback) {
        mCallback = callback;
    }

    @Override
    public void onSuccess(String token) {

    }

    @Override
    public void beginRefreshTokenEvent(Context context, long expires_in) {
    }

    @Override
    public void onFailure(Exception e) {
        if(mCallback != null) mCallback.failure(e);
    }

}
