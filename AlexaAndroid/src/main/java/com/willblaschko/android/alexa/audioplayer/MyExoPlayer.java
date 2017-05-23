package com.willblaschko.android.alexa.audioplayer;

import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.os.SystemClock;

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
    private int mPlaybackState;
    private long bufferBeginTime;
    private EventLogger eventLogger;

    public MyExoPlayer(Context context, IMyExoPlayerListener listener, boolean needLogger) {
        mListener = listener;
        DefaultTrackSelector trackSelector = new DefaultTrackSelector();
        mMediaPlayer = ExoPlayerFactory.newSimpleInstance(context
                , trackSelector, new DefaultLoadControl(),
                null, SimpleExoPlayer.EXTENSION_RENDERER_MODE_PREFER);

        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        setPlayWhenReady(false);
        mMediaPlayer.addListener(this);

        if(needLogger) {
            eventLogger = new EventLogger(trackSelector);
            mMediaPlayer.addListener(eventLogger);
            mMediaPlayer.setAudioDebugListener(eventLogger);
            mMediaPlayer.setVideoDebugListener(eventLogger);
            mMediaPlayer.setMetadataOutput(eventLogger);
        }
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
        if (ExoPlayer.STATE_BUFFERING == mPlaybackState && playbackState == ExoPlayer.STATE_READY) {
            if (bufferBeginTime > 0 && mListener != null)
                mListener.onBufferReady(mMediaPlayer.getCurrentPosition(),
                        SystemClock.elapsedRealtime() - bufferBeginTime);
        }

        mPlaybackState = playbackState;
        if (ExoPlayer.STATE_READY == playbackState) {
            if (beginOffset > 0 && beginOffset < mMediaPlayer.getDuration()) {
                mMediaPlayer.seekTo(beginOffset);
                beginOffset = 0;
            } else {
                // 加上正式准备好的提示
                if (!mFiredPrepareEvent) {
                    setPlayWhenReady(true);
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
        } else if (ExoPlayer.STATE_ENDED == playbackState) {
            long duration = mMediaPlayer.getDuration();
            deletePlayFile();
            if (mListener != null) mListener.onComplete(duration);
        } else if (ExoPlayer.STATE_BUFFERING == playbackState) {
            long current = SystemClock.elapsedRealtime();
            if (bufferBeginTime > 0 && current - bufferBeginTime > 2000) {
                mListener.onBuffering(mMediaPlayer.getCurrentPosition());
            }
            bufferBeginTime = current;
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

//    public void resume(){
//        mMediaPlayer.setPlayWhenReady(true);
//    }
//
//    public long pause(){
//        mMediaPlayer.setPlayWhenReady(false);
//        return mMediaPlayer.getCurrentPosition();
//    }

    public long stop() {
        long pos = mMediaPlayer.getCurrentPosition();
        if (mAsyncTask != null) {
            mAsyncTask.cancel();
            mAsyncTask = null;
        }
        mMediaPlayer.stop();
        deletePlayFile();
        return pos;
    }

    public void prepare(MediaSource mediaSource) {
        prepare(mediaSource, null, 0);
    }

    public void prepare(MediaSource mediaSource, File deleteWhenFinishPath, long beginOffset) {
        this.beginOffset = beginOffset;
        mFiredPrepareEvent = false;
        bufferBeginTime = 0;
        deletePlayFile();
        mDeleteWhenFinishPath = deleteWhenFinishPath;
        mMediaPlayer.prepare(mediaSource);
    }

    private void deletePlayFile() {
        if (mDeleteWhenFinishPath != null && mDeleteWhenFinishPath.exists()) {
            boolean del = mDeleteWhenFinishPath.delete();
            if (del) mDeleteWhenFinishPath = null;
            Log.d("MyExoPlayer", "delete file:" + del);
        }
    }

    public int getPlaybackState() {
        return mMediaPlayer.getPlaybackState();
    }

    public void setPlayWhenReady(boolean playWhenReady) {
        mMediaPlayer.setPlayWhenReady(playWhenReady);
    }

    public boolean getPlayWhenReady(){
        return mMediaPlayer.getPlayWhenReady();
    }

    public void release() {
        eventLogger = null;
        mMediaPlayer.release();
        mMediaPlayer = null;
        mListener = null;
        deletePlayFile();
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

    public boolean isMediaReadyToPlay() {
        int state = mMediaPlayer.getPlaybackState();
        return ExoPlayer.STATE_READY == state || ExoPlayer.STATE_BUFFERING == state;
    }

    public boolean isPlaying() {
        int state = mMediaPlayer.getPlaybackState();
        if(ExoPlayer.STATE_READY == state || ExoPlayer.STATE_BUFFERING == state) {
            return mMediaPlayer.getPlayWhenReady();
        }
        return false;
    }

    private Handler handler = new Handler();

    private class TaskRunnar implements Runnable {
        private volatile boolean isTaskFinish;

        @Override
        public void run() {
            if (!isTaskFinish && mMediaPlayer != null ) {

                if(isPlaying()) {
                    long pos = mMediaPlayer.getCurrentPosition();
                    long duration = mMediaPlayer.getDuration();
                    final float percent = (float) pos / (float) duration;

                    if (mListener != null) mListener.onProgress(percent, duration - pos);
                }

                handler.postDelayed(this, 100);
            }
        }

        public void cancel() {
            isTaskFinish = true;
            handler.removeCallbacks(this);
        }
    }

    public interface IMyExoPlayerListener {
        void onComplete(long duration);

        void onError(ExoPlaybackException exception);

        void onPrepare();

        void onProgress(float percent, long remaining);

        void onBuffering(long offsetInMilliseconds);

        void onBufferReady(long offsetInMilliseconds, long stutterDurationInMilliseconds);
    }
}
