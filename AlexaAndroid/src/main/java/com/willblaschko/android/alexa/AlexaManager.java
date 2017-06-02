package com.willblaschko.android.alexa;

import android.accounts.AuthenticatorException;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.annotation.NonNull;

import com.ggec.voice.toollibrary.Util;
import com.ggec.voice.toollibrary.log.Log;
import com.willblaschko.android.alexa.callbacks.AsyncCallback;
import com.willblaschko.android.alexa.callbacks.IGetContextEventCallBack;
import com.willblaschko.android.alexa.callbacks.ImplTokenCallback;
import com.willblaschko.android.alexa.data.Event;
import com.willblaschko.android.alexa.data.message.request.speechrecognizer.Initiator;
import com.willblaschko.android.alexa.interfaces.AvsException;
import com.willblaschko.android.alexa.interfaces.AvsResponse;
import com.willblaschko.android.alexa.interfaces.GenericSendEvent;
import com.willblaschko.android.alexa.interfaces.PingSendEvent;
import com.willblaschko.android.alexa.interfaces.context.ContextUtil;
import com.willblaschko.android.alexa.interfaces.errors.AvsResponseException;
import com.willblaschko.android.alexa.interfaces.speechrecognizer.SpeechSendAudio;
import com.willblaschko.android.alexa.interfaces.speechrecognizer.SpeechSendText;
import com.willblaschko.android.alexa.interfaces.system.OpenDownchannel;
import com.willblaschko.android.alexa.requestbody.DataRequestBody;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.RequestBody;
import okio.BufferedSink;

/**
 * The overarching instance that handles all the state when requesting intents to the Alexa Voice Service servers, it creates all the required instances and confirms that users are logged in
 * and authenticated before allowing them to send intents.
 * <p>
 * Beyond initialization, mostly it supplies wrapped helper functions to the other classes to assure authentication state.
 */
public class AlexaManager {

//    static {
//        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
//    }

    private static final String TAG = "AlexaManager";

    private static volatile AlexaManager mInstance;
    private SpeechSendText mSpeechSendText;
    private SpeechSendAudio mSpeechSendAudio;
    private volatile OpenDownchannel openDownchannel;
    private Context mContext;
    private long mLastUserActivityElapsedTime;
    private String mEndPoint;
    private Handler mMainHandler = new Handler(Looper.getMainLooper());

