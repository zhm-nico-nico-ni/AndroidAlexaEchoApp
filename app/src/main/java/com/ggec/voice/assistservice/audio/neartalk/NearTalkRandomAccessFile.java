package com.ggec.voice.assistservice.audio.neartalk;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by ggec on 2017/4/25.
 */

public class NearTalkRandomAccessFile extends RandomAccessFile {
    private volatile boolean mIsClose, mCanceled, mIsActuallyClose;
    private volatile long actuallyLong;

    public NearTalkRandomAccessFile(String name) throws FileNotFoundException {
        super(name, "rws");
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
        try {
            doActuallyClose();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isCanceled() {
        return mCanceled;
    }

    public void doActuallyClose() throws IOException {
        mIsActuallyClose = true;
        super.close();
    }

    public boolean getIsActuallyClose(){
        return mIsActuallyClose;
    }

    public void setActuallyLong(long actuallyLong){
        this.actuallyLong = actuallyLong;
    }

    public long getActuallyLong() {
        return actuallyLong;
    }
}
