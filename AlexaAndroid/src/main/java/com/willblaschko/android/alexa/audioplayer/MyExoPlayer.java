package com.willblaschko.android.alexa.audioplayer;

import android.content.Context;
import android.media.AudioManager;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
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

    private File mDeleteWhenFinishPath;

    private EventLogger eventLogger;

    public MyExoPlayer(Context context, IMyExoPlayerListener listener, boolean needLogger) {
        mListener = listener;
        DefaultTrackSelector trackSelector = new DefaultTrackSelector();
        mMediaPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector);

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
        if (mListener != null) mListener.onPlayerStateChanged(playWhenReady, playbackState);
        if (ExoPlayer.STATE_ENDED == playbackState) {
            deletePlayFile();
        }
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {

    }

    @Override
    public void onShuffleModeEnabledChanged(boolean b) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        deletePlayFile();
        if (mListener != null) mListener.onError(error);
    }

    @Override
    public void onPositionDiscontinuity(int i) {

    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    @Override
    public void onSeekProcessed() {

    }

    public long stop() {
        long pos = mMediaPlayer.getCurrentPosition();
        mMediaPlayer.stop();
        deletePlayFile();
        return pos;
    }

    public void prepare(MediaSource mediaSource, boolean playWhenReady, File deleteWhenFinishPath) {
        setPlayWhenReady(playWhenReady);
        deletePlayFile();
        mDeleteWhenFinishPath = deleteWhenFinishPath;
        mMediaPlayer.prepare(mediaSource);
    }

    private void deletePlayFile() {
//        if (mDeleteWhenFinishPath != null && mDeleteWhenFinishPath.exists()) {
//            boolean del = mDeleteWhenFinishPath.delete();
//            if (del) mDeleteWhenFinishPath = null;
//            Log.d("MyExoPlayer", "delete file:" + del);
//        }
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

    public SimpleExoPlayer getMediaPlayer() {
        return mMediaPlayer;
    }

    public boolean needPrepare(){
        return mMediaPlayer.getPlaybackState() == ExoPlayer.STATE_ENDED || mMediaPlayer.getPlaybackState() == ExoPlayer.STATE_IDLE;
    }

    public interface IMyExoPlayerListener {
        void onPlayerStateChanged(boolean playWhenReady, int playbackState);

        void onError(ExoPlaybackException exception);

    }
}
