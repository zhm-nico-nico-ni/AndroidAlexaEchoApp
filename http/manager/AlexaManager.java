package com.ggec.voice.assistservice.http.manager;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;


import com.ggec.voice.assistservice.R;
import com.ggec.voice.assistservice.http.manager.callbacks.AsyncCallback;
import com.ggec.voice.assistservice.http.manager.callbacks.AuthorizationCallback;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.security.Security;

import okio.BufferedSink;

/**
 * The overarching instance that handles all the state when requesting intents to the Alexa Voice Service servers, it creates all the required instances and confirms that users are logged in
 * and authenticated before allowing them to send intents.
 *
 * Beyond initialization, mostly it supplies wrapped helper functions to the other classes to assure authentication state.
 */
public class AlexaManager {

//    static {
//        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
//    }

    private static final String TAG = "AlexaManager";

    private static AlexaManager mInstance;
    private AuthorizationManager mAuthorizationManager;
//    private SpeechSendVoice mSpeechSendVoice;
//    private SpeechSendText mSpeechSendText;
    private SpeechSendAudio mSpeechSendAudio;
//    private OpenDownchannel openDownchannel;
    private Context mContext;
    private boolean mIsRecording = false;

    private AlexaManager(Context context, String productId){
        mContext = context.getApplicationContext();
        mAuthorizationManager = new AuthorizationManager(mContext, productId);
//        new Handler(Looper.getMainLooper()).post(new Runnable() {
//            @Override
//            public void run() {
//                ProviderInstaller.installIfNeededAsync(mContext, providerInstallListener);
//            }
//        });
    }

//    private ProviderInstaller.ProviderInstallListener providerInstallListener = new ProviderInstaller.ProviderInstallListener() {
//        @Override
//        public void onProviderInstalled() {
//            // Provider installed
//        }
//
//        @Override
//        public void onProviderInstallFailed(int errorCode, Intent recoveryIntent) {
//            // Provider installation failed
//        }
//    };

    public static AlexaManager getInstance(Context context, String productId){
        if(mInstance == null){
            mInstance = new AlexaManager(context, productId);
        }
        return mInstance;
    }

//    public SpeechSendVoice getSpeechSendVoice(){
//        if(mSpeechSendVoice == null){
//            mSpeechSendVoice = new SpeechSendVoice();
//        }
//        return mSpeechSendVoice;
//    }
//
//    public SpeechSendText getSpeechSendText(){
//        if(mSpeechSendText == null){
//            mSpeechSendText = new SpeechSendText();
//        }
//        return mSpeechSendText;
//    }

    public SpeechSendAudio getSpeechSendAudio(){
        if(mSpeechSendAudio == null){
            mSpeechSendAudio = new SpeechSendAudio();
        }
        return mSpeechSendAudio;
    }

//    public VoiceHelper getVoiceHelper(){
//        return mVoiceHelper;
//    }

    /**
     * Check if the user is logged in to the Amazon service, uses an async callback with a boolean to return response
     * @param callback state callback
     */
    public void checkLoggedIn(@NotNull final AsyncCallback<Boolean, Throwable> callback){
        mAuthorizationManager.checkLoggedIn(mContext, new AsyncCallback<Boolean, Throwable>() {
            @Override
            public void start() {}

            @Override
            public void success(Boolean result) {
                callback.success(result);
            }

            @Override
            public void failure(Throwable error) {
                callback.failure(error);
            }

            @Override
            public void complete() {}
        });
    }

    /**
     * Send a log in request to the Amazon Authentication Manager
     * @param callback state callback
     */
    public void logIn(@Nullable final AuthorizationCallback callback){
        //check if we're already logged in
        mAuthorizationManager.checkLoggedIn(mContext, new AsyncCallback<Boolean, Throwable>() {
            @Override
            public void start() {

            }

            @Override
            public void success(Boolean result) {
                //if we are, return a success
                if(result){
                    if(callback != null){
                        callback.onSuccess();
                    }
                }else{
                    //otherwise start the authorization process
                    mAuthorizationManager.authorizeUser(callback);
                }
            }

            @Override
            public void failure(Throwable error) {
                if(callback != null) {
                    callback.onError(new Exception(error));
                }
            }

            @Override
            public void complete() {

            }
        });

    }

//    public void closeOpenDownchannel() {
//        new AsyncTask<Void, Void, Void>() {
//            @Override
//            protected Void doInBackground(Void... params) {
//                if (openDownchannel != null) {
//                    openDownchannel.closeConnection();
//                    openDownchannel = null;
//                }
//                return null;
//            }
//            @Override
//            protected void onPostExecute(Void v) {
//                super.onPostExecute(v);
//            }
//        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//    }

