package com.ggec.voice.assistservice.audio;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.ggec.voice.assistservice.MyApplication;

import java.io.IOException;

/**
 * Created by ggec on 2017/4/5.
 */

public class MyShortAudioPlayer2 implements MediaPlayer.OnCompletionListener {
    private static Handler sMainHandler = new Handler(Looper.getMainLooper());

    MediaPlayer.OnCompletionListener mOnCompleteListener;

    public MyShortAudioPlayer2(String path, MediaPlayer.OnCompletionListener listener) throws IOException {
        mOnCompleteListener = listener;
        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setDataSource(MyApplication.getContext(), Uri.parse(path));
        mediaPlayer.prepare();
        mediaPlayer.setOnCompletionListener(this);

        mediaPlayer.start();
    }


    @Override
    public void onCompletion(final MediaPlayer mp) {
        sMainHandler.post(new Runnable() {
            @Override
            public void run() {
//                Log.d("TAGG", "onCompleteeee");
                mp.release();
                if(mOnCompleteListener != null){
                    mOnCompleteListener.onCompletion(mp);
                }
            }
        });

//        mp.reset();
    }
}
