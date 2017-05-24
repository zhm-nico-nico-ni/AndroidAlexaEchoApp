package com.willblaschko.android.alexa.audioplayer.player;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.ggec.voice.toollibrary.log.Log;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.source.MediaSource;
import com.willblaschko.android.alexa.audioplayer.Callback;
import com.willblaschko.android.alexa.audioplayer.MyAVSAudioParser;
import com.willblaschko.android.alexa.audioplayer.MyExoPlayer;
import com.willblaschko.android.alexa.interfaces.AvsItem;
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

import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by ggec on 2017/5/23.
 */

public class GGECMediaAudioPlayer implements MyExoPlayer.IMyExoPlayerListener {

    public static final String TAG = "GGECMediaAudioPlayer";

    private final static int STATE_IDLE = 0;
    private final static int STATE_PLAYING = 1;
    private final static int STATE_STOPPED = 2;
    private final static int STATE_PAUSED = 3;
    private final static int STATE_BUFFER_UNDER_RUN = 4;
    private final static int STATE_FINISHED = 5;

    private MyExoPlayer mMediaPlayer;
    private Context mContext;
    private AvsItem mItem;
    private final List<Callback> mCallbacks = new ArrayList<>();
    private int mMediaState = STATE_IDLE;
    private MyAVSAudioParser myAVSAudioParser;

    /**
     * Create our new AlexaAudioPlayer
     *
     * @param context any context, we will get the application level to store locally
     */
    public GGECMediaAudioPlayer(Context context) {
        mContext = context.getApplicationContext();
        com.ggec.voice.toollibrary.Util.trimCache(context);
    }

