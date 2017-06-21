package com.willblaschko.android.alexa;


import com.ggec.voice.toollibrary.DiskLruCache;

import java.io.File;
import java.io.IOException;


/**
 * Created by ggec on 2017/6/21.
 */

public class DishLruCacheHelper {

    private static DishLruCacheHelper sDishLruCacheHelper;
    private DiskLruCache diskLruCache;

    public static DishLruCacheHelper getHelper() throws IOException {
        if (sDishLruCacheHelper == null) {
            sDishLruCacheHelper = new DishLruCacheHelper();
        }
        return sDishLruCacheHelper;
    }

    private DishLruCacheHelper() throws IOException {
        diskLruCache = DiskLruCache.open(new File(ConstParam.OctetStreamPath), 1, 1, 2* 1024 * 1024);
    }

    //////////////////////////////
    public DiskLruCache.Editor edit(String key) throws IOException {
        return diskLruCache.edit((key));
    }

    public synchronized DiskLruCache.Snapshot get(String key) throws IOException {
        return diskLruCache.get((key));
    }

}
