package com.willblaschko.android.alexa.data.message.request.system;

import com.willblaschko.android.alexa.data.message.Payload;

/**
 * Created by ggec on 2017/6/1.
 *  RecognizerState is only required if your client uses Cloud-Based Wake Word Verification.
 */

public class RecognizerStatePayload extends Payload {
    private String wakeword;

    public RecognizerStatePayload(String wakeword){
        this.wakeword = wakeword;
    }
}
