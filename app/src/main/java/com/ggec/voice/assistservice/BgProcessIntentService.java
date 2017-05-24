package com.ggec.voice.assistservice;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.annotation.Nullable;

import com.ggec.voice.assistservice.audio.IMyVoiceRecordListener;
import com.ggec.voice.assistservice.audio.MyShortAudioPlayer2;
import com.ggec.voice.assistservice.audio.MyVoiceRecord;
import com.ggec.voice.assistservice.data.BackGroundProcessServiceControlCommand;
import com.ggec.voice.assistservice.data.ImplAsyncCallback;
import com.ggec.voice.toollibrary.Util;
import com.ggec.voice.toollibrary.log.Log;
import com.willblaschko.android.alexa.AlexaManager;
import com.willblaschko.android.alexa.BroadCast;
import com.willblaschko.android.alexa.TokenManager;
import com.willblaschko.android.alexa.callbacks.AsyncCallback;
import com.willblaschko.android.alexa.callbacks.ImplTokenCallback;
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
import com.willblaschko.android.alexa.requestbody.FileDataRequestBody;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

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

    private Handler handler = new Handler(Looper.getMainLooper());

    public BgProcessIntentService() {
        this("test");
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public BgProcessIntentService(String name) {
        super(name);
        Log.i("BgProcessIntentService", "construct : " + name);
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestory");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {// TODO 修改接入OpenDownChannel ，简化这段代码
        final BackGroundProcessServiceControlCommand cmd = new BackGroundProcessServiceControlCommand(intent.getIntExtra(EXTRA_CMD, -1));
        Bundle b = intent.getBundleExtra("cmd_bundle");
        if (b != null) {
            cmd.bundle = b;
        }
        AlexaManager alexaManager = AlexaManager.getInstance(this, BuildConfig.PRODUCT_ID);

        if (cmd.type == BackGroundProcessServiceControlCommand.START_VOICE_RECORD) {
            final long waitMicDelayMillSecond = intent.getLongExtra("waitMicDelayMillSecond", 0);
            //start
//            startRecord1(cmd.waitMicDelayMillSecond);
            handler.postAtFrontOfQueue(new Runnable() {
                @Override
                public void run() {
                    startNearTalkRecord(waitMicDelayMillSecond);
                }
            });


        } else if (cmd.type == 2) {
            //stop
            textTest();
        } else if (cmd.type == 3) {
//            alexaManager.closeOpenDownchannel(false);
//            search();
            startRecord1(0);
        } else if (cmd.type == BackGroundProcessServiceControlCommand.BEGIN_ALARM) {
            final String token = intent.getStringExtra("token");
            final String messageId = intent.getStringExtra("messageId");
            Log.i(TAG, "BEGIN_ALARM: "+ messageId+ " ,token:"+token);
            SetAlertHelper.sendAlertStarted(alexaManager, token, getCallBack("sendAlertStarted"));
            handler.post(new Runnable() {
                @Override
                public void run() {
                    AvsHandleHelper.getAvsHandleHelper().handleAvsItem(new AvsAlertPlayItem(token, messageId));
                }
            });

//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    Intent it = new Intent(MyApplication.getContext(), BgProcessIntentService.class);
//                    it.putExtra(EXTRA_CMD, new BackGroundProcessServiceControlCommand(BackGroundProcessServiceControlCommand.STOP_ALARM));
//                    it.putExtra("token", token);
//                    it.putExtra("messageId", messageId);
//                    startService(it);
//
//                }
//            }, 15000);
//        } else if (cmd.type == BackGroundProcessServiceControlCommand.STOP_ALARM) {
//            final String token = intent.getStringExtra("token");
//            final String messageId = intent.getStringExtra("messageId");
//            Log.d(TAG, "STOP_ALARM: "+ messageId+ " ,token:"+token);
//
//            handler.post(new Runnable() {
//                @Override
//                public void run() {
//                    AvsHandleHelper.getAvsHandleHelper().handleAvsItem(new AvsAlertStopItem(token));
//                }
//            });
        } else if(cmd.type == BackGroundProcessServiceControlCommand.MUTE_CHANGE){
            SpeakerUtil.setMute(MyApplication.getContext()
                    , AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID)
                    , cmd.bundle.getBoolean("mute")
                    , new ImplAsyncCallback("setMute")
            );
        } else if(cmd.type == BackGroundProcessServiceControlCommand.VOLUME_CHANGE){
            SpeakerUtil.setVolume(MyApplication.getContext()
                    , AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID)
                    , cmd.bundle.getLong("volume")
                    , false
                    , new ImplAsyncCallback("setVolume"));
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
            alexaManager.tryRefreshToken(new ImplTokenCallback(){
                @Override
                public void beginRefreshTokenEvent(Context context, long expires_in) {
                    setTimerEvent(context, REFRESH_TOKEN_DELAY_JOB_ID, BackGroundProcessServiceControlCommand.REFRESH_TOKEN
                            , expires_in > REFRESH_TOKEN_MIN_INTERVAL? expires_in: REFRESH_TOKEN_MIN_INTERVAL);
                }

                @Override
                public void onFailure(Throwable e) {
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

    MyVoiceRecord myVoiceRecord;

    Runnable waitMicTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (myVoiceRecord != null && (myVoiceRecord.isAlive() || !myVoiceRecord.isInterrupted())) {
                myVoiceRecord.doActuallyInterrupt();
            }
        }
    };

    /**
     * @param waitMicTimeOut 这个值禁止手工设置
     */
    private void startRecord1(final long waitMicTimeOut) {
//        Uri path = Uri.parse("android.resource://"+BuildConfig.APPLICATION_ID+ "/"+R.raw.start);

        if (waitMicTimeOut > 0) {
            handler.postDelayed(waitMicTimeoutRunnable, waitMicTimeOut);
        }

        playStart(new MyShortAudioPlayer2.IOnCompletionListener() {
            @Override
            public void onCompletion() {
                if (myVoiceRecord != null && !myVoiceRecord.isInterrupted()) {
                    myVoiceRecord.doActuallyInterrupt();
                }
                myVoiceRecord = new MyVoiceRecord(MyVoiceRecord.DEFAULT_SILENT_THRESHOLD, new IMyVoiceRecordListener() {
                    @Override
                    public void recordFinish(boolean recordSuccess, String path, long actuallyLong) {
                        if (recordSuccess) {
                            handler.removeCallbacks(waitMicTimeoutRunnable);
                            AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID);
                            alexaManager.sendAudioRequest(new FileDataRequestBody(new File(path), actuallyLong), getFileCallBack("record", path));
                        } else {
                            if (waitMicTimeOut > 0) {
                                handler.removeCallbacks(waitMicTimeoutRunnable);
                                AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID);
                                alexaManager.sendExpectSpeechTimeoutEvent(getCallBack("sendExpectSpeechTimeoutEvent"));
                            }
                            playError();
                        }
                    }
                });
                myVoiceRecord.start();
            }
        });
    }

    private void startNearTalkRecord(final long waitMicTimeOut) {
        AvsHandleHelper.getAvsHandleHelper().pauseSound();
        AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID);
        SpeechSendAudio audio = alexaManager.getSpeechSendAudio();
        if (audio != null) audio.cancelRequest();
        if(!Util.isNetworkAvailable(this)){
            //TODO play no net work
            Log.e(TAG, "return because no net work");
            playError();
            return;
        }

        String path = MyApplication.getContext().getExternalFilesDir("near_talk").getPath() + "/" + System.currentTimeMillis();

        AvsHandleHelper.getAvsHandleHelper().startNearTalkVoiceRecord(path, new IMyVoiceRecordListener() {
            @Override
            public void recordFinish(boolean recordSuccess, String path, long actuallyLong) {
                if (recordSuccess) {

                } else {
                    if (waitMicTimeOut > 0 && actuallyLong == -1) {
                        AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID);
                        alexaManager.sendExpectSpeechTimeoutEvent(getCallBack("sendExpectSpeechTimeoutEvent"));
                    }
//                            playError();
                }
            }
        }, getFileCallBack("record", path));

    }

    private void textTest() {
        AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID);
        alexaManager.sendTextRequest("Tell me some news", getCallBack("textTest"));
        //Set a timer after 15 seconds from now" "Tell me some news" "Tell me the baseball news" Play TuneIn music radio"
        // "Set an alarm for 9:49 morning on everyday" "How's my day look"
    }

    private void search() {
        try {
            InputStream is = getAssets().open("intros/joke.raw");
            byte[] fileBytes = new byte[is.available()];
            is.read(fileBytes);
            is.close();
            AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID);
            alexaManager.sendAudioRequest(fileBytes, getCallBack("search"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private AsyncCallback<AvsResponse, Exception> getCallBack(final String name) {
        return new ImplAsyncCallback(name);
    }

    private AsyncCallback<AvsResponse, Exception> getFileCallBack(final String name, final String filePath) {
        return new ImplAsyncCallback(name) {

            @Override
            public void success(AvsResponse result) {
                super.success(result);
                deleteFile(filePath);
                if(result.continueWakeWordDetect) {
                    continueWakeWordDetect();
                }
            }

            @Override
            public void failure(Exception error) {
                super.failure(error);
                playError();
                deleteFile(filePath);
                if(error instanceof AvsAudioException) {
                    Log.e(TAG, "Speech Recognize event send, but receive nothing, http response code = 204");
//                    AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID);
//                    alexaManager.sendEvent(Event
//                            .createExceptionEncounteredEvent(ContextUtil.getActuallyContextList(MyApplication.getContext()
//                                    , AvsHandleHelper.getAvsHandleHelper().getAudioAndSpeechState())
//                            ,"", "UNEXPECTED_INFORMATION_RECEIVED", "Speech Recognize event send, but receive nothing, http response code = 204")
//                            , null);
                }
            }

            private void deleteFile(String fp) {
                if (true) {//TODO 清除文件
                    File file = new File(fp);
                    if (file.exists()) {
                        Log.w(TAG, "delete cache file state:" + file.delete() + " p:" + fp);
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

    /*
     * 这个由于ready性能太慢，因此不在这里调用了，提示音放在assistant server中播放
     */
    @Deprecated
    private void playStart(final MyShortAudioPlayer2.IOnCompletionListener listener) {
        new MyShortAudioPlayer2("asset:///start.mp3", listener);
//        handler.post(new Runnable() {
//            @Override
//            public void run() {
//                //"android.resource://" + BuildConfig.APPLICATION_ID + "/" + R.raw.start
//
//            }
//        });

    }

    private void playError() {
        Log.d(TAG, "playError");
        handler.post(new Runnable() {
            @Override
            public void run() {
                //"android.resource://" + BuildConfig.APPLICATION_ID + "/" + R.raw.start
                new MyShortAudioPlayer2("asset:///error.mp3", new MyShortAudioPlayer2.IOnCompletionListener() {
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
                AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID);
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
                AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID);
                alexaManager.closeOpenDownchannel(false);
            }
        });
    }

    private void openDownChannel(AlexaManager alexaManager){
        if (!alexaManager.hasOpenDownchannel()) {
            alexaManager.sendOpenDownchannelDirective(new ImplAsyncCallback("{opendownchannel}"){
                @Override
                public void start() {
                    AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID);
                    alexaManager.sendSynchronizeStateEvent2(ContextUtil.getActuallyContextList(MyApplication.getContext()
                            , AvsHandleHelper.getAvsHandleHelper().getAudioAndSpeechState()), getCallBack("SynchronizeState"));
                    setTimerEvent(MyApplication.getContext(), PING_JOB_ID, BackGroundProcessServiceControlCommand.SEND_PING, SEND_PING_INTERVAL);
                    setTimerEvent(MyApplication.getContext(), REFRESH_TOKEN_DELAY_JOB_ID, BackGroundProcessServiceControlCommand.REFRESH_TOKEN, REFRESH_TOKEN_MIN_INTERVAL);
                    Log.i(TAG, "opendownchannel connected");
                }

                @Override
                public void failure(Exception error) {
                    super.failure(error);
                    if(error instanceof AvsResponseException){
//        TODO open after test
               com.willblaschko.android.alexa.utility.Util.getPreferences(MyApplication.getContext()).edit().remove(TokenManager.PREF_TOKEN_EXPIRES).commit();
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