    /**
     * Send a get {@link com.willblaschko.android.alexa.data.Directive} request to the Alexa server to open a persistent connection
     */
//    public void sendOpenDownchannelDirective(@Nullable final AsyncCallback<AvsResponse, Exception> callback) {
//        if (openDownchannel != null) {
//            return;
//        }
//
//        //check if the user is already logged in
//        mAuthorizationManager.checkLoggedIn(mContext, new ImplCheckLoggedInCallback() {
//
//            @Override
//            public void success(Boolean result) {
//                if (result) {
//                    //if the user is logged in
//                    openDownchannel = new OpenDownchannel(getDirectivesUrl(), new AsyncEventHandler(AlexaManager.this, callback));
//                    //get our access token
//                    TokenManager.getAccessToken(mAuthorizationManager.getAmazonAuthorizationManager(), mContext, new TokenManager.TokenCallback() {
//                        @Override
//                        public void onSuccess(final String token) {
//                            //do this off the main thread
//                            new AsyncTask<Void, Void, Boolean>() {
//                                @Override
//                                protected Boolean doInBackground(Void... params) {
//                                    try {
//                                        //create a new OpenDownchannel object and send our request
//                                        if (openDownchannel != null) {
//                                            return openDownchannel.connect(token);
//                                        }
//                                    } catch (IOException e) {
//                                        e.printStackTrace();
//                                    }
//                                    return false;
//                                }
//                                @Override
//                                protected void onPostExecute(Boolean canceled) {
//                                    super.onPostExecute(canceled);
//                                    openDownchannel = null;
//                                    if (!canceled) {
//                                        try {
//                                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//                                                @Override
//                                                public void run() {
//                                                    sendOpenDownchannelDirective(callback);
//                                                }
//                                            }, 5000);
//                                        }catch (RuntimeException e){
//                                            e.printStackTrace();
//                                        }
//                                    }
//                                }
//                            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//                        }
//
//                        @Override
//                        public void onFailure(Throwable e) {
//
//                        }
//                    });
//                } else {
//                    //if the user is not logged in, log them in and then call the function again
//                    logIn(new ImplDefaultAuthorizationCallback<AvsResponse>(null) {
//                        @Override
//                        public void onSuccess() {
//                            //call our function again
//                            sendOpenDownchannelDirective(callback);
//                        }
//                    });
//                }
//            }
//
//        });
//    }

    /**
     * Send a synchronize state {@link Event} request to Alexa Servers to retrieve pending {@link com.willblaschko.android.alexa.data.Directive}
     * See: {@link #sendEvent(String, AsyncCallback)}
     * @param callback state callback
     */
    public void sendSynchronizeStateEvent(@Nullable final AsyncCallback<AvsResponse, Exception> callback){
        sendEvent(Event.getSynchronizeStateEvent(), callback);
    }

//    public boolean hasOpenDownchannel() {
//        return openDownchannel != null;
//    }

    /**
     * Helper function to check if we're currently recording
     * @return
     */
    public boolean isRecording(){
        return mIsRecording;
    }

