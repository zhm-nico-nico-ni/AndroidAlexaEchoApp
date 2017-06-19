package com.ggec.voice.assistservice.audio;

import com.ggec.voice.assistservice.data.ImplAsyncCallback;
import com.willblaschko.android.alexa.interfaces.AvsResponse;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by ggec on 2017/4/5.
 */

public abstract class IMyVoiceRecordListener extends ImplAsyncCallback {

    private AtomicBoolean needNotifyVoice = new AtomicBoolean(true);
    private final String mPath;

    public IMyVoiceRecordListener(String filePath) {
        super("IMyVoiceRecordListener");
        mPath = filePath;
    }

    public abstract void success(AvsResponse result, String filePath);

    public abstract void failure(Exception error, String filePath, long actuallyLong);

    @Override
    public void success(final AvsResponse result) {
        super.success(result);
        success(result, mPath);
    }

    @Override
    public void handle(AvsResponse result) {
        super.handle(result);
        success(result, mPath);
    }

    @Override
    public void failure(Exception error) {
        failure(error, mPath, -1);
    }


    public boolean needNotifyVoice() {
        return needNotifyVoice.getAndSet(false);
    }
}
