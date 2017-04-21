package com.ggec.voice.assistservice.mediaplayer;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;

import com.ggec.voice.assistservice.BgProcessIntentService;
import com.ggec.voice.assistservice.BuildConfig;
import com.ggec.voice.assistservice.MyApplication;
import com.ggec.voice.assistservice.data.BackGroundProcessServiceControlCommand;
import com.ggec.voice.toollibrary.log.Log;
import com.willblaschko.android.alexa.AlexaManager;
import com.willblaschko.android.alexa.audioplayer.AlexaAudioExoPlayer;
import com.willblaschko.android.alexa.audioplayer.Callback;
import com.willblaschko.android.alexa.data.Event;
import com.willblaschko.android.alexa.interfaces.AvsItem;
import com.willblaschko.android.alexa.interfaces.alerts.AvsAlertPlayItem;
import com.willblaschko.android.alexa.interfaces.alerts.AvsAlertStopItem;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsAudioItem;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsPlayAudioItem;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsPlayContentItem;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsPlayRemoteItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsMediaNextCommandItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsMediaPauseCommandItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsMediaPlayCommandItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsMediaPreviousCommandItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsReplaceAllItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsReplaceEnqueuedItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsStopItem;
import com.willblaschko.android.alexa.interfaces.speechrecognizer.AvsExpectSpeechItem;
import com.willblaschko.android.alexa.interfaces.speechsynthesizer.AvsSpeakItem;

import java.util.LinkedHashMap;

/**
 * Created by ggec on 2017/4/21.
 */

public class GGECMediaManager {
    private final static String TAG = "GGECMediaManager";
    private volatile LinkedHashMap<String, AvsItem> avsQueue1 = new LinkedHashMap<>();

    private AlexaAudioExoPlayer mSpeechSynthesizerPlayer;
    private AlexaAudioExoPlayer mMediaAudioPlayer;
    private boolean needSendPlaybackStartEvent;
    //    private volatile int mediaState; // 0 idle, 1 play ,2 pause, 3 stop.


    public GGECMediaManager() {
        //instantiate our audio player
        mSpeechSynthesizerPlayer = new AlexaAudioExoPlayer(MyApplication.getContext());
        mMediaAudioPlayer = new AlexaAudioExoPlayer(MyApplication.getContext());

        //Remove the current item and check for more items once we've finished playing
        mSpeechSynthesizerPlayer.addCallback(mSpeechSynthesizerCallback);
        mMediaAudioPlayer.addCallback(mMediaAudioPlayerCallback);
    }


//    public void appendAllAtBegin(List<AvsItem> other) {
//        LinkedHashMap<String, AvsItem> to = new LinkedHashMap<>();
//        for (AvsItem item : other) {
//            to.put(item.messageID, item);
//        }
//        to.putAll(avsQueue1);
//        avsQueue1 = to;
//    }

