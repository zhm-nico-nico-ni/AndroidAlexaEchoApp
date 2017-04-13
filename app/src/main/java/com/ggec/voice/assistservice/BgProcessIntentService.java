package com.ggec.voice.assistservice;

import android.app.IntentService;
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
import com.ggec.voice.assistservice.log.Log;
import com.ggec.voice.assistservice.speechutil.RawAudioRecorder;
import com.ggec.voice.assistservice.test.AudioCapture;
import com.ggec.voice.assistservice.test.RecordingRMSListener;
import com.ggec.voice.assistservice.test.RecordingStateListener;
import com.willblaschko.android.alexa.AlexaManager;
import com.willblaschko.android.alexa.callbacks.AsyncCallback;
import com.willblaschko.android.alexa.interfaces.AvsResponse;
import com.willblaschko.android.alexa.interfaces.alerts.AvsAlertPlayItem;
import com.willblaschko.android.alexa.interfaces.alerts.AvsAlertStopItem;
import com.willblaschko.android.alexa.interfaces.alerts.SetAlertHelper;
import com.willblaschko.android.alexa.interfaces.speaker.SpeakerUtil;
import com.willblaschko.android.alexa.interfaces.speechrecognizer.SpeechSendAudio;
import com.willblaschko.android.alexa.requestbody.DataRequestBody;
import com.willblaschko.android.alexa.requestbody.FileDataRequestBody;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import okio.BufferedSink;

/**
 * Created by ggec on 2017/3/29.
 */

public class BgProcessIntentService extends IntentService {
    private static String TAG = "BgProcessIntentService";

    public static final String EXTRA_CMD = "Control-Command";

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
        BackGroundProcessServiceControlCommand cmd = intent.getParcelableExtra(EXTRA_CMD);
        AlexaManager alexaManager = AlexaManager.getInstance(this, BuildConfig.PRODUCT_ID);
//        if (!alexaManager.hasOpenDownchannel()) {
//            alexaManager.sendOpenDownchannelDirective(getCallBack("{opendownchannel}"));
//            return;
//        }

        if (cmd.type == 1) {
            //start
//            TestRun run = new TestRun();
//            currentRunId = run.time;
            Log.d(TAG, "start " + currentRunId);
//            try {
//                startRecord();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            alexaManager.sendSynchronizeStateEvent2(getCallBack("{SynchronizeStateEvent}"));
            startRecord1(cmd.waitMicDelayMillSecond);
//            search();
        } else if (cmd.type == 2) {
            //stop
            Log.d(TAG, "stop " + currentRunId);
            currentRunId = 0;

            textTest();
        } else if (cmd.type == 3) {
            alexaManager.sendSynchronizeStateEvent2(getCallBack("{SynchronizeStateEvent}"));
            alexaManager.sendPingEvent();
//            search();
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
            String token = intent.getStringExtra("token");
            String messageId = intent.getStringExtra("messageId");
            Log.d(TAG, "STOP_ALARM: "+ messageId+ " ,token:"+token);
            SetAlertHelper.sendAlertStopped(alexaManager, token, getCallBack("sendAlertStopped"));
            SetAlertHelper.deleteAlertSP(MyApplication.getContext(), messageId);
            AvsHandleHelper.getAvsHandleHelper().handleAvsItem(new AvsAlertStopItem(token, messageId));
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
        }  else {
            //cancel
            Log.d(TAG, "unknow cmd:" + cmd.type);
        }
    }

    MyVoiceRecord myVoiceRecord;

    Runnable waitMicTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (myVoiceRecord != null && (myVoiceRecord.isAlive() || !myVoiceRecord.isInterrupted())) {
                myVoiceRecord.interrupt();
            }
        }
    };

    /**
     * @param waitMicTimeOut 这个值禁止手工设置
     */
    private void startRecord1(final long waitMicTimeOut) {
//        Uri path = Uri.parse("android.resource://"+BuildConfig.APPLICATION_ID+ "/"+R.raw.start);
//        playSong(path);
        if (waitMicTimeOut > 0) {
            handler.postDelayed(waitMicTimeoutRunnable, waitMicTimeOut);
        }

        playStart(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (myVoiceRecord != null && !myVoiceRecord.isInterrupted()) {
                    myVoiceRecord.interrupt();
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
        SpeechSendAudio audio = alexaManager.getSpeechSendAudio();
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

    public void finishProcessing() {
        Log.d(TAG, "finishProcessing");
//       TODO
// controller.processingFinished();
    }

    private volatile long currentRunId;

    class TestRun implements Runnable {
        public final long time;

        public TestRun() {
            time = SystemClock.elapsedRealtime();
        }

        @Override
        public void run() {
            if (currentRunId != time) {

            }
            Log.d(TAG, "TestRun is cancel = " + (currentRunId != time));
        }
    }

    private void textTest() {
        AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID);
        alexaManager.sendTextRequest("Hello", getCallBack("textTest"));//Set a timer after 15 seconds from now"Set a timer after one minutes from now"
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
            }

            @Override
            public void failure(Exception error) {
                super.failure(error);
                playError();
                deleteFile(filePath);
            }

            private void deleteFile(String fp) {
                Log.d(TAG, "delete cache file p:" + fp);
                if (true) {//TODO 清除文件
                    File file = new File(fp);
                    if (file.exists()) {
                        Log.w(TAG, "delete cache file state:" + file.delete() + " p:" + fp);
                    }
                }
            }
        };
    }

    ;
//    private void playSong(Uri rawFile){
//        Log.d(TAG, "playSong "+rawFile);
//        AsyncPlayer player = new AsyncPlayer("state sound");
//        player.play(MyApplication.getContext(), rawFile, false,
//                new AudioAttributes.Builder()
////                        .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
//                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
////                        .setFlags()
//                        .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
//                        .build());
//
//    }

//    private void playSong(Uri rawFile){
//        Log.d(TAG, "playSong "+rawFile);
//        MyShortAudioPlayer player = new MyShortAudioPlayer(MyApplication.getContext(), rawFile, false,
//                new AudioAttributes.Builder()
////                        .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
//                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
////                        .setFlags()
//                        .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
//                        .build());
//        player.play();
//
//    }

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
        handler.post(new Runnable() {
            @Override
            public void run() {
                //"android.resource://" + BuildConfig.APPLICATION_ID + "/" + R.raw.start
                new MyShortAudioPlayer2("asset:///error.mp3", null);
            }
        });
    }
}
