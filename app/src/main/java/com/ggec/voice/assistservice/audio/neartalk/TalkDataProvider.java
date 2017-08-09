package com.ggec.voice.assistservice.audio.neartalk;

import java.io.IOException;

import okio.Buffer;

/**
 * Created by ggec on 2017/8/9.
 */

public class TalkDataProvider {
    private boolean mIsEnd, mCanceled;
    private volatile long actuallyLong;
    private volatile long writeLength;
    private volatile Buffer mBuffer = new Buffer();

    public TalkDataProvider(String name) {

    }

    public synchronized void setEnd(long actuallyLong) {
        this.actuallyLong = actuallyLong;
        mIsEnd = true;
    }

    public boolean isEnd() {
        return mIsEnd;
    }

    public void cancel() {
        mCanceled = true;
        setEnd(-1);
    }

    public boolean isCanceled() {
        return mCanceled;
    }

//    public void doActuallyClose() throws IOException { // 这个调用好危险，最好别调用
//        mIsActuallyClose = true;
//        super.close();
//    }
//
//    public boolean getIsActuallyClose(){
//        return mIsActuallyClose;
//    }


    public synchronized long getActuallyLong() {
        return actuallyLong;
    }

    public synchronized long getWriteLength(){
        return writeLength;
    }

    public synchronized void write(byte b[], int off, int len) throws IOException {
        mBuffer.write(b, off, len);
        writeLength += len;
    }

    public synchronized int read(byte[] buffer) {
        return mBuffer.read(buffer);
    }
}
