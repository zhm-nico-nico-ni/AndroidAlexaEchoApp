package com.willblaschko.android.alexa.audioplayer;

import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;

import com.ggec.voice.toollibrary.log.Log;
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

import java.io.File;

/**
 * Created by ggec on 2017/4/17.
 */

public class MyExoPlayer implements ExoPlayer.EventListener {
    private SimpleExoPlayer mMediaPlayer;
    private IMyExoPlayerListener mListener;
    private boolean mFiredPrepareEvent;
    private TaskRunnar mAsyncTask;
    private File mDeleteWhenFinishPath;
    private long beginOffset;

    public MyExoPlayer(Context context, IMyExoPlayerListener listener) {
        mListener = listener;
        mMediaPlayer = ExoPlayerFactory.newSimpleInstance(context
                , new DefaultTrackSelector(), new DefaultLoadControl(),
                null, SimpleExoPlayer.EXTENSION_RENDERER_MODE_PREFER);

        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setPlayWhenReady(false);
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
        if (ExoPlayer.STATE_READY == playbackState){
            if(beginOffset>0 && beginOffset < mMediaPlayer.getDuration()) {
                mMediaPlayer.seekTo(beginOffset);
            }
            mMediaPlayer.setPlayWhenReady(true);
        }

        if (ExoPlayer.STATE_ENDED == playbackState) {
            long duration = mMediaPlayer.getDuration();
            deletePlayFile();
            if (mListener != null) mListener.onComplete(duration);
        } else if (ExoPlayer.STATE_READY == playbackState && !mFiredPrepareEvent) {
            mFiredPrepareEvent = true;
            if (mAsyncTask != null) {
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
        deletePlayFile();
        if (mListener != null) mListener.onError(error);
    }

    @Override
    public void onPositionDiscontinuity() {

    }

    public void stop() {
        mMediaPlayer.stop();
    }

    public void prepare(MediaSource mediaSource) {
        prepare(mediaSource, null, 0);
    }

    public void prepare(MediaSource mediaSource, File deleteWhenFinishPath, long beginOffset) {
        this.beginOffset = beginOffset;
        mFiredPrepareEvent = false;
        deletePlayFile();
        mDeleteWhenFinishPath = deleteWhenFinishPath;
        mMediaPlayer.prepare(mediaSource);
    }

    private void deletePlayFile() {
        if (mDeleteWhenFinishPath != null && mDeleteWhenFinishPath.exists()) {
            boolean del = mDeleteWhenFinishPath.delete();
            if(del) mDeleteWhenFinishPath = null;
            Log.d("zhm", "delete file:" + del);
        }
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

    public boolean isPlaying() {
        int state = mMediaPlayer.getPlaybackState();
        return ExoPlayer.STATE_READY == state || ExoPlayer.STATE_BUFFERING == state;
    }

    private Handler handler = new Handler();

    private class TaskRunnar implements Runnable {
        private volatile boolean isTaskFinish;

        @Override
        public void run() {
            if (!isTaskFinish && mMediaPlayer != null && isPlaying()) {
                long pos = mMediaPlayer.getCurrentPosition();
                final float percent = (float) pos / (float) mMediaPlayer.getDuration();
                if (mListener != null) mListener.onProgress(percent);

                handler.postDelayed(this, 100);
            }
        }

        public void cancel() {
            isTaskFinish = true;
            handler.removeCallbacks(this);
        }
    }

    ;

    public interface IMyExoPlayerListener {
        void onComplete(long duration);

        void onError(ExoPlaybackException exception);

        void onPrepare();

        void onProgress(float percent);
    }
}
