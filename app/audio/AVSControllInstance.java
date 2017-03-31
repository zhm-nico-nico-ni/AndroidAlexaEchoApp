package com.ggec.voice.assistservice.audio;

import com.amazon.alexa.avs.AlertManagerFactory;
import com.amazon.alexa.avs.DialogRequestIdAuthority;

/**
 * Created by ggec on 2017/3/30.
 */

public class AVSControllInstance {

    private static AVSControllInstance sInstance;

    private AVSController mAVSController;

    private AVSControllInstance() throws Exception{
//        AlertManagerFactory alarmFactory, AVSClientFactory avsClientFactory,
//                DialogRequestIdAuthority dialogRequestIdAuthority,
//                         /*WakeWordIPCFactory wakewordIPCFactory,*/ DeviceConfig config, WakeWordDetectedHandler wakeWakeDetectedHandler
//        mAVSController = new AVSController( new AlertManagerFactory(),
//                getAVSClientFactory(deviceConfig), DialogRequestIdAuthority.getInstance(),
//                new WakeWordIPCFactory(), deviceConfig, this);
    }

    public static AVSControllInstance getInstance() {
        if (sInstance == null) {
            try {
                sInstance = new AVSControllInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return sInstance;
    }
}
