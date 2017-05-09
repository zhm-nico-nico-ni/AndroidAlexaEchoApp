package com.ggec.voice.bluetoothconnect.proto.common;


import com.ggec.voice.bluetoothconnect.proto.IProtocol;
import com.ggec.voice.bluetoothconnect.proto.InvalidProtocolData;
import com.ggec.voice.bluetoothconnect.proto.ProtoHelper;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * Created by ggec on 2017/5/8.
 */

public class CommonAck implements IProtocol {

    protected static int URI;

    public int seqId;
    public byte resCode;
    public String message;

    @Override
    public int size() {
        return 9 + ProtoHelper.calcMarshallSize(message);
    }

    @Override
    public ByteBuffer marshall(ByteBuffer out) {
        out.putInt(seqId);
        out.put(resCode);
        ProtoHelper.marshall(out, message);

        return out;
    }

    @Override
    public void unMarshall(ByteBuffer in) throws InvalidProtocolData {
        try{
            seqId = in.getInt();
            resCode = in.get();
            message = ProtoHelper.unMarshallShortString(in);
        } catch (BufferUnderflowException e){
            throw new InvalidProtocolData(e);
        }
    }

    @Override
    public int uri() {
        return URI;
    }

    @Override
    public int seq() {
        return seqId;
    }
}