    /**
     * Send a text string request to the AVS server, this is run through Text-To-Speech to create the raw audio file needed by the AVS server.
     *
     * This allows the developer to pre/post-pend or send any arbitrary text to the server, versus the startRecording()/stopRecording() combination which
     * expects input from the user. This operation, because of the extra steps is generally slower than the above option.
     *
     * @param text the arbitrary text that we want to send to the AVS server
     * @param callback the state change callback
     */
    public void sendTextRequest(final String text, @Nullable final AsyncCallback<AvsResponse, Exception> callback){
        //check if the user is already logged in
        mAuthorizationManager.checkLoggedIn(mContext, new ImplCheckLoggedInCallback() {
            @Override
            public void success(Boolean result) {
                if (result) {
                    //if the user is logged in

                    //set our URL
                    final String url = getEventsUrl();
                    //do this off the main thread
                    new AsyncTask<Void, Void, AvsResponse>() {
                        @Override
                        protected AvsResponse doInBackground(Void... params) {
                            //get our access token
                            TokenManager.getAccessToken(mAuthorizationManager.getAmazonAuthorizationManager(), mContext, new TokenManager.TokenCallback() {
                                @Override
                                public void onSuccess(String token) {

                                    try {
                                        getSpeechSendText().sendText(mContext, url, token, text, new AsyncEventHandler(AlexaManager.this, callback));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        //bubble up the error
                                        if(callback != null) {
                                            callback.failure(e);
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Throwable e) {

                                }
                            });
                            return null;
                        }


                        @Override
                        protected void onPostExecute(AvsResponse avsResponse) {
                            super.onPostExecute(avsResponse);
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } else {
                    //if the user is not logged in, log them in and then call the function again
                    logIn(new ImplAuthorizationCallback<AvsResponse>(callback) {

                        @Override
                        public void onSuccess() {
                            //call our function again
                            sendTextRequest(text, callback);
                        }

                    });
                }
            }

        });
    }

    @Deprecated
    public void sendAudioRequest(final int requestType, final byte[] data, final AsyncCallback<AvsResponse, Exception> callback){
        sendAudioRequest(data, callback);
    }

    /**
     * Send raw audio data to the Alexa servers, this is a more advanced option to bypass other issues (like only one item being able to use the mic at a time).
     *
     * @param data the audio data that we want to send to the AVS server
     * @param callback the state change callback
     */
    public void sendAudioRequest(final byte[] data, @Nullable final AsyncCallback<AvsResponse, Exception> callback){
        sendAudioRequest(new DataRequestBody() {
            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                sink.write(data);
            }
        }, callback);
    }

    /**
     * Send streamed raw audio data to the Alexa servers, this is a more advanced option to bypass other issues (like only one item being able to use the mic at a time).
     *
     * @param requestBody a request body that incorporates either a static byte[] write to the BufferedSink or a streamed, managed byte[] data source
     * @param callback the state change callback
     */
    public void sendAudioRequest(final DataRequestBody requestBody, @Nullable final AsyncCallback<AvsResponse, Exception> callback){
        //check if the user is already logged in
        mAuthorizationManager.checkLoggedIn(mContext, new ImplCheckLoggedInCallback() {

            @Override
            public void success(Boolean result) {
                if (result) {
                    //if the user is logged in

                    //set our URL
                    final String url = getEventsUrl();
                    //get our access token
                    TokenManager.getAccessToken(mAuthorizationManager.getAmazonAuthorizationManager(), mContext, new TokenManager.TokenCallback() {
                        @Override
                        public void onSuccess(final String token) {
                            //do this off the main thread
                            new AsyncTask<Void, Void, AvsResponse>() {
                                @Override
                                protected AvsResponse doInBackground(Void... params) {
                                    try {
                                        getSpeechSendAudio().sendAudio(url, token, requestBody, new AsyncEventHandler(AlexaManager.this, callback));
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        //bubble up the error
                                        if(callback != null) {
                                            callback.failure(e);
                                        }
                                    }
                                    return null;
                                }
                                @Override
                                protected void onPostExecute(AvsResponse avsResponse) {
                                    super.onPostExecute(avsResponse);
                                }
                            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        }

                        @Override
                        public void onFailure(Throwable e) {

                        }
                    });
                } else {
                    //if the user is not logged in, log them in and then call the function again
                    logIn(new ImplAuthorizationCallback<AvsResponse>(callback) {
                        @Override
                        public void onSuccess() {
                            //call our function again
                            sendAudioRequest(requestBody, callback);
                        }
                    });
                }
            }

        });
    }

    public void cancelAudioRequest() {
        //check if the user is already logged in
        mAuthorizationManager.checkLoggedIn(mContext, new ImplCheckLoggedInCallback() {
            @Override
            public void success(Boolean result) {
                if (result) {
                    //if the user is logged in
                    getSpeechSendAudio().cancelRequest();
                }
            }

        });
    }

    /** Send a confirmation to the Alexa server that the device volume has been changed in response to a directive
     * See: {@link #sendEvent(String, AsyncCallback)}
     *
     * @param volume volume as reported by the {@link com.willblaschko.android.alexa.interfaces.speaker.AvsAdjustVolumeItem} Directive
     * @param isMute report whether the device is currently muted
     * @param callback state callback
     */
    public void sendVolumeChangedEvent(final long volume, final boolean isMute, @Nullable final AsyncCallback<AvsResponse, Exception> callback){
        sendEvent(Event.getVolumeChangedEvent(volume, isMute), callback);
    }


    /** Send a confirmation to the Alexa server that the mute state has been changed in response to a directive
     * See: {@link #sendEvent(String, AsyncCallback)}
     *
     * @param isMute mute state as reported by the {@link com.willblaschko.android.alexa.interfaces.speaker.AvsSetMuteItem} Directive
     * @param callback
     */
    public void sendMutedEvent(final boolean isMute, @Nullable final AsyncCallback<AvsResponse, Exception> callback){
        sendEvent(Event.getMuteEvent(isMute), callback);
    }

    /**
     * Send confirmation that the device has timed out without receiving a speech request when expected
     * See: {@link #sendEvent(String, AsyncCallback)}
     *
     * @param callback
     */
    public void sendExpectSpeechTimeoutEvent(final AsyncCallback<AvsResponse, Exception> callback){
        sendEvent(Event.getExpectSpeechTimedOutEvent(), callback);
    }

    /**
     * Send an event to indicate that playback of media has nearly completed
     * See: {@link #sendEvent(String, AsyncCallback)}
     *
     * @param item our playback item
     * @param callback
     */


    public void sendPlaybackNearlyFinishedEvent(AvsItem item, final long offsetMilliseconds, final AsyncCallback<AvsResponse, Exception> callback){
        if (item == null || !isAudioPlayItem(item)) {
            return;
        }

        sendEvent(Event.getPlaybackNearlyFinishedEvent(item.getToken(), offsetMilliseconds), callback);
    }

    /**
     * Send an event to indicate that playback of a speech item has started
     * See: {@link #sendEvent(String, AsyncCallback)}
     *
     * @param item our speak item
     * @param callback
     */
    public void sendPlaybackStartedEvent(AvsItem item, final AsyncCallback<AvsResponse, Exception> callback) {
        if (item == null) {
            return;
        }
        String event;
        try {
            if (item instanceof AvsSpeakItem) {
                event = Event.getSpeechStartedEvent(item.getToken());
            } else {
                event = Event.getPlaybackStartedEvent(item.getToken());
            }
        }catch (NullPointerException e){
            if(callback != null) {
                callback.failure(e);
            }
            return;
        }
        sendEvent(event, callback);
    }

    /**
     * Send an event to indicate that playback of a speech item has finished
     * See: {@link #sendEvent(String, AsyncCallback)}
     *
     * @param item our speak item
     * @param callback
     */
    public void sendPlaybackFinishedEvent(AvsItem item, final AsyncCallback<AvsResponse, Exception> callback){
        if (item == null) {
            return;
        }
        String event;
        if (isAudioPlayItem(item)) {
            event = Event.getPlaybackFinishedEvent(item.getToken());
        } else {
            event = Event.getSpeechFinishedEvent(item.getToken());
        }
        sendEvent(event, callback);
    }

    /**
     * Send a generic event to the AVS server, this is generated using {@link com.willblaschko.android.alexa.data.Event.Builder}
     * @param event the string JSON event
     * @param callback
     */
    public void sendEvent(final String event, final AsyncCallback<AvsResponse, Exception> callback){
        //check if the user is already logged in
        mAuthorizationManager.checkLoggedIn(mContext, new ImplCheckLoggedInCallback() {

            @Override
            public void success(Boolean result) {
                if (result) {
                    //if the user is logged in

                    //set our URL
                    final String url = getEventsUrl();
                    //get our access token
                    TokenManager.getAccessToken(mAuthorizationManager.getAmazonAuthorizationManager(), mContext, new TokenManager.TokenCallback() {
                        @Override
                        public void onSuccess(final String token) {
                            //do this off the main thread
                            new AsyncTask<Void, Void, AvsResponse>() {
                                @Override
                                protected AvsResponse doInBackground(Void... params) {
                                    new GenericSendEvent(url, token, event, new AsyncEventHandler(AlexaManager.this, callback));
                                    return null;
                                }
                                @Override
                                protected void onPostExecute(AvsResponse avsResponse) {
                                    super.onPostExecute(avsResponse);
                                }
                            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        }

                        @Override
                        public void onFailure(Throwable e) {

                        }
                    });
                } else {
                    //if the user is not logged in, log them in and then call the function again
                    logIn(new ImplAuthorizationCallback<AvsResponse>(callback) {
                        @Override
                        public void onSuccess() {
                            //call our function again
                            sendEvent(event, callback);
                        }
                    });
                }
            }

        });
    }

    private boolean isAudioPlayItem (AvsItem item) {
        return item != null && (item instanceof AvsPlayAudioItem || !(item instanceof AvsSpeakItem));
    }

    private String getEventsUrl(){
        return new StringBuilder()
                .append(mContext.getString(R.string.alexa_api))
                .append("/")
                .append(mContext.getString(R.string.alexa_api_version))
                .append("/")
                .append("events")
                .toString();
    }

    private String getDirectivesUrl(){
        return new StringBuilder()
                .append(mContext.getString(R.string.alexa_api))
                .append("/")
                .append(mContext.getString(R.string.alexa_api_version))
                .append("/")
                .append("directives")
                .toString();
    }

    private static class AsyncEventHandler implements AsyncCallback<AvsResponse, Exception>{

        AsyncCallback<AvsResponse, Exception> callback;
        AlexaManager manager;

        public AsyncEventHandler(AlexaManager manager, AsyncCallback<AvsResponse, Exception> callback){
            this.callback = callback;
            this.manager = manager;
        }

        @Override
        public void start() {
            if (callback != null) {
                callback.start();
            }
        }

        @Override
        public void success(AvsResponse result) {
            //parse our response
            if (callback != null) {
                callback.success(result);
            }
        }

        @Override
        public void failure(Exception error) {
            //bubble up the error
            if (callback != null) {
                callback.failure(error);
            }
        }

        @Override
        public void complete() {

            if (callback != null) {
                callback.complete();
            }
            manager.mSpeechSendAudio = null;
//            manager.mSpeechSendVoice = null;
//            manager.mSpeechSendText = null;
        }
    }




}
