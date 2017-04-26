package com.ggec.voice.assistservice.audio.neartalk;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by ggec on 2017/4/25.
 */

public class NearTalkRandomAccessFile extends RandomAccessFile {
    private boolean mIsClose, mCanceled;
    private long actuallyLong;

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

    public void setActuallyLong(long actuallyLong){
        this.actuallyLong = actuallyLong;
    }

    public long getActuallyLong() {
        return actuallyLong;
    }

    @Override
    public long length() {
        try{
            return super.length();
        } catch (IOException ioe){
            ioe.printStackTrace();
        }
        return 0;
    }
}
