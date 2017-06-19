package com.willblaschko.android.alexa.interfaces.response;

import java.util.HashSet;

/**
 * Created by ggec on 2017/6/19.
 */

public class SingleFileLockHelper {
    private static SingleFileLockHelper sHelper;

    private SingleFileLockHelper() {
    }

    public static synchronized SingleFileLockHelper getHelper() {
        if (sHelper == null) sHelper = new SingleFileLockHelper();
        return sHelper;
    }

    private HashSet<String> isFileWriting = new HashSet<>();

    public void put(String path) {
        isFileWriting.add(path);
    }

    public void removeWritingFlag(String path){
        isFileWriting.remove(path);
    }

    public boolean getIsWriting(String path) {
        return isFileWriting.contains(path);
    }

}
