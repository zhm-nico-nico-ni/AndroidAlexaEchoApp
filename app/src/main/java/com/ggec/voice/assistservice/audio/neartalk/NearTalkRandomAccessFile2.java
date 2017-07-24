package com.ggec.voice.assistservice.audio.neartalk;


import com.ggec.voice.assistservice.wakeword.sphinx.frontend.DataProcessingException;
import com.ggec.voice.assistservice.wakeword.sphinx.frontend.DoubleData;

import java.io.FileNotFoundException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by ggec on 2017/4/25.
 */

public class NearTalkRandomAccessFile2 {
    private boolean mIsClose, mCanceled;
    private volatile long actuallyLong;
    private volatile long writeLength;
    private LinkedBlockingQueue<DoubleData> audioList = new LinkedBlockingQueue();

    public NearTalkRandomAccessFile2(String name) throws FileNotFoundException {

    }

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

    public synchronized void write(DoubleData data){
        audioList.add(data);
        writeLength += 1;
    }

    public synchronized DoubleData take(){
        if(audioList.isEmpty()) return null;

        DoubleData output = null;
        try {
            output = audioList.take();
        } catch (InterruptedException ie) {
            throw new DataProcessingException("cannot take Data from audioList", ie);
        }
        return output;
    }
}
