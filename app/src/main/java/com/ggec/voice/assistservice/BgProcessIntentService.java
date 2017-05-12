package com.ggec.voice.assistservice;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.annotation.Nullable;

import com.ggec.voice.assistservice.audio.IMyVoiceRecordListener;
import com.ggec.voice.assistservice.audio.MyShortAudioPlayer2;
import com.ggec.voice.assistservice.audio.MyVoiceRecord;
import com.ggec.voice.assistservice.data.BackGroundProcessServiceControlCommand;
import com.ggec.voice.assistservice.data.ImplAsyncCallback;
import com.ggec.voice.assistservice.speechutil.RawAudioRecorder;
import com.ggec.voice.assistservice.test.AudioCapture;
import com.ggec.voice.assistservice.test.RecordingRMSListener;
import com.ggec.voice.assistservice.test.RecordingStateListener;
import com.ggec.voice.toollibrary.Util;
import com.ggec.voice.toollibrary.log.Log;
import com.willblaschko.android.alexa.AlexaManager;
import com.willblaschko.android.alexa.BroadCast;
import com.willblaschko.android.alexa.callbacks.AsyncCallback;
import com.willblaschko.android.alexa.callbacks.IGetContextEventCallBack;
import com.willblaschko.android.alexa.callbacks.ImplTokenCallback;
import com.willblaschko.android.alexa.data.Event;
import com.willblaschko.android.alexa.interfaces.AvsAudioException;
import com.willblaschko.android.alexa.interfaces.AvsResponse;
import com.willblaschko.android.alexa.interfaces.alerts.AvsAlertPlayItem;
import com.willblaschko.android.alexa.interfaces.alerts.AvsAlertStopItem;
import com.willblaschko.android.alexa.interfaces.alerts.SetAlertHelper;
import com.willblaschko.android.alexa.interfaces.context.ContextUtil;
import com.willblaschko.android.alexa.interfaces.speaker.SpeakerUtil;
import com.willblaschko.android.alexa.interfaces.speechrecognizer.SpeechSendAudio;
import com.willblaschko.android.alexa.requestbody.DataRequestBody;
import com.willblaschko.android.alexa.requestbody.FileDataRequestBody;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import okio.BufferedSink;

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
    private RawAudioRecorder recorder;

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
        final BackGroundProcessServiceControlCommand cmd = intent.getParcelableExtra(EXTRA_CMD);
        AlexaManager alexaManager = AlexaManager.getInstance(this, BuildConfig.PRODUCT_ID);

        if (cmd.type == BackGroundProcessServiceControlCommand.START_VOICE_RECORD) {
            //start
//            startRecord1(cmd.waitMicDelayMillSecond);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    startNearTalkRecord(cmd.waitMicDelayMillSecond);
                }
            });


        } else if (cmd.type == 2) {
            //stop
            textTest();
        } else if (cmd.type == 3) {
//            alexaManager.closeOpenDownchannel(false);
//            search();
            startRecord1(cmd.waitMicDelayMillSecond);
        } else if (cmd.type == BackGroundProcessServiceControlCommand.BEGIN_ALARM) {
            final String token = intent.getStringExtra("token");
            final String messageId = intent.getStringExtra("messageId");
            Log.d(TAG, "BEGIN_ALARM: "+ messageId+ " ,token:"+token);
            SetAlertHelper.sendAlertStarted(alexaManager, token, getCallBack("sendAlertStarted"));
            SetAlertHelper.sendAlertEnteredForeground(alexaManager, token, getCallBack("sendAlertEnteredForeground"));
            handler.post(new Runnable() {
                @Override
                public void run() {
                    AvsHandleHelper.getAvsHandleHelper().handleAvsItem(new AvsAlertPlayItem(token, messageId));
                }
            });

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent it = new Intent(MyApplication.getContext(), BgProcessIntentService.class);
                    it.putExtra(EXTRA_CMD, new BackGroundProcessServiceControlCommand(BackGroundProcessServiceControlCommand.STOP_ALARM));
                    it.putExtra("token", token);
                    it.putExtra("messageId", messageId);
                    startService(it);

                }
            }, 15000);
        } else if (cmd.type == BackGroundProcessServiceControlCommand.STOP_ALARM) {
            final String token = intent.getStringExtra("token");
            final String messageId = intent.getStringExtra("messageId");
            Log.d(TAG, "STOP_ALARM: "+ messageId+ " ,token:"+token);
            SetAlertHelper.sendAlertStopped(alexaManager, token, getCallBack("sendAlertStopped"));
            SetAlertHelper.deleteAlertSP(MyApplication.getContext(), token);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    AvsHandleHelper.getAvsHandleHelper().handleAvsItem(new AvsAlertStopItem(token));
                }
            });
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
        }else {
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

        playStart(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
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
        SpeechSendAudio audio = alexaManager.getSpeechSendAudio(new IGetContextEventCallBack() {
            @Override
            public List<Event> getContextEvent() {
                return ContextUtil.getContextList(MyApplication.getContext());
            }
        });
        if (audio != null) audio.cancelRequest();
        if(!Util.isNetworkAvailable(this)){
            //TODO play no net work
            Log.e(TAG, "return because no net work");
            playError();
            continueWakeWordDetect();
            return;
        }
        playStart(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
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
                            playError();
                        }
                    }
                }, getFileCallBack("record", path));
            }
        });
    }

    private void startRecord() throws IOException {
//        RequestListener requestListener = new RequestListener() {
//
//            @Override
//            public void onRequestSuccess() {
//                Log.d(TAG, "startRecord# onRequestSuccess");
//                finishProcessing();
//            }
//
//            @Override
//            public void onRequestError(Throwable e) {
//                Log.e(TAG, "startRecord# onRequestError", e);
//                finishProcessing();
//            }
//        };
//
//        //// TODO: 2017/3/29
//        controller.startRecording(rmsListener, requestListener);


        AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID);
        SpeechSendAudio audio = alexaManager.getSpeechSendAudio(new IGetContextEventCallBack() {
            @Override
            public List<Event> getContextEvent() {
                return ContextUtil.getContextList(MyApplication.getContext());
            }
        });
        if (audio != null) audio.cancelRequest(); // TODO 这里要cancel Http request
        if (recorder != null) {
            recorder.stop();
            recorder.release();
            recorder = null;
        }
        alexaManager.sendAudioRequest(getAudioSendRequestBody(), getCallBack(""));
    }

    private DataRequestBody getAudioSendRequestBody() {
        if (recorder == null) {
            recorder = new RawAudioRecorder(16000);
        }
        recorder.start();
        android.util.Log.i("zhm", "writeTo 111");
        DataRequestBody requestBody = new DataRequestBody() {
            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                android.util.Log.i("zhm", "writeTo 1");
                while (recorder != null && !recorder.isPausing()) {
                    if (sink != null && recorder != null) {
//                        final float rmsdb = recorder.getRmsdb();
                        sink.write(recorder.consumeRecording());
//                        Log.d(TAG, "RMSDB: " + rmsdb);
                    }
                }
                stopListening();
                Log.i("zhm", "writeTo1 end");
            }

            private void stopListening() {
                if (recorder != null) {
                    recorder.stop();
                    recorder.release();
                    recorder = null;
                }
            }
        };
        return requestBody;
    }

    private DataRequestBody getInputRequestBody() throws IOException {
        final AudioCapture audioCapture = AudioCapture.getAudioHardware(MyApplication.getContext(), 16000);
        audioCapture.stopCapture();
        final InputStream inputStream = audioCapture.getAudioInputStream(new RecordingStateListener() {
            @Override
            public void recordingStarted() {
                Log.i("zhm", "recordingStarted ");
            }

            @Override
            public void recordingCompleted() {
                Log.i("zhm", "recordingCompleted ");
            }
        }, new RecordingRMSListener() {
            @Override
            public void rmsChanged(int rms) {
                Log.i("zhm", "rmsChanged " + rms);
            }
        });

        final byte[] buffer = new byte[1024];
        DataRequestBody requestBody = new DataRequestBody() {
            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                android.util.Log.i("zhm", "writeTo ");
                while (!audioCapture.isPausing()) {
                    writeSink(sink);

//                try {
//                    Thread.sleep(25);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                }
                writeSink(sink);
                inputStream.close();
//                stopListening();
                android.util.Log.i("zhm", "writeTo end");
            }

            private void writeSink(BufferedSink sink) throws IOException {
                if (inputStream.available() > 0 && sink != null) {
                    int available = inputStream.read(buffer);
                    sink.write(buffer, 0, available);
                    sink.flush();
                }
            }

        };
        return requestBody;
    }

    private void textTest() {
        AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID);
        alexaManager.sendTextRequest("Play TuneIn music radio", getCallBack("textTest"));//Set a timer after 15 seconds from now" "Tell me some news" "Tell me the baseball news" Play TuneIn music radio"
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
                continueWakeWordDetect();
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
    }

    private void playStart(final MediaPlayer.OnCompletionListener listener) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                //"android.resource://" + BuildConfig.APPLICATION_ID + "/" + R.raw.start
                new MyShortAudioPlayer2("asset:///start.mp3", listener);
            }
        });

    }

    private void playError() {
        Log.d(TAG, "playError");
        handler.post(new Runnable() {
            @Override
            public void run() {
                //"android.resource://" + BuildConfig.APPLICATION_ID + "/" + R.raw.start
                new MyShortAudioPlayer2("asset:///error.mp3", null);
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