    public void clear(boolean stopCurrent) {
        avsQueue1.clear();
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

    private final void pauseMediaAudio() {
        long lastPosition = mMediaAudioPlayer.stop();
        AvsItem item = mMediaAudioPlayer.getCurrentItem();
        if (item instanceof AvsAudioItem) {
            ((AvsAudioItem) item).pausePosition = lastPosition;
        } else if (item instanceof AvsAlertPlayItem || item == null) {

        } else {
            avsQueue1.remove(item.messageID);
        }
    }

    private void checkQueue() {
        mMediaPlayHandler.sendEmptyMessage(3);
    }

    /**
     * Check our current queue of items, and if we have more to parse (once we've reached a play or listen callback) then proceed to the
     * next item in our list.
     * <p>
     * We're handling the AvsReplaceAllItem in handleResponse() because it needs to clear everything currently in the queue, before
     * the new items are added to the list, it should have no function here.
     */
    private synchronized void checkQueueImpl() {

        //if we're out of things, hang up the phone and move on
        if (avsQueue1.size() == 0) {
            return;
        }
        final String key = getFirstKey();
        final AvsItem current = avsQueue1.get(key);

        Log.d(TAG, "Item type " + current.getClass().getName());

        if (current instanceof AvsPlayRemoteItem) {
            //play a URL
            if (!mSpeechSynthesizerPlayer.isPlaying() && !mMediaAudioPlayer.isPlaying()) {
                mMediaAudioPlayer.playItem((AvsPlayRemoteItem) current);
                sendPlaybackStartedEvent(current, 0);
            }
//        } else if (current instanceof AvsPlayContentItem) {
//            //play a URL
//            if (!audioPlayer.isPlaying()) {
//                audioPlayer.playItem((AvsPlayContentItem) current);
//            }
        } else if (current instanceof AvsPlayAudioItem) {
            if (!mSpeechSynthesizerPlayer.isPlaying() && !mMediaAudioPlayer.isPlaying()) {
                mMediaAudioPlayer.playItem((AvsSpeakItem) current);
                sendPlaybackStartedEvent(current, 0);
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
//            mSpeechSynthesizerPlayer.stop();
//            mMediaAudioPlayer.stop();
            avsQueue1.remove(current.messageID);
            mMediaPlayHandler.sendEmptyMessage(2);
            startListening(((AvsExpectSpeechItem) current).getTimeoutInMiliseconds());
        } else if (current instanceof AvsAlertPlayItem) {
            if (!mSpeechSynthesizerPlayer.isPlaying()) {
                tryPauseMediaAudio();
                mSpeechSynthesizerPlayer.playItem((AvsAlertPlayItem) current);
            }
        } else {
            avsQueue1.remove(key);
            Log.w(TAG, "pop a unhandle item !" + current.getClass());
            checkQueueImpl();
        }
    }

    private void tryPauseMediaAudio() {
        if (mMediaAudioPlayer.isPlaying()) {
            pauseMediaAudio();
        }
    }


    //Our callback that deals with removing played items in our media player and then checking to see if more items exist
    private Callback mSpeechSynthesizerCallback = new Callback() {

        @Override
        public void playerPrepared(AvsItem pendingItem) {
            sendSpeakStartedEvent(pendingItem);
        }

        @Override
        public void playerProgress(AvsItem item, long offsetInMilliseconds, float percent) {
        }

        @Override
        public void itemComplete(AvsItem completedItem, boolean error, long offsetInMilliseconds) {
            boolean isRemove = avsQueue1.remove(completedItem.messageID) != null;
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "Complete " + completedItem.getToken() + " fired, remove old:" + isRemove);
            }

            if (completedItem instanceof AvsSpeakItem) {
                AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID)
                        .sendEvent(Event.getSpeechFinishedEvent(completedItem.getToken()), null);
            }

            checkQueue();
        }

        @Override
        public boolean playerError(AvsItem item, int what, int extra) {
            itemComplete(item, true, 0);
            return true;
        }

        @Override
        public void dataError(AvsItem item, Exception e) {
            itemComplete(item, true, 0);
            Log.e(TAG, "mSpeechSynthesizerCallback dataError!", e);
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
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "PlaybackStarted " + item.getToken() + " fired: " + percent);
                }
                playbackStartedFired = true;
                sendPlaybackStartedEvent(item, offsetInMilliseconds);
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
                Log.i(TAG, "Complete " + completedItem.getToken() + " fired");
            }

            almostDoneFired = false;
            playbackStartedFired = false;
            avsQueue1.remove(completedItem.messageID);
            checkQueue();
            if (completedItem instanceof AvsPlayContentItem) {
                return;
            }

            if (!(completedItem instanceof AvsAlertPlayItem))
                sendPlaybackCompleteEvent(completedItem, offsetInMilliseconds, offsetInMilliseconds > 0);
        }

        @Override
        public boolean playerError(AvsItem item, int what, int extra) {
            itemComplete(item, true, 0);
            return true;
        }

        @Override
        public void dataError(AvsItem item, Exception e) {
            itemComplete(item, true, 0);
            Log.e(TAG, "mMediaAudioPlayerCallback error!", e);
        }
    };

    private void startListening(long waitMicMillseconds) {
        Intent it = new Intent(MyApplication.getContext(), BgProcessIntentService.class);

        BackGroundProcessServiceControlCommand cmd = new BackGroundProcessServiceControlCommand(BackGroundProcessServiceControlCommand.START_VOICE_RECORD);
//        BackGroundProcessServiceControlCommand.createIntentByType(MyApplication.getContext(), BackGroundProcessServiceControlCommand.START_VOICE_RECORD)
        cmd.waitMicDelayMillSecond = waitMicMillseconds;
        it.putExtra(BgProcessIntentService.EXTRA_CMD, cmd);
        MyApplication.getContext().startService(it);
    }

    /**
     * Send an event back to Alexa that we're starting a speech event
     * 如果是PlaybackStartedEvent ，则只需要在一开始发一次即可
     * https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/audioplayer#PlaybackNearlyFinished Event
     */
    private void sendSpeakStartedEvent(AvsItem item) {
        if (item instanceof AvsSpeakItem) {
            Log.i(TAG, "Sending SpeechStartedEvent");
            AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID).sendEvent(Event.getSpeechStartedEvent(item.getToken()), null);
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
     * Send an event back to Alexa that we're done with our current speech event, this should supply us with the next item
     * https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/audioplayer#PlaybackNearlyFinished Event
     */
    private void sendPlaybackCompleteEvent(AvsItem item, long offset, boolean success) {
        if (item != null) {
            String event = null;
            if (item instanceof AvsPlayAudioItem) {
                if (success) {
                    event = Event.getPlaybackFinishedEvent(item.getToken(), offset);
                } else {
                    //TODO
                }
            } else if (item instanceof AvsSpeakItem) {
                event = Event.getSpeechFinishedEvent(item.getToken());
            }
            if (event != null)
                AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID).sendEvent(event, null);
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
                    .sendEvent(Event.getPlaybackNearlyFinishedEvent(item.getToken(), offsetInMilliseconds), null);
        }
    }

    private String getFirstKey() {
        String out = null;
        for (String key : avsQueue1.keySet()) {
            out = (key);
            break;
        }
        return out;
    }

    public boolean addAvsItemToQueue(AvsItem response) {

        if (response instanceof AvsAlertStopItem) { //FIXME 这个需要专门处理
            Log.d(TAG, "stop alarm right now :" + response.messageID);
            avsQueue1.remove(response.messageID);
            mSpeechSynthesizerPlayer.release(false);
        } else if (response instanceof AvsReplaceAllItem) {
            //clear our queue
            clear(true);
            Log.w(TAG, "Immediately begin playback of the stream returned with the Play directive, and replace current and enqueued streams");
        } else if (response instanceof AvsReplaceEnqueuedItem) {
            //Replace all streams in the queue.
            // This does not impact the currently playing stream.
            clear(false);
            Log.w(TAG, "Replace all streams in the queue. This does not impact the currently playing stream.");
        } else if (response instanceof AvsPlayRemoteItem
                || response instanceof AvsPlayAudioItem) {

            avsQueue1.put(response.messageID, response);
        } else if (response instanceof AvsSpeakItem
                || response instanceof AvsExpectSpeechItem
                || response instanceof AvsAlertPlayItem) {

            avsQueue1.put(response.messageID, response);
        } else if (response instanceof AvsStopItem) {
            //stop our play
            mMediaAudioPlayer.stop(); //FIXME 注意这个,现在是完全停止
            mSpeechSynthesizerPlayer.stop();
            avsQueue1.clear();
        } else if (response instanceof AvsMediaPlayCommandItem) {
        } else if (response instanceof AvsMediaPauseCommandItem) {
        } else if (response instanceof AvsMediaNextCommandItem) {
        } else if (response instanceof AvsMediaPreviousCommandItem) {
            Log.e(TAG, "Can not handle :" + response.getClass().getName());
            return true;
        } else {
            return false;
        }

        mMediaPlayHandler.sendEmptyMessage(1);

        return true;
    }

    private Handler mMediaPlayHandler = new Handler() {
        private boolean running;

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                if (avsQueue1.size() == 0){
                    running = false;
                }
                if (!running && (avsQueue1.size() > 0 )) {
                    running = true;
                    checkQueueImpl();
                }
            } else if (msg.what == 2) {
                running = false;
                this.removeMessages(1);
            } else if (msg.what == 3) {
                if (avsQueue1.size() == 0) {
                    running = false;
                }
                if (running) {
                    checkQueueImpl();
                }
            }
        }
    };
}
