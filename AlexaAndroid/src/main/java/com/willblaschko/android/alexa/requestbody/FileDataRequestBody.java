package com.willblaschko.android.alexa.requestbody;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * Created by ggec on 2017/4/5.
 */

public class FileDataRequestBody {
    public static RequestBody createRequestBody(String file) {
        return RequestBody.create(MediaType.parse("application/octet-stream"),
                new File(file));
    }

}
