package com.ggec.voice.assistservice;

import android.support.annotation.MainThread;
import android.text.TextUtils;

import com.ggec.voice.assistservice.audio.IMyVoiceRecordListener;
import com.ggec.voice.assistservice.audio.neartalk.NearTalkVoiceRecord;
import com.ggec.voice.assistservice.data.BackGroundProcessServiceControlCommand;
import com.ggec.voice.assistservice.data.ImplAsyncCallback;
import com.ggec.voice.assistservice.mediaplayer.GGECMediaManager;
import com.ggec.voice.toollibrary.log.Log;
import com.willblaschko.android.alexa.AlexaManager;
import com.willblaschko.android.alexa.callbacks.IGetContextEventCallBack;
import com.willblaschko.android.alexa.data.Event;
import com.willblaschko.android.alexa.data.message.request.speechrecognizer.Initiator;
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

import java.io.FileNotFoundException;
import java.util.List;

/**
 * Created by ggec on 2017/3/31.
 */

public class AvsHandleHelper {
    private static final String TAG = "GGECAvsHandleHelper";
    private static volatile AvsHandleHelper sAvsHandleHelper;
    private GGECMediaManager audioManager;
    private NearTalkVoiceRecord myNearTalkVoiceRecord;


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
                    , AlexaManager.getInstance(MyApplication.getContext())
                    , ((AvsSetVolumeItem) current).getVolume()
                    , false
                    , new ImplAsyncCallback("setVolume"));
        } else if (current instanceof AvsAdjustVolumeItem) {
            //adjust the volume
            SpeakerUtil.setVolume(MyApplication.getContext()
                    , AlexaManager.getInstance(MyApplication.getContext())
                    , ((AvsAdjustVolumeItem) current).getAdjustment()
                    , true
                    , new ImplAsyncCallback("AdjustVolume")
            );
        } else if (current instanceof AvsSetMuteItem) {
            //mute/unmute the device
            SpeakerUtil.setMute(MyApplication.getContext()
                    , AlexaManager.getInstance(MyApplication.getContext())
                    , ((AvsSetMuteItem) current).isMute()
                    , new ImplAsyncCallback("setMute")
            );
        } else if(current instanceof AvsUnableExecuteItem){
            AvsUnableExecuteItem item = (AvsUnableExecuteItem) current;
            AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext());
            alexaManager.sendEvent(Event
                            .createExceptionEncounteredEvent(ContextUtil.getActuallyContextList(MyApplication.getContext()
                                    , getAudioAndSpeechState())
                                    , item.unparsedDirective
                                    , item.type
                                    , item.message)
                    , null);
        } else if(current instanceof AvsResetUserInactivityItem){
            AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext());
            alexaManager.resetUserInactivityTime();
        } else if(current instanceof AvsSetEndPointItem){
            Log.w(TAG, " handle clear queue ! AvsSetEndPointItem");
            audioManager.clear(true);
            AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext());
            alexaManager.setEndPoint(((AvsSetEndPointItem) current).endPoint);
        } else if(current instanceof AvsStopCaptureItem) {
            Log.w(TAG, "handle AvsStopCaptureItem");
            stopCaptureNearTalkVoiceRecord(true);
        } else if (current instanceof AvsSetAlertItem) {
            AvsSetAlertItem setAlertItem = (AvsSetAlertItem) current;
            boolean setSuccess = SetAlertHelper.setAlert(MyApplication.getContext(), setAlertItem
                    , BackGroundProcessServiceControlCommand.createIntentByType(MyApplication.getContext(), BackGroundProcessServiceControlCommand.BEGIN_ALARM));
            if (setSuccess) {
                SetAlertHelper.putAlert(MyApplication.getContext(), setAlertItem);
                SetAlertHelper.sendSetAlertSucceeded(AlexaManager.getInstance(MyApplication.getContext())
                        , current.getToken(), new ImplAsyncCallback("SetAlertSucceeded"));
            } else {
                SetAlertHelper.sendSetAlertFailed(AlexaManager.getInstance(MyApplication.getContext())
                        , current.getToken(), new ImplAsyncCallback("SetAlertFail"));
            }
        } else if (current instanceof AvsDeleteAlertItem) {
            boolean res = SetAlertHelper.cancelOrDeleteAlert(MyApplication.getContext(), current.getToken()
                    , BackGroundProcessServiceControlCommand.createIntentByType(MyApplication.getContext(), BackGroundProcessServiceControlCommand.BEGIN_ALARM));
            Log.d(TAG, "Delete Alert res:" + res);
            if (res) {
                SetAlertHelper.sendDeleteAlertSucceeded(AlexaManager.getInstance(MyApplication.getContext())
                        , current.getToken(), new ImplAsyncCallback("DeleteAlertSucceeded"));
            } else {
                SetAlertHelper.sendDeleteAlertFail(AlexaManager.getInstance(MyApplication.getContext())
                        , current.getToken(), new ImplAsyncCallback("DeleteAlertFail"));
            }
        } else {
            return false;
        }
        Log.d(TAG, "processAvsItemImmediately type:" + current.getClass());
        return true;
    }

    public List<Event> getAudioAndSpeechState(){
        return audioManager.getAudioAndSpeechState();
    }

    private void stopCaptureNearTalkVoiceRecord(boolean justStopMic){
        if (myNearTalkVoiceRecord != null && !myNearTalkVoiceRecord.isInterrupted()) {
            if(justStopMic) {
                myNearTalkVoiceRecord.interrupt(false);
            } else {
                myNearTalkVoiceRecord.doActuallyInterrupt();
                AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext());
                alexaManager.cancelAudioRequest();
            }

            Log.d(TAG, "stopCaptureNearTalkVoiceRecord called");
        }
    }

    public void pauseSound(){
        audioManager.pauseSound();
    }

    public void startNearTalkVoiceRecord(String path, final IMyVoiceRecordListener callback, final Initiator initiator){
        Log.d(TAG, "startNearTalkVoiceRecord");
        stopCaptureNearTalkVoiceRecord(false);
        long endIndexInSamples = initiator == null ? 0 : initiator.getEndIndexInSamples();

        try {
            myNearTalkVoiceRecord = new NearTalkVoiceRecord(endIndexInSamples, path ,NearTalkVoiceRecord.DEFAULT_SILENT_THRESHOLD, callback);
        } catch (FileNotFoundException e) {
            callback.failure(e);
            audioManager.continueSound();
            return;
        }
        myNearTalkVoiceRecord.start();
        myNearTalkVoiceRecord.startHttpRequest(endIndexInSamples, new IMyVoiceRecordListener(path){
            @Override
            public void start() {
                callback.start();
            }

            @Override
            public void success(AvsResponse result, String filePath) {
                callback.success(result, filePath);
                if(result.continueAudio) audioManager.continueSound();
            }

            @Override
            public void failure(Exception error, String filePath, long actuallyLong) {
                callback.failure(error, filePath, actuallyLong);
                audioManager.continueSound();
            }

            @Override
            public void complete() {
                callback.complete();
            }
        }, new IGetContextEventCallBack() {
            @Override
            public List<Event> getContextEvent() {
                return ContextUtil.getActuallyContextList(MyApplication.getContext(), getAudioAndSpeechState());
            }

            @Override
            public Initiator getInitiator() {
                return initiator;
            }
        });
    }
}
