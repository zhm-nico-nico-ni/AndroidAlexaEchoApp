package com.ggec.voice.assistservice;

import android.support.annotation.MainThread;
import android.text.TextUtils;
import android.util.Log;

import com.ggec.voice.assistservice.data.BackGroundProcessServiceControlCommand;
import com.ggec.voice.assistservice.data.ImplAsyncCallback;
import com.ggec.voice.assistservice.mediaplayer.GGECMediaManager;
import com.willblaschko.android.alexa.AlexaManager;
import com.willblaschko.android.alexa.data.Event;
import com.willblaschko.android.alexa.interfaces.AvsItem;
import com.willblaschko.android.alexa.interfaces.AvsResponse;
import com.willblaschko.android.alexa.interfaces.alerts.AvsDeleteAlertItem;
import com.willblaschko.android.alexa.interfaces.alerts.AvsSetAlertItem;
import com.willblaschko.android.alexa.interfaces.alerts.SetAlertHelper;
import com.willblaschko.android.alexa.interfaces.context.ContextUtil;
import com.willblaschko.android.alexa.interfaces.speaker.AvsAdjustVolumeItem;
import com.willblaschko.android.alexa.interfaces.speaker.AvsSetMuteItem;
import com.willblaschko.android.alexa.interfaces.speaker.AvsSetVolumeItem;
import com.willblaschko.android.alexa.interfaces.speaker.SpeakerUtil;
import com.willblaschko.android.alexa.interfaces.speechrecognizer.AvsStopCaptureItem;
import com.willblaschko.android.alexa.interfaces.system.AvsResetUserInactivityItem;
import com.willblaschko.android.alexa.interfaces.system.AvsSetEndPointItem;
import com.willblaschko.android.alexa.interfaces.system.AvsUnableExecuteItem;

/**
 * Created by ggec on 2017/3/31.
 */

public class AvsHandleHelper {
    private static final String TAG = "com.ggec.voice.assistservice.AvsHandleHelper";
    private static AvsHandleHelper sAvsHandleHelper;
    private GGECMediaManager audioManager;

//    private AlexaAudioExoPlayer exoPlayer;
    //TODO 這裏把microphone 的控制加上

    private AvsHandleHelper() {
        audioManager = new GGECMediaManager();
    }

    public static synchronized AvsHandleHelper getAvsHandleHelper() {
        if (sAvsHandleHelper == null) {
            sAvsHandleHelper = new AvsHandleHelper();
        }
        return sAvsHandleHelper;
    }


    /**
     * Handle the response sent back from Alexa's parsing of the Intent, these can be any of the AvsItem types (play, speak, stop, clear, listen)
     *
     * @param response a List<AvsItem> returned from the mAlexaManager.sendTextRequest() call in sendVoiceToAlexa()
     */
    public synchronized void handleResponse(AvsResponse response) {
        if (response != null) {
            //if we have a clear queue item in the list, we need to clear the current queue before proceeding
            //iterate backwards to avoid changing our array positions and getting all the nasty errors that come
            //from doing that
            for (int i = 0; i < response.size(); i++) {
                handleAvsItem(response.get(i));
            }
//            if (BuildConfig.DEBUG) {
//                Log.i(TAG, "Adding " + response.size() + " items to our queue, " + " old is: " + audioManager.getQueueSize());
//                for (int i = 0; i < response.size(); i++) {
//                    Log.i(TAG, "\tAdding: " + response.get(i).getToken());
//                }
//            }
        }
    }

    @MainThread
    public void handleAvsItem(AvsItem response){
        if(response == null) return;

        if(processAvsItemImmediately(response)){
            return;
        }

        if(audioManager.addAvsItemToQueue(response)){
            return;
        }

        if(TextUtils.isEmpty(response.messageID)){
            //AvsResponseException， 已经在parseResponse 中throw，AvsUnableExecuteItem 已在processAvsItemImmediately处理
            if(BuildConfig.DEBUG) {
                Log.e(TAG, "[important]handleAvsItem messageID is empty! " + response.getClass());
            }
            return;
        } else {
            Log.w(TAG, "pop a unhandle item !" + response.getClass());
        }
    }

