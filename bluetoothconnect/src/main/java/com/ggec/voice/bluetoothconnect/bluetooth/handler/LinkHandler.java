package com.ggec.voice.bluetoothconnect.bluetooth.handler;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.ggec.voice.bluetoothconnect.bluetooth.BluetoothChatService;
import com.ggec.voice.bluetoothconnect.bluetooth.Constants;
import com.ggec.voice.bluetoothconnect.proto.IProtocol;
import com.ggec.voice.bluetoothconnect.proto.ProtoHelper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Created by ggec on 2017/5/9.
 */

public abstract class LinkHandler extends Handler {
    protected static final String TAG = "LinkHandler";

    protected final BluetoothChatService mChannel;
    private final Object mSendBufLock = new Object();

    private ByteBuffer mReadBuf;
    private ByteBuffer mProtoBuf;
    private ByteBuffer mOutBuf;
    private byte[] mBytesBuf;

    public LinkHandler(){
        mChannel = new BluetoothChatService(null, this);
    }

    public BluetoothChatService getBlueToothChannel() {
        return mChannel;
    }

    private void onData(ByteBuffer data){
        int uri = ProtoHelper.peekUri(data);
        short res = ProtoHelper.skipHeaderAndGetRes(data);
        if(ProtoHelper.RES_SUCCESS == res) {
            onDataImpl(uri, data, true);
        } else {
            Log.e("LinkHandler", "peek uri:"+ uri +" with unExcept res:"+res);
        }
    }

    public abstract void onDataImpl(int uri, ByteBuffer data, boolean skipHead);

    protected void sendData(IProtocol protocol) {
        ByteBuffer buffer = ProtoHelper.protoToByteBuffer(protocol.uri(), protocol);
        doSend(buffer);
    }

    private int doSend(ByteBuffer buf)
    {
        if ((buf == null) /*&& (this.mSendBuf == null)*/) {
            return -2;
        }
        try
        {
            if ((this.mChannel == null) || (!this.mChannel.isConnected()))
            {
                Log.e(TAG, "Socket trying to write null or not connected channel ");
                return -1;
            }
//            ByteBuffer crypted = null;
//            if ((this.mExchangeKeyHandler != null) && (buf != null)) {
//                crypted = this.mExchangeKeyHandler.encrypt(buf);
//            }
            ByteBuffer sendData = buf;
            synchronized (this.mSendBufLock)
            {
//                SendItem sendItem = null;
//                if (this.mSendBuf != null)
//                {
//                    Log.w(TAG, "send buffer data len: " + this.mSendBuf.capacity());
//                    if (crypted != null)
//                    {
//                        sendData = ByteBuffer.allocate(this.mSendBuf.capacity() + crypted.capacity());
//                        sendData.put(this.mSendBuf);
//                        sendData.put(crypted);
//                        sendData.flip();
//
//                        sendItem = new SendItem();
//                        sendItem.time = System.currentTimeMillis();
//                        sendItem.uri = ProtoHelper.peekUri(buf);
//                        sendItem.len = buf.capacity();
//                        sendItem.blocked = true;
//                        if (this.mSendItems.size() >= 256) {
//                            this.mSendItems.removeFirst();
//                        }
//                        this.mSendItems.addLast(sendItem);
//                    }
//                    else
//                    {
//                        sendData = this.mSendBuf;
//                    }
//                    this.mSendBuf = null;
//                }
//                if (sendData == null)
//                {
//                    sendData = crypted;
//                    if (buf != null)
//                    {
//                        sendItem = new SendItem();
//                        sendItem.time = System.currentTimeMillis();
//                        sendItem.uri = ProtoHelper.peekUri(buf);
//                        sendItem.len = buf.capacity();
//                        sendItem.blocked = false;
//                        if (this.mSendItems.size() >= 128) {
//                            this.mSendItems.removeFirst();
//                        }
//                        this.mSendItems.addLast(sendItem);
//                    }
//                }
                if (sendData != null)
                {
                    int writeLen = this.mChannel.write(sendData.array());
                    if (writeLen < 0) {
                        return writeLen;
                    }
//                    if (writeLen != sendData.capacity())
//                    {
//                        Log.w(TAG, "send data partly: " + writeLen + "/" + sendData.capacity());
//
//                        int leftLen = sendData.capacity() - writeLen;
//                        if (leftLen > 10240)
//                        {
//                            Log.e(TAG, "send buffer over limit");
//                            if (!SEND_ITEMS.contains(this.mSendItems))
//                            {
//                                if (SEND_ITEMS.size() >= 4) {
//                                    SEND_ITEMS.removeFirst();
//                                }
//                                SEND_ITEMS.addLast(this.mSendItems);
//                            }
//                            FLAG_OVERFLOW = true;
//
//                            onError();
//                            return -1;
//                        }
//                        this.mSendBuf = ByteBuffer.allocate(leftLen);
//                        this.mSendBuf.put(sendData.array(), writeLen, leftLen);
//                        this.mSendBuf.flip();
//                        if (sendItem != null) {
//                            sendItem.blocked = true;
//                        }
//                    }
                    return writeLen;
                }
                Log.e(TAG, "TCP doSend crypt failed");
                return 0;
            }

        }
        catch (IOException e)
        {
            Log.e(TAG, "Socket doSend exception, " + this.mChannel.isConnected(), e);
            onError();
            return -1;
        }
        catch (NullPointerException e)
        {
            Log.e(TAG, "Socket doSend exception, " + this.mChannel.isConnected(), e);
            return -1;
        }
    }

