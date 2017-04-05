package com.ggec.voice.assistservice;

import android.app.IntentService;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AsyncPlayer;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.Nullable;

import com.ggec.voice.assistservice.audio.IMyVoiceRecordListener;
import com.ggec.voice.assistservice.audio.MyShortAudioPlayer;
import com.ggec.voice.assistservice.audio.MyShortAudioPlayer2;
import com.ggec.voice.assistservice.audio.MyVoiceRecord;
import com.ggec.voice.assistservice.data.BackGroundProcessServiceControlCommand;
import com.ggec.voice.assistservice.log.Log;
import com.ggec.voice.assistservice.speechutil.RawAudioRecorder;
import com.ggec.voice.assistservice.test.AudioCapture;
import com.ggec.voice.assistservice.test.RecordingRMSListener;
import com.ggec.voice.assistservice.test.RecordingStateListener;
import com.willblaschko.android.alexa.AlexaManager;
import com.willblaschko.android.alexa.callbacks.AsyncCallback;
import com.willblaschko.android.alexa.interfaces.AvsResponse;
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

    private Handler handler = new Handler();
    private RawAudioRecorder recorder;

    public BgProcessIntentService() {
        super("test");
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
    protected void onHandleIntent(@Nullable Intent intent) {
        BackGroundProcessServiceControlCommand cmd = intent.getParcelableExtra(EXTRA_CMD);

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
            startRecord1();
        } else if (cmd.type == 2) {
            //stop
            Log.d(TAG, "stop " + currentRunId);
            currentRunId = 0;
            textTest();
        } else if (cmd.type == 3) {
            search();
        } else {
            //cancel
        }
    }

    MyVoiceRecord myVoiceRecord;

    private void startRecord1() {
//        Uri path = Uri.parse("android.resource://"+BuildConfig.APPLICATION_ID+ "/"+R.raw.start);
//        playSong(path);

        playStart(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (myVoiceRecord != null && !myVoiceRecord.isInterrupted()) {
                    myVoiceRecord.interrupt();
                }
                myVoiceRecord = new MyVoiceRecord(MyVoiceRecord.DEFAULT_SILENT_THRESHOLD, new IMyVoiceRecordListener() {
                    @Override
                    public void recordFinish(boolean recordSuccess, String path, long actuallyLong) {
                        if(recordSuccess){
                            AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID);
                            alexaManager.sendAudioRequest(new FileDataRequestBody(new File(path), actuallyLong), getFileCallBack(path));
                        } else {
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
        alexaManager.sendAudioRequest(getAudioSendRequestBody(), getCallBack());
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
        alexaManager.sendTextRequest("tell me the time", getCallBack());//"Where i am"
    }

    private void search() {
        try {
            InputStream is = getAssets().open("intros/joke.raw");
            byte[] fileBytes = new byte[is.available()];
            is.read(fileBytes);
            is.close();
            AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID);
            alexaManager.sendAudioRequest(fileBytes, getCallBack());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private AsyncCallback<AvsResponse, Exception> getCallBack() {
        return new AsyncCallback<AvsResponse, Exception>() {
            public long startTime;

            @Override
            public void start() {
                startTime = System.currentTimeMillis();
                Log.i(TAG, "Event Start");
//                setState(STATE_PROCESSING);
            }

            @Override
            public void success(AvsResponse result) {
                Log.i(TAG, "Event Success " + result);
                AvsHandleHelper.getAvsHandleHelper().handleResponse(result);
//                handleResponse(result);
            }

            @Override
            public void failure(Exception error) {
                Log.e(TAG, "Event Error", error);
//                setState(STATE_FINISHED);
            }

            @Override
            public void complete() {
                long totalTime = System.currentTimeMillis() - startTime;
                Log.i(TAG, "Event Complete, " + "Total request time: " + totalTime + " miliseconds");
            }
        };
    }

    private AsyncCallback<AvsResponse, Exception> getFileCallBack(final String filePath) {
        return new AsyncCallback<AvsResponse, Exception>() {
            public long startTime;

            @Override
            public void start() {
                startTime = System.currentTimeMillis();
                Log.i(TAG, "Event Start");
//                setState(STATE_PROCESSING);
            }

            @Override
            public void success(AvsResponse result) {
                Log.i(TAG, "Event Success " + result);
                AvsHandleHelper.getAvsHandleHelper().handleResponse(result);
//                handleResponse(result);
                deleteFile(filePath);
            }

            @Override
            public void failure(Exception error) {
                Log.e(TAG, "Event Error", error);
//                setState(STATE_FINISHED);
                playError();
                deleteFile(filePath);
            }

            @Override
            public void complete() {
                long totalTime = System.currentTimeMillis() - startTime;
                Log.i(TAG, "Event Complete, " + "Total request time: " + totalTime + " miliseconds");
            }

            private void deleteFile(String fp){
                Log.d(TAG, "delete cache file p:"+fp);
                if(false) {//TODO 清除文件
                    File file = new File(fp);
                    if (file.exists()){
                        Log.w(TAG, "delete cache file state:" + file.delete()+ " p:"+fp);
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

    private void playStart(final MediaPlayer.OnCompletionListener listener){
//        MyShortAudioPlayer.getInstance(this).playStart();
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    new MyShortAudioPlayer2("android.resource://"+BuildConfig.APPLICATION_ID+ "/"+R.raw.error, listener);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }
    private void playError(){
        try {
            new MyShortAudioPlayer2("android.resource://"+BuildConfig.APPLICATION_ID+ "/"+R.raw.start, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        MyShortAudioPlayer.getInstance(this).playError();
    }
}
