package com.willblaschko.android.alexa.audioplayer;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.willblaschko.android.alexa.audioplayer.AlexaAudioPlayer.Callback;
import com.willblaschko.android.alexa.interfaces.AvsItem;
import com.willblaschko.android.alexa.interfaces.alerts.AvsAlertPlayItem;
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

/**
 * A class that abstracts the Android MediaPlayer and adds additional functionality to handle AvsItems
 * as well as properly handle multiple callbacks--be care not to leak Activities by not removing the callback
 */
public class AlexaAudioExoPlayer implements MyExoPlayer.IMyExoPlayerListener {

    public static final String TAG = "AlexaAudioExoPlayer";

    private static AlexaAudioExoPlayer mInstance;

    private MyExoPlayer mMediaPlayer;
    private Context mContext;
    private AvsItem mItem;
    private final List<AlexaAudioPlayer.Callback> mCallbacks = new ArrayList<>();

    /**
     * Create our new AlexaAudioPlayer
     * @param context any context, we will get the application level to store locally
     */
    private AlexaAudioExoPlayer(Context context){
       mContext = context.getApplicationContext();
    }

    /**
     * Get a reference to the AlexaAudioPlayer instance, if it's null, we will create a new one
     * using the supplied context.
     * @param context any context, we will get the application level to store locally
     * @return our instance of the AlexaAudioPlayer
     */
    public static AlexaAudioExoPlayer getInstance(Context context){
        if(mInstance == null){
            mInstance = new AlexaAudioExoPlayer(context);
            trimCache(context); // FIXME 这里建议使用lrudishcache
        }
        return mInstance;
    }

