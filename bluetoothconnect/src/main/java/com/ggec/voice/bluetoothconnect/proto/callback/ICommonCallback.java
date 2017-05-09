package com.ggec.voice.bluetoothconnect.proto.callback;

/**
 * Created by ggec on 2017/5/9.
 */

public interface ICommonCallback {
    void onSuccess();
    void onFail(byte resCode);
}
