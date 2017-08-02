package com.ggec.voice.assistservice.audio.neartalk;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by ggec on 2017/4/25.
 */

public class NearTalkRandomAccessFile extends RandomAccessFile {
    private boolean mIsClose, mCanceled;
    private volatile long actuallyLong;
    private volatile long writeLength;

    public NearTalkRandomAccessFile(String name) throws FileNotFoundException {
        super(name, "rwd");
    }

    @Override
    public void close() {
        mIsClose = true;
    }

    public boolean isClose() {
        return mIsClose;
    }

    public void cancel() {
        mCanceled = true;
        close();
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

    public synchronized void setActuallyLong(long actuallyLong){
        this.actuallyLong = actuallyLong;
    }

    public synchronized long getActuallyLong() {
        return actuallyLong;
    }

    public synchronized long getWriteLength(){
        return writeLength;
    }

    public synchronized void write(byte b[], int off, int len) throws IOException {
        super.write(b, off, len);
        writeLength += len;
    }
}
