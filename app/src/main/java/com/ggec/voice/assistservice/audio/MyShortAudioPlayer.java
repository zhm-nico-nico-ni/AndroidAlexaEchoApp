package com.ggec.voice.assistservice.audio;

import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.ggec.voice.assistservice.MyApplication;
import com.ggec.voice.toollibrary.log.Log;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.wav.WavExtractor;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

import java.io.IOException;

/**
 * Created by ggec on 2017/4/5.
 */

public class MyShortAudioPlayer implements ExoPlayer.EventListener {
    private static Handler sMainHandler = new Handler(Looper.getMainLooper());
    private IOnCompletionListener mOnCompleteListener;
    private SimpleExoPlayer exoPlayer;
    private long begin;
    private String path;

    //        path = "asset:///start.mp3";  use this
    public MyShortAudioPlayer(String path){
        begin = SystemClock.elapsedRealtime();

        exoPlayer = ExoPlayerFactory.newSimpleInstance(MyApplication.getContext(), new DefaultTrackSelector());

        exoPlayer.addListener(this);
        exoPlayer.setPlayWhenReady(false);
        exoPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        this.path = path;
        loadMedia();

//        path = "asset:///start.mp3";  use this

        Log.d("MyShortAudioPlayer", "init use:"+ (SystemClock.elapsedRealtime() - begin));
    }

    private void loadMedia(){
        ExtractorMediaSource mediaSource = new ExtractorMediaSource(
                Uri.parse(path),
                new DefaultDataSourceFactory(MyApplication.getContext(), "GGEC"),
                WavExtractor.FACTORY,
                null,
                new ExtractorMediaSource.EventListener() {
                    @Override
                    public void onLoadError(IOException e) {
                        e.printStackTrace();
                    }
                });
        exoPlayer.prepare(mediaSource);
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
            Log.d("MyShortAudioPlayer", "ready use:"+ (SystemClock.elapsedRealtime() - begin));
        }
        if (playbackState == ExoPlayer.STATE_ENDED){
            exoPlayer.setPlayWhenReady(false);

            if (mOnCompleteListener != null) {
                mOnCompleteListener.onCompletion();
                mOnCompleteListener = null;

                sMainHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        exoPlayer.seekTo(0);
                    }
                }, 2000);
                Log.d("MyShortAudioPlayer", "play and release use:"+ (SystemClock.elapsedRealtime() - begin));
            }
        }
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        error.printStackTrace();
    }

    @Override
    public void onPositionDiscontinuity() {

    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    public void play(IOnCompletionListener listener){
        if(exoPlayer.getPlaybackState() == ExoPlayer.STATE_IDLE){
            listener.onCompletion();
            return;
        }
        begin = SystemClock.elapsedRealtime();
        mOnCompleteListener = listener;
        exoPlayer.setPlayWhenReady(true);
        Log.d(Log.TAG_APP,"playplayplay" );
    }

    public interface IOnCompletionListener{
        void onCompletion();
    }
}
