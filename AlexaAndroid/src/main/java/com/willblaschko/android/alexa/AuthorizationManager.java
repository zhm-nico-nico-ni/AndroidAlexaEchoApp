package com.willblaschko.android.alexa;

import android.content.Context;

import com.willblaschko.android.alexa.callbacks.AsyncCallback;
import com.willblaschko.android.alexa.callbacks.ImplTokenCallback;

import java.util.Random;

/**
 * A static instance class that manages Authentication with the Amazon servers, it uses the TokenManager helper class to do most of its operations
 * including get new/refresh tokens from the server
 *
 * Some more details here: https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/docs/authorizing-your-alexa-enabled-product-from-a-website
 */
public class AuthorizationManager {

    public static final String CODE_VERIFIER = "code_verifier";

    /**
     * Check if the user is currently logged in by checking for a valid access token (present and not expired).
     * @param context
     * @param callback
     */
    public static void checkLoggedIn(Context context, final AsyncCallback<Boolean, Throwable> callback){

        TokenManager.getAccessToken(context, new ImplTokenCallback() {
            @Override
            public void onSuccess(String token) {
                callback.success(true);
            }

            @Override
            public void onFailure(Throwable e) {
                callback.success(false);
                callback.failure(e);
            }
        });
    }

    /**
     * Create a new code verifier for our token exchanges
     * @return the new code verifier
     */
    public static String createCodeVerifier() {
        char[] chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".toCharArray();
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 128; i++) {
            char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }
        String verifier = sb.toString();
        return verifier;
    }
}
