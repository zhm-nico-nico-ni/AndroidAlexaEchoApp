package com.ggec.voice.bluetoothconnect.proto;

import java.nio.ByteBuffer;

public interface Marshallable
{
    int size();
    
    ByteBuffer marshall(ByteBuffer out);
    
    void unMarshall(ByteBuffer in) throws InvalidProtocolData;
}
