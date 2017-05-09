package com.ggec.voice.bluetoothconnect.proto;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.util.Log;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.Map;

public final class ProtoHelper
{
    public static final int PACKET_LEN_POSITION = 0;
    public static final int PACKET_URI_POSITION = 4;
    public static final int PACKET_RES_POSITION = 8;
    public static final int HEAD_LEN = 10;
    public static final int RES_SUCCESS = 200;
    private static final String TAG = "ProtoHelper";
    public static final int PROTOBUF_VERSION = 0;
    public static final int STRING_LEN_MAX_LIMIT = 256;
    
    public static ByteBuffer protoToByteBuffer(final int uri, final Marshallable msg) {
        final int size = msg.size();
        ByteBuffer bb = ByteBuffer.allocate(size + HEAD_LEN);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(size + HEAD_LEN);
        bb.putInt(uri);
        bb.putShort((short)RES_SUCCESS);
        bb = msg.marshall(bb);
        bb.flip();
        return bb;
    }
    
    public static void skipHeader(final ByteBuffer bb) {
        bb.position(HEAD_LEN);
    }
    
    public static short skipHeaderAndGetRes(final ByteBuffer bb) {
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.position(PACKET_RES_POSITION);
        return bb.getShort();
    }
    
    public static int peekUri(final ByteBuffer message) {
        message.order(ByteOrder.LITTLE_ENDIAN);
        return message.getInt(PACKET_URI_POSITION);
    }
    
    public static int peekLength(final ByteBuffer message) {
        return message.getInt(PACKET_LEN_POSITION);
    }
    
    public static void marshall(final ByteBuffer bb, final byte[] data) {
        if (data != null) {
            bb.putShort((short)data.length);
            bb.put(data);
        }
        else {
            bb.putShort((short)0);
        }
    }
    
    public static void marshall(final ByteBuffer bb, final String string) {
        if (string != null && string.length() > 0) {
            final byte[] data = string.getBytes();
            bb.putShort((short)data.length);
            bb.put(data);
        }
        else {
            bb.putShort((short)0);
        }
    }
    
    public static byte[] unMarshallByteArray(final ByteBuffer bb) throws InvalidProtocolData {
        try {
            byte[] data = null;
            final short byteLen = bb.getShort();
            if (byteLen < 0) {
                throw new InvalidProtocolData("byteLen < 0");
            }
            if (byteLen > 0) {
                data = new byte[byteLen];
                bb.get(data);
                return data;
            }
            return null;
        }
        catch (BufferUnderflowException e) {
            throw new InvalidProtocolData(e);
        }
    }
    
    public static int calcMarshallSize(final byte[] byteArray) {
        if (byteArray != null) {
            return 2 + byteArray.length;
        }
        return 2;
    }
    
    public static int calcMarshallSize(final String string) {
        if (string != null) {
            return 2 + string.getBytes().length;
        }
        return 2;
    }
    
    public static <T> ByteBuffer marshall(ByteBuffer bb, final Collection<T> data, final Class<T> elemClass) {
        if (data == null || data.size() == 0) {
            bb.putInt(0);
        }
        else {
            bb.putInt(data.size());
            for (final T elem : data) {
                if (elemClass == Integer.class) {
                    bb.putInt((Integer) elem);
                }
                else if (elemClass == Short.class) {
                    bb.putShort((Short) elem);
                }
                else if (elemClass == Byte.class) {
                    bb.put((Byte)elem);
                }
                else if (elemClass == Long.class) {
                    bb.putLong((Long)elem);
                }
                else if (elemClass == String.class) {
                    marshall(bb, (String)elem);
                }
                else if (elemClass == byte[].class) {
                    marshall(bb, (byte[])(Object)elem);
                }
                else {
                    if (!(elem instanceof Marshallable)) {
                        throw new RuntimeException("unable to marshal element of class " + elemClass.getName());
                    }
                    bb = ((Marshallable)elem).marshall(bb);
                }
            }
        }
        return bb;
    }