    /**
     * Return a reference to the MediaPlayer instance, if it does not exist,
     * then create it and configure it to our needs
     *
     * @return Android native MediaPlayer
     */
    private MyExoPlayer getMediaPlayer() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MyExoPlayer(mContext, this, true);

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

//    /**
//     * A helper function to play an AvsPlayContentItem, this is passed to play() and handled accordingly,
//     *
//     * @param item a speak type item
//     */
//    public void playItem(AvsPlayContentItem item) {
//        play(item);
//    }
//
    /**
     * A helper function to play an AvsSpeakItem, this is passed to play() and handled accordingly,
     *
     * @param item a AvsPlayAudioItem type item
     */
    public void playItem(AvsPlayAudioItem item) {
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


    /**
     * Request our MediaPlayer to play an item, if it's an AvsPlayRemoteItem (url, usually), we set that url as our data source for the MediaPlayer
     * if it's an AvsSpeakItem, then we write the raw audio to a file and then read it back using the MediaPlayer
     *
     * @param item
     */
    private void play(@NonNull AvsItem item) {
        if (item.equals(mItem)) {
            if(item instanceof AvsPlayRemoteItem && !getMediaPlayer().getPlayWhenReady()) {
                Log.w(TAG, "play the same item");
                play();
                return;
            }
        }
        mItem = item;
        //if we're playing, stop playing before we continue
        getMediaPlayer().stop();

        if (!TextUtils.isEmpty(mItem.getToken()) && mItem.getToken().contains("PausePrompt")) {
            Log.e(TAG, "what happen ? token:" + mItem.getToken());
            //a gross work around for a broke pause mp3 coming from Amazon, play the local mp3
            getMediaPlayer().prepare(buildMediaSource(Uri.parse("asset:///shhh.mp3"), "mp3"));
        } else if (mItem instanceof AvsPlayRemoteItem) {
            handleRemoteAVSItem((AvsPlayRemoteItem) mItem);
        } else if (mItem instanceof AvsPlayContentItem) {
            AvsPlayContentItem playItem = (AvsPlayContentItem) item;
            getMediaPlayer().prepare(buildMediaSource(playItem.getUri(), null));
        } else if (mItem instanceof AvsPlayAudioItem) {
            AvsPlayAudioItem playAudioItem = (AvsPlayAudioItem) mItem;
            long offset = playAudioItem.mStream.getOffsetInMilliseconds();
            long startOffset = playAudioItem.pausePosition > offset
                    ? playAudioItem.pausePosition : offset;
            playitem(playAudioItem, startOffset);
        } else {

            bubbleUpError(new Exception("not suitable process"));

        }
    }

    private MediaSource buildMediaSource(Uri uri, String overrideExtension) {
        return com.willblaschko.android.alexa.utility.Util.buildMediaSource(mContext, uri, overrideExtension);
    }

    /**
     * Check whether our MediaPlayer is currently playing
     *
     * @return true playing, false not
     */
    public boolean isPlaying() {
        return mMediaPlayer != null && mMediaPlayer.isMediaReadyToPlay();
    }

    /**
     * A helper function to pause the MediaPlayer
     */
    public void pause() {
        mMediaState = STATE_PAUSED;
        getMediaPlayer().setPlayWhenReady(false);
        Log.d(TAG, "pause ");
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
    private void postProgress(final float percent, long remaining) {
        synchronized (mCallbacks) {
            for (Callback callback : mCallbacks) {
                if (mMediaPlayer != null && callback != null) {
                    callback.playerProgress(mItem, mMediaPlayer.getCurrentPosition(), percent, remaining);
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
            callback.playerProgress(mItem, 1, 1, 0);
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
            callback.playerProgress(mItem, mMediaPlayer.getCurrentPosition(), 0, 0);
        }
    }

    @Override
    public void onProgress(float percent, long remaining) {
        postProgress(percent, remaining);
    }

    @Override
    public void onBuffering(long offset) {
        mMediaState = STATE_BUFFER_UNDER_RUN;
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
            case STATE_BUFFER_UNDER_RUN:
                return "BUFFER_UNDERRUN";
            case STATE_FINISHED:
                return "FINISHED";
            default:
                return "";
        }
    }

    private void cancelAvsRemoteCallRequest(){
        if(myAVSAudioParser != null){
            myAVSAudioParser.cancelRequest();
        }
    }

    private void handleRemoteAVSItem(final AvsPlayRemoteItem playItem) {
        cancelAvsRemoteCallRequest();
        final long startOffset = playItem.pausePosition > playItem.getStartOffset()
                ? playItem.pausePosition : playItem.getStartOffset();

        Log.d(TAG, "play remote item offset:"+startOffset+" , url -> " + playItem.getUrl() + "\n convert -> " + playItem.getConvertUrl());
        if (TextUtils.isEmpty(playItem.getConvertUrl())) {
            Flowable.fromCallable(new Callable<ConvertAudioItem>() {
                @Override
                public ConvertAudioItem call() throws Exception {
                    String playUri;

                    try {
                        myAVSAudioParser = new MyAVSAudioParser(playItem);
                        playUri = myAVSAudioParser.request(playItem.getUrl());
                    } catch (Exception ex) {
                        Log.e(TAG, "handleRemoteAVSItem exception", ex);
                        playUri = playItem.getUrl();
                    }

                    if (myAVSAudioParser != null) {
                        return new ConvertAudioItem(!myAVSAudioParser.isCanceled() ? playUri : "", playItem.extension);
                    } else {
                        return new ConvertAudioItem(null, null);
                    }
                }
            }).subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.single())
                    .subscribe(new Consumer<ConvertAudioItem>() {
                        @Override
                        public void accept(@io.reactivex.annotations.NonNull ConvertAudioItem s) throws Exception {
                            if (!TextUtils.isEmpty(s.convertUrl)) {
                                Log.d(TAG, "final play "+ s.convertUrl+ " extension:"+s.overrideExtension);
                                getMediaPlayer().prepare(buildMediaSource(Uri.parse(s.convertUrl), s.overrideExtension), null, startOffset);
                            } else {
                                Log.w(TAG, "handleRemoteAVSItem end, may cancel or error :" + s.convertUrl);
                            }
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(@io.reactivex.annotations.NonNull Throwable throwable) throws Exception {
                            bubbleUpError((Exception) throwable);
                        }
                    });
        } else {
            getMediaPlayer().prepare(buildMediaSource(Uri.parse(playItem.getConvertUrl()), playItem.extension), null, startOffset);
        }
    }


}
