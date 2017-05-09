package com.ggec.voice.bluetoothconnect.proto.impl;

import com.ggec.voice.bluetoothconnect.proto.IProtocol;
import com.ggec.voice.bluetoothconnect.proto.InvalidProtocolData;
import com.ggec.voice.bluetoothconnect.proto.ProtoHelper;
import com.ggec.voice.bluetoothconnect.proto.common.ProtoURI;
import com.ggec.voice.bluetoothconnect.proto.data.WifiScanInfo;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ggec on 2017/5/8.
 */

public class GetDeviceWifiScanRes implements IProtocol {
    public final static int URI = ProtoURI.GetDeviceWifiScansResURI;

    public int seqId;
    public byte resCode;
    public String message;

    public List<WifiScanInfo> data = new ArrayList<>();

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
        return 4+1+ ProtoHelper.calcMarshallSize(message)+ ProtoHelper.calcMarshallSize(data);
    }

    @Override
    public ByteBuffer marshall(ByteBuffer out) {
        out.putInt(seqId);
        out.put(resCode);
        ProtoHelper.marshall(out, message);
        ProtoHelper.marshall(out, data, WifiScanInfo.class);
        return out;
    }

    @Override
    public void unMarshall(ByteBuffer in) throws InvalidProtocolData {
        try {
            seqId = in.getInt();
            resCode = in.get();
            message = ProtoHelper.unMarshallShortString(in);
            ProtoHelper.unMarshall(in, data, WifiScanInfo.class);
        } catch (BufferUnderflowException e){
            throw new InvalidProtocolData(e);
        }
    }
}
