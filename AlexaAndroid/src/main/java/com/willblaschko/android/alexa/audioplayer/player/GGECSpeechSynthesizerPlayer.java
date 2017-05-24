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
import com.willblaschko.android.alexa.audioplayer.MyExoPlayer;
import com.willblaschko.android.alexa.interfaces.AvsItem;
import com.willblaschko.android.alexa.interfaces.alerts.AvsAlertPlayItem;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsPlayContentItem;
import com.willblaschko.android.alexa.interfaces.speechsynthesizer.AvsSpeakItem;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


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

        if (!TextUtils.isEmpty(mItem.getToken()) && mItem.getToken().contains("PausePrompt")) {
            Log.e(TAG, "what happen ? token:" + mItem.getToken());
            //a gross work around for a broke pause mp3 coming from Amazon, play the local mp3
            getMediaPlayer().prepare(buildMediaSource(Uri.parse("asset:///shhh.mp3"), "mp3"));
        } else if (mItem instanceof AvsPlayContentItem) {
            AvsPlayContentItem playItem = (AvsPlayContentItem) item;
            getMediaPlayer().prepare(buildMediaSource(playItem.getUri(), null));
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


    @Override
    public void onComplete(long duration) {
        mMediaState = STATE_FINISHED;
        if(mCallback != null) {
            mCallback.playerProgress(mItem, 1, 1, 0);
            mCallback.itemComplete(mItem, true, duration);
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
        if(mCallback != null) {
            mCallback.playerPrepared(mItem);
            mCallback.playerProgress(mItem, mMediaPlayer.getCurrentPosition(), 0, 0);
        }
    }

    @Override
    public void onProgress(float percent, long remaining) {
        if (mMediaPlayer != null && mCallback != null) {
            mCallback.playerProgress(mItem, mMediaPlayer.getCurrentPosition(), percent, remaining);
        }
    }

    @Override
    public void onBuffering(long offset) {
        mMediaState = STATE_BUFFER_UNDER_RUN;
        if(mCallback != null) {
            mCallback.onBuffering(getCurrentItem(), offset);
        }
    }

    @Override
    public void onBufferReady(long offsetInMilliseconds, long stutterDurationInMilliseconds) {
        mMediaState = STATE_PLAYING;
        if(mCallback != null) {
            mCallback.onBufferReady(getCurrentItem(), offsetInMilliseconds, stutterDurationInMilliseconds);
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
            case STATE_PLAYING:
            case STATE_BUFFER_UNDER_RUN:
                return "PLAYING";
            default:
                return "FINISHED";
        }
    }

    public void releaseAvsItem(){
        mItem = null;
    }
}
