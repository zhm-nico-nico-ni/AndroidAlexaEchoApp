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

    public String accessToken;
    public String refreshToken;
    public String codeVerify;
    public String clientId;

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
        return 4
                + ProtoHelper.calcMarshallSize(accessToken)
                + ProtoHelper.calcMarshallSize(refreshToken)
                + ProtoHelper.calcMarshallSize(clientId)
                + ProtoHelper.calcMarshallSize(codeVerify);
    }

    @Override
    public ByteBuffer marshall(ByteBuffer out) {
        out.putInt(seqId);
        ProtoHelper.marshall(out, accessToken);
        ProtoHelper.marshall(out, refreshToken);
        ProtoHelper.marshall(out, codeVerify);
        ProtoHelper.marshall(out, clientId);
        return out;
    }

    @Override
    public void unMarshall(ByteBuffer in) throws InvalidProtocolData {
        try {
            seqId = in.getInt();
            accessToken = ProtoHelper.unMarshallShortString(in);
            refreshToken = ProtoHelper.unMarshallShortString(in);
            codeVerify = ProtoHelper.unMarshallShortString(in);
            clientId = ProtoHelper.unMarshallShortString(in);
        } catch (BufferUnderflowException e) {
            throw new InvalidProtocolData(e);
        }
    }
}
