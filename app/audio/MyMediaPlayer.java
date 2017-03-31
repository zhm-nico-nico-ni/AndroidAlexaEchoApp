package com.ggec.voice.assistservice.audio;

import android.media.MediaPlayer;

import java.io.IOException;

/**
 * Created by ggec on 2017/3/30.
 */

public class MyMediaPlayer extends MediaPlayer {
    private String currentPath;
    @Override
    public void setDataSource(String path) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        super.setDataSource(path);
        currentPath = path;
    }

    public String mrl() {
        return currentPath;
    }

    public static class IOnCompleteListener implements MediaPlayer.OnCompletionListener {

        @Override
        public void onCompletion(MediaPlayer mp) {
            onComplete((MyMediaPlayer) mp);
        }

        public void onComplete(MyMediaPlayer mediaplayer){

        }
    }
}
