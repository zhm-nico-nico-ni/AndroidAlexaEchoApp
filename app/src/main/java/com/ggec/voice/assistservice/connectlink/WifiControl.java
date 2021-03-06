package com.ggec.voice.assistservice.connectlink;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.text.TextUtils;

import com.ggec.voice.bluetoothconnect.proto.common.ProtoResult;
import com.ggec.voice.bluetoothconnect.proto.data.WifiScanInfo;
import com.ggec.voice.toollibrary.Util;
import com.ggec.voice.toollibrary.log.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by ggec on 2017/5/10.
 */

public class WifiControl {
    private final static String TAG = "WifiControl";


    private static volatile WifiControl sInstance;


    public synchronized static WifiControl getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new WifiControl(context);
        }
        return sInstance;
    }

    public static void deInit() {
        if (sInstance != null) {
            sInstance.release();
            sInstance = null;
        }
    }

    private Context mContext;
    private final WifiManager mWifiManager;
    private BroadcastReceiver mWifiConnectBroadCast;
    private IControlListener mIControlListener;
    private String mConnectingSSID;

    private IAddNetWorkCallBack mIAddNetWorkCallBack;
    private Handler handler = new Handler();
    private Runnable timeOutRunner = new Runnable() {
        @Override
        public void run() {
            Log.e(TAG, "wifi connected time out");
            unRegisterListener(ProtoResult.TIME_OUT, null, null);
            mIAddNetWorkCallBack = null;
        }
    };

    private WifiControl(Context context) {
        this.mContext = context;
        mWifiManager = (WifiManager) this.mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    public void addNetwork(String ssid, String password, String authAlgorithm, IAddNetWorkCallBack callBack) {
        if (!isWifiEnabled()) {
            callBack.onConnect(ProtoResult.FAIL, "Wifi now is disable.");
            mIAddNetWorkCallBack = null;
            return;
        } else if (Util.isWifiAvailable(mContext)) { // 当前ssid 是同一个
            Log.d(TAG, "ConnectionInfo:"+mWifiManager.getConnectionInfo().toString() + " \n co:"+ssid);
            if (TextUtils.equals("\"" + ssid + "\"", mWifiManager.getConnectionInfo().getSSID())) {
                Log.d(TAG, "Same Wifi already connected");
                callBack.onConnect(ProtoResult.SUCCESS, "Same Wifi already connected.");
                mIAddNetWorkCallBack = null;
                return;
            }
            Log.d(TAG, "current wifi available, but not same ssid");
        }
        if (authAlgorithm == null) authAlgorithm = "";
        mIAddNetWorkCallBack = callBack;

        // 配置网络信息类
        WifiConfiguration customerWifiConfig = new WifiConfiguration();
        // 清空配置网络属性
        customerWifiConfig.allowedAuthAlgorithms.clear();
        customerWifiConfig.allowedGroupCiphers.clear();
        customerWifiConfig.allowedKeyManagement.clear();
        customerWifiConfig.allowedPairwiseCiphers.clear();
        customerWifiConfig.allowedProtocols.clear();

        { // wifi连接
            customerWifiConfig.SSID = ("\"" + ssid + "\"");
            // 检测热点是否已存在
            WifiConfiguration tempConfiguration = isExists(ssid);
            if (tempConfiguration != null) {
                mWifiManager.removeNetwork(tempConfiguration.networkId); // 从列表中删除指定的网络配置网络
            }


            if (authAlgorithm.contains("WPA")) { // WPA_PSK加密
                customerWifiConfig.preSharedKey = ("\"" + password + "\"");
                customerWifiConfig.hiddenSSID = true;
                customerWifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);

                customerWifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                customerWifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                customerWifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                customerWifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                customerWifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                customerWifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                customerWifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                customerWifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                customerWifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

            } else if (authAlgorithm.contains("WEP")) { // WEP密码
                customerWifiConfig.hiddenSSID = true;
                customerWifiConfig.wepKeys[0] = ("\"" + password + "\"");

                customerWifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                customerWifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                customerWifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                customerWifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                customerWifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                customerWifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                customerWifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                customerWifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                customerWifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            } else {// 没有密码
                customerWifiConfig.wepKeys[0] = "";
                customerWifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                customerWifiConfig.wepTxKeyIndex = 0;

//                customerWifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                customerWifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                customerWifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                customerWifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                customerWifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                customerWifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                customerWifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                customerWifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                customerWifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                customerWifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            }
        }

        addNetworkImpl(customerWifiConfig);

    }

    /**
     * 添加并连接指定网络
     **/
    private void addNetworkImpl(WifiConfiguration paramWifiConfiguration) {
        if (mWifiConnectBroadCast == null) {
            mWifiConnectBroadCast = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    WifiInfo wifiInfo = mWifiManager.getConnectionInfo();

                    Log.d(TAG, "isWifiAvailable connecting:"+mConnectingSSID+" " + wifiInfo.toString());
                    if(TextUtils.equals(mConnectingSSID, wifiInfo.getSSID())){
                        boolean isValidState = SupplicantState.isValidState(wifiInfo.getSupplicantState());
                        if(!isValidState){
                            Log.i(TAG, "wifi connect fail, " + wifiInfo.getSupplicantState().describeContents());
                            unRegisterListener(ProtoResult.FAIL, "Connect fail, Please check your password.", null);
                        } else if(SupplicantState.COMPLETED.equals(wifiInfo.getSupplicantState())){
                            Log.i(TAG, "wifi connected");
                            unRegisterListener(ProtoResult.SUCCESS, null, wifiInfo.getSSID());
                        }
                    }
                }
            };
            mContext.registerReceiver(mWifiConnectBroadCast, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
        }

        mConnectingSSID = paramWifiConfiguration.SSID;
        WifiConfiguration tempConfiguration = isExists(paramWifiConfiguration.SSID);
        if (tempConfiguration != null) {
            mWifiManager.removeNetwork(tempConfiguration.networkId); // 从列表中删除指定的网络配置网络
        }
        int i = mWifiManager.addNetwork(paramWifiConfiguration);
        boolean isEnable = mWifiManager.enableNetwork(i, true);
        if (!isEnable) {
            unRegisterListener(ProtoResult.FAIL, "Can not enable network.", null);
        } else {
            handler.postDelayed(timeOutRunner, 10000);
        }
    }

    private void unRegisterListener(byte result, String message, String ssid) {
        handler.removeCallbacks(timeOutRunner);

        if (mIAddNetWorkCallBack != null) {
            mIAddNetWorkCallBack.onConnect(result, message);
            mIAddNetWorkCallBack = null;
        } else if (mIControlListener != null) {
            mIControlListener.onConnectStateChange(result, message, ssid);
        }

        if(ProtoResult.SUCCESS == result){
            release();
        }
    }

    /**
     * 获取Wifi状态
     **/
    public boolean isWifiEnabled() {
        return this.mWifiManager.isWifiEnabled();
    }

    /**
     * 打开Wifi
     **/
    public void openWifi() {
        if (!this.mWifiManager.isWifiEnabled()) { // 当前wifi不可用
            this.mWifiManager.setWifiEnabled(true);
        }
    }


