package com.ggec.voice.assistservice.mediaplayer;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.ggec.voice.assistservice.BuildConfig;
import com.ggec.voice.assistservice.MyApplication;
import com.ggec.voice.assistservice.data.BackGroundProcessServiceControlCommand;
import com.ggec.voice.assistservice.data.ImplAsyncCallback;
import com.ggec.voice.toollibrary.log.Log;
import com.willblaschko.android.alexa.AlexaManager;
import com.willblaschko.android.alexa.audioplayer.Callback;
import com.willblaschko.android.alexa.audioplayer.player.GGECMediaAudioPlayer;
import com.willblaschko.android.alexa.audioplayer.player.GGECSpeechSynthesizerPlayer;
import com.willblaschko.android.alexa.data.Event;
import com.willblaschko.android.alexa.data.message.PayloadFactory;
import com.willblaschko.android.alexa.data.message.request.audioplayer.PlaybackError;
import com.willblaschko.android.alexa.interfaces.AvsItem;
import com.willblaschko.android.alexa.interfaces.alerts.AvsAlertPlayItem;
import com.willblaschko.android.alexa.interfaces.alerts.SetAlertHelper;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsAudioItem;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsClearQueueItem;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsLocalResumeItem;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsPlayAudioItem;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsPlayRemoteItem;
import com.willblaschko.android.alexa.interfaces.audioplayer.IAvsPlayDirectiveBaseItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsMediaNextCommandItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsMediaPauseCommandItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsMediaPlayCommandItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsMediaPreviousCommandItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsReplaceAllItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsReplaceEnqueuedItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsStopItem;
import com.willblaschko.android.alexa.interfaces.speechrecognizer.AvsExpectSpeechItem;
import com.willblaschko.android.alexa.interfaces.speechsynthesizer.AvsSpeakItem;
import com.willblaschko.android.alexa.keep.AVSAPIConstants;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by ggec on 2017/4/21.
 */

public class GGECMediaManager {
    private final static String TAG = "GGECMediaManager";
    private final static int QUEUE_STATE_START = 1;
    private final static int QUEUE_STATE_PAUSE = 2;
    private final static int QUEUE_STATE_DO_WORK = 3;
    private final static int QUEUE_STATE_SET_RUN_STOP = 4;

    private volatile LinkedHashMap<String, AvsItem> avsQueue1 = new LinkedHashMap<>();
    private volatile LinkedHashMap<String, AvsItem> avsQueue2 = new LinkedHashMap<>();

    private GGECSpeechSynthesizerPlayer mSpeechSynthesizerPlayer;
    private GGECMediaAudioPlayer mMediaAudioPlayer;
    private boolean needSendPlaybackStartEvent;


    public GGECMediaManager() {
        //instantiate our audio player
        mSpeechSynthesizerPlayer = new GGECSpeechSynthesizerPlayer(MyApplication.getContext());
        mMediaAudioPlayer = new GGECMediaAudioPlayer(MyApplication.getContext());

        //Remove the current item and check for more items once we've finished playing
        mSpeechSynthesizerPlayer.addCallback(mSpeechSynthesizerCallback);
        mMediaAudioPlayer.addCallback(mMediaAudioPlayerCallback);
    }


    private boolean appendAllAtBegin(AvsItem other) {
        if (!avsQueue1.isEmpty()) {
            LinkedHashMap<String, AvsItem> to = new LinkedHashMap<>(1 + avsQueue1.size());
            to.put(other.messageID, other);
            to.putAll(avsQueue1);
            avsQueue1 = to;
            return true;
        } else {
            avsQueue1.put(other.messageID, other);
            return false;
        }
    }

    public void clear(boolean stopCurrent) {
        avsQueue1.clear();
        avsQueue2.clear();
        if (stopCurrent) {
            if (mSpeechSynthesizerPlayer.isPlaying() && !(mSpeechSynthesizerPlayer.getCurrentItem() instanceof AvsAlertPlayItem)) {
                mSpeechSynthesizerPlayer.release(false);
            }

            if (mMediaAudioPlayer.isPlaying()) {
                mMediaAudioPlayer.release(false);
            }
            doWhenMediaPauseOrStop();
        }
    }

    //For each URL that AVS sends,
    // it expects no more than one PlaybackStarted event.
    // If you receive a playlist URL (composed of multiple URLs) only send one PlaybackStarted event
    public void setNeedSendPlaybackStartEvent() {
        needSendPlaybackStartEvent = true;
    }

    private final AvsAudioItem pauseMediaAudio() {
        if(canPlayMedia == 0) {
            canPlayMedia = 2;
        }

        mMediaAudioPlayer.pause();
        long lastPosition = mMediaAudioPlayer.getCurrentPosition();
        AvsItem item = mMediaAudioPlayer.getCurrentItem();
        if (item instanceof AvsAudioItem) {
            ((AvsAudioItem) item).pausePosition = lastPosition;
            return (AvsAudioItem) item;
        } else {
            avsQueue1.remove(item.messageID);
        }
        return null;
    }

    private void checkQueue() {
        mMediaPlayHandler.sendEmptyMessage(QUEUE_STATE_DO_WORK);
    }

    private void tryPauseMediaAudio() {
        if (mMediaAudioPlayer.isPlaying()) {
            AvsAudioItem item = pauseMediaAudio();
            sendPlaybackPauseOrResumeEvent(true, item);
        }

        doWhenMediaPauseOrStop();
    }


    //Our callback that deals with removing played items in our media player and then checking to see if more items exist
    private Callback mSpeechSynthesizerCallback = new Callback() {

        @Override
        public void playerPrepared(AvsItem pendingItem) {
            if (pendingItem instanceof AvsSpeakItem) {
                Log.i(TAG, "Sending SpeechStartedEvent");
                AlexaManager.getInstance(MyApplication.getContext())
                        .sendEvent(Event.getSpeechStartedEvent(pendingItem.getToken()), null);
            } else if(pendingItem instanceof AvsAlertPlayItem){
                SetAlertHelper.sendAlertEnteredForeground(AlexaManager.getInstance(MyApplication.getContext())
                        , pendingItem.getToken(), new ImplAsyncCallback("sendAlertEnteredForeground"));
            }
        }

        @Override
        public void playerProgress(AvsItem item, long offsetInMilliseconds, float percent, long remaining) {
        }

        @Override
        public void itemComplete(AvsItem completedItem, boolean error, long offsetInMilliseconds) {
            boolean isRemove = avsQueue1.remove(completedItem.messageID) != null;
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "SpeechSynthesizerCallback Complete " + completedItem.getToken() + " fired, remove old:" + isRemove);
            }

            doWhenSpeechSynthesizerEnd(completedItem);
            checkQueue();
        }

        @Override
        public void dataError(AvsItem item, Exception e) {
            itemComplete(item, true, 0);
            Log.e(TAG, "mSpeechSynthesizerCallback dataError!", e);
        }

        @Override
        public void onBufferReady(AvsItem item, long offsetInMilliseconds, long stutterDurationInMilliseconds) {

        }

        @Override
        public void onBuffering(AvsItem item, long offset) {

        }
    };

    //Our callback that deals with removing played items in our media player and then checking to see if more items exist
    private Callback mMediaAudioPlayerCallback = new Callback() {

        private boolean almostDoneFired = false; // FIXME 这两个标志貌似会无法复位，需要检查
        private boolean playbackStartedFired = false;

        @Override
        public void playerPrepared(AvsItem pendingItem) {
            almostDoneFired = false;
            playbackStartedFired = false;
            if(pendingItem instanceof AvsPlayRemoteItem){
                mCurrentPlayMediaRemoteItem = (AvsPlayRemoteItem) pendingItem;
                beginReportDelayElapsedEvent(mCurrentPlayMediaRemoteItem);
                beginReportIntervalElapsedEvent(mCurrentPlayMediaRemoteItem);
            }
        }

        @Override
        public void playerProgress(AvsItem item, long offsetInMilliseconds, float percent, long remaining) {
            if (item == null) {
                return;
            }
            if (!playbackStartedFired) {
                playbackStartedFired = true;
                if(item instanceof AvsAudioItem){
                    sendPlaybackStartedEvent(item, offsetInMilliseconds);
                }
            }

            if (!almostDoneFired && (percent >= 1 || (percent > 0.8f && remaining < 8000))) {
                Log.d(TAG, "AlmostDone " + item.getToken() + " fired: " + percent);
                almostDoneFired = true;
                if (item instanceof AvsAudioItem ) {
                    sendPlaybackNearlyFinishedEvent((AvsAudioItem) item, offsetInMilliseconds);
                }
            }
        }

        @Override
        public void itemComplete(AvsItem completedItem, boolean error, long offsetInMilliseconds) {
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "MediaAudioPlayerCallback Complete " + completedItem.getToken() + " fired");
            }

            almostDoneFired = false;
            playbackStartedFired = false;
            if(completedItem instanceof IAvsPlayDirectiveBaseItem){
                avsQueue2.remove(((IAvsPlayDirectiveBaseItem) completedItem).getUrl());
            } else {
                avsQueue2.remove(completedItem.messageID);
            }
            checkQueue();
            if(completedItem instanceof AvsPlayAudioItem){
                ((AvsPlayAudioItem) completedItem).releaseAudio();
            }

            if (!(completedItem instanceof AvsAlertPlayItem))
                sendPlaybackCompleteEvent(completedItem, offsetInMilliseconds, offsetInMilliseconds > 0);
        }

        @Override
        public void dataError(AvsItem item, Exception e) {
            long position = mMediaAudioPlayer.getCurrentPosition();
            itemComplete(item, true, 0);
            Log.e(TAG, "mMediaAudioPlayerCallback error!", e);
            if(item == null) return;
            //String directiveToken, long offset, String playerActivity, String errorType, String errorMessage
            Log.w(TAG, "send getPlaybackFailEvent:");
            AlexaManager.getInstance(MyApplication.getContext())
                    .sendEvent(Event.getPlaybackFailEvent(item.getToken(), position, mMediaAudioPlayer.getStateString()
                    , new PlaybackError(e)), new ImplAsyncCallback("getPlaybackFailEvent"));
        }

        @Override
        public void onBufferReady(AvsItem item, long offsetInMilliseconds, long stutterDurationInMilliseconds) {

            if(item != null) {
                Log.i(TAG, "Sending getPlaybackStutterFinishEvent");
                AlexaManager.getInstance(MyApplication.getContext())
                        .sendEvent(Event.getPlaybackStutterFinishEvent(item.getToken(), offsetInMilliseconds, stutterDurationInMilliseconds), null);
            }
        }

        @Override
        public void onBuffering(AvsItem item, long offset) {
            if(item != null) {
                Log.i(TAG, "Sending getPlaybackStutterStartedEvent");
                AlexaManager.getInstance(MyApplication.getContext())
                        .sendEvent(Event.getPlaybackStutterStartedEvent(item.getToken(), offset), null);
            }
        }

        private void sendPlaybackStartedEvent(AvsItem item, long offset) {
            if (needSendPlaybackStartEvent) {
                if (item instanceof AvsPlayAudioItem || item instanceof AvsPlayRemoteItem) {
                    needSendPlaybackStartEvent = false;
                    Log.i(TAG, "Sending PlaybackStarted");
                    AlexaManager.getInstance(MyApplication.getContext()).sendEvent(Event.getPlaybackStartedEvent(item.getToken(), offset), null);
                }
            }
        }

        /**
         * Send an event back to Alexa that we're nearly done with our current playback event, this should supply us with the next item
         * https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/audioplayer#PlaybackNearlyFinished Event
         */
        private void sendPlaybackNearlyFinishedEvent(AvsAudioItem item, long offsetInMilliseconds) {
            if (item != null) {
                Log.i(TAG, "Sending PlaybackNearlyFinishedEvent");
                AlexaManager.getInstance(MyApplication.getContext())
                        .sendEvent(Event.getPlaybackNearlyFinishedEvent(item.getToken(), offsetInMilliseconds), new ImplAsyncCallback("PlaybackNearlyFinishedEvent"));
            }
        }

        /**
         * Send an event back to Alexa that we're done with our current speech event, this should supply us with the next item
         * https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/audioplayer#PlaybackComplete Event
         */
        private void sendPlaybackCompleteEvent(AvsItem item, long offset, boolean success) {
            if (success) {
                String event = null;
                if (item instanceof AvsPlayAudioItem || item instanceof AvsPlayRemoteItem) {
                    if (avsQueue2.isEmpty()) {
                        event = Event.getPlaybackFinishedEvent(item.getToken(), offset);
                    }
                } else if (item instanceof AvsSpeakItem) {
                    Log.e(TAG, "why AvsSpeakItem appear here?");
                }
                if (event != null)
                    AlexaManager.getInstance(MyApplication.getContext()).sendEvent(event, new ImplAsyncCallback("PlaybackComplete"));
            }
        }
    };

    private AvsPlayRemoteItem mCurrentPlayMediaRemoteItem;
    private Runnable mRunReportDelayElapsedEvent = new Runnable() {
        @Override
        public void run() {
            if(mCurrentPlayMediaRemoteItem!=null
                    && mCurrentPlayMediaRemoteItem.equals(mMediaAudioPlayer.getCurrentItem())
                    && mMediaAudioPlayer.isPlaying()) {
                AlexaManager.getInstance(MyApplication.getContext())
                        .sendEvent(Event.createProgressReportDelayElapsedEvent(mCurrentPlayMediaRemoteItem.getToken(), mMediaAudioPlayer.getCurrentPosition()),
                                new ImplAsyncCallback("ReportDelayElapsedEvent"));
            }
        }
    };
    private void beginReportDelayElapsedEvent(AvsPlayRemoteItem item){
        if(item.getProgressReportDelayInMilliseconds() > 0){
            mMediaPlayHandler.postDelayed(mRunReportDelayElapsedEvent, item.getProgressReportDelayInMilliseconds());
        }
    }
    private Runnable mRunReportIntervalElapsedEvent = new Runnable() {
        @Override
        public void run() {
            if(mCurrentPlayMediaRemoteItem!=null
                    && mCurrentPlayMediaRemoteItem.equals(mMediaAudioPlayer.getCurrentItem())
                    && mMediaAudioPlayer.isPlaying()) {
                AlexaManager.getInstance(MyApplication.getContext()).sendEvent(
                        Event.createProgressReportIntervalElapsedEvent(mCurrentPlayMediaRemoteItem.getToken()
                                , mMediaAudioPlayer.getCurrentPosition()),
                                new ImplAsyncCallback("ReportDelayElapsedEvent"));

                beginReportIntervalElapsedEvent(mCurrentPlayMediaRemoteItem);
            }
        }
    };
    private void beginReportIntervalElapsedEvent(AvsPlayRemoteItem item){
        if(item.getProgressReportIntervalInMilliseconds() > 0){
            mMediaPlayHandler.postDelayed(mRunReportIntervalElapsedEvent, item.getProgressReportIntervalInMilliseconds());
        }
    }

    private void startListening(long waitMicMillseconds, String initiator) {
        Intent it = BackGroundProcessServiceControlCommand.createIntentByType(MyApplication.getContext(), BackGroundProcessServiceControlCommand.START_VOICE_RECORD);
        it.putExtra("waitMicDelayMillSecond", waitMicMillseconds);
        it.putExtra("initiator", initiator);
        MyApplication.getContext().startService(it);
    }

    private void setCanPlayMedia(){
        if(2 == canPlayMedia) canPlayMedia = 0;
    }

    public boolean addAvsItemToQueue(AvsItem response) {
        Log.d(TAG, "addAvsItemToQueue "+response.getClass());
        if(response instanceof AvsClearQueueItem){
            if(((AvsClearQueueItem) response).isClearAll()){
                Log.w(TAG, "ClearQueue Directive, and replace current and enqueued streams");
                long position = mMediaAudioPlayer.getCurrentPosition();
                clear(true);
                sendPlaybackStoppedEvent(mMediaAudioPlayer.getCurrentItem(), position);
            } else {
                Log.w(TAG, "ClearQueue Directive. This does not impact the currently playing stream.");
                clear(false);
            }
            Log.d(TAG, "send PlaybackQueueCleared Event");
            AlexaManager.getInstance(MyApplication.getContext()).sendEvent(Event.getPlaybackQueueClearedEvent(), null);
        } else if (response instanceof AvsReplaceAllItem) {
            //clear our queue
            Log.w(TAG, "Immediately begin playback of the stream returned with the Play directive, and replace current and enqueued streams");
            long position = mMediaAudioPlayer.getCurrentPosition();
            clear(true);
            sendPlaybackStoppedEvent(mMediaAudioPlayer.getCurrentItem(), position);
            return true;
        } else if (response instanceof AvsReplaceEnqueuedItem) {
            //Replace all streams in the queue.
            // This does not impact the currently playing stream.
            Log.w(TAG, "Replace all streams in the queue. This does not impact the currently playing stream.");
            clear(false);
        } else if (response instanceof AvsPlayRemoteItem
                || response instanceof AvsPlayAudioItem) {

            //Note: When adding streams to your playback queue, you must ensure that the token for the
            // active stream matches the expectedPreviousToken in the stream being added to the queue.
            // If the tokens do not match the stream must be ignored. However,
            // if no expectedPreviousToken is returned, the stream must be added to the queue.
            IAvsPlayDirectiveBaseItem baseItem = (IAvsPlayDirectiveBaseItem)response;
            if(baseItem.canAddToQueue()) {
                avsQueue2.put(baseItem.getUrl(), response);
                if(!mMediaAudioPlayer.isPlaying())
                    setNeedSendPlaybackStartEvent();
            } else {
                Log.w(TAG, "expectedPreviousToken not equal token, ignore!");
            }

        } else if (response instanceof AvsSpeakItem
                || response instanceof AvsExpectSpeechItem){
            avsQueue1.put(response.messageID, response);
        } else if(response instanceof AvsAlertPlayItem) {
            if(appendAllAtBegin(response)){ // 队列不为空时才报告进入background
                SetAlertHelper.sendAlertEnteredBackground(AlexaManager.getInstance(MyApplication.getContext())
                        , response.getToken(), new ImplAsyncCallback("sendAlertEnteredBackground"));
            }
            mMediaPlayHandler.sendEmptyMessage(QUEUE_STATE_SET_RUN_STOP);
        } else if (response instanceof AvsStopItem) {
            // The Stop directive is sent to your client to stop playback of an audio stream.
            // Your client may receive a Stop directive as the result of a voice request, a physical button press or GUI affordance.
            Log.w(TAG, "handle AvsStopItem");

            long position = mMediaAudioPlayer.stop(false); //注意这个,停止全部audio
            clear(true);
            mMediaPlayHandler.sendEmptyMessage(QUEUE_STATE_PAUSE);
            sendPlaybackStoppedEvent(mMediaAudioPlayer.getCurrentItem(), position);

            return true;
        } else if (response instanceof AvsMediaPlayCommandItem) { //TODO
        } else if (response instanceof AvsMediaPauseCommandItem) {
        } else if (response instanceof AvsMediaNextCommandItem) {
        } else if (response instanceof AvsMediaPreviousCommandItem) {
            Log.e(TAG, "Can not handle :" + response.getClass().getName());
            return true;
        } else if(response instanceof AvsLocalResumeItem){
            setCanPlayMedia();
        } else {
            return false;
        }

        mMediaPlayHandler.sendEmptyMessage(QUEUE_STATE_START);

        return true;
    }

    public boolean cancelAvsItem(List<AvsItem> list) {
        boolean result = false;
        for (AvsItem item : list) {
            if (!TextUtils.isEmpty(item.messageID)) {
                AvsItem current = avsQueue1.remove(item.messageID);
                if (null != current && current == mSpeechSynthesizerPlayer.getCurrentItem()) {
                    mSpeechSynthesizerPlayer.release(false);
                    result = true;
                } else {
                    current = avsQueue2.remove(item.messageID);
                    if (null != current && current == mMediaAudioPlayer.getCurrentItem()) {
                        mMediaAudioPlayer.release(false);
                        result = true;
                    }
                }
            }
        }
        return result;
    }

    private void sendPlaybackStoppedEvent(AvsItem playingItem, long position){
        if(playingItem!=null) {
            Log.d(TAG, "sendPlaybackStoppedEvent");
            AlexaManager.getInstance(MyApplication.getContext())
                    .sendEvent(Event.getPlaybackStoppedEvent(playingItem.getToken(), position), null);
        }
    }

    private void sendPlaybackPauseOrResumeEvent(boolean isPause, AvsAudioItem item){
        if(item == null || TextUtils.isEmpty(item.getToken())) return;

        String event;
        if(isPause){
            Log.d(TAG, "sendPlaybackPausedEvent");
            event = Event.getPlaybackPausedEvent(item.getToken(), item.pausePosition);
        } else {
            Log.d(TAG, "sendPlaybackResumeEvent");
            event = Event.getPlaybackResumedEvent(item.getToken(), item.pausePosition);
        }
        AlexaManager.getInstance(MyApplication.getContext()).sendEvent(event, null);
    }

    private void sendStopAlertEvent(String token){
        SetAlertHelper.sendAlertStopped(AlexaManager.getInstance(MyApplication.getContext())
                , token, new ImplAsyncCallback("sendAlertStopped"));
        SetAlertHelper.deleteAlertSP(MyApplication.getContext(), token);
    }


    public List<Event> getAudioAndSpeechState(){
        String playDirectiveToken = mMediaAudioPlayer.getCurrentToken();


        List<Event> list = new ArrayList<>();
        Event.Builder playbackEventBuilder = new Event.Builder()
                .setHeaderNamespace(AVSAPIConstants.AudioPlayer.NAMESPACE)
                .setHeaderName(AVSAPIConstants.AudioPlayer.Events.PlaybackState.NAME)
                .setPayload(PayloadFactory.createPlaybackStatePayload(playDirectiveToken,
                        mMediaAudioPlayer.getCurrentPosition(), mMediaAudioPlayer.getStateString()));
        list.add(playbackEventBuilder.build().getEvent());

        // 注意！！ 如果是Alert item的话，不要把Token 及 Offset 报上去
        boolean isLastPlayAlert = mSpeechSynthesizerPlayer.getCurrentItem() instanceof AvsAlertPlayItem;
        String speakDirectiveToken = isLastPlayAlert ? "" : mSpeechSynthesizerPlayer.getCurrentToken();
        Event.Builder speechSynthesizerEventBuilder = new Event.Builder()
                .setHeaderNamespace(AVSAPIConstants.SpeechSynthesizer.NAMESPACE)
                .setHeaderName(AVSAPIConstants.SpeechSynthesizer.Events.SpeechState.NAME)
                .setPayload(PayloadFactory.createSpeechStatePayload(speakDirectiveToken,
                        // 不要把Alert 的 Token 及 Offset 报上去,否则会出现不合理的url导致请求失败
                        isLastPlayAlert ? 0 : mSpeechSynthesizerPlayer.getCurrentPosition(),
                        /////////////////////////////////////////////////////////////////////////
                        mSpeechSynthesizerPlayer.getStateString()));
        list.add(speechSynthesizerEventBuilder.build().getEvent());
        return list;
    }

    public void pauseSound(){
        avsQueue1.clear();
        tryPauseMediaAudio();
        if(mSpeechSynthesizerPlayer.isPlaying()) {
            mSpeechSynthesizerPlayer.release(false);
            AvsItem currentItem = mSpeechSynthesizerPlayer.getCurrentItem();
            if(currentItem instanceof AvsAlertPlayItem){
                sendStopAlertEvent(currentItem.getToken());
            }
        }

        mMediaPlayHandler.sendEmptyMessage(QUEUE_STATE_PAUSE);
    }

    public void continueSound(){
        mMediaPlayHandler.sendEmptyMessageDelayed(QUEUE_STATE_START, 2000);
    }

    private void doWhenMediaPauseOrStop(){
        mMediaPlayHandler.removeCallbacks(mRunReportDelayElapsedEvent);
        mMediaPlayHandler.removeCallbacks(mRunReportIntervalElapsedEvent);
    }

    private void doWhenSpeechSynthesizerEnd(AvsItem completedItem){
        if (completedItem instanceof AvsSpeakItem) {
            AlexaManager.getInstance(MyApplication.getContext())
                    .sendEvent(Event.getSpeechFinishedEvent(completedItem.getToken()), null);
            ((AvsSpeakItem) completedItem).releaseAudio();
        } else if(completedItem instanceof AvsAlertPlayItem) {
            sendStopAlertEvent(completedItem.getToken());
        }
        setCanPlayMedia();
//        mSpeechSynthesizerPlayer.releaseAvsItem();
    }

    private volatile int canPlayMedia = 0; // 0 can ,1 not, 2 local susspent
    private Handler mMediaPlayHandler = new Handler(Looper.getMainLooper()) {
        private volatile boolean running;

        @Override
        public synchronized void handleMessage(Message msg) {
            if (msg.what == QUEUE_STATE_START) {
                checkIsFinish();

                if (!running &&
                        (avsQueue1.size() > 0 || (canPlayMedia == 0 && avsQueue2.size() > 0))) {
                    running = true;
                    checkQueueImpl();
                } else if(running && avsQueue1.size() == 0 ){
                    checkQueueImpl();
                }

            } else if (msg.what == QUEUE_STATE_PAUSE) { // STOP
                running = false;
                this.removeMessages(QUEUE_STATE_START);
                this.removeMessages(QUEUE_STATE_DO_WORK);

            } else if (msg.what == QUEUE_STATE_DO_WORK) { // continue

                if (running) {
                    checkQueueImpl();
                }
            } else if(msg.what == QUEUE_STATE_SET_RUN_STOP) {
                running = false;
            }
        }

        private synchronized boolean checkIsFinish() {
            if (canPlayMedia == 0) {
                if (avsQueue1.size() == 0 && (avsQueue2.size() == 0)) {
                    running = false;
                }
            } else {
                if (avsQueue1.size() == 0) {
                    running = false;
                }
            }
            return running;
        }

        /**
         * Check our current queue of items, and if we have more to parse (once we've reached a play or listen callback) then proceed to the
         * next item in our list.
         * <p>
         * We're handling the AvsReplaceAllItem in handleResponse() because it needs to clear everything currently in the queue, before
         * the new items are added to the list, it should have no function here.
         */
        private synchronized void checkQueueImpl() {
            if (!checkIsFinish()) {
                Log.d(TAG, "Audio Handle finish: " + running + " m:" + canPlayMedia);
                return;
            }


            final AvsItem current = getItem();

            Log.d(TAG, "Item type " + current.getClass().getName());

            if (current instanceof AvsPlayRemoteItem) {
                //play a URL
                if (!mSpeechSynthesizerPlayer.isPlaying() && !mMediaAudioPlayer.isPlaying()) {
                    AvsPlayRemoteItem audioItem = (AvsPlayRemoteItem) current;
                    mMediaAudioPlayer.playItem(audioItem);
                    if(audioItem.pausePosition > 0){
                        sendPlaybackPauseOrResumeEvent(false, audioItem);
                    }
                }
            } else if (current instanceof AvsPlayAudioItem) {
                if (!mSpeechSynthesizerPlayer.isPlaying() && !mMediaAudioPlayer.isPlaying()) {
                    AvsPlayAudioItem audioItem = (AvsPlayAudioItem) current;
                    mMediaAudioPlayer.playItem(audioItem);
                    if(audioItem.pausePosition > 0){
                        sendPlaybackPauseOrResumeEvent(false, audioItem);
                    }
                }
            } else if (current instanceof AvsSpeakItem) {
                //play a sound file
                if (!mSpeechSynthesizerPlayer.isPlaying()) {
                    tryPauseMediaAudio();
                    mSpeechSynthesizerPlayer.playItem((AvsSpeakItem) current);
                }
//            setState(STATE_SPEAKING);
            } else if (current instanceof AvsExpectSpeechItem) {
                //listen for user input
                pauseSound();
                AvsExpectSpeechItem speechItem = (AvsExpectSpeechItem) current;
                startListening(speechItem.getTimeoutInMiliseconds(), speechItem.initiator);
            } else if (current instanceof AvsAlertPlayItem) {
                if (!mSpeechSynthesizerPlayer.isPlaying()) {
                    tryPauseMediaAudio();
                    mSpeechSynthesizerPlayer.playItem((AvsAlertPlayItem) current);
                }
            } else {
                avsQueue1.remove(current.messageID);
                avsQueue2.remove(current.messageID);

                Log.w(TAG, "pop a unhandle item !" + current.getClass());
                checkQueueImpl();
            }
        }

        private AvsItem getItem(){
            AvsItem current;
            if(!avsQueue1.isEmpty()){
                current = avsQueue1.get(getFirstKey(avsQueue1));
            } else {
                current = avsQueue2.get(getFirstKey(avsQueue2));
            }

            return current;
        }

        private String getFirstKey(LinkedHashMap<String, AvsItem> queue) {
            String out = null;
            for (String key : queue.keySet()) {
                out = (key);
                break;
            }
            return out;
        }
    };
}
