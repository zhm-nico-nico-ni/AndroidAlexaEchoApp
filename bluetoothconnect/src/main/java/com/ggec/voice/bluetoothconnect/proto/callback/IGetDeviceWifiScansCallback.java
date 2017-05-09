package com.ggec.voice.bluetoothconnect.proto.callback;


import com.ggec.voice.bluetoothconnect.proto.data.WifiScanInfo;

import java.util.List;

/**
 * Created by ggec on 2017/5/9.
 */

public interface IGetDeviceWifiScansCallback {
    void onSuccess(List<WifiScanInfo> list);

    void onFail(byte resCode);
}