//    /** 获取ip地址 **/
//    public int getIPAddress() {
//        return (mWifiInfo == null) ? 0 : mWifiInfo.getIpAddress();
//    }
//
//    /** 获取网关IP **/
//    public int getGatewayIP() {
//        return (this.mWifiManager == null) ? 0 : this.mWifiManager.getDhcpInfo().gateway;
//    }
//
//    /** 获取物理地址(Mac) **/
//    public String getMacAddress() {
//        return (mWifiInfo == null) ? null : mWifiInfo.getMacAddress();
//    }
//
//    /** 获取网络id **/
//    public int getNetworkId() {
//        return (mWifiInfo == null) ? 0 : mWifiInfo.getNetworkId();
//    }

    /** 获取wifi连接信息 **/

    /**
     * 已配置的无线热点中，是否存在网络信息
     *
     * @param str 热点名称
     * @return
     */
    protected WifiConfiguration isExists(String str) {
        if (this.mWifiManager.getConfiguredNetworks() == null) {
            return null;
        }
        Iterator<WifiConfiguration> localIterator = this.mWifiManager.getConfiguredNetworks().iterator();
        WifiConfiguration localWifiConfiguration;
        do {
            if (!localIterator.hasNext()) {
                return null;
            }
            localWifiConfiguration = localIterator.next();
        } while (!localWifiConfiguration.SSID.equals("\"" + str + "\""));
        return localWifiConfiguration;
    }

    public List<WifiScanInfo> getScanResult() {
        mWifiManager.startScan();
        List<ScanResult> scanResultList = mWifiManager.getScanResults();
        ArrayList<WifiScanInfo> result = new ArrayList<>(scanResultList.size());

        for (ScanResult scan : scanResultList) {
            WifiScanInfo info = new WifiScanInfo();
            info.ssid = scan.SSID;
            info.level = scan.level;
            info.authtype = scan.capabilities;
            result.add(info);
        }
        return result;
    }

    public void setIControlListener(IControlListener listener) {
        mIControlListener = listener;
    }

    private void release() {
        handler.removeCallbacksAndMessages(null);
        mIControlListener = null;
        if (mWifiConnectBroadCast != null) {
            mContext.unregisterReceiver(mWifiConnectBroadCast);
            mWifiConnectBroadCast = null;
        }
    }

    public interface IAddNetWorkCallBack {
        void onConnect(byte result, String message);
    }

    public interface IControlListener {
        void onConnectStateChange(byte state, String msg, String ssid);
    }
}