    private AlexaManager(Context context) {
        resetUserInactivityTime();
        mContext = context.getApplicationContext();
        mEndPoint = SharedPreferenceUtil.getEndPointUrl(mContext);
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

    public static synchronized AlexaManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new AlexaManager(context);
        }
        return mInstance;
    }

    private SpeechSendText getSpeechSendText() {
        if (mSpeechSendText == null) {
            mSpeechSendText = new SpeechSendText(){
                @Override
                protected List<Event> getContextStateEvents() {
                    return ContextUtil.getContextList(mContext);
                }
                @Override
                protected Initiator getInitiatorState() {
                    return null;
                }
            };
        }
        return mSpeechSendText;
    }

    private SpeechSendAudio getSpeechSendAudio(@NonNull final IGetContextEventCallBack contextEventCallBack) {
        if (mSpeechSendAudio == null) {
            mSpeechSendAudio = new SpeechSendAudio(){
                @Override
                protected List<Event> getContextStateEvents() {
                    return contextEventCallBack.getContextEvent();
                }

                @Override
                protected Initiator getInitiatorState() {
                    return contextEventCallBack.getInitiator();
                }
            };
        }
        return mSpeechSendAudio;
    }

    public SpeechSendAudio getSpeechSendAudio(){
        return mSpeechSendAudio;
    }

    public void closeOpenDownchannel(final boolean stop) {
        if (openDownchannel != null) {
            Log.i(TAG, "closeOpenDownchannel");
            openDownchannel.closeConnection(stop);
            openDownchannel = null;
        }
    }

    private volatile boolean isSendingOpenDownchannelDirective;
    private volatile AsyncCallback<AvsResponse, Exception> pOpenDownChannelCallback;
    private Runnable retryOpenDownChannel = new Runnable() {
        @Override
        public void run() {
            sendOpenDownchannelDirective(pOpenDownChannelCallback);
        }
    };
    /**
     * Send a get {@link com.willblaschko.android.alexa.data.Directive} request to the Alexa server to open a persistent connection
     */
    public void sendOpenDownchannelDirective(@Nullable AsyncCallback<AvsResponse, Exception> callback) {
        if (openDownchannel != null || isSendingOpenDownchannelDirective) {
            return;
        }
        pOpenDownChannelCallback = callback;
        isSendingOpenDownchannelDirective = true;
        Log.d(TAG, "sendOpenDownchannelDirective begin");

        openDownchannel = new OpenDownchannel(getDirectivesUrl(), pOpenDownChannelCallback);
        //get our access token
        TokenManager.getAccessToken(mContext, new ImplTokenCallback(pOpenDownChannelCallback) {
            @Override
            public void onSuccess(final String token) {
                Observable
                        .fromCallable(new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                isSendingOpenDownchannelDirective = false;
                                if (openDownchannel != null) {
                                    return openDownchannel.connect(token);
                                }
                                return false;
                            }
                        })
                        .onErrorReturn(new Function<Throwable, Boolean>() {
                            @Override
                            public Boolean apply(Throwable throwable) throws Exception {
                                Log.w(TAG, "sendOpenDownchannelDirective error", throwable);
                                return false;
                            }
                        })
                        .observeOn(Schedulers.newThread())
                        .subscribeOn(Schedulers.single())
                        .subscribe(new Consumer<Boolean>() {
                            @Override
                            public void accept(Boolean aBoolean) throws Exception {
                                reconnect(aBoolean);
                            }
                        });
                //do this off the main thread
            }

            @Override
            public void onFailure(Exception e) {
                if(e instanceof AuthenticatorException){
                    Log.d(TAG, "open down channel fail, no auth");
                }
                reconnect(false);
            }

            private void reconnect(boolean canceled){
                isSendingOpenDownchannelDirective = false;
                openDownchannel = null;
                Log.d(TAG, "onPostExecute sendOpenDownchannel Directive :"+canceled);
                if(!Util.isNetworkAvailable(mContext)){
                    canceled = true;
                }
                if (!canceled) {
                    mMainHandler.removeCallbacks(retryOpenDownChannel);
                    mMainHandler.postDelayed(retryOpenDownChannel, 5000);
                }
            }
        });
    }

    /**
     * Send a synchronize state {@link Event} request to Alexa Servers to retrieve pending {@link com.willblaschko.android.alexa.data.Directive}
     * See: {@link #sendEvent(String, AsyncCallback)}
     *
     * @param callback state callback
     */
    public void sendSynchronizeStateEvent2(List<Event> contextEvents,@Nullable final AsyncCallback<AvsResponse, Exception> callback) {
        sendEvent(Event.createSystemSynchronizeStateEvent(contextEvents), callback);
    }

    public boolean hasOpenDownchannel() { //FIXME 这个不需要检查连接是否close的吗？
        return openDownchannel != null;
    }

    /**
     * Send a text string request to the AVS server, this is run through Text-To-Speech to create the raw audio file needed by the AVS server.
     * <p>
     * This allows the developer to pre/post-pend or send any arbitrary text to the server, versus the startRecording()/stopRecording() combination which
     * expects input from the user. This operation, because of the extra steps is generally slower than the above option.
     *
     * @param text     the arbitrary text that we want to send to the AVS server
     * @param callback the state change callback
     */
    public void sendTextRequest(final String text, @Nullable final AsyncCallback<AvsResponse, Exception> callback) {
        final String url = getEventsUrl();
        //do this off the main thread
        new AsyncTask<Void, Void, AvsResponse>() {
            @Override
            protected AvsResponse doInBackground(Void... params) {
                //get our access token
                TokenManager.getAccessToken(mContext, new ImplTokenCallback(callback) {
                    @Override
                    public void onSuccess(String token) {
                        mLastUserActivityElapsedTime = SystemClock.elapsedRealtime();
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

                });
                return null;
            }

        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Send raw audio data to the Alexa servers, this is a more advanced option to bypass other issues (like only one item being able to use the mic at a time).
     *
     * @param data     the audio data that we want to send to the AVS server
     * @param callback the state change callback
     */
    public void sendAudioRequest(final byte[] data, @Nullable final AsyncCallback<AvsResponse, Exception> callback) {
        sendAudioRequest(new DataRequestBody() {
            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                sink.write(data);
            }
        }, callback);
    }
    public void sendAudioRequest(final RequestBody requestBody, @Nullable final AsyncCallback<AvsResponse, Exception> callback){
        sendAudioRequest("CLOSE_TALK", requestBody, callback, new IGetContextEventCallBack() {
            @Override
            public List<Event> getContextEvent() {
                return ContextUtil.getContextList(mContext);
            }

            @Override
            public Initiator getInitiator() {
                return null;
            }
        });
    }
    /**
     * Send streamed raw audio data to the Alexa servers, this is a more advanced option to bypass other issues (like only one item being able to use the mic at a time).
     *
     * @param requestBody a request body that incorporates either a static byte[] write to the BufferedSink or a streamed, managed byte[] data source
     * @param callback    the state change callback
     */
    public void sendAudioRequest(final String profile, final RequestBody requestBody, @Nullable final AsyncCallback<AvsResponse, Exception> callback, @NonNull final IGetContextEventCallBack getContextEventCallBack) {
        //set our URL
        final String url = getEventsUrl();
        //get our access token
        TokenManager.getAccessToken(mContext, new ImplTokenCallback(callback) {
            @Override
            public void onSuccess(final String token) {
                //do this off the main thread
                Observable
                        .fromCallable(new Callable<Object>() {
                            @Override
                            public Object call() throws Exception {
                                mLastUserActivityElapsedTime = SystemClock.elapsedRealtime();
                                try {
                                    getSpeechSendAudio(getContextEventCallBack).sendAudio(profile, url, token, requestBody, new AsyncEventHandler(AlexaManager.this, callback));
                                } catch (AvsResponseException e) {
                                    if (e.isUnAuthorized()) {
                                        com.willblaschko.android.alexa.utility.Util.getPreferences(mContext).edit().remove(TokenManager.PREF_TOKEN_EXPIRES).apply();
                                    }
                                    if (callback != null) {
                                        callback.failure(e);
                                    }
                                } catch (AvsException e) {
                                    //bubble up the error
                                    // report to avs
                                    if (callback != null) {
                                        callback.failure(e);
                                    }
                                }
                                return new Object();
                            }
                        })
                        .onErrorReturn(new Function<Throwable, Object>() {
                            @Override
                            public Object apply(Throwable throwable) throws Exception {
                                return new Object();
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .subscribe();
            }
        });
    }

    public void cancelAudioRequest() {
        if(mSpeechSendAudio != null){
            mSpeechSendAudio.cancelRequest();
        }
    }

    /**
     * Send a generic event to the AVS server, this is generated using {@link com.willblaschko.android.alexa.data.Event.Builder}
     *
     * @param event    the string JSON event
     * @param callback back
     */
    public void sendEvent(@NonNull final String event, final AsyncCallback<AvsResponse, Exception> callback) {
        //set our URL
        final String url = getEventsUrl();
        //get our access token
        TokenManager.getAccessToken(mContext, new ImplTokenCallback(callback) {
            @Override
            public void onSuccess(final String token) {
                //do this off the main thread
                Observable
                        .fromCallable(new Callable<Object>() {
                            @Override
                            public Object call() throws Exception {
                                new GenericSendEvent(url, token, event, callback);
                                return new Object();
                            }
                        })
                        .onErrorReturn(new Function<Throwable, Object>() {
                            @Override
                            public Object apply(Throwable throwable) throws Exception {
                                return new Object();
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .subscribe();
            }
        });
    }

    public void sendPingEvent(final AsyncCallback<AvsResponse, Exception> callback) {
        //check if the user is already logged in
        //set our URL
        final String url = getPingUrl();
        //get our access token
        TokenManager.getAccessToken(mContext, new ImplTokenCallback(callback) {
            @Override
            public void onSuccess(final String token) {
                //do this off the main thread
                Observable
                        .fromCallable(new Callable<AvsResponse>() {
                            @Override
                            public AvsResponse call() throws Exception {
                                return new PingSendEvent(url, token, callback).doWork();
                            }
                        })
                        .onErrorReturn(getErrorConsumer("sendPingEvent", callback))
                        .subscribeOn(Schedulers.io())
                        .subscribe();
            }
        });
    }

    private String getEventsUrl() {
        return mEndPoint + "/" +
                mContext.getString(R.string.alexa_api_version) +
                "/events";
    }

    private String getDirectivesUrl() {
        return mEndPoint + "/" +
                mContext.getString(R.string.alexa_api_version) +
                "/directives";
    }

    private String getPingUrl() {
        return mEndPoint + "/" + "ping";
    }

    public void resetUserInactivityTime (){
        mLastUserActivityElapsedTime = SystemClock.elapsedRealtime();
    }

    public void setEndPoint(String endPoint){
        if(SharedPreferenceUtil.putEndPoint(mContext, endPoint)){
            mEndPoint = endPoint;
            Log.w(TAG, "Set end point success: " + endPoint);
        } else{
            Log.e(TAG, "Set end point fail: " + endPoint);
        }
    }

    public void sendUserInactivityReport(){
        final long second = (SystemClock.elapsedRealtime() - mLastUserActivityElapsedTime )/ 1000;
        if(second<3) return;
        sendEvent(Event.createUserInactivityReportEvent(second), null);
    }

    public void tryRefreshToken(TokenManager.TokenCallback callback){
        TokenManager.tryRefreshToken(mContext, callback);
    }

    private Function<Throwable, AvsResponse> getErrorConsumer(final String method, final AsyncCallback<AvsResponse, Exception> callback){
        return new Function<Throwable, AvsResponse>() {
            @Override
            public AvsResponse apply(Throwable throwable) throws Exception {
                if(null != callback) callback.failure((Exception) throwable);
                else Log.e(TAG, "call method <"+method+"> encounter err", throwable);

                return new AvsResponse();
            }
        };
    }

    private static class AsyncEventHandler implements AsyncCallback<AvsResponse, Exception> {

        AsyncCallback<AvsResponse, Exception> callback;
        AlexaManager manager;

        AsyncEventHandler(AlexaManager manager, AsyncCallback<AvsResponse, Exception> callback) {
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
            manager.mSpeechSendText = null;
        }
    }

}