    @SuppressWarnings("unchecked")
    public static <T> void unMarshall(final ByteBuffer bb, final Collection<T> data, final Class<T> elemClass) throws InvalidProtocolData {
        try {
            for (int size = bb.getInt(), i = 0; i < size; ++i) {
                try {
                    T elem = null;
                    if (elemClass == Integer.class) {
                        elem = (T)Integer.valueOf(bb.getInt());
                    }
                    else if (elemClass == Short.class) {
                        elem = (T)Short.valueOf(bb.getShort());
                    }
                    else if (elemClass == Byte.class) {
                        elem = (T)Byte.valueOf(bb.get());
                    }
                    else if (elemClass == Long.class) {
                        elem = (T)Long.valueOf(bb.getLong());
                    }
                    else if (elemClass == String.class) {
                        elem = (T)unMarshallShortString(bb);
                    }
                    else if (elemClass == byte[].class) {
                        elem = (T)(Object)unMarshallByteArray(bb);
                    }
                    else {
                        elem = elemClass.newInstance();
                        if (elem instanceof Marshallable) {
                            ((Marshallable)elem).unMarshall(bb);
                        }
                        else {
                            Log.e("ProtoHelper", "IProtoHelper::unMarshall invalid elemClass type " + elemClass.getName());
                        }
                    }
                    data.add(elem);
                }
                catch (Exception e) {
                    Log.w("ProtoHelper", "unmarshal failed", e);
                }
            }
        }
        catch (BufferUnderflowException e2) {
            throw new InvalidProtocolData(e2);
        }
    }
    
    public static <T> int calcMarshallSize(final Collection<T> data) {
        int pkgSize = 4;
        if (data != null) {
            for (final T elem : data) {
                if (elem instanceof Integer) {
                    pkgSize += 4;
                }
                else if (elem instanceof Short) {
                    pkgSize += 2;
                }
                else if (elem instanceof Byte) {
                    ++pkgSize;
                }
                else if (elem instanceof Long) {
                    pkgSize += 8;
                }
                else if (elem instanceof Marshallable) {
                    pkgSize += ((Marshallable)elem).size();
                }
                else if (elem instanceof String) {
                    pkgSize += calcMarshallSize((String)elem);
                }
                else {
                    if (!(elem instanceof byte[])) {
                        throw new IllegalStateException("IProtoHelper::calcMarshallSize invalid T type:" + elem);
                    }
                    pkgSize += calcMarshallSize((byte[])(Object)elem);
                }
            }
        }
        return pkgSize;
    }
    
    public static <K, T> ByteBuffer marshall(ByteBuffer bb, final Map<K, T> data, final Class<T> elemClass) {
        if (data == null || data.size() == 0) {
            bb.putInt(0);
        }
        else {
            bb.putInt(data.size());
            for (final Map.Entry<K, T> entry : data.entrySet()) {
                final K key = entry.getKey();
                if (key instanceof Short) {
                    bb.putShort((Short)key);
                }
                else if (key instanceof Integer) {
                    bb.putInt((Integer) key);
                }
                else if (key instanceof Byte) {
                    bb.put((Byte)key);
                }
                else if (key instanceof Long) {
                    bb.putLong((Long)key);
                }
                else if (key instanceof String) {
                    marshall(bb, (String)key);
                }
                else {
                    if (!(key instanceof byte[])) {
                        throw new IllegalStateException("marshall Map but unknown key type: " + key.getClass().getName());
                    }
                    marshall(bb, (byte[])(Object)key);
                }
                final T elem = entry.getValue();
                if (elemClass == Integer.class) {
                    bb.putInt((Integer) elem);
                }
                else if (elemClass == Short.class) {
                    bb.putShort((Short)elem);
                }
                else if (elemClass == Byte.class) {
                    bb.put((Byte)elem);
                }
                else if (elemClass == Long.class) {
                    bb.putLong((Long)elem);
                }
                else if (elem instanceof Marshallable) {
                    bb = ((Marshallable)elem).marshall(bb);
                }
                else if (elem instanceof String) {
                    marshall(bb, (String)elem);
                }
                else {
                    if (!(elem instanceof byte[])) {
                        throw new IllegalStateException("marshall Map but unknown value type: " + elem.getClass().getName());
                    }
                    marshall(bb, (byte[])(Object)elem);
                }
            }
        }
        return bb;
    }
    
