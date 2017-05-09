package com.ggec.voice.bluetoothconnect.proto;

import java.nio.BufferUnderflowException;

public class InvalidProtocolData extends Exception
{
    private static final long serialVersionUID = 1L;
    
    public InvalidProtocolData(final BufferUnderflowException e) {
        super("Invalid Protocol Data", e);
    }
    
    public InvalidProtocolData(final String error) {
        super(error);
    }
}
