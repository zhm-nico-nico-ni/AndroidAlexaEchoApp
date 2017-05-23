package com.willblaschko.android.alexa.audioplayer.player;

public class ConvertAudioItem {
    public String convertUrl;
    public String overrideExtension;

    public ConvertAudioItem(String url, String extension) {
        convertUrl = url;
        overrideExtension = extension;
    }
}