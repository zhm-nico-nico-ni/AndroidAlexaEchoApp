package com.ggec.voice.bluetoothconnect.bluetooth;

import com.ggec.voice.bluetoothconnect.proto.IProtocol;

/**
 * Created by ggec on 2017/5/9.
 */

public class ProtoQueueObject {
    public final IProtocol mProtocol;
    public final Object mListener;

    public ProtoQueueObject(IProtocol protocol, Object listener) {
        mProtocol = protocol;
        mListener = listener;
    }
}
