package com.ggec.voice.bluetoothconnect.proto.impl;

import com.ggec.voice.bluetoothconnect.proto.IProtocol;
import com.ggec.voice.bluetoothconnect.proto.InvalidProtocolData;
import com.ggec.voice.bluetoothconnect.proto.ProtoHelper;
import com.ggec.voice.bluetoothconnect.proto.common.ProtoURI;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * Created by ggec on 2017/6/9.
 */

public class DeviceConnectStateBroadcast implements IProtocol {
    public final static int URI = ProtoURI.DeviceConnectStateBroadcastURI;

    public byte state;
    public String ssid;
    public String msg;

    @Override
    public int uri() {
        return URI;
    }

    @Override
    public int seq() {
        return 0;
    }

    @Override
    public int size() {
        return 1 + ProtoHelper.calcMarshallSize(ssid) + ProtoHelper.calcMarshallSize(msg);
    }

    @Override
    public ByteBuffer marshall(ByteBuffer out) {
        out.put(state);
        ProtoHelper.marshall(out, ssid);
        ProtoHelper.marshall(out, msg);
        return out;
    }

    @Override
    public void unMarshall(ByteBuffer in) throws InvalidProtocolData {
        try {
            state = in.get();
            ssid = ProtoHelper.unMarshallShortString(in);
            msg = ProtoHelper.unMarshallShortString(in);
        } catch (BufferUnderflowException e) {
            throw new InvalidProtocolData(e);
        }
    }
}