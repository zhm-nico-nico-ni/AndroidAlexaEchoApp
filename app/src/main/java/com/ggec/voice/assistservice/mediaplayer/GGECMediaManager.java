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
import com.willblaschko.android.alexa.audioplayer.AlexaAudioExoPlayer;
import com.willblaschko.android.alexa.audioplayer.Callback;
import com.willblaschko.android.alexa.data.Event;
import com.willblaschko.android.alexa.data.message.PayloadFactory;
import com.willblaschko.android.alexa.data.message.request.audioplayer.PlaybackError;
import com.willblaschko.android.alexa.interfaces.AvsItem;
import com.willblaschko.android.alexa.interfaces.alerts.AvsAlertPlayItem;
import com.willblaschko.android.alexa.interfaces.alerts.AvsAlertStopItem;
import com.willblaschko.android.alexa.interfaces.alerts.AvsSetAlertItem;
import com.willblaschko.android.alexa.interfaces.alerts.SetAlertHelper;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsAudioItem;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsClearQueueItem;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsPlayAudioItem;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsPlayContentItem;
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

    private volatile LinkedHashMap<String, AvsItem> avsQueue1 = new LinkedHashMap<>();
    private volatile LinkedHashMap<String, AvsItem> avsQueue2 = new LinkedHashMap<>();

    private AlexaAudioExoPlayer mSpeechSynthesizerPlayer;
    private AlexaAudioExoPlayer mMediaAudioPlayer;
    private boolean needSendPlaybackStartEvent;


    public GGECMediaManager() {
        //instantiate our audio player
        mSpeechSynthesizerPlayer = new AlexaAudioExoPlayer(MyApplication.getContext());
        mMediaAudioPlayer = new AlexaAudioExoPlayer(MyApplication.getContext());

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
            if (mSpeechSynthesizerPlayer.isPlaying()) {
                mSpeechSynthesizerPlayer.release(false);
            }

            if (mMediaAudioPlayer.isPlaying()) {
                mMediaAudioPlayer.release(false);
            }
        }
        checkQueue();
    }

    //For each URL that AVS sends,
    // it expects no more than one PlaybackStarted event.
    // If you receive a playlist URL (composed of multiple URLs) only send one PlaybackStarted event
    public void setNeedSendPlaybackStartEvent() {
        needSendPlaybackStartEvent = true;
    }

    private final AvsAudioItem pauseMediaAudio() {
        long lastPosition = mMediaAudioPlayer.stop(true);
        AvsItem item = mMediaAudioPlayer.getCurrentItem();
        if (item instanceof AvsAudioItem) {
            ((AvsAudioItem) item).pausePosition = lastPosition;
            return (AvsAudioItem) item;
        } else if (item instanceof AvsAlertPlayItem || item == null) {

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
    }


    //Our callback that deals with removing played items in our media player and then checking to see if more items exist
    private Callback mSpeechSynthesizerCallback = new Callback() {

        @Override
        public void playerPrepared(AvsItem pendingItem) {
            if (pendingItem instanceof AvsSpeakItem) {
                Log.i(TAG, "Sending SpeechStartedEvent");
                AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID)
                        .sendEvent(Event.getSpeechStartedEvent(pendingItem.getToken()), null);
            } else if(pendingItem instanceof AvsAlertPlayItem){
                SetAlertHelper.sendAlertEnteredForeground(AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID)
                        , pendingItem.getToken(), new ImplAsyncCallback("sendAlertEnteredForeground"));
            }
        }

        @Override
        public void playerProgress(AvsItem item, long offsetInMilliseconds, float percent) {
        }

        @Override
        public void itemComplete(AvsItem completedItem, boolean error, long offsetInMilliseconds) {
            boolean isRemove = avsQueue1.remove(completedItem.messageID) != null;
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "SpeechSynthesizerCallback Complete " + completedItem.getToken() + " fired, remove old:" + isRemove);
            }

            if (completedItem instanceof AvsSpeakItem) {
                AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID)
                        .sendEvent(Event.getSpeechFinishedEvent(completedItem.getToken()), null);
            } else if(completedItem instanceof AvsAlertPlayItem) {
                sendStopAlertEvent(completedItem.getToken());
            }

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
        }

        @Override
        public void playerProgress(AvsItem item, long offsetInMilliseconds, float percent) {
            if (item instanceof AvsPlayContentItem || item == null) {
                return;
            }
            if (!playbackStartedFired) {
                playbackStartedFired = true;
                if(item instanceof AvsAudioItem){
                    AvsAudioItem audioItem = (AvsAudioItem) item;
                    if(audioItem.pausePosition > 0){
                        sendPlaybackPauseOrResumeEvent(false, audioItem);
                    }
                    sendPlaybackStartedEvent(item, offsetInMilliseconds);
                }
            }
            if (!almostDoneFired && percent > .8f) {
                Log.d(TAG, "AlmostDone " + item.getToken() + " fired: " + percent);
                almostDoneFired = true;
                if (item instanceof AvsPlayAudioItem) {
                    sendPlaybackNearlyFinishedEvent((AvsPlayAudioItem) item, offsetInMilliseconds);
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
            avsQueue2.remove(completedItem.messageID);
            checkQueue();
            if (completedItem instanceof AvsPlayContentItem) {
                return;
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
            AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID)
                    .sendEvent(Event.getPlaybackFailEvent(item.getToken(), position, mMediaAudioPlayer.getStateString()
                    , new PlaybackError(e)), new ImplAsyncCallback("getPlaybackFailEvent"));
        }

        @Override
        public void onBufferReady(AvsItem item, long offsetInMilliseconds, long stutterDurationInMilliseconds) {
//            send PlaybackStutterFinished Event
            if(item != null) {
                Log.i(TAG, "Sending PlaybackNearlyFinishedEvent");
                AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID)
                        .sendEvent(Event.getPlaybackStutterFinishEvent(item.getToken(), offsetInMilliseconds, stutterDurationInMilliseconds), null);
            }
        }

        @Override
        public void onBuffering(AvsItem item, long offset) {
            if(item != null) {
                Log.i(TAG, "Sending getPlaybackStutterStartedEvent");
                AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID)
                        .sendEvent(Event.getPlaybackStutterStartedEvent(item.getToken(), offset), null);
            }
        }

        private void sendPlaybackStartedEvent(AvsItem item, long offset) {
            if (needSendPlaybackStartEvent) {
                if (item instanceof AvsPlayAudioItem || item instanceof AvsPlayRemoteItem) {
                    needSendPlaybackStartEvent = false;
                    Log.i(TAG, "Sending PlaybackStarted");
                    AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID).sendEvent(Event.getPlaybackStartedEvent(item.getToken(), offset), null);
                }
            }
        }

        /**
         * Send an event back to Alexa that we're nearly done with our current playback event, this should supply us with the next item
         * https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/audioplayer#PlaybackNearlyFinished Event
         */
        private void sendPlaybackNearlyFinishedEvent(AvsPlayAudioItem item, long offsetInMilliseconds) {
            if (item != null) {
                Log.i(TAG, "Sending PlaybackNearlyFinishedEvent");
                AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID)
                        .sendEvent(Event.getPlaybackNearlyFinishedEvent(item.getToken(), offsetInMilliseconds), new ImplAsyncCallback("PlaybackNearlyFinishedEvent"));
            }
        }

        /**
         * Send an event back to Alexa that we're done with our current speech event, this should supply us with the next item
         * https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/audioplayer#PlaybackNearlyFinished Event
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
                    AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID).sendEvent(event, new ImplAsyncCallback("PlaybackComplete"));
            }
        }
    };

    private void startListening(long waitMicMillseconds) {
        Intent it = BackGroundProcessServiceControlCommand.createIntentByType(MyApplication.getContext(), BackGroundProcessServiceControlCommand.START_VOICE_RECORD);
        it.putExtra("waitMicDelayMillSecond", waitMicMillseconds);
        MyApplication.getContext().startService(it);
    }

    public boolean addAvsItemToQueue(AvsItem response) {

        if (response instanceof AvsAlertStopItem) { //FIXME 这个需要专门处理, 暂时不需要先不管
            Log.d(TAG, "stop alarm right now :" + response.getToken());
            AvsSetAlertItem item = SetAlertHelper.getAlertItemByToken(MyApplication.getContext(), response.getToken());
            if(item != null) {
                sendStopAlertEvent(response.getToken());
                avsQueue1.remove(item.messageID);
                if(TextUtils.equals(response.getToken(), mSpeechSynthesizerPlayer.getCurrentToken())) {
                    mSpeechSynthesizerPlayer.release(false);
                }
            }
        } else if(response instanceof AvsClearQueueItem){
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
            AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID).sendEvent(Event.getPlaybackQueueClearedEvent(), null);
        } else if (response instanceof AvsReplaceAllItem) {
            //clear our queue
            Log.w(TAG, "Immediately begin playback of the stream returned with the Play directive, and replace current and enqueued streams");
            long position = mMediaAudioPlayer.getCurrentPosition();
            clear(true);
            sendPlaybackStoppedEvent(mMediaAudioPlayer.getCurrentItem(), position);
        } else if (response instanceof AvsReplaceEnqueuedItem) {
            //Replace all streams in the queue.
            // This does not impact the currently playing stream.
            Log.w(TAG, "Replace all streams in the queue. This does not impact the currently playing stream.");
            clear(false);
        } else if (response instanceof AvsPlayRemoteItem
                || response instanceof AvsPlayAudioItem) {
            canPlayMedia = true;
            //Note: When adding streams to your playback queue, you must ensure that the token for the
            // active stream matches the expectedPreviousToken in the stream being added to the queue.
            // If the tokens do not match the stream must be ignored. However,
            // if no expectedPreviousToken is returned, the stream must be added to the queue.
            if(((IAvsPlayDirectiveBaseItem)response).canAddToQueue()) {
                avsQueue2.put(response.messageID, response);
                setNeedSendPlaybackStartEvent();
            } else {
                Log.w(TAG, "expectedPreviousToken not equal token, ignore!");
            }

        } else if (response instanceof AvsSpeakItem
                || response instanceof AvsExpectSpeechItem){
            avsQueue1.put(response.messageID, response);
        } else if(response instanceof AvsAlertPlayItem) {
            if(appendAllAtBegin(response)){ // 队列不为空时才报告进入background
                SetAlertHelper.sendAlertEnteredBackground(AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID)
                        , response.getToken(), new ImplAsyncCallback("sendAlertEnteredBackground"));
            }
        } else if (response instanceof AvsStopItem) {
            // The Stop directive is sent to your client to stop playback of an audio stream.
            // Your client may receive a Stop directive as the result of a voice request, a physical button press or GUI affordance.
            Log.w(TAG, "handle AvsStopItem");
            canPlayMedia = false;
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
        } else {
            return false;
        }

        mMediaPlayHandler.sendEmptyMessage(QUEUE_STATE_START);

        return true;
    }

    private void sendPlaybackStoppedEvent(AvsItem playingItem, long position){
        if(playingItem!=null) {
            Log.d(TAG, "sendPlaybackStoppedEvent");
            AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID)
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
        AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID).sendEvent(event, null);
    }

    private void sendStopAlertEvent(String token){
        SetAlertHelper.sendAlertStopped(AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID)
                , token, new ImplAsyncCallback("sendAlertStopped"));
        SetAlertHelper.deleteAlertSP(MyApplication.getContext(), token);
    }

    private String getSpeechSynthesizerState(){
        return "PLAYING".equals(mSpeechSynthesizerPlayer.getStateString())? "PLAYING" : "FINISHED";
    }

    private String getAudioState(){
        return mMediaAudioPlayer.getStateString();
    }

    public List<Event> getAudioAndSpeechState(){
        String playDirectiveToken = mMediaAudioPlayer.getCurrentToken();
        String SpeakDirectiveToken = mSpeechSynthesizerPlayer.getCurrentToken();
        List<Event> list = new ArrayList<>();
        Event.Builder playbackEventBuilder = new Event.Builder()
                .setHeaderNamespace(AVSAPIConstants.AudioPlayer.NAMESPACE)
                .setHeaderName(AVSAPIConstants.AudioPlayer.Events.PlaybackState.NAME)
                .setPayload(PayloadFactory.createPlaybackStatePayload(playDirectiveToken, mMediaAudioPlayer.getCurrentPosition(), getAudioState()))
                ;
        list.add(playbackEventBuilder.build().getEvent());

        Event.Builder speechSynthesizerEventBuilder = new Event.Builder()
                .setHeaderNamespace(AVSAPIConstants.SpeechSynthesizer.NAMESPACE)
                .setHeaderName(AVSAPIConstants.SpeechSynthesizer.Events.SpeechState.NAME)
                .setPayload(PayloadFactory.createSpeechStatePayload(SpeakDirectiveToken, mSpeechSynthesizerPlayer.getCurrentPosition(), getSpeechSynthesizerState()));
        list.add(speechSynthesizerEventBuilder.build().getEvent());
        return list;
    }

    public void pauseSound(){
        tryPauseMediaAudio();
        mSpeechSynthesizerPlayer.stop(false);
        AvsItem currentItem = mSpeechSynthesizerPlayer.getCurrentItem();
        if(mMediaAudioPlayer.isPlaying() && currentItem instanceof AvsAlertPlayItem){
            sendStopAlertEvent(currentItem.getToken());
        }

        avsQueue1.clear();
        mMediaPlayHandler.sendEmptyMessage(QUEUE_STATE_PAUSE);
    }

    public void continueSound(){
        mMediaPlayHandler.sendEmptyMessageDelayed(QUEUE_STATE_START, 2000);
    }


    private boolean canPlayMedia = true;
    private Handler mMediaPlayHandler = new Handler(Looper.getMainLooper()) {
        private boolean running;

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == QUEUE_STATE_START) {
                checkIsFinish();

                if (!running &&
                        (avsQueue1.size() > 0 || (canPlayMedia && avsQueue2.size() > 0))) {
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
            }
        }

        private boolean checkIsFinish() {
            if (canPlayMedia) {
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
                    mMediaAudioPlayer.playItem((AvsPlayRemoteItem) current);
                }
//        } else if (current instanceof AvsPlayContentItem) {
//            //play a URL
//            if (!audioPlayer.isPlaying()) {
//                audioPlayer.playItem((AvsPlayContentItem) current);
//            }
            } else if (current instanceof AvsPlayAudioItem) {
                if (!mSpeechSynthesizerPlayer.isPlaying() && !mMediaAudioPlayer.isPlaying()) {
                    mMediaAudioPlayer.playItem((AvsPlayAudioItem) current);
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
                startListening(((AvsExpectSpeechItem) current).getTimeoutInMiliseconds());
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
