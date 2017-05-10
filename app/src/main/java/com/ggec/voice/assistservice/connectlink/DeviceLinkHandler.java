package com.ggec.voice.assistservice.connectlink;

import android.provider.Settings;

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
import com.willblaschko.android.alexa.SharedPreferenceUtil;

import java.nio.ByteBuffer;

/**
 * Created by ggec on 2017/5/9.
 */

public class DeviceLinkHandler extends LinkHandler {

    public DeviceLinkHandler() {
        super();
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

            GetDeviceInfoRes ack = new GetDeviceInfoRes();
            ack.seqId = received.seqId;
            ack.resCode = ProtoResult.SUCCESS;
            ack.deviceSerialNumber = BuildConfig.PRODUCT_ID;
            ack.productId = Settings.Secure.getString(MyApplication.getContext().getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            sendData(ack);
        } else if (uri == ProtoURI.SendAuth2DeviceReqURI) {
            SendAuth2DeviceReq received = new SendAuth2DeviceReq();
            try {
                received.unMarshall(data);
            } catch (InvalidProtocolData invalidProtocolData) {
                invalidProtocolData.printStackTrace();
                return;
            }

            boolean res = SharedPreferenceUtil.putAuthToken2(MyApplication.getContext(), received.clientId,
                    received.codeVerify, received.accessToken, received.refreshToken, 0);

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
//            ack.data.add();

            sendData(ack);
        } else if (uri == ProtoURI.SendWifiConfig2DeviceReqURI) {
            SendWifiConfig2DeviceReq req = new SendWifiConfig2DeviceReq();
            try {
                req.unMarshall(data);

            } catch (InvalidProtocolData invalidProtocolData) {
                invalidProtocolData.printStackTrace();
            }

            SendWifiConfig2DeviceAck ack = new SendWifiConfig2DeviceAck();
            ack.resCode = ProtoResult.SUCCESS;
            ack.seqId = req.seqId;

            sendData(ack);
        }
    }


}
