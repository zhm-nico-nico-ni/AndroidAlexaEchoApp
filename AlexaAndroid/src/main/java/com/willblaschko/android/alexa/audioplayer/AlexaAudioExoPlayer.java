package com.willblaschko.android.alexa.audioplayer;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.willblaschko.android.alexa.connection.ClientUtil;
import com.willblaschko.android.alexa.interfaces.AvsItem;
import com.willblaschko.android.alexa.interfaces.alerts.AvsAlertPlayItem;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsPlayAudioItem;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsPlayContentItem;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsPlayRemoteItem;
import com.willblaschko.android.alexa.interfaces.speechsynthesizer.AvsSpeakItem;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * A class that abstracts the Android MediaPlayer and adds additional functionality to handle AvsItems
 * as well as properly handle multiple callbacks--be care not to leak Activities by not removing the callback
 */
public class AlexaAudioExoPlayer implements MyExoPlayer.IMyExoPlayerListener {

    public static final String TAG = "AlexaAudioExoPlayer";

    private final static int STATE_IDLE = 0;
    private final static int STATE_PLAYING = 1;
    private final static int STATE_STOPPED = 2;
    private final static int STATE_PAUSED = 3;
    private final static int STATE_BUFFER_UNDERRUN = 4;
    private final static int STATE_FINISHED = 5;

    private MyExoPlayer mMediaPlayer;
    private Context mContext;
    private AvsItem mItem;
    private final List<Callback> mCallbacks = new ArrayList<>();
    private int mMediaState = STATE_IDLE;
    private Call mAvsRemoteCall;

    /**
     * Create our new AlexaAudioPlayer
     *
     * @param context any context, we will get the application level to store locally
     */
    public AlexaAudioExoPlayer(Context context) {
        mContext = context.getApplicationContext();
        trimCache(context);
    }

    private static void trimCache(Context context) {
        try {
            File dir = context.getCacheDir();
            if (dir != null && dir.isDirectory()) {
                deleteDir(dir);
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    private static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        if (dir != null) {
            // The directory is now empty so delete it
            return dir.delete();
        }
        return false;
    }

    /**
     * Return a reference to the MediaPlayer instance, if it does not exist,
     * then create it and configure it to our needs
     *
     * @return Android native MediaPlayer
     */
    private MyExoPlayer getMediaPlayer() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MyExoPlayer(mContext, this);

//            mMediaPlayer.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);
//            mMediaPlayer.setPlayWhenReady(true);
        }

        return mMediaPlayer;
    }

    /**
     * Add a callback to our AlexaAudioPlayer, this is added to our list of callbacks
     *
     * @param callback Callback that listens to changes of player state
     */
    public void addCallback(Callback callback) {
        synchronized (mCallbacks) {
            if (!mCallbacks.contains(callback)) {
                mCallbacks.add(callback);
            }
        }
    }

    @Nullable
    public AvsItem getCurrentItem() {
        return mItem;
    }

    public String getCurrentToken() {
        return mItem == null ? "" :
                (TextUtils.isEmpty(mItem.getToken()) ? "" : mItem.getToken());
    }

//    /**
//     * Remove a callback from our AlexaAudioPlayer, this is removed from our list of callbacks
//     * @param callback Callback that listens to changes of player state
//     */
//    public void removeCallback(Callback callback){
//        synchronized (mCallbacks) {
//            mCallbacks.remove(callback);
//        }
//    }

    /**
     * A helper function to play an AvsPlayContentItem, this is passed to play() and handled accordingly,
     *
     * @param item a speak type item
     */
    public void playItem(AvsPlayContentItem item) {
        play(item);
    }

    /**
     * A helper function to play an AvsSpeakItem, this is passed to play() and handled accordingly,
     *
     * @param item a speak type item
     */
    public void playItem(AvsSpeakItem item) {
        play(item);
    }

