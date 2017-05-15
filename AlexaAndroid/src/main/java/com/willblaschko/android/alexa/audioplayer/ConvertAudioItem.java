package com.willblaschko.android.alexa.audioplayer;

class ConvertAudioItem {
    String convertUrl;
    String overrideExtension;

    protected ConvertAudioItem(String url, String extension) {
        convertUrl = url;
        overrideExtension = extension;
    }
}