package com.ggec.voice.assistservice;

import android.accounts.AuthenticatorException;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.ggec.voice.assistservice.audio.IMyVoiceRecordListener;
import com.ggec.voice.assistservice.audio.MyShortAudioPlayer2;
import com.ggec.voice.assistservice.data.BackGroundProcessServiceControlCommand;
import com.ggec.voice.assistservice.data.ImplAsyncCallback;
import com.ggec.voice.toollibrary.Util;
import com.ggec.voice.toollibrary.log.Log;
import com.google.gson.Gson;
import com.willblaschko.android.alexa.AlexaManager;
import com.willblaschko.android.alexa.BroadCast;
import com.willblaschko.android.alexa.SharedPreferenceUtil;
import com.willblaschko.android.alexa.TokenManager;
import com.willblaschko.android.alexa.callbacks.AsyncCallback;
import com.willblaschko.android.alexa.callbacks.ImplTokenCallback;
import com.willblaschko.android.alexa.data.Event;
import com.willblaschko.android.alexa.data.message.request.speechrecognizer.Initiator;
import com.willblaschko.android.alexa.interfaces.AvsAudioException;
import com.willblaschko.android.alexa.interfaces.AvsResponse;
import com.willblaschko.android.alexa.interfaces.alerts.AvsAlertPlayItem;
import com.willblaschko.android.alexa.interfaces.alerts.AvsSetAlertItem;
import com.willblaschko.android.alexa.interfaces.alerts.SetAlertHelper;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsLocalResumeItem;
import com.willblaschko.android.alexa.interfaces.context.ContextUtil;
import com.willblaschko.android.alexa.interfaces.errors.AvsResponseException;
import com.willblaschko.android.alexa.interfaces.speaker.SpeakerUtil;
import com.willblaschko.android.alexa.interfaces.speechrecognizer.SpeechSendAudio;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;

/**
 * Created by ggec on 2017/3/29.
 */

public class BgProcessIntentService extends IntentService {
    private static String TAG = "BgProcessIntentService";

    public static final String EXTRA_CMD = "Control-Command";
    private final static int PING_JOB_ID = 3001;
    private final static int USER_INACTIVITY_REPORT_JOB_ID = 3002;
    private final static int REFRESH_TOKEN_DELAY_JOB_ID = 3003;
    private final static int SEND_PING_INTERVAL = 300 * 1000;
    private final static int REFRESH_TOKEN_MIN_INTERVAL = 300 * 1000;

