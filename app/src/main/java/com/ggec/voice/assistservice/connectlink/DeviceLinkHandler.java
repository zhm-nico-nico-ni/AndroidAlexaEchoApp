package com.ggec.voice.assistservice.connectlink;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;

import com.ggec.voice.assistservice.BuildConfig;
import com.ggec.voice.assistservice.MyApplication;
import com.ggec.voice.bluetoothconnect.bluetooth.handler.LinkHandler;
import com.ggec.voice.bluetoothconnect.proto.InvalidProtocolData;
import com.ggec.voice.bluetoothconnect.proto.common.ProtoResult;
import com.ggec.voice.bluetoothconnect.proto.common.ProtoURI;
import com.ggec.voice.bluetoothconnect.proto.impl.GetDeviceInfoReq;
import com.ggec.voice.bluetoothconnect.proto.impl.GetDeviceInfoRes;
import com.ggec.voice.bluetoothconnect.proto.impl.GetDeviceWifiScanReq;
import com.ggec.voice.bluetoothconnect.proto.impl.GetDeviceWifiScanRes;
import com.ggec.voice.bluetoothconnect.proto.impl.SendAuth2DeviceAck;
import com.ggec.voice.bluetoothconnect.proto.impl.SendAuth2DeviceReq;
import com.ggec.voice.bluetoothconnect.proto.impl.SendWifiConfig2DeviceAck;
import com.ggec.voice.bluetoothconnect.proto.impl.SendWifiConfig2DeviceReq;
import com.ggec.voice.toollibrary.Util;
import com.ggec.voice.toollibrary.log.Log;
import com.willblaschko.android.alexa.SharedPreferenceUtil;

import java.nio.ByteBuffer;

/**
 * Created by ggec on 2017/5/9.
 */

public class DeviceLinkHandler extends LinkHandler {
    private final IDeviceLinkCallback mIDeviceLinkCallback;
    public DeviceLinkHandler(IDeviceLinkCallback callback) {
        super();
        mIDeviceLinkCallback = callback;
        if (BluetoothAdapter.getDefaultAdapter()!=null&& BluetoothAdapter.getDefaultAdapter().getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) { // FIXME remove ! because system will do this
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
            discoverableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            MyApplication.getContext().startActivity(discoverableIntent);
        }
        String id = Util.getProductId(MyApplication.getContext());

        mChannel.setDeviceName("GGEC_BOX_"+id);
        mChannel.start();
    }

    @Override
    public void onDataImpl(int uri, ByteBuffer data, boolean skipHead) {
        if (uri == ProtoURI.GetDeviceInfoReqURI) {
            GetDeviceInfoReq received = new GetDeviceInfoReq();
            try {
                received.unMarshall(data);
            } catch (InvalidProtocolData invalidProtocolData) {
                invalidProtocolData.printStackTrace();
                return;
            }

            Log.d(TAG, "receive GetDeviceInfoReq:"+ received.seqId);
            GetDeviceInfoRes ack = new GetDeviceInfoRes();
            ack.seqId = received.seqId;
            ack.resCode = ProtoResult.SUCCESS;
            ack.deviceSerialNumber = Util.getProductId(MyApplication.getContext());;
            ack.productId = BuildConfig.PRODUCT_ID;
            sendData(ack);
        } else if (uri == ProtoURI.SendAuth2DeviceReqURI) {
            SendAuth2DeviceReq received = new SendAuth2DeviceReq();
            try {
                received.unMarshall(data);
            } catch (InvalidProtocolData invalidProtocolData) {
                invalidProtocolData.printStackTrace();
                return;
            }

            Log.d(TAG, "receive SendAuth2DeviceReq:"+ received.seqId + "\naccess:"+received.accessToken+ "\nrefresh:"+received.refreshToken+"\nverify:"+received.codeVerify);
            boolean res = SharedPreferenceUtil.putAuthToken2(MyApplication.getContext(), received.clientId,
                    received.codeVerify, received.accessToken, received.refreshToken, 300);

            SendAuth2DeviceAck ack = new SendAuth2DeviceAck();
            ack.seqId = received.seqId;
            ack.resCode = res ? ProtoResult.SUCCESS : ProtoResult.FAIL;
            sendData(ack);
        } else if (uri == ProtoURI.GetDeviceWifiScansReqURI) {
            GetDeviceWifiScanReq received = new GetDeviceWifiScanReq();
            try {
                received.unMarshall(data);
            } catch (InvalidProtocolData invalidProtocolData) {
                invalidProtocolData.printStackTrace();
                return;
            }

            GetDeviceWifiScanRes ack = new GetDeviceWifiScanRes();
            ack.seqId = received.seqId;
            ack.resCode = ProtoResult.SUCCESS;
            ack.data.addAll(WifiControl.getInstance(MyApplication.getContext()).getScanResult());

            Log.d(TAG, "Scan size:"+ack.data.size());
            sendData(ack);
        } else if (uri == ProtoURI.SendWifiConfig2DeviceReqURI) {
            final SendWifiConfig2DeviceReq req = new SendWifiConfig2DeviceReq();
            try {
                req.unMarshall(data);

            } catch (InvalidProtocolData invalidProtocolData) {
                invalidProtocolData.printStackTrace();
                return;
            }
            Log.d(TAG, "recv SendWifiConfig2DeviceReq, try connect Wifi ssid:"+req.ssid + " psw:"+req.password+" ca:"+req.capabilities );
            WifiControl.getInstance(MyApplication.getContext()).addNetwork(req.ssid, req.password,
                    req.capabilities, new WifiControl.IAddNetWorkCallBack() {
                @Override
                public void onConnect(byte result, String msg) {
                    SendWifiConfig2DeviceAck ack = new SendWifiConfig2DeviceAck();
                    ack.resCode = result;
                    ack.seqId = req.seqId;
                    ack.message = msg;
                    sendData(ack);
                }
            });
            mIDeviceLinkCallback.onConnectingWifi();
        }
    }

    public interface IDeviceLinkCallback{
        void onConnectingWifi();
    }
}
