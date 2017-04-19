package com.willblaschko.android.alexa.callbacks;

import android.content.Context;

import com.willblaschko.android.alexa.TokenManager;

/**
 * Created by ggec on 2017/4/19.
 */

public class ImplTokenCallback implements TokenManager.TokenCallback {
//    private static Handler sHandler = new Handler(Looper.getMainLooper());
//    private static RunRun sRunnable = new RunRun();
    @Override
    public void onSuccess(String token) {

    }

    @Override
    public void beginRefreshTokenEvent(Context context, long expires_in) {
    }

    @Override
    public void onFailure(Throwable e) {

    }

}