    private static void trimCache(Context context) {
        try {
            File dir = context.getCacheDir();
            if (dir != null && dir.isDirectory()) {
                deleteDir(dir);
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    private static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        if(dir != null){
            // The directory is now empty so delete it
            return dir.delete();
        }
        return false;
    }

    /**
     * Return a reference to the MediaPlayer instance, if it does not exist,
     * then create it and configure it to our needs
     * @return Android native MediaPlayer
     */
    private MyExoPlayer getMediaPlayer(){
        if(mMediaPlayer == null){
            mMediaPlayer = new MyExoPlayer(mContext, this);

//            mMediaPlayer.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);
//            mMediaPlayer.setPlayWhenReady(true);
//            mMediaPlayer.setOnCompletionListener(mCompletionListener);
//            mMediaPlayer.setOnPreparedListener(mPreparedListener);
//            mMediaPlayer.setOnErrorListener(mErrorListener);
        }

        return mMediaPlayer;
    }

    /**
     * Add a callback to our AlexaAudioPlayer, this is added to our list of callbacks
     * @param callback Callback that listens to changes of player state
     */
    public void addCallback(AlexaAudioPlayer.Callback callback){
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

//    /**
//     * Remove a callback from our AlexaAudioPlayer, this is removed from our list of callbacks
//     * @param callback Callback that listens to changes of player state
//     */
//    public void removeCallback(Callback callback){
//        synchronized (mCallbacks) {
//            mCallbacks.remove(callback);
//        }
//    }

    /**
     * A helper function to play an AvsPlayContentItem, this is passed to play() and handled accordingly,
     * @param item a speak type item
     */
    public void playItem(AvsPlayContentItem item){
        play(item);
    }

    /**
     * A helper function to play an AvsSpeakItem, this is passed to play() and handled accordingly,
     * @param item a speak type item
     */
    public void playItem(AvsSpeakItem item){
        play(item);
    }

    /**
     * A helper function to play an AvsPlayRemoteItem, this is passed to play() and handled accordingly,
     * @param item a play type item, usually a url
     */
    public void playItem(AvsPlayRemoteItem item){
        play(item);
    }

    public void playItem(AvsAlertPlayItem item) {
        play(item);
    }

    /**
     * Request our MediaPlayer to play an item, if it's an AvsPlayRemoteItem (url, usually), we set that url as our data source for the MediaPlayer
     * if it's an AvsSpeakItem, then we write the raw audio to a file and then read it back using the MediaPlayer
     * @param item
     */
    private void play(@NonNull AvsItem item){
        if(isPlaying()){
            Log.w(TAG, "Already playing an item, did you mean to play another?");
        }

        mItem = item;
//        if(getMediaPlayer().getPlaybackState().isPlaying()){
            //if we're playing, stop playing before we continue
            getMediaPlayer().stop();
//        }

        //reset our player
//        getMediaPlayer().setLooping(false);



        if(!TextUtils.isEmpty(mItem.getToken()) && mItem.getToken().contains("PausePrompt")){
            Log.e(TAG, "what happen ? token:" + mItem.getToken());
            //a gross work around for a broke pause mp3 coming from Amazon, play the local mp3
            getMediaPlayer().prepare(buildMediaSource(Uri.parse("asset:///start.mp3"), "mp3"));
        }else if(mItem instanceof AvsPlayRemoteItem){
            //cast our item for easy access
            AvsPlayRemoteItem playItem = (AvsPlayRemoteItem) item;
            //play new url
            getMediaPlayer().prepare(buildMediaSource(Uri.parse(playItem.getUrl()), null));
        }else if(mItem instanceof AvsPlayContentItem){
            //cast our item for easy access
            AvsPlayContentItem playItem = (AvsPlayContentItem) item;
            getMediaPlayer().prepare(buildMediaSource(playItem.getUri(), null));
        }else if(mItem instanceof AvsSpeakItem){
            //cast our item for easy access
            AvsSpeakItem playItem = (AvsSpeakItem) item;
            //write out our raw audio data to a file
            File path=new File(mContext.getCacheDir(), System.currentTimeMillis()+".pcm");
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(path);
                fos.write(playItem.getAudio());
                fos.close();
                //play our newly-written file
                getMediaPlayer().prepare(buildMediaSource(Uri.fromFile(path), ".pcm"), path);
            } catch (IOException e) {
                e.printStackTrace();
                //bubble up our error
                bubbleUpError(e);
            } finally {
                if(fos!=null) IOUtils.closeQuietly(fos);
            }
        } else if(mItem instanceof AvsAlertPlayItem){
//            AvsAlertPlayItem playItem = (AvsAlertPlayItem) item;
            Uri path = Uri.parse("asset:///alarm.mp3");
            getMediaPlayer().prepare(buildMediaSource(path,"mp3"));
        } else {

            bubbleUpError(new Exception("not suitable process"));

        }
    }

    private MediaSource buildMediaSource(Uri uri, String overrideExtension) {
        int type = TextUtils.isEmpty(overrideExtension) ? Util.inferContentType(uri)
                : Util.inferContentType("." + overrideExtension);
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(mContext, "ExoPlayerExtVp9Test");
        switch (type) {
            case C.TYPE_SS:
                return new SsMediaSource(uri, dataSourceFactory,
                        new DefaultSsChunkSource.Factory(dataSourceFactory), null, null);
            case C.TYPE_DASH:
                return new DashMediaSource(uri, dataSourceFactory,
                        new DefaultDashChunkSource.Factory(dataSourceFactory), null, null);
            case C.TYPE_HLS:
                return new HlsMediaSource(uri, dataSourceFactory, null, null);
            case C.TYPE_OTHER:
                return new ExtractorMediaSource(uri, dataSourceFactory, new DefaultExtractorsFactory(),
                        null, null);
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }

    /**
     * Check whether our MediaPlayer is currently playing
     * @return true playing, false not
     */
    public boolean isPlaying(){
        return getMediaPlayer().isPlaying();
    }

    /**
     * A helper function to pause the MediaPlayer
     */
    public void pause(){
        getMediaPlayer().setPlayWhenReady(false);
    }

    /**
     * A helper function to play the MediaPlayer
     */
    public void play(){
        getMediaPlayer().setPlayWhenReady(true);
    }

    /**
     * A helper function to stop the MediaPlayer
     */
    public void stop(){
        getMediaPlayer().stop();
    }

    /**
     * A helper function to release the media player and remove it from memory
     */
    public void release(){
        if(mMediaPlayer != null){
//            if(mMediaPlayer.isPlaying()){
                mMediaPlayer.stop();
//            }
//            mMediaPlayer.reset();
            mMediaPlayer.release();
        }
        mMediaPlayer = null;
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
    private void postProgress(final float percent){
        synchronized (mCallbacks) {
            for (AlexaAudioPlayer.Callback callback : mCallbacks) {
                if(mMediaPlayer != null && callback != null) {
                    callback.playerProgress(mItem, mMediaPlayer.getCurrentPosition(), percent);
                }
            }
        }
    }


//    /**
//     * A callback to keep track of the state of the MediaPlayer and various AvsItem states
//     */
//    public interface Callback{
//        void playerPrepared(AvsItem pendingItem);
//        void playerProgress(AvsItem currentItem, long offsetInMilliseconds, float percent);
//        void itemComplete(AvsItem completedItem);
//        boolean playerError(AvsItem item, int what, int extra);
//        void dataError(AvsItem item, Exception e);
//    }

    /**
     * Pass our Exception to all the Callbacks, handle it at the top level
     * @param e the thrown exception
     */
    private void bubbleUpError(Exception e){
        for(Callback callback: mCallbacks){
            callback.dataError(mItem, e);
        }
    }

    /**
     * Pass our MediaPlayer completion state to all the Callbacks, handle it at the top level
     */
    private MediaPlayer.OnCompletionListener mCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            for(Callback callback: mCallbacks){
                callback.playerProgress(mItem, 1, 1);
                callback.itemComplete(mItem);
            }
        }
    };


    /**
     * Pass our MediaPlayer prepared state to all the Callbacks, handle it at the top level
     */
    private MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            for(Callback callback: mCallbacks){
                callback.playerPrepared(mItem);
                callback.playerProgress(mItem, mMediaPlayer.getCurrentPosition(), 0);
            }
            mMediaPlayer.setPlayWhenReady(true);
        }
    };

    /**
     * Pass our MediaPlayer error state to all the Callbacks, handle it at the top level
     */
//    private MediaPlayer.OnErrorListener mErrorListener = new MediaPlayer.OnErrorListener() {
//        @Override
//        public boolean onError(MediaPlayer mp, int what, int extra) {
//            for(Callback callback: mCallbacks){
//                boolean response = callback.playerError(mItem, what, extra);
//                if(response){
//                    return response;
//                }
//            }
//            return false;
//        }
//    };

    @Override
    public void onComplete() {
        mCompletionListener.onCompletion(null);
    }

    @Override
    public void onError(ExoPlaybackException exception) {
        release();
        bubbleUpError(exception);
    }

    @Override
    public void onPrepare() {
        mPreparedListener.onPrepared(null);
    }

    @Override
    public void onProgress(float percent) {
        postProgress(percent);
    }
}
