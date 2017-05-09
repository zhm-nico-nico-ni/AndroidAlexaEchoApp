package com.ggec.voice.bluetoothconnect.proto.impl;

import com.ggec.voice.bluetoothconnect.proto.IProtocol;
import com.ggec.voice.bluetoothconnect.proto.InvalidProtocolData;
import com.ggec.voice.bluetoothconnect.proto.ProtoHelper;
import com.ggec.voice.bluetoothconnect.proto.common.ProtoURI;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * Created by ggec on 2017/5/8.
 */

public class SendAuth2DeviceReq implements IProtocol {
    public final static int URI = ProtoURI.SendAuth2DeviceReqURI;

    public int seqId;
    public String authCode;

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
        return 4 + ProtoHelper.calcMarshallSize(authCode);
    }

    @Override
    public ByteBuffer marshall(ByteBuffer out) {
        out.putInt(seqId);
        ProtoHelper.marshall(out, authCode);
        return out;
    }

    @Override
    public void unMarshall(ByteBuffer in) throws InvalidProtocolData {
        try {
            seqId = in.getInt();
            authCode = ProtoHelper.unMarshallShortString(in);
        } catch (BufferUnderflowException e) {
            throw new InvalidProtocolData(e);
        }
    }
}
