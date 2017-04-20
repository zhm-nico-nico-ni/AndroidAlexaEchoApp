package com.ggec.voice.assistservice.audio;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.ggec.voice.assistservice.MyApplication;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
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

public class MyShortAudioPlayer2 implements MediaPlayer.OnCompletionListener, ExoPlayer.EventListener {
    private static Handler sMainHandler = new Handler(Looper.getMainLooper());

    MediaPlayer.OnCompletionListener mOnCompleteListener;
    SimpleExoPlayer exoPlayer;

    //        path = "asset:///start.mp3";  use this
    public MyShortAudioPlayer2(String path, MediaPlayer.OnCompletionListener listener){
//        long begin = SystemClock.elapsedRealtime();
        mOnCompleteListener = listener;
        exoPlayer = ExoPlayerFactory.newSimpleInstance(MyApplication.getContext()
                , new DefaultTrackSelector(), new DefaultLoadControl(),
                null, SimpleExoPlayer.EXTENSION_RENDERER_MODE_OFF);

        exoPlayer.addListener(this);
        exoPlayer.setPlayWhenReady(true);

//        path = "asset:///start.mp3";  use this
        ExtractorMediaSource mediaSource = new ExtractorMediaSource(
                Uri.parse(path),
                new DefaultDataSourceFactory(MyApplication.getContext(), "ExoPlayerExtVp9Test"),
                Mp3Extractor.FACTORY,
                null,
                null);
        exoPlayer.prepare(mediaSource);

//        Log.d("zhm", "MyShortAudioPlayer2 prepared, cost:" + (SystemClock.elapsedRealtime() - begin));
//        MediaPlayer mediaPlayer = new MediaPlayer();
//        mediaPlayer.setDataSource(MyApplication.getContext(), Uri.parse(path));
//        mediaPlayer.prepare();
//        mediaPlayer.setOnCompletionListener(this);
//
//        mediaPlayer.start();
    }


    @Override
    public void onCompletion(final MediaPlayer mp) {
        mp.reset();
        mp.release();
        sMainHandler.post(new Runnable() {
            @Override
            public void run() {
//                Log.d("TAGG", "onCompleteeee");

                if (mOnCompleteListener != null) {
                    mOnCompleteListener.onCompletion(mp);
                }
            }
        });

//        mp.reset();
    }

//    private MediaSource buildMediaSource(Uri uri, String overrideExtension) {
//        int type =  TextUtils.isEmpty(overrideExtension) ? Util.inferContentType(uri)
//                : Util.inferContentType("." + overrideExtension);
////        int type = C.TYPE_OTHER;
//        switch (type) {
////            case C.TYPE_SS:
////                return new SsMediaSource(uri, buildDataSourceFactory(false),
////                        new DefaultSsChunkSource.Factory(mediaDataSourceFactory), mainHandler, eventLogger);
////            case C.TYPE_DASH:
////                return new DashMediaSource(uri, buildDataSourceFactory(false),
////                        new DefaultDashChunkSource.Factory(mediaDataSourceFactory), mainHandler, eventLogger);
////            case C.TYPE_HLS:
////                return new HlsMediaSource(uri, mediaDataSourceFactory, mainHandler, eventLogger);
//            case C.TYPE_OTHER:
//                return new ExtractorMediaSource(uri, new FileDataSourceFactory( null)
//                        , new DefaultExtractorsFactory(),
//                        sMainHandler, null);
//            default: {
//                throw new IllegalStateException("Unsupported type: " + type);
//            }
//        }
//    }

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
        if (playbackState == ExoPlayer.STATE_ENDED){
            if(exoPlayer != null){
                exoPlayer.release();
            }

            if (mOnCompleteListener != null) {
                mOnCompleteListener.onCompletion(null);
            }
        }
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {

    }

    @Override
    public void onPositionDiscontinuity() {

    }
}