    /**
     * A helper function to play an AvsPlayRemoteItem, this is passed to play() and handled accordingly,
     *
     * @param item a play type item, usually a url
     */
    public void playItem(AvsPlayRemoteItem item) {
        play(item);
    }

    public void playItem(AvsAlertPlayItem item) {
        play(item);
    }

    /**
     * Request our MediaPlayer to play an item, if it's an AvsPlayRemoteItem (url, usually), we set that url as our data source for the MediaPlayer
     * if it's an AvsSpeakItem, then we write the raw audio to a file and then read it back using the MediaPlayer
     *
     * @param item
     */
    private void play(@NonNull AvsItem item) {
        if (mItem == item && mAvsRemoteCall != null && mAvsRemoteCall.isExecuted()) {
            Log.w(TAG, "play the same item");
            return;
        }
        mItem = item;
        //if we're playing, stop playing before we continue
        getMediaPlayer().stop();

        if (!TextUtils.isEmpty(mItem.getToken()) && mItem.getToken().contains("PausePrompt")) {
            Log.e(TAG, "what happen ? token:" + mItem.getToken());
            //a gross work around for a broke pause mp3 coming from Amazon, play the local mp3
            getMediaPlayer().prepare(buildMediaSource(Uri.parse("asset:///start.mp3"), "mp3"));
        } else if (mItem instanceof AvsPlayRemoteItem) {
            cancelAvsRemoteCallRequest();
            final AvsPlayRemoteItem playItem = (AvsPlayRemoteItem) item;
            final long startOffset = playItem.pausePosition > playItem.getStartOffset()
                    ? playItem.pausePosition : playItem.getStartOffset();
            //play new url
//            getMediaPlayer().prepare(buildMediaSource(Uri.parse(playItem.getUrl()), null), null, startOffset);
            Log.d(TAG, "play remote item, url -> " + playItem.getUrl() + "\n convert -> " + playItem.getConvertUrl());
            if(TextUtils.isEmpty(playItem.getConvertUrl())) {
                Flowable.fromCallable(new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        String playUri;
                        final HttpUrl urll = HttpUrl.parse(playItem.getUrl());
                        OkHttpClient okHttpClient;
                        if(urll.isHttps()){
                            okHttpClient = ClientUtil.getHttp2Client();
                        } else {
                            okHttpClient = new OkHttpClient.Builder()
                                    .readTimeout(12, TimeUnit.SECONDS)
                                    .writeTimeout(12, TimeUnit.SECONDS)
                                    .connectTimeout(15, TimeUnit.SECONDS)
                                    .retryOnConnectionFailure(true)
                                    .addNetworkInterceptor(new StethoInterceptor()).build();
                        }

                        try {
                            mAvsRemoteCall = okHttpClient
                                    .newCall(new Request.Builder()
                                            .url(urll)
                                            .build());
                            Response response = mAvsRemoteCall.execute();
                            long responseLong = response.body().contentLength();
                            if (responseLong > 0 && responseLong < 1024 * 3) {
                                String bodyString = response.body().string();
                                Log.i(TAG, "get remote result: " + bodyString);
                                HttpUrl maybeUrl = HttpUrl.parse(bodyString.trim());
                                if(maybeUrl!= null && !TextUtils.isEmpty(maybeUrl.scheme()) && maybeUrl.scheme().contains("http")){
                                    playItem.setConvertUrl(bodyString.trim());
                                    playUri = playItem.getConvertUrl();
                                } else {
                                    playUri = playItem.getUrl();
                                }
                            } else {
                                playUri = playItem.getUrl();
                            }
                        } catch (Exception ex){
                            ex.printStackTrace();
                            playUri = playItem.getUrl();
                        }

                        if(playUri==null) playUri = "";
                        if(mAvsRemoteCall!= null) {
                            return !mAvsRemoteCall.isCanceled() ? playUri : "";
                        } else {
                            return "";
                        }
                    }
                }).subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.single())
                        .subscribe(new Consumer<String>() {
                            @Override
                            public void accept(@io.reactivex.annotations.NonNull String s) throws Exception {
                                if (!TextUtils.isEmpty(s)) {
                                    getMediaPlayer().prepare(buildMediaSource(Uri.parse(s), null), null, startOffset);
                                }
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(@io.reactivex.annotations.NonNull Throwable throwable) throws Exception {
                                bubbleUpError((Exception) throwable);
                            }
                        });
            } else {
                getMediaPlayer().prepare(buildMediaSource(Uri.parse(playItem.getConvertUrl()), null), null, startOffset);
            }

        } else if (mItem instanceof AvsPlayContentItem) {
            AvsPlayContentItem playItem = (AvsPlayContentItem) item;
            getMediaPlayer().prepare(buildMediaSource(playItem.getUri(), null));
        } else if (mItem instanceof AvsPlayAudioItem) {
            AvsPlayAudioItem playAudioItem = (AvsPlayAudioItem) mItem;
            long offset = playAudioItem.mStream.getOffsetInMilliseconds();
            long startOffset = playAudioItem.pausePosition > offset
                    ? playAudioItem.pausePosition : offset;
            playitem(playAudioItem, startOffset);
        } else if (mItem instanceof AvsSpeakItem) {
            playitem((AvsSpeakItem) mItem, 0);
        } else if (mItem instanceof AvsAlertPlayItem) {
            Uri path = Uri.parse("asset:///alarm.mp3");
            getMediaPlayer().prepare(buildMediaSource(path, "mp3"));
        } else {

            bubbleUpError(new Exception("not suitable process"));

        }
    }

    private MediaSource buildMediaSource(Uri uri, String overrideExtension) {
        int type = TextUtils.isEmpty(overrideExtension) ? Util.inferContentType(uri)
                : Util.inferContentType("." + overrideExtension);
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(mContext, "GGEC");
        switch (type) {
            case C.TYPE_SS:
                return new SsMediaSource(uri, dataSourceFactory,
                        new DefaultSsChunkSource.Factory(dataSourceFactory), null, null);
            case C.TYPE_DASH:
                return new DashMediaSource(uri, dataSourceFactory,
                        new DefaultDashChunkSource.Factory(dataSourceFactory), null, null);
            case C.TYPE_HLS:
                return new HlsMediaSource(uri, dataSourceFactory, null, null);
            case C.TYPE_OTHER:
                return new ExtractorMediaSource(uri, dataSourceFactory, new DefaultExtractorsFactory(),
                        null, null);
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }

    /**
     * Check whether our MediaPlayer is currently playing
     *
     * @return true playing, false not
     */
    public boolean isPlaying() {
        return getMediaPlayer().isPlaying();
    }

    /**
     * A helper function to pause the MediaPlayer
     */
    public void pause() {
        getMediaPlayer().setPlayWhenReady(false);
    }

    /**
     * A helper function to play the MediaPlayer
     */
    public void play() {
        getMediaPlayer().setPlayWhenReady(true);
    }

    /**
     * A helper function to stop the MediaPlayer
     */
    public long stop(boolean setStatePause) {
        cancelAvsRemoteCallRequest();
        mMediaState = setStatePause ? STATE_PAUSED : STATE_STOPPED;
        return getMediaPlayer().stop();
    }

    /**
     * A helper function to release the media player and remove it from memory
     */
    public void release(boolean reportComplete) {
        cancelAvsRemoteCallRequest();
        long duration = 0;
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            duration = mMediaPlayer.getDuration();
            if (duration == C.TIME_UNSET) {
                duration = 0;
            }
            mMediaPlayer.release();
        }
        mMediaPlayer = null;
        mMediaState = STATE_STOPPED;
        if (reportComplete) {
            onComplete(duration);
        }
    }

    //Sets the audio volume, with 0 being silence and 1 being unity gain.
    public void duck(float value) {
        if (mMediaPlayer != null) {
            mMediaPlayer.setVolume(value);
        }
    }

    public void unDuck() {
        if (mMediaPlayer != null) {
            mMediaPlayer.setVolume(1F);
        }
    }

    /**
     * If our callback is not null, post our player progress back to the controlling
     * application so we can do "almost done" type of calls
     */
    private void postProgress(final float percent) {
        synchronized (mCallbacks) {
            for (Callback callback : mCallbacks) {
                if (mMediaPlayer != null && callback != null) {
                    callback.playerProgress(mItem, mMediaPlayer.getCurrentPosition(), percent);
                }
            }
        }
    }


    /**
     * Pass our Exception to all the Callbacks, handle it at the top level
     *
     * @param e the thrown exception
     */
    private void bubbleUpError(Exception e) {
        mMediaState = STATE_STOPPED;
        for (Callback callback : mCallbacks) {
            callback.dataError(mItem, e);
        }
    }


    @Override
    public void onComplete(long duration) {
        mMediaState = STATE_FINISHED;
        for (Callback callback : mCallbacks) {
            callback.playerProgress(mItem, 1, 1);
            callback.itemComplete(mItem, true, duration);
        }
    }

    @Override
    public void onError(ExoPlaybackException exception) {
        release(false);
        bubbleUpError(exception);
    }

    @Override
    public void onPrepare() {
        mMediaState = STATE_PLAYING;
        for (Callback callback : mCallbacks) {
            callback.playerPrepared(mItem);
            callback.playerProgress(mItem, mMediaPlayer.getCurrentPosition(), 0);
        }
        mMediaPlayer.setPlayWhenReady(true);
    }

    @Override
    public void onProgress(float percent) {
        postProgress(percent);
    }

    @Override
    public void onBuffering(long offset) {
        mMediaState = STATE_BUFFER_UNDERRUN;
        for (Callback callback : mCallbacks) {
            callback.onBuffering(getCurrentItem(), offset);
        }
    }

    @Override
    public void onBufferReady(long offsetInMilliseconds, long stutterDurationInMilliseconds) {
        mMediaState = STATE_PLAYING;
        for (Callback callback : mCallbacks) {
            callback.onBufferReady(getCurrentItem(), offsetInMilliseconds, stutterDurationInMilliseconds);
        }
    }

    private void playitem(AvsSpeakItem playItem, long offset) {
        //write out our raw audio data to a file
        File path = new File(mContext.getCacheDir(), playItem.messageID);
        if (path.exists()) {
            getMediaPlayer().prepare(buildMediaSource(Uri.fromFile(path), null), path, offset);
            return;
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(path);
            fos.write(playItem.getAudio());
            fos.close();
            //play our newly-written file
            getMediaPlayer().prepare(buildMediaSource(Uri.fromFile(path), null), path, offset);
        } catch (IOException e) {
            //bubble up our error
            bubbleUpError(e);
        } finally {
            if (fos != null) IOUtils.closeQuietly(fos);
        }
    }

    public long getCurrentPosition() {
        return mMediaPlayer == null ? 0 : mMediaPlayer.getCurrentPosition();
    }

    public String getStateString() {
        switch (mMediaState) {
            case STATE_IDLE:
                return "IDLE";
            case STATE_STOPPED:
                return "STOPPED";
            case STATE_PLAYING:
                return "PLAYING";
            case STATE_PAUSED:
                return "PAUSED";
            case STATE_BUFFER_UNDERRUN:
                return "BUFFER_UNDERRUN";
            case STATE_FINISHED:
                return "FINISHED";
            default:
                return "";
        }
    }

    private void cancelAvsRemoteCallRequest(){
        if(mAvsRemoteCall!=null){
            if(!mAvsRemoteCall.isCanceled()){
                mAvsRemoteCall.cancel();
            }
            mAvsRemoteCall = null;
        }
    }
}