    @SuppressLint({ "UseSparseArrays" })
    public static <K, T> void unMarshall(final ByteBuffer bb, final Map<K, T> out, final Class<K> keyClass, final Class<T> elemClass) throws InvalidProtocolData {
        try {
            for (int size = bb.getInt(), i = 0; i < size; ++i) {
                K key = null;
                if (keyClass == Short.class) {
                    key = (K)Short.valueOf(bb.getShort());
                }
                else if (keyClass == Integer.class) {
                    key = (K)Integer.valueOf(bb.getInt());
                }
                else if (keyClass == Byte.class) {
                    key = (K)Byte.valueOf(bb.get());
                }
                else if (keyClass == Long.class) {
                    key = (K)Long.valueOf(bb.getLong());
                }
                else if (keyClass == byte[].class) {
                    key = (K)(Object)unMarshallByteArray(bb);
                }
                else {
                    if (keyClass != String.class) {
                        throw new IllegalStateException("unMarshall Map but unknown key type: " + keyClass.getName());
                    }
                    key = (K)unMarshallShortString(bb);
                }
                try {
                    T elem = null;
                    if (elemClass == Integer.class) {
                        elem = (T)Integer.valueOf(bb.getInt());
                    }
                    else if (elemClass == Short.class) {
                        elem = (T)Short.valueOf(bb.getShort());
                    }
                    else if (elemClass == Byte.class) {
                        elem = (T)Byte.valueOf(bb.get());
                    }
                    else if (elemClass == Long.class) {
                        elem = (T)Long.valueOf(bb.getLong());
                    }
                    else if (elemClass == byte[].class) {
                        elem = (T)(Object)unMarshallByteArray(bb);
                    }
                    else if (elemClass == String.class) {
                        elem = (T)unMarshallShortString(bb);
                    }
                    else {
                        elem = elemClass.newInstance();
                        if (!(elem instanceof Marshallable)) {
                            throw new IllegalStateException("unMarshall Map but unknown value type: " + elemClass.getName());
                        }
                        ((Marshallable)elem).unMarshall(bb);
                    }
                    out.put(key, elem);
                }
                catch (Exception e) {
                    Log.w("ProtoHelper", "unmarshal failed", e);
                }
            }
        }
        catch (BufferUnderflowException e2) {
            throw new InvalidProtocolData(e2);
        }
    }
    
    public static <K, T> int calcMarshallSize(final Map<K, T> data) {
        int pkgSize = 4;
        if (data != null) {
            for (final Map.Entry<K, T> entry : data.entrySet()) {
                final K key = entry.getKey();
                if (key instanceof Short) {
                    pkgSize += 2;
                }
                else if (key instanceof Integer) {
                    pkgSize += 4;
                }
                else if (key instanceof Long) {
                    pkgSize += 8;
                }
                else if (key instanceof byte[]) {
                    pkgSize += calcMarshallSize((byte[])(Object)key);
                }
                else if (key instanceof String) {
                    pkgSize += calcMarshallSize((String)key);
                }
                else {
                    if (!(key instanceof Byte)) {
                        throw new IllegalStateException("calcMarshallSize Map but unknown key type: " + key.getClass().getName());
                    }
                    ++pkgSize;
                }
                final T value = entry.getValue();
                if (value instanceof Integer) {
                    pkgSize += 4;
                }
                else if (value instanceof Short) {
                    pkgSize += 2;
                }
                else if (value instanceof Long) {
                    pkgSize += 8;
                }
                else if (value instanceof Marshallable) {
                    pkgSize += ((Marshallable)value).size();
                }
                else if (value instanceof String) {
                    pkgSize += calcMarshallSize((String)value);
                }
                else if (value instanceof byte[]) {
                    pkgSize += calcMarshallSize((byte[])(Object)value);
                }
                else {
                    if (!(value instanceof Byte)) {
                        throw new IllegalStateException("calcMarshallSize Map but unknown value type: " + value);
                    }
                    ++pkgSize;
                }
            }
        }
        return pkgSize;
    }
    
    public static String unMarshallShortString(final ByteBuffer bb) throws InvalidProtocolData {
        try {
            final short byteLen = bb.getShort();
            if (byteLen < 0) {
                throw new InvalidProtocolData("byteLen < 0");
            }
            if (byteLen > 0) {
                final byte[] data = new byte[byteLen];
                bb.get(data);
                return new String(data);
            }
            return null;
        }
        catch (BufferUnderflowException e) {
            throw new InvalidProtocolData(e);
        }
    }
    
    public static String limitStringLength(String input, final int length) {
        if (TextUtils.isEmpty((CharSequence)input)) {
            return input;
        }
        if (input.length() > length) {
            input = input.substring(0, length);
        }
        return input;
    }
    
    public static <T> void limitStringLengthForMap(final Map<T, String> values, final int length) {
        if (values == null || values.size() == 0) {
            return;
        }
        for (final T key : values.keySet()) {
            final String oldValue = values.get(key);
            final String newValue = limitStringLength(oldValue, length);
            if (!TextUtils.equals((CharSequence)oldValue, (CharSequence)newValue)) {
                values.put(key, newValue);
            }
        }
    }
}
