package com.willblaschko.android.alexa.audioplayer;

import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;

/**
 * Created by ggec on 2017/4/17.
 */

public class MyExoPlayer implements ExoPlayer.EventListener {
    private SimpleExoPlayer mMediaPlayer;
    private IMyExoPlayerListener mListener;
    private boolean mFiredPrepareEvent;
    private TaskRunnar mAsyncTask;

    public MyExoPlayer(Context context, IMyExoPlayerListener listener) {
        mListener = listener;
        mMediaPlayer = ExoPlayerFactory.newSimpleInstance(context
                , new DefaultTrackSelector(), new DefaultLoadControl(),
                null, SimpleExoPlayer.EXTENSION_RENDERER_MODE_PREFER);

        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setPlayWhenReady(true);
        mMediaPlayer.addListener(this);
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if (ExoPlayer.STATE_ENDED == playbackState) {
            if (mListener != null) mListener.onComplete();
        } else if (ExoPlayer.STATE_READY == playbackState && !mFiredPrepareEvent) {
            mFiredPrepareEvent = true;
            if(mAsyncTask!=null){
                mAsyncTask.cancel();
                mAsyncTask = null;
            }
            if (mListener != null) mListener.onPrepare();
            mAsyncTask = new TaskRunnar();
            handler.postDelayed(mAsyncTask, 100);
        }
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        if (mListener != null) mListener.onError(error);
    }

    @Override
    public void onPositionDiscontinuity() {

    }

    public void stop() {
        mMediaPlayer.stop();
    }

    public void prepare(MediaSource mediaSource) {
        mFiredPrepareEvent = false;
        mMediaPlayer.prepare(mediaSource);
    }

    public int getPlaybackState() {
        return mMediaPlayer.getPlaybackState();
    }

    public void setPlayWhenReady(boolean playWhenReady) {
        mMediaPlayer.setPlayWhenReady(playWhenReady);
    }

    public void release() {
        mMediaPlayer.release();
        mMediaPlayer = null;
        mListener = null;
    }

    public void setVolume(float value) {
        mMediaPlayer.setVolume(value);
    }

    public long getCurrentPosition() {
        return mMediaPlayer.getCurrentPosition();
    }

    public long getDuration() {
        return mMediaPlayer.getDuration();
    }

    public boolean isPlaying(){
        int state = mMediaPlayer.getPlaybackState();
        return ExoPlayer.STATE_READY == state || ExoPlayer.STATE_BUFFERING == state;
    }

    private Handler handler = new Handler();

    private class TaskRunnar implements Runnable {
        private volatile boolean isTaskFinish;
        @Override
        public void run() {
            if(!isTaskFinish && mMediaPlayer != null && isPlaying()){
                long pos = mMediaPlayer.getCurrentPosition();
                final float percent = (float) pos / (float) mMediaPlayer.getDuration();
                if(mListener != null) mListener.onProgress(percent);

                handler.postDelayed(this, 100);
            }
        }

        public void cancel(){
            isTaskFinish = true;
            handler.removeCallbacks(this);
        }
    };

    public interface IMyExoPlayerListener {
        void onComplete();

        void onError(ExoPlaybackException exception);

        void onPrepare();

        void onProgress(float percent);
    }
}
