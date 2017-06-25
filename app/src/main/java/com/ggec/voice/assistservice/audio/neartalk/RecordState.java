package com.ggec.voice.assistservice.audio.neartalk;

public class RecordState{
    public static final byte EMPTY = 0;
    public static final byte START = 1;
    public static final byte FINISH = 2;
    public static final byte CANCEL = 3;
    public static final byte ERROR = 4;
    public static final byte STOP_CAPTURE = 5;
    public static final byte LOCAL_RECORD_FINISH = 6;
}