package com.ggec.voice.assistservice;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.Nullable;

import com.ggec.voice.assistservice.data.BackGroundProcessServiceControlCommand;
import com.ggec.voice.assistservice.log.Log;
import com.willblaschko.android.alexa.AlexaManager;
import com.willblaschko.android.alexa.callbacks.AsyncCallback;
import com.willblaschko.android.alexa.interfaces.AvsResponse;

/**
 * Created by ggec on 2017/3/29.
 */

public class BgProcessIntentService extends IntentService {
    private static String TAG = "BgProcessIntentService";

    public static final String EXTRA_CMD = "Control-Command";

    private Handler handler = new Handler();

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
            startRecord();
        } else if (cmd.type == 2) {
            //stop
            Log.d(TAG, "stop " + currentRunId);
            currentRunId = 0;
        } else {
            //cancel
        }
    }


    private void startRecord() {
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
        alexaManager.sendAudioRequest();
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

    private void textTest(){
        AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID);
        alexaManager.sendTextRequest("Where i am", new AsyncCallback<AvsResponse, Exception>() {
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
                Log.i(TAG, "Event Complete, " +"Total request time: "+totalTime+" miliseconds");
            }
        });
    }
}