    /**
     * warning 这个不能删
     */
    public BgProcessIntentService(){
        this("test");
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public BgProcessIntentService(String name) {
        super(name);
        Log.d(TAG, "construct : " + name);
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestory");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {// TODO 修改接入OpenDownChannel ，简化这段代码
        final BackGroundProcessServiceControlCommand cmd = new BackGroundProcessServiceControlCommand(intent.getIntExtra(EXTRA_CMD, -1));
        Bundle b = intent.getBundleExtra("cmd_bundle");
        if (b != null) {
            cmd.bundle = b;
        }
        AlexaManager alexaManager = AlexaManager.getInstance(this);

        if (cmd.type == BackGroundProcessServiceControlCommand.START_VOICE_RECORD) {
            final long waitMicDelayMillSecond = intent.getLongExtra("waitMicDelayMillSecond", 0);
            final String initiator = intent.getStringExtra("initiator");
            final String rawPath = intent.getStringExtra("rawPath");
            //start
            MyApplication.mainHandler.postAtFrontOfQueue(new Runnable() {
                @Override
                public void run() {
                    startNearTalkRecord(rawPath, waitMicDelayMillSecond, initiator);
                }
            });


        } else if (cmd.type == 2) {
            //stop
//            textTest();
            search();
        } else if (cmd.type == 3) {
//            alexaManager.closeOpenDownchannel(false);
//            search();
            textTest();
        } else if (cmd.type == BackGroundProcessServiceControlCommand.BEGIN_ALARM) {
            final String token = intent.getStringExtra("token");
            final String messageId = intent.getStringExtra("messageId");
            Log.i(TAG, "BEGIN_ALARM: "+ messageId+ " ,token:"+token);
            SetAlertHelper.sendAlertStarted(alexaManager, token, getCallBack("sendAlertStarted"));
            MyApplication.mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    AvsHandleHelper.getAvsHandleHelper().handleAvsItem(new AvsAlertPlayItem(token, messageId));
                }
            });

        } else if(cmd.type == BackGroundProcessServiceControlCommand.VOLUME_CHANGE){
            SharedPreferences sp = com.willblaschko.android.alexa.utility.Util.getPreferences(this);
            if(Math.abs(System.currentTimeMillis()  - sp.getLong("lastSetMuteTime", 0)) < 5000){
                SpeakerUtil.sendMuteEvent(this, alexaManager, null);
            } else {
                SpeakerUtil.sendVolumeEvent(this, alexaManager, null);
            }
        } else if(cmd.type == BackGroundProcessServiceControlCommand.SEND_PING){
            sendPingEvent(alexaManager);
        } else if(cmd.type == BackGroundProcessServiceControlCommand.NETWORK_CONNECT){
            boolean hasNetWork = Util.isNetworkAvailable(this);
            Log.i(TAG, "on NETWORK_CONNECT, hasWifi:"+hasNetWork +" hasChannel:"+alexaManager.hasOpenDownchannel());
            if(hasNetWork) {
                if (!alexaManager.hasOpenDownchannel()) {
                    openDownChannel(alexaManager);
                }
            } else {
                alexaManager.closeOpenDownchannel(true);
            }
        } else if(cmd.type == BackGroundProcessServiceControlCommand.USER_INACTIVITY_REPORT){
            if(Util.isNetworkAvailable(this)) {
                alexaManager.sendUserInactivityReport();
            }
            setTimerEvent(this, USER_INACTIVITY_REPORT_JOB_ID, BackGroundProcessServiceControlCommand.USER_INACTIVITY_REPORT, 60 * 60 * 1000);
        } else if(cmd.type == BackGroundProcessServiceControlCommand.REFRESH_TOKEN){
            Log.i(TAG, "REFRESH_TOKEN");
            alexaManager.tryRefreshToken(new ImplTokenCallback(null){
                @Override
                public void beginRefreshTokenEvent(Context context, long expires_in) {
                    setTimerEvent(context, REFRESH_TOKEN_DELAY_JOB_ID, BackGroundProcessServiceControlCommand.REFRESH_TOKEN
                            , expires_in > REFRESH_TOKEN_MIN_INTERVAL? expires_in: REFRESH_TOKEN_MIN_INTERVAL);
                }

                @Override
                public void onFailure(Exception e) {
                    setTimerEvent(MyApplication.getContext(), REFRESH_TOKEN_DELAY_JOB_ID, BackGroundProcessServiceControlCommand.REFRESH_TOKEN, REFRESH_TOKEN_MIN_INTERVAL);
                }
            });
        } else if(cmd.type == BackGroundProcessServiceControlCommand.LOAD_ALARM){
            List<AvsSetAlertItem> alertItems = SetAlertHelper.getAllAlerts(this);
            long currentTime = System.currentTimeMillis();
            for(AvsSetAlertItem item : alertItems) {
                if(SetAlertHelper.isScheduledTimeAvailable(item.getScheduledTime(), currentTime)){
                    AvsHandleHelper.getAvsHandleHelper().handleAvsItem(item);
                } else {
                    SetAlertHelper.sendAlertStopped(alexaManager, item.getToken(), null);
                }
            }
        } else {
            //cancel
            Log.d(TAG, "unknow cmd:" + cmd.type);
        }
    }

    private void startNearTalkRecord(String rawPath , final long waitMicTimeOut, String strInitiator) {
        AvsHandleHelper.getAvsHandleHelper().pauseSound();
        AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext());
        SpeechSendAudio audio = alexaManager.getSpeechSendAudio();
        if (audio != null) audio.cancelRequest();
        if(!Util.isNetworkAvailable(this)){
            //TODO play no net work
            Log.e(TAG, "return because no net work");
            playError("asset:///error_not_connected.mp3");
            return;
        }

        Initiator initiator = TextUtils.isEmpty(strInitiator) ? null : new Gson().fromJson(strInitiator, Initiator.class);
        String path = !TextUtils.isEmpty(rawPath) ? rawPath :
                MyApplication.getContext().getExternalFilesDir("near_talk").getPath() + "/" + System.currentTimeMillis();

        AvsHandleHelper.getAvsHandleHelper().startNearTalkVoiceRecord(path, getFileCallBack(waitMicTimeOut, "record", path), initiator);

    }

    private void textTest() {
        AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext());
        alexaManager.sendTextRequest("Set an alarm", getCallBack("textTest"));
        //Set a timer after 15 seconds from now" "Tell me some news" "Tell me the baseball news" Play TuneIn music radio"
        // "Set an alarm for 9:49 morning on everyday" "How's my day look"
    }

    private void search() {
        try {
            InputStream is = getAssets().open("intros/joke.raw");
            byte[] fileBytes = new byte[is.available()];
            is.read(fileBytes);
            is.close();
            AlexaManager.getInstance(MyApplication.getContext()).sendAudioRequest(fileBytes, getCallBack("search"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private AsyncCallback<AvsResponse, Exception> getCallBack(final String name) {
        return new ImplAsyncCallback(name);
    }

    private IMyVoiceRecordListener getFileCallBack(final long waitMicTimeOut, final String name, String filePath) {
        return new IMyVoiceRecordListener(filePath) {

            @Override
            public void success(AvsResponse result, String filePath) {
                deleteFile(filePath);
                if(result.continueWakeWordDetect) {
                    continueWakeWordDetect();
                }
            }

            @Override
            public void failure(Exception error, String filePath, long actuallyLong) {
                if(error != null) Log.w(TAG, "IMyVoiceRecordListener fail", error);
                if((error instanceof AvsResponseException && ((AvsResponseException) error).isUnAuthorized()) || error instanceof AuthenticatorException){
                    if(needNotifyVoice()) playError("asset:///error_not_authorization.mp3");
                } else {
                    if(needNotifyVoice()) playError("asset:///error.mp3");
                }
                deleteFile(filePath);
                if (waitMicTimeOut > 0 && actuallyLong == -1) {
                    AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext());
                    alexaManager.sendEvent(Event.getExpectSpeechTimedOutEvent(), getCallBack("sendExpectSpeechTimeoutEvent"));
                }
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
        sendBroadcast(new Intent(BroadCast.RECEIVE_START_WAKE_WORD_LISTENER));
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

    private void sendPingEvent(AlexaManager alexaManager){
        alexaManager.sendPingEvent(new ImplAsyncCallback("sendPing"){
            @Override
            public void success(AvsResponse result) {
                super.success(result);
                AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext());
                if (!alexaManager.hasOpenDownchannel()) {
                    alexaManager.sendOpenDownchannelDirective(getCallBack("{opendownchannel}"));
                    return;
                } else {
                    setTimerEvent(MyApplication.getContext(), PING_JOB_ID, BackGroundProcessServiceControlCommand.SEND_PING, SEND_PING_INTERVAL);
                }
            }

            @Override
            public void failure(Exception error) {
                super.failure(error);
                AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext());
                alexaManager.closeOpenDownchannel(false);
            }
        });
    }

    private void openDownChannel(AlexaManager alexaManager){
        if (!alexaManager.hasOpenDownchannel()) {
            alexaManager.sendOpenDownchannelDirective(new ImplAsyncCallback("{opendownchannel}"){
                @Override
                public void start() {
                    AlexaManager.getInstance(MyApplication.getContext()).sendSynchronizeStateEvent2(ContextUtil.getActuallyContextList(MyApplication.getContext()
                            , AvsHandleHelper.getAvsHandleHelper().getAudioAndSpeechState()), getCallBack("SynchronizeState"));
                    setTimerEvent(MyApplication.getContext(), PING_JOB_ID, BackGroundProcessServiceControlCommand.SEND_PING, SEND_PING_INTERVAL);
                    long nextRefreshTime = SharedPreferenceUtil.getLongByKey(MyApplication.getContext(), TokenManager.PREF_TOKEN_EXPIRES, 0) - System.currentTimeMillis();
                    setTimerEvent(MyApplication.getContext(), REFRESH_TOKEN_DELAY_JOB_ID, BackGroundProcessServiceControlCommand.REFRESH_TOKEN
                            , nextRefreshTime < 2 * REFRESH_TOKEN_MIN_INTERVAL? 3000 : nextRefreshTime);
                    Log.i(TAG, "opendownchannel connected");
                }

                @Override
                public void failure(Exception error) {
                    super.failure(error);
                    if (error instanceof AvsResponseException && ((AvsResponseException) error).isUnAuthorized()) {
                        SharedPreferenceUtil.clearUtcTimeKey(MyApplication.getContext());
                    }
                    cancelTimerEvent(MyApplication.getContext(), PING_JOB_ID, BackGroundProcessServiceControlCommand.SEND_PING);
                    cancelTimerEvent(MyApplication.getContext(), REFRESH_TOKEN_DELAY_JOB_ID, BackGroundProcessServiceControlCommand.REFRESH_TOKEN);
                }
            });
            return;
        }
    }

    private void setTimerEvent(Context context, int requestCode, int type, long delay) {
        Log.d(TAG, "setTimerEvent " + type + " req:" + requestCode + " delay:" + delay);
        Intent it = BackGroundProcessServiceControlCommand.createIntentByType(context, type);
        PendingIntent intent = PendingIntent.getService(context, requestCode, it, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delay, intent);
    }

    private void cancelTimerEvent(Context context, int requestCode, int type) {
        Log.d(TAG, "cancelTimerEvent " + type + " req:" + requestCode);
        Intent it = BackGroundProcessServiceControlCommand.createIntentByType(context, type);
        PendingIntent intent = PendingIntent.getService(context, requestCode, it, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(intent);
    }
}
