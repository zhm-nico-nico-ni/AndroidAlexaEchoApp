package com.ggec.voice.bluetoothconnect.proto.common;

/**
 * Created by ggec on 2017/5/8.
 */

public class ProtoURI {
    public final static int GROUP_PHONE_TO_DEVICE = 0x01;


    public final static int GetDeviceInfoReqURI = (1 << 8) | GROUP_PHONE_TO_DEVICE;
    public final static int GetDeviceInfoResURI = (2 << 8) | GROUP_PHONE_TO_DEVICE;

    public final static int SendAuth2DeviceReqURI = (3 << 8) | GROUP_PHONE_TO_DEVICE;
    public final static int SendAuth2DeviceAckURI = (4 << 8) | GROUP_PHONE_TO_DEVICE;

    public final static int GetDeviceWifiScansReqURI = (5 << 8) | GROUP_PHONE_TO_DEVICE;
    public final static int GetDeviceWifiScansResURI = (6 << 8) | GROUP_PHONE_TO_DEVICE;

    public final static int SendWifiConfig2DeviceReqURI = (7 << 8) | GROUP_PHONE_TO_DEVICE;
    public final static int SendWifiConfig2DeviceResURI = (8 << 8) | GROUP_PHONE_TO_DEVICE;
}