    private synchronized boolean processAvsItemImmediately(AvsItem current) {
//        TODO 后续还要添加其他处理
        if (current instanceof AvsSetVolumeItem) {
            //set our volume
            SpeakerUtil.setVolume(MyApplication.getContext()
                    , AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID)
                    , ((AvsSetVolumeItem) current).getVolume()
                    , false
                    , new ImplAsyncCallback("setVolume"));
        } else if (current instanceof AvsAdjustVolumeItem) {
            //adjust the volume
            SpeakerUtil.setVolume(MyApplication.getContext()
                    , AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID)
                    , ((AvsAdjustVolumeItem) current).getAdjustment()
                    , true
                    , new ImplAsyncCallback("AdjustVolume")
            );
        } else if (current instanceof AvsSetMuteItem) {
            //mute/unmute the device
            SpeakerUtil.setMute(MyApplication.getContext()
                    , AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID)
                    , ((AvsSetMuteItem) current).isMute()
                    , new ImplAsyncCallback("setMute")
            );
        } else if(current instanceof AvsUnableExecuteItem){
            AvsUnableExecuteItem item = (AvsUnableExecuteItem) current;
            AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID);
            alexaManager.sendEvent(Event
                            .createExceptionEncounteredEvent(ContextUtil.getContextList(MyApplication.getContext())
                                    , item.unparsedDirective
                                    , item.type
                                    , item.message)
                    , null);
        } else if(current instanceof AvsResetUserInactivityItem){
            AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID);
            alexaManager.resetUserInactivityTime();
        } else if(current instanceof AvsSetEndPointItem){
            audioManager.clear(true);
            AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID);
            alexaManager.setEndPoint(((AvsSetEndPointItem) current).endPoint);
            Log.w(TAG, " handle clear queue ! AvsSetEndPointItem");
        } else if(current instanceof AvsStopCaptureItem) {
//            AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID);
//            alexaManager.cancelAudioRequest(); //FIXME 不应该是cancel request，应该是停止录音，但是现在用的都是close talk，在client这边判断语音是否结束，因此不需要。
//            avsQueue.remove(current.messageID);
            Log.w(TAG, "handle AvsStopCaptureItem");
        } else if (current instanceof AvsSetAlertItem) {
            AvsSetAlertItem setAlertItem = (AvsSetAlertItem) current;
            boolean setSuccess = SetAlertHelper.setAlert(MyApplication.getContext(), setAlertItem
                    , BackGroundProcessServiceControlCommand.createIntentByType(MyApplication.getContext(), BackGroundProcessServiceControlCommand.BEGIN_ALARM));
            if (setSuccess) {
                SetAlertHelper.putAlert(MyApplication.getContext(), setAlertItem);
                SetAlertHelper.sendSetAlertSucceeded(AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID)
                        , current.getToken(), new ImplAsyncCallback("SetAlertSucceeded"));
            } else {
                SetAlertHelper.sendSetAlertFailed(AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID)
                        , current.getToken(), new ImplAsyncCallback("SetAlertFail"));
            }
        } else if (current instanceof AvsDeleteAlertItem) {
            AvsDeleteAlertItem deleteAlertItem = (AvsDeleteAlertItem) current;
            SetAlertHelper.cancelOrDeleteAlert(MyApplication.getContext(), deleteAlertItem.getMessageID()
                    , BackGroundProcessServiceControlCommand.createIntentByType(MyApplication.getContext(), BackGroundProcessServiceControlCommand.BEGIN_ALARM));
            SetAlertHelper.sendDeleteAlertSucceeded(AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID)
                    , deleteAlertItem.getToken()
                    , new ImplAsyncCallback("DeleteAlertSucceeded"));
        } else {
            return false;
        }
        Log.d(TAG, "processAvsItemImmediately type:" + current.getClass());
        return true;
    }

}
