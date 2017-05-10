package com.ggec.voice.bluetoothconnect.proto.data;

import com.ggec.voice.bluetoothconnect.proto.InvalidProtocolData;
import com.ggec.voice.bluetoothconnect.proto.Marshallable;
import com.ggec.voice.bluetoothconnect.proto.ProtoHelper;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * Created by ggec on 2017/5/8.
 */

public class WifiScanInfo implements Marshallable {

    public String ssid;
    public int level; //The detected signal level in dBm, also known as the RSSI
    public String authtype;

    @Override
    public int size() {
        return ProtoHelper.calcMarshallSize(ssid) + ProtoHelper.calcMarshallSize(authtype) + 4;
    }

    @Override
    public ByteBuffer marshall(ByteBuffer out) {
        ProtoHelper.marshall(out, ssid);
        out.putInt(level);
        ProtoHelper.marshall(out, authtype);
        return out;
    }

    @Override
    public void unMarshall(ByteBuffer in) throws InvalidProtocolData {
        try{
            ssid = ProtoHelper.unMarshallShortString(in);
            level = in.getInt();
            authtype = ProtoHelper.unMarshallShortString(in);
        } catch (BufferUnderflowException e){
            throw new InvalidProtocolData(e);
        }
    }
}
