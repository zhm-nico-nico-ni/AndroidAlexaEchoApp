package com.ggec.voice.bluetoothconnect.proto.callback;

/**
 * Created by ggec on 2017/5/9.
 */

public interface IGetDeviceInfoCallback {
    void onSuccess(String productId, String dsn);
    void onFail(byte resCode);
}
