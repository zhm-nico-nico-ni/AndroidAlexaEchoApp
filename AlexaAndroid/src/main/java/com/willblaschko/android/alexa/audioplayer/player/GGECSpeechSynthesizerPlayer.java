package com.willblaschko.android.alexa.audioplayer.player;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.willblaschko.android.alexa.audioplayer.Callback;
import com.willblaschko.android.alexa.audioplayer.MyExoPlayer;
import com.willblaschko.android.alexa.interfaces.AvsItem;
import com.willblaschko.android.alexa.interfaces.alerts.AvsAlertPlayItem;
import com.willblaschko.android.alexa.interfaces.speechsynthesizer.AvsSpeakItem;

import org.jetbrains.annotations.Nullable;

import java.io.File;


/**
 * Created by ggec on 2017/5/23.
 * 用来播放speech
 */

public class GGECSpeechSynthesizerPlayer implements MyExoPlayer.IMyExoPlayerListener {

    public static final String TAG = "GGECSpeechSynthesizerPlayer";

    private final static int STATE_IDLE = 0;
    private final static int STATE_PLAYING = 1;
    private final static int STATE_STOPPED = 2;
    private final static int STATE_PAUSED = 3;
    private final static int STATE_BUFFER_UNDER_RUN = 4;
    private final static int STATE_FINISHED = 5;

    private MyExoPlayer mMediaPlayer;
    private Context mContext;
    private AvsItem mItem;
    private int mMediaState = STATE_IDLE;
    private Callback mCallback;
    private boolean mFiredPrepareEvent;

    /**
     * Create our new AlexaAudioPlayer
     *
     * @param context any context, we will get the application level to store locally
     */
    public GGECSpeechSynthesizerPlayer(Context context) {
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
            mMediaPlayer = new MyExoPlayer(mContext, this, false);

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
        mCallback = callback;
    }

    @Nullable
    public AvsItem getCurrentItem() {
        return mItem;
    }

    public String getCurrentToken() {
        return mItem == null ? "" :
                (TextUtils.isEmpty(mItem.getToken()) ? "" : mItem.getToken());
    }

    /**
     * A helper function to play an AvsSpeakItem, this is passed to play() and handled accordingly,
     *
     * @param item a speak type item
     */
    public void playItem(AvsSpeakItem item) {
        play(item);
    }


    public void playItem(AvsAlertPlayItem item) {
        play(item);
    }

    /**
     * Request our MediaPlayer to play an item, if it's an AvsPlayRemoteItem (url, usually), we set that url as our data source for the MediaPlayer
     * if it's an AvsSpeakItem, then we write the raw audio to a file and then read it back using the MediaPlayer
     */
    private void play(@NonNull AvsItem item) {
//        if (item.equals(mItem)) {
//            if(item instanceof AvsPlayRemoteItem && !getMediaPlayer().getPlayWhenReady()) {
//                Log.w(TAG, "play the same item");
//                play();
//                return;
//            }
//        }
        mItem = item;
        //if we're playing, stop playing before we continue
        getMediaPlayer().stop();

        if (mItem instanceof AvsSpeakItem) {
            playitem((AvsSpeakItem) mItem);
        } else if (mItem instanceof AvsAlertPlayItem) {
            Uri path = Uri.parse("asset:///alarm.mp3");
            prepare(buildMediaSource(path, "mp3"), true, null);
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
        return  STATE_PLAYING == mMediaState || (mItem != null && STATE_BUFFER_UNDER_RUN == mMediaState);//mMediaPlayer != null && mMediaPlayer.isMediaReadyToPlay();
    }

//    /**
//     * A helper function to pause the MediaPlayer
//     */
//    public void pause() {
//        mMediaState = STATE_PAUSED;
//        getMediaPlayer().setPlayWhenReady(false);
//        Log.d(TAG, "pause ");
//    }

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
        mMediaState = setStatePause ? STATE_PAUSED : STATE_STOPPED;
        return getMediaPlayer().stop();
    }

    /**
     * A helper function to release the media player and remove it from memory
     */
    public void release(boolean reportComplete) {
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
     * Pass our Exception to all the Callbacks, handle it at the top level
     *
     * @param e the thrown exception
     */
    private void bubbleUpError(Exception e) {
        mMediaState = STATE_STOPPED;
        if( mCallback != null) {

            mCallback.dataError(mItem, e);
        }
    }


    private void onComplete(long duration) {
        mMediaState = STATE_FINISHED;
        if(mCallback != null) {
            mCallback.playerProgress(mItem, 1, 1, 0);
            mCallback.itemComplete(mItem, true, duration);
        }
    }

    public void onError(ExoPlaybackException exception) {
        release(false);
        bubbleUpError(exception);
    }

    private void onPrepare() {
        if(mCallback != null) {
            mCallback.playerPrepared(mItem);
            mCallback.playerProgress(mItem, mMediaPlayer.getCurrentPosition(), 0, 0);
        }
    }


    private void playitem(AvsSpeakItem playItem) {
        //play our newly-written file
        prepare(buildMediaSource(Uri.parse("cid://" + playItem.getUrl()), null), true, null);
    }

    public long getCurrentPosition() {
        return mMediaPlayer == null ? 0 : mMediaPlayer.getCurrentPosition();
    }

    public String getStateString() {
        switch (mMediaState) {
            case STATE_PLAYING:
            case STATE_BUFFER_UNDER_RUN:
                return "PLAYING";
            default:
                return "FINISHED";
        }
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if (playbackState == ExoPlayer.STATE_READY) {
            // 加上正式准备好的提示
            mMediaState = STATE_PLAYING;
            if (!mFiredPrepareEvent) {
                mFiredPrepareEvent = true;
                onPrepare();
            }
        } else if (ExoPlayer.STATE_ENDED == playbackState) {
            long duration = mMediaPlayer.getDuration();
            onComplete(duration);
        }
    }

    private void prepare(MediaSource mediaSource, boolean playWhenReady, File deleteWhenFinishPath) {
        mFiredPrepareEvent = false;
        getMediaPlayer().prepare(mediaSource, playWhenReady, deleteWhenFinishPath);
    }
}
