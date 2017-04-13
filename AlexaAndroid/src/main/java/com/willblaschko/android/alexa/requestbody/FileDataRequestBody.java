package com.willblaschko.android.alexa.requestbody;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

/**
 * Created by ggec on 2017/4/5.
 */

public class FileDataRequestBody extends RequestBody {
    private File mFile;
    private long mActuallyLong;
    public FileDataRequestBody(final File file, final long actuallyLong){
        if (file == null) throw new NullPointerException("content == null");
        mFile = file;
        mActuallyLong = actuallyLong;
    }

    @Override
    public long contentLength() throws IOException {
        return super.contentLength();
    }

    @Override
    public MediaType contentType() {
        return MediaType.parse("application/octet-stream");
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        Source source = null;
        try {
            source = Okio.source(mFile);
            if(mActuallyLong > 0){
                sink.write(source, mActuallyLong);
            } else {
                sink.writeAll(source);
            }
        } finally {
            Util.closeQuietly(source);
        }
    }
}
