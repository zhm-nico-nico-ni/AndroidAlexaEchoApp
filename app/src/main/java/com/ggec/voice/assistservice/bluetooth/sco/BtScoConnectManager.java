package com.ggec.voice.assistservice.bluetooth.sco;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.ParcelUuid;
import android.text.TextUtils;

import com.ggec.voice.assistservice.audio.SingleAudioRecord;
import com.ggec.voice.assistservice.audio.record.GGECAudioRecorder;
import com.ggec.voice.toollibrary.log.Log;

import java.lang.reflect.Method;
import java.util.Set;


/**
 * Created by ggec on 2018/5/16.
 */

public class BtScoConnectManager extends BroadcastReceiver {

    private final static String TAG = "BtScoConnectManager";

    private Context mContext;
    private BluetoothAdapter mAdapter;
    boolean mIsConnectSco = false;
    private AudioManager mAudioManager;

    private GGECAudioRecorder mGGECAudioRecorder;

    public void init(Context context){
        mContext = context;
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        checkIsConnectSco();

        IntentFilter filter = new IntentFilter();
        filter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        mContext.registerReceiver(this, filter);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "receive:"+intent.getAction());
        if (TextUtils.equals(intent.getAction() ,BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)){
            int connectState = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1);
            if(connectState == BluetoothAdapter.STATE_CONNECTED){
                checkIsConnectSco();
                if(mIsConnectSco){
                    SingleAudioRecord.release();
                }
            } else {
                mIsConnectSco = false;
            }
        } else if(TextUtils.equals(intent.getAction(), AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)) {
            int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, AudioManager.SCO_AUDIO_STATE_ERROR);
            Log.d(TAG, "EXTRA_SCO_AUDIO_STATE:"+state);
            if (AudioManager.SCO_AUDIO_STATE_CONNECTED == state) {
                //Log.i(TAG,"AudioManager.SCO_AUDIO_STATE_CONNECTED");
                Log.d(TAG, "SCO音频连接已建立 " + (mGGECAudioRecorder== null? "mGGECAudioRecorderNull" : "is End:" + mGGECAudioRecorder.isEnd()));

                //Log.i(TAG,"Routing:" + mAudioManager.isBluetoothScoOn());
                //mAudioManager.setMode(AudioManager.STREAM_MUSIC);
//                mAudioManager.setMode(AudioManager.MODE_IN_CALL);

                if (mGGECAudioRecorder != null && !mGGECAudioRecorder.isEnd()) {
                    Log.d(TAG, " now start sco!");
                    mAudioManager.setBluetoothScoOn(true);//打开SCO
//                    mAudioManager.startBluetoothSco();
                    mGGECAudioRecorder.start();
                } else {
                    Log.w(TAG, " cancel start sco!");
                    mAudioManager.setBluetoothScoOn(false);
                    mAudioManager.stopBluetoothSco();
                    mAudioManager.setMode(AudioManager.MODE_NORMAL);
                }
            } else if (AudioManager.SCO_AUDIO_STATE_DISCONNECTED == state || AudioManager.SCO_AUDIO_STATE_ERROR == state) {
                int preState = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_PREVIOUS_STATE, -1);
                Log.d(TAG, "setSpeakerphoneOn state:" + state + "    pre:" + preState);

                if (mAudioManager.isBluetoothScoOn()) {
                    mAudioManager.setBluetoothScoOn(false);
                    mAudioManager.stopBluetoothSco();
                }
                mAudioManager.setMode(AudioManager.MODE_NORMAL);
                if(AudioManager.SCO_AUDIO_STATE_CONNECTED == preState || AudioManager.SCO_AUDIO_STATE_CONNECTING == preState) {
                    if (mGGECAudioRecorder != null && !mGGECAudioRecorder.isEnd()) {
                        Log.w(TAG, "SCO_AUDIO_STATE_DISCONNECTED, mGGECAudioRecorder.interruptAll()");
                        mGGECAudioRecorder.interruptAll();
                    }
                }
            } else if(AudioManager.SCO_AUDIO_STATE_ERROR == state){
                mAudioManager.stopBluetoothSco();
            }
        }
    }

    public boolean isBlueScoConnected(){
        return mIsConnectSco && mAdapter.isEnabled();
    }

    private void checkIsConnectSco(){
        mIsConnectSco = mAdapter.isEnabled() && mAudioManager.isBluetoothScoAvailableOffCall() && isBlueConnected();
    }

    private boolean isBlueConnected(){
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        Class<BluetoothAdapter> bluetoothAdapterClass = BluetoothAdapter.class;//得到BluetoothAdapter的Class对象
        try {//得到连接状态的方法
            Method method = bluetoothAdapterClass.getDeclaredMethod("getConnectionState", (Class[]) null);
            //打开权限
            method.setAccessible(true);
            int state = (int) method.invoke(adapter, (Object[]) null);

            if(state == BluetoothAdapter.STATE_CONNECTED){
                android.util.Log.i("BLUETOOTH","BluetoothAdapter.STATE_CONNECTED");
                Set<BluetoothDevice> devices = adapter.getBondedDevices();
                android.util.Log.i("BLUETOOTH","devices:"+devices.size());

                ParcelUuid HFP_HS_UUID = ParcelUuid.fromString("0000111e-0000-1000-8000-00805f9b34fb");

                for(BluetoothDevice device : devices){
                    Method isConnectedMethod = BluetoothDevice.class.getDeclaredMethod("isConnected", (Class[]) null);
                    method.setAccessible(true);
                    boolean isConnected = (boolean) isConnectedMethod.invoke(device, (Object[]) null);
                    if(isConnected){
                        android.util.Log.i("BLUETOOTH","connected:"+device.getName());
//                        deviceList.add(device);

                        for (ParcelUuid uuid: device.getUuids()){
//                            android.util.Log.d("BLUETOOTH", "uuid:"+uuid.toString());
                            if(uuid.equals(HFP_HS_UUID)){ // support HFP_HS_UUID, generally mean has microphone
//                                android.util.Log.d("BLUETOOTH", "support microphone");
                                return true;
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public GGECAudioRecorder getAudioRecord(){
        if(mGGECAudioRecorder == null || !mGGECAudioRecorder.isRecording()){
            Log.d(TAG, "new GGECAudioRecorder");
            mGGECAudioRecorder = new GGECAudioRecorder();
        }
        return mGGECAudioRecorder;
    }

}
