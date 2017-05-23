package com.ggec.voice.assistservice.audio;

import android.media.AudioManager;
import android.net.Uri;
import android.os.SystemClock;

import com.ggec.voice.assistservice.MyApplication;
import com.ggec.voice.toollibrary.log.Log;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

/**
 * Created by ggec on 2017/4/5.
 */

public class MyShortAudioPlayer2 implements ExoPlayer.EventListener {

    private IOnCompletionListener mOnCompleteListener;
    private SimpleExoPlayer exoPlayer;
    private final long begin;

    //        path = "asset:///start.mp3";  use this
    public MyShortAudioPlayer2(String path, IOnCompletionListener listener){
        begin = SystemClock.elapsedRealtime();
        mOnCompleteListener = listener;
        exoPlayer = ExoPlayerFactory.newSimpleInstance(MyApplication.getContext(), new DefaultTrackSelector());

        exoPlayer.addListener(this);
        exoPlayer.setPlayWhenReady(true);
        exoPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

//        path = "asset:///start.mp3";  use this
        ExtractorMediaSource mediaSource = new ExtractorMediaSource(
                Uri.parse(path),
                new DefaultDataSourceFactory(MyApplication.getContext(), "GGEC"),
                Mp3Extractor.FACTORY,
                null,
                null);
        exoPlayer.prepare(mediaSource);
        Log.d("MyShortAudioPlayer2", "init use:"+ (SystemClock.elapsedRealtime() - begin));
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
        if(playbackState == ExoPlayer.STATE_READY){
            Log.d("MyShortAudioPlayer2", "ready use:"+ (SystemClock.elapsedRealtime() - begin));
        }
        if (playbackState == ExoPlayer.STATE_ENDED){
            if(exoPlayer != null){
                exoPlayer.release();
                exoPlayer = null;
            }
            Log.d("MyShortAudioPlayer2", "play and release use:"+ (SystemClock.elapsedRealtime() - begin));
            if (mOnCompleteListener != null) {
                mOnCompleteListener.onCompletion();
            }
        }
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {

    }

    @Override
    public void onPositionDiscontinuity() {

    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    public interface IOnCompletionListener{
        void onCompletion();
    }
}
