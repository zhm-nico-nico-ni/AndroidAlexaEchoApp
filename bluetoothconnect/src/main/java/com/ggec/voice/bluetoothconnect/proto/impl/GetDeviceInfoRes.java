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

public class GetDeviceInfoRes implements IProtocol {
    public final static int URI = ProtoURI.GetDeviceInfoResURI;

    public int seqId;
    public byte resCode;
    public String message;
    public String productId;
    public String deviceSerialNumber;

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
        return 4 + 1 + ProtoHelper.calcMarshallSize(message) + ProtoHelper.calcMarshallSize(productId) + ProtoHelper.calcMarshallSize(deviceSerialNumber);
    }

    @Override
    public ByteBuffer marshall(ByteBuffer out) {
        out.putInt(seqId);
        out.put(resCode);

        ProtoHelper.marshall(out, message);
        ProtoHelper.marshall(out, productId);
        ProtoHelper.marshall(out, deviceSerialNumber);
        return out;
    }

    @Override
    public void unMarshall(ByteBuffer in) throws InvalidProtocolData {
        try {
            seqId = in.getInt();
            resCode = in.get();

            message = ProtoHelper.unMarshallShortString(in);
            productId = ProtoHelper.unMarshallShortString(in);
            deviceSerialNumber = ProtoHelper.unMarshallShortString(in);
        } catch (BufferUnderflowException e) {
            throw new InvalidProtocolData(e);
        }
    }
}
