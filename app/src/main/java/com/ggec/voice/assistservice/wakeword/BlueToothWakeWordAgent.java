package com.ggec.voice.assistservice.wakeword;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;

import com.csr.gaia.library.Gaia;
import com.csr.gaia.library.GaiaError;
import com.csr.gaia.library.GaiaLink;
import com.csr.gaia.library.GaiaPacket;
import com.ggec.voice.toollibrary.Util;
import com.ggec.voice.toollibrary.log.Log;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by ggec on 2018/5/18.
 * gaia bluetooth wake word
 */

public class BlueToothWakeWordAgent extends WakeWordAgent {
    private final String TAG = "BlueToothWakeWordAgent";

    private AtomicBoolean mCanDetectWakeWord = new AtomicBoolean(true);

    @SuppressLint("HandlerLeak")
    private Handler mGaiaHandler = new Handler() {


        @Override
        public void handleMessage(Message msg) {
//            String handleMessage = "Handle a message from Gaia: ";
            GaiaLink.Message message = GaiaLink.Message.valueOf(msg.what);
            if (message == null) {
                return;
            }
            switch (message) {
                case PACKET:
                    handlePacket(msg);

                    break;

                case CONNECTED:
                    Log.d(TAG, "gaia连接建立");
                    try {
                        GaiaLink.getInstance().registerNotification(Gaia.VENDOR_CSR, Gaia.EventId.USER_ACTION);//gaia连接建立成功后，登记一个User Action的notication
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    break;

                case DISCONNECTED:
                    //parentActivity.makeToast(R.string.toast_disconnected, Toast.LENGTH_SHORT);
                    //parentActivity.startConnectionActivity();
//                    Intent intent = new Intent(parentActivity, blepairActivity.class);
//                    parentActivity.startActivity(intent);
//                    parentActivity.finish();

                    break;

                case ERROR:
                    GaiaError error = (GaiaError) msg.obj;
                    Log.e(TAG, error.getStringException());
                    break;

                case STREAM:
                    break;

                default:
                    break;
            }
        }
    };;

    public BlueToothWakeWordAgent(Context context, IWakeWordAgentEvent listener) {
        super(context, listener);
        init2();
    }

    @Override
    protected void init() {
        //empty
    }

    protected void init2() {
        GaiaLink.getInstance().setReceiveHandler(this.mGaiaHandler);
        //
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int connectState = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1);
                if (connectState == BluetoothAdapter.STATE_CONNECTED) {
                    tryConnectBTdevice();
                } else if (connectState == BluetoothAdapter.STATE_DISCONNECTED) {
                    if (GaiaLink.getInstance().isConnected()) {
                        GaiaLink.getInstance().disconnect();
                    }
                }
            }
        }, filter);

        tryConnectBTdevice();
    }

    @Override
    public void continueSearch() {
        mCanDetectWakeWord.set(true);
    }

    @Override
    public void pauseSearch() {
        mCanDetectWakeWord.set(false);
    }

    private void tryConnectBTdevice() {
        Log.d(TAG, "tryConnectBTdevice start " + this.mGaiaHandler);
        BluetoothManager bm = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = bm.getAdapter();

        BluetoothDevice mBluetoothDeviceConnected = null;
        for (BluetoothDevice bt : mBluetoothAdapter.getBondedDevices()) {
            if (bt.getName().contains("GGEC IAR BT SMART SPEAKER")) {
                mBluetoothDeviceConnected = bt; // get the selected Bluetooth device
                Log.d(TAG, "find CSR8670");
                break;
            }
        }

        GaiaLink.getInstance().connect(mBluetoothDeviceConnected, GaiaLink.Transport.BT_SPP);//set up gaia connection through spp
    }

    /**
     * To manage packets from Gaia device which are "PACKET" directly by the library.
     *
     * @param msg The message coming from the handler which calls this method.
     */
    private void handlePacket(Message msg) {
        GaiaPacket packet = (GaiaPacket) msg.obj;
        Gaia.Status status = packet.getStatus();

        switch (packet.getCommand()) {
            case Gaia.COMMAND_GET_LED_CONTROL:

                if (checkStatus(packet))
                    //receivePacketGetLedControl(packet);
                    break;

            case Gaia.COMMAND_GET_CURRENT_BATTERY_LEVEL:

                if (checkStatus(packet))
                    //receivePacketGetCurrentBatteryLevel(packet);
                    break;

            case Gaia.COMMAND_GET_CURRENT_RSSI:

                if (checkStatus(packet))
                    //receivePacketGetCurrentRSSI(packet);
                    break;

            case Gaia.COMMAND_GET_API_VERSION:

                if (checkStatus(packet))
                    //receivePacketGetAPIVersion(packet);
                    break;

            case Gaia.COMMAND_EVENT_NOTIFICATION:

                handleNotification(packet);
                break;

            case Gaia.COMMAND_GET_USER_EQ_CONTROL:
                // to know if the EQ feature is available

                //if (!status.equals(Gaia.Status.NOT_SUPPORTED))
                //    findViewById(R.id.bt_equalizer).setVisibility(View.VISIBLE);
                break;

            case Gaia.COMMAND_GET_3D_ENHANCEMENT_CONTROL:
                // to know if the EQ feature is available

                //if (!status.equals(Gaia.Status.NOT_SUPPORTED))
                //    findViewById(R.id.bt_equalizer).setVisibility(View.VISIBLE);
                break;

            case Gaia.COMMAND_GET_BASS_BOOST_CONTROL:
                // to know if the EQ feature is available

                //if (!status.equals(Gaia.Status.NOT_SUPPORTED))
                //    findViewById(R.id.bt_equalizer).setVisibility(View.VISIBLE);
                break;

            case Gaia.COMMAND_GET_TWS_AUDIO_ROUTING:
                // to know if the TWS feature is available

                if (!status.equals(Gaia.Status.NOT_SUPPORTED))
                    //    findViewById(R.id.bt_tws).setVisibility(View.VISIBLE);
                    break;

            case Gaia.COMMAND_GET_TWS_VOLUME:
                // to know if the TWS feature is available

                if (!status.equals(Gaia.Status.NOT_SUPPORTED))
                    //    findViewById(R.id.bt_tws).setVisibility(View.VISIBLE);
                    break;

            case Gaia.COMMAND_VM_UPGRADE_CONNECT:
                // to know if the TWS feature is available

                //if (!status.equals(Gaia.Status.NOT_SUPPORTED)) {
                //    findViewById(R.id.bt_update).setVisibility(View.VISIBLE);
                //}
                //sendGaiaPacket(Gaia.COMMAND_VM_UPGRADE_DISCONNECT);
                break;

            case Gaia.COMMAND_AV_REMOTE_CONTROL:
                // To know if the remote control feature is available

                //if (!status.equals(Gaia.Status.NOT_SUPPORTED))
                //    findViewById(R.id.bt_remote).setVisibility(View.VISIBLE);
                break;

            default:

        }

    }

    /**
     * To handle notifications coming from the Gaia device.
     */
    private void handleNotification(GaiaPacket packet) {
        Gaia.EventId event = packet.getEvent();
        switch (event) {
            case CHARGER_CONNECTION:
                //isCharging = packet.getPayload()[1] == 0x01;
                //updateDisplayBattery();
                break;
            case USER_ACTION:
                //
                //boolean speechFlag = packet.getPayload()[1] == 0x01;
                byte[] eventId = packet.getPayload();
                //int b = a[1]<<8+a[2];

                if ((eventId[1] == (byte) 0x40) && (eventId[2] == (byte) 0xF9)) { //如果传过来标志位为1，则开始语音输入
                    Log.w(TAG, " receive 40f9");
                    if (mCanDetectWakeWord.get()) {
                        mListener.onDetectWakeWord(null, 0, 0);
                    }
                } else if ((eventId[1] == 0x40) && (eventId[2] == 0xFA)) { //如果传过来标志位为0，则停止语音输入
//                    stopSpeechInput2();
                } else {
                    Log.d(TAG, "receive "+ Util.bytesToHex(eventId));
                }
                break;
            default:

        }
    }

    /**
     * To check the status of an acknowledgement packet.
     *
     * @param packet the packet to check.
     * @return true if the status is SUCCESS and the packet is an acknowledgment, false otherwise.
     */
    private boolean checkStatus(GaiaPacket packet) {
        if (!packet.isAcknowledgement()) {
            return false;
        }
        if (packet.getStatus() == Gaia.Status.SUCCESS) {
            return true;
        } else {

            switch (packet.getStatus()) {
                case NOT_SUPPORTED:
                    Log.w(TAG, "receive un-support packet");
                    //    receivePacketCommandNotSupported(packet);
                    break;
                default:
                    Log.w(TAG, "receive packet state:" + packet.getStatus().name());
                    break;
            }
            return false;
        }
    }

}
