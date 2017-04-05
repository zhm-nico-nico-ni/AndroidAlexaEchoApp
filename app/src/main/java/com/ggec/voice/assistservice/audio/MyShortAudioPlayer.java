package com.ggec.voice.assistservice.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.support.annotation.NonNull;

import com.ggec.voice.assistservice.R;
import com.ggec.voice.assistservice.log.Log;

/**
 * Created by ggec on 2017/4/5.
 * 同步的音乐播放
 */

public class MyShortAudioPlayer {

    //    private final Context context;
//    private final Uri uri;
//    private final boolean looping;
//    private final AudioAttributes attributes;
    private SoundPool player;
    private int mStartSoundID;
    private int mStopSoundID;
    private int mErrorSoundID;

    private static MyShortAudioPlayer sInstance;

    public static synchronized MyShortAudioPlayer getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new MyShortAudioPlayer(context, new AudioAttributes.Builder()
//                        .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setFlags(AudioAttributes.FLAG_HW_AV_SYNC)
                    .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
                    .build());
        }

        return sInstance;
    }

    private MyShortAudioPlayer(@NonNull Context context,
                               @NonNull AudioAttributes attributes) {
        player = new SoundPool.Builder()
                .setAudioAttributes(attributes)
                .setMaxStreams(2)
                .build();
//        this.context = context;
//        this.uri = uri;
//        this.looping = looping;
//        this.attributes = attributes;

        mStartSoundID = player.load(context, R.raw.start, 1);
        mStopSoundID = player.load(context, R.raw.stop, 1);
        mErrorSoundID = player.load(context, R.raw.error, 1);
//        player.release();
    }

    public void playStart() {
        play(mStartSoundID);
    }

    public void playStop() {
        play(mStopSoundID);
    }

    public void playError() {
        play(mErrorSoundID);
    }

    private void play(int soundId) {
        Log.d(Log.TAG_APP, "play " + player.play(soundId, 1, 1, 0, 0, 1) + " id:" + soundId);
    }
}
