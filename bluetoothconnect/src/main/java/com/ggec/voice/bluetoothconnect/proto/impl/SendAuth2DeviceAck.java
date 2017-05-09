package com.ggec.voice.bluetoothconnect.proto.impl;

import com.ggec.voice.bluetoothconnect.proto.common.CommonAck;
import com.ggec.voice.bluetoothconnect.proto.common.ProtoURI;

/**
 * Created by ggec on 2017/5/8.
 */

public class SendAuth2DeviceAck extends CommonAck {


    public SendAuth2DeviceAck(){
        URI = ProtoURI.SendAuth2DeviceAckURI;
    }
}