    private void onError(){

    }

    @Override
    public final void handleMessage(Message msg) {
        if (msg.what == Constants.MESSAGE_WRITE){
            Log.d(TAG, "write success bufferlength:" + msg.arg1 +" byte:" + Arrays.toString((byte[]) msg.obj));
        } else if(msg.what == Constants.MESSAGE_READ){
            Log.d(TAG, "read success, size:" + msg.arg1);
            handleRawData((byte[]) msg.obj, msg.arg1);
        } else if(msg.what == Constants.MESSAGE_STATE_CHANGE){
            Log.d(TAG, "MESSAGE_STATE_CHANGE, state:" + msg.arg1);
            if(BluetoothChatService.STATE_CONNECTED==msg.arg1) setBuffer();
            handleStateChange(msg.arg1);
        } else if(msg.what == Constants.MESSAGE_DEVICE_NAME){
            Bundle bundle = msg.getData();
            if(bundle!=null) {
                String name = bundle.getString(Constants.DEVICE_NAME);
                Log.d(TAG, "MESSAGE_DEVICE_NAME, name:" + name);
                handleConnect(name);
            }
        } else if(msg.what == Constants.MESSAGE_TOAST){
            Bundle bundle = msg.getData();
            if(bundle!=null) {
                String name = bundle.getString(Constants.TOAST);
                Log.d(TAG, "MESSAGE_TOAST, msg:" + name);
                handleToastMessage(name);
            }
        } else {

        }
    }

    protected void handleStateChange(int arg1) {

    }

    protected void handleToastMessage(String msg){

    }

    protected void handleConnect(String deviceName){

    }

    private void handleRawData(byte[] array, int limit) {
        Log.d(TAG, "read success, size:" + limit + " byte:" + Arrays.toString(array));
        if (this.mProtoBuf.capacity() - this.mProtoBuf.position() < limit) {
            ByteBuffer nbuf = ByteBuffer.allocate(this.mProtoBuf.position() + limit);
            nbuf.put(this.mProtoBuf.array(), 0, mProtoBuf.position());
            this.mProtoBuf = nbuf;
        }
        this.mProtoBuf.put(array, 0, limit);
        this.mProtoBuf.order(ByteOrder.LITTLE_ENDIAN);

        Log.d(TAG, "mProtoBuf:"+mProtoBuf.getInt(0)+"-"+ProtoHelper.peekLength(this.mProtoBuf)+mProtoBuf.toString()+"\n"+Arrays.toString(mProtoBuf.array()));

        if (this.mProtoBuf.capacity() >= 4) {
            final int protoLen = ProtoHelper.peekLength(this.mProtoBuf);

            if(protoLen<=0 || protoLen > 10000) {
                Log.e(TAG, "receive a large result, discard:"+protoLen+ "\n"+Arrays.toString(mProtoBuf.array()));
                setBuffer();
                return;
            }
            if (this.mProtoBuf.capacity() < protoLen) {
                Log.e(TAG, "peek length exceed buf capacity," + protoLen + " " +mProtoBuf.capacity());
                return;
            }
            if (this.mBytesBuf.length < protoLen) {
                this.mBytesBuf = new byte[protoLen];
            }
            this.mProtoBuf.flip();
            this.mProtoBuf.get(this.mBytesBuf, 0, protoLen);
            this.mProtoBuf.compact();
            if (this.mOutBuf.capacity() < protoLen) {
                this.mOutBuf = ByteBuffer.allocate(protoLen);
            } else {
                this.mOutBuf.limit(protoLen);
            }
            this.mOutBuf.clear();
            this.mOutBuf.put(this.mBytesBuf, 0, protoLen);
            this.mOutBuf.flip();
            final ByteBuffer data = ByteBuffer.allocate(this.mOutBuf.limit());
            data.order(ByteOrder.LITTLE_ENDIAN);
            this.mOutBuf.rewind();
            data.put(this.mOutBuf);
            this.mOutBuf.rewind();
            data.flip();
            onData(data);
            setBuffer();
        }
    }



    private void setBuffer(){
        int baseSize = 0;
        this.mOutBuf = ByteBuffer.allocate(baseSize);
        this.mProtoBuf = ByteBuffer.allocate(baseSize);
        mBytesBuf = new byte[2* baseSize];
    }
}
