package com.ggec.voice.bluetoothconnect.bluetooth;

import java.util.concurrent.atomic.AtomicInteger;

public class SeqIdGenerator
{
    private final AtomicInteger mSeqId;
    
    public SeqIdGenerator() {
        this.mSeqId = new AtomicInteger((int)System.currentTimeMillis());
    }
    
    public int nextSeqId() {
        return this.mSeqId.incrementAndGet();
    }
}