package com.ggec.voice.assistservice;

import android.accounts.AuthenticatorException;
import android.content.Intent;
import android.support.annotation.MainThread;
import android.text.TextUtils;

import com.ggec.voice.assistservice.audio.IMyVoiceRecordListener;
import com.ggec.voice.assistservice.audio.MyShortAudioPlayer;
import com.ggec.voice.assistservice.audio.MyShortAudioPlayer2;
import com.ggec.voice.assistservice.audio.neartalk.NearTalkVoiceRecord;
import com.ggec.voice.assistservice.data.BackGroundProcessServiceControlCommand;
import com.ggec.voice.assistservice.data.ImplAsyncCallback;
import com.ggec.voice.assistservice.mediaplayer.GGECMediaManager;
import com.ggec.voice.toollibrary.log.Log;
import com.google.gson.Gson;
import com.willblaschko.android.alexa.AlexaManager;
import com.willblaschko.android.alexa.BroadCast;
import com.willblaschko.android.alexa.callbacks.IGetContextEventCallBack;
import com.willblaschko.android.alexa.data.Event;
import com.willblaschko.android.alexa.data.message.request.speechrecognizer.Initiator;
import com.willblaschko.android.alexa.interfaces.AvsAudioException;
import com.willblaschko.android.alexa.interfaces.AvsItem;
import com.willblaschko.android.alexa.interfaces.AvsResponse;
import com.willblaschko.android.alexa.interfaces.alerts.AvsDeleteAlertItem;
import com.willblaschko.android.alexa.interfaces.alerts.AvsSetAlertItem;
import com.willblaschko.android.alexa.interfaces.alerts.SetAlertHelper;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsLocalResumeItem;
import com.willblaschko.android.alexa.interfaces.context.ContextUtil;
import com.willblaschko.android.alexa.interfaces.errors.AvsResponseException;
import com.willblaschko.android.alexa.interfaces.speaker.AvsAdjustVolumeItem;
import com.willblaschko.android.alexa.interfaces.speaker.AvsSetMuteItem;
import com.willblaschko.android.alexa.interfaces.speaker.AvsSetVolumeItem;
import com.willblaschko.android.alexa.interfaces.speaker.SpeakerUtil;
import com.willblaschko.android.alexa.interfaces.speechrecognizer.AvsStopCaptureItem;
import com.willblaschko.android.alexa.interfaces.system.AvsResetUserInactivityItem;
import com.willblaschko.android.alexa.interfaces.system.AvsSetEndPointItem;
import com.willblaschko.android.alexa.interfaces.system.AvsUnableExecuteItem;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;

/**
 * Created by ggec on 2017/3/31.
 * handle
 */

public class AvsHandleHelper {
    private static final String TAG = "GGECAvsHandleHelper";
    private static volatile AvsHandleHelper sAvsHandleHelper;
    private GGECMediaManager audioManager;
    private NearTalkVoiceRecord myNearTalkVoiceRecord;
    private MyShortAudioPlayer mMyShortAudioPlayer;

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

    public void stopCaptureNearTalkVoiceRecord(boolean justStopMic){
        Log.d(TAG, "stopCaptureNearTalkVoiceRecord called " + justStopMic);
        if (myNearTalkVoiceRecord != null && !myNearTalkVoiceRecord.isInterrupted()) {
            if(justStopMic) {
                myNearTalkVoiceRecord.interrupt(false);
            } else {
                myNearTalkVoiceRecord.doActuallyInterrupt();
                AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext());
                alexaManager.cancelAudioRequest();
            }
        }
    }

    public void pauseSound(){
        audioManager.pauseSound();
    }

    public void startNearTalkRecord(String rawPath , final long waitMicTimeOut, String strInitiator) {
        Initiator initiator = TextUtils.isEmpty(strInitiator) ? null : new Gson().fromJson(strInitiator, Initiator.class);
        String path = !TextUtils.isEmpty(rawPath) ? rawPath :
                MyApplication.getContext().getExternalFilesDir("near_talk").getPath() + "/" + System.currentTimeMillis();

        startNearTalkVoiceRecord(path, getFileCallBack(waitMicTimeOut, "record", path), initiator, waitMicTimeOut<= 0);
    }

    private void startNearTalkVoiceRecord(String path, final IMyVoiceRecordListener callback, final Initiator initiator, boolean needTips){
        Log.d(TAG, "startNearTalkVoiceRecord " + path +  " initiator:"+initiator+" "+ needTips );

        long endIndexInSamples = initiator == null ? 0 : initiator.getEndIndexInSamples();

        try {
            myNearTalkVoiceRecord = new NearTalkVoiceRecord(endIndexInSamples, path ,NearTalkVoiceRecord.DEFAULT_SILENT_THRESHOLD, callback);
        } catch (FileNotFoundException e) {
            callback.failure(e);
            audioManager.continueSound();
            return;
        }

        myNearTalkVoiceRecord.startHttpRequest(endIndexInSamples, new IMyVoiceRecordListener(path){
            @Override
            public void start() {
                callback.start();
            }

            @Override
            public void success(AvsResponse result, String filePath, boolean isAllSuccess) {
                callback.success(result, filePath, isAllSuccess);
                if(isAllSuccess && result.continueAudio) audioManager.continueSound();
            }

            @Override
            public void failure(Exception error, String filePath, long actuallyLong, AvsResponse response) {
                callback.failure(error, filePath, actuallyLong, response);
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

        if (needTips) {
            mMyShortAudioPlayer.play(new MyShortAudioPlayer.IOnCompletionListener() {
                @Override
                public void onCompletion() {
                    Log.d(TAG, "mMyShortAudioPlayer# onCompletion");
                    myNearTalkVoiceRecord.start();
                }
            });
        } else {// FIXME 这里很慢。。。
            myNearTalkVoiceRecord.start();
        }
    }

    private IMyVoiceRecordListener getFileCallBack(final long waitMicTimeOut, final String name, String filePath) {
        return new IMyVoiceRecordListener(filePath) {

            @Override
            public void success(AvsResponse result, String filePath, boolean allSuccess) {
                deleteFile(filePath);
                if(allSuccess && result.continueWakeWordDetect) {
                    continueWakeWordDetect();
                }
            }

            @Override
            public void failure(Exception error, String filePath, long actuallyLong, AvsResponse response) {
                if(error != null) Log.w(TAG, "IMyVoiceRecordListener fail", error);
                if(response != null){
                    boolean r = audioManager.cancelAvsItem(response);
                    Log.w(TAG, "audioManager.cancelAvsItem "+r);
                }

                if((error instanceof AvsResponseException && ((AvsResponseException) error).isUnAuthorized()) || error instanceof AuthenticatorException){
                    if(needNotifyVoice()) playError("asset:///error_not_authorization.mp3");
                } else if (waitMicTimeOut > 0 && actuallyLong == -1) {
                    AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext());
                    alexaManager.sendEvent(Event.getExpectSpeechTimedOutEvent(), new ImplAsyncCallback("sendExpectSpeechTimeoutEvent"){

                        @Override
                        public void success(AvsResponse result) {
                            super.success(result);
                            if(result.continueWakeWordDetect) {
                                continueWakeWordDetect();
                            }
                        }

                        @Override
                        public void failure(Exception error) {
                            super.failure(error);
                            continueWakeWordDetect();
                        }
                    });
                } else {
                    if(needNotifyVoice()) playError("asset:///error.mp3");
                }
                deleteFile(filePath);

                if(error instanceof AvsAudioException) {
                    Log.e(TAG, "Speech Recognize event send, but receive nothing, http response code = 204");
//                    AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext());
//                    alexaManager.sendEvent(Event
//                            .createExceptionEncounteredEvent(ContextUtil.getActuallyContextList(MyApplication.getContext()
//                                    , AvsHandleHelper.getAvsHandleHelper().getAudioAndSpeechState())
//                            ,"", "UNEXPECTED_INFORMATION_RECEIVED", "Speech Recognize event send, but receive nothing, http response code = 204")
//                            , null);
                }
            }

            private void deleteFile(String fp) {
                if (true) {// 清除文件
                    File file = new File(fp);
                    if (file.exists()) {
                        boolean res = file.delete();
                        Log.d(TAG, "delete cache file state:" + res + " p:" + fp);
                    }
                } else {
                    Log.w(TAG, "Not delete cache file p:" + fp);
                }
            }

        };
    }

    private void continueWakeWordDetect(){
        MyApplication.getContext().sendBroadcast(new Intent(BroadCast.RECEIVE_START_WAKE_WORD_LISTENER));
        AvsHandleHelper.getAvsHandleHelper().handleAvsItem(new AvsLocalResumeItem());
    }

    //"asset:///error.mp3"
    private void playError(String res) {
        Log.d(TAG, "playError:"+res);
        Observable.just(res).subscribeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                new MyShortAudioPlayer2(s, new MyShortAudioPlayer2.IOnCompletionListener() {
                    @Override
                    public void onCompletion() {
                        continueWakeWordDetect();
                    }
                });
            }
        });
    }
    public void initAudioPlayer(){
        if(mMyShortAudioPlayer== null){
            mMyShortAudioPlayer = new MyShortAudioPlayer("asset:///start.mp3");
        }
    }
}
