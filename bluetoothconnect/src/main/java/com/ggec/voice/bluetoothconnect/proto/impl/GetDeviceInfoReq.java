package com.ggec.voice.bluetoothconnect.proto.impl;

import com.ggec.voice.bluetoothconnect.proto.IProtocol;
import com.ggec.voice.bluetoothconnect.proto.InvalidProtocolData;
import com.ggec.voice.bluetoothconnect.proto.common.ProtoURI;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * Created by ggec on 2017/5/8.
 */

public class GetDeviceInfoReq implements IProtocol {
    public final static int URI = ProtoURI.GetDeviceInfoReqURI;

    public int seqId;

    @Override
    public int uri() {
        return URI;
    }

    @Override
    public int seq() {
        return seqId;
    }

    @Override
    public int size() {
        return 4;
    }

    @Override
    public ByteBuffer marshall(ByteBuffer out) {
        out.putInt(seqId);
        return out;
    }

    @Override
    public void unMarshall(ByteBuffer in) throws InvalidProtocolData {
        try {
            seqId = in.getInt();
        } catch (BufferUnderflowException e) {
            throw new InvalidProtocolData(e);
        }
    }
}
