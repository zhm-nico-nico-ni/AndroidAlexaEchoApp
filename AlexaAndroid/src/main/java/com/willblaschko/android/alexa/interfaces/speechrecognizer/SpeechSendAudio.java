package com.willblaschko.android.alexa.interfaces.speechrecognizer;

import com.ggec.voice.toollibrary.log.Log;
import com.willblaschko.android.alexa.callbacks.AsyncCallback;
import com.willblaschko.android.alexa.interfaces.AvsAudioException;
import com.willblaschko.android.alexa.interfaces.AvsException;
import com.willblaschko.android.alexa.interfaces.AvsResponse;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.RequestBody;

/**
 * A subclass of {@link SpeechSendEvent} that sends a RequestBody to the AVS servers, this request body can either be a byte[]
 * straight write, or a threaded write loop based on incoming data (recorded audio).
 *
 * @author will on 4/17/2016.
 */
public abstract class SpeechSendAudio extends SpeechSendEvent {

    private final static String TAG = "SpeechSendAudio";

    long start = 0;
    RequestBody requestBody;

    /**
     * Post an audio byte[] to the Alexa Speech Recognizer API
     * @param url the URL to which we're sending the AVS post
     * @param accessToken our user's access token for the server
     * @param requestBody our OkHttp RequestBody for our mulitpart send, this request body can either be a byte[]
     *                    straight write, or a threaded write loop based on incoming data (recorded audio).
     * @param callback our event callbacks
     * @throws IOException
     */
    public void sendAudio(final String profile, final String url, final String accessToken, @NotNull RequestBody requestBody,
                          final AsyncCallback<AvsResponse, Exception> callback) throws AvsException {
        mCallback = callback;
        mProfile = profile;
        this.requestBody = requestBody;
        if(callback != null){
            callback.start();
        }
        start = System.currentTimeMillis();

        //call the parent class's prepareConnection() in order to prepare our URL POST
        try {
            prepareConnection(url, accessToken);
            final AvsResponse response = completePost();

            if (response == null || (!isSuccessful(response.responseCode) && response.isEmpty())) {
                if (callback != null) {
                    String errorMsg = "Nothing came back:";
                    if(response!=null){
                        errorMsg+=" resCode:"+response.responseCode;
                    }
                    callback.failure(new AvsAudioException(errorMsg));
                }
                return;
            }

            if (callback != null) {
                callback.success(response);
                callback.complete();
            }

            Log.i(TAG, "Audio sending finish, process took: " + (System.currentTimeMillis() - start));
        } catch (IOException e) {
            onError(callback, e);
        }
    }

    public void cancelRequest() {
        cancelCall();
    }

    public void onError(final AsyncCallback<AvsResponse, Exception> callback, Exception e) {
        if(callback != null){
            callback.failure(e);
            callback.complete();
        }
    }

    @NotNull
    @Override
    protected RequestBody getRequestBody() {
        return requestBody;
    }

    /**
     *
     * @param code -1 http cancel, but if set success, wake word detect will not resume
     * @return
     */
    private boolean isSuccessful(int code) {
        return code == -1 || (code >= 200 && code < 300/* && code != 204*/);
    }
}
