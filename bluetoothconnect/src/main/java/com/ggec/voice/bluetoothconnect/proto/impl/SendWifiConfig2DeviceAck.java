package com.ggec.voice.bluetoothconnect.proto.impl;

import com.ggec.voice.bluetoothconnect.proto.common.CommonAck;
import com.ggec.voice.bluetoothconnect.proto.common.ProtoURI;

/**
 * Created by ggec on 2017/5/8.
 */

public class SendWifiConfig2DeviceAck extends CommonAck {

    public SendWifiConfig2DeviceAck(){
        URI = ProtoURI.SendWifiConfig2DeviceResURI;
    }
}
