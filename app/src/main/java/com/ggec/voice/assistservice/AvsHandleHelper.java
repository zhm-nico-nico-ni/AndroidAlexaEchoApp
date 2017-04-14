package com.ggec.voice.assistservice;

import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.ggec.voice.assistservice.data.BackGroundProcessServiceControlCommand;
import com.ggec.voice.assistservice.data.ImplAsyncCallback;
import com.willblaschko.android.alexa.AlexaManager;
import com.willblaschko.android.alexa.audioplayer.AlexaAudioPlayer;
import com.willblaschko.android.alexa.data.Event;
import com.willblaschko.android.alexa.interfaces.AvsItem;
import com.willblaschko.android.alexa.interfaces.AvsResponse;
import com.willblaschko.android.alexa.interfaces.alerts.AvsAlertPlayItem;
import com.willblaschko.android.alexa.interfaces.alerts.AvsAlertStopItem;
import com.willblaschko.android.alexa.interfaces.alerts.AvsDeleteAlertItem;
import com.willblaschko.android.alexa.interfaces.alerts.AvsSetAlertItem;
import com.willblaschko.android.alexa.interfaces.alerts.SetAlertHelper;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsPlayAudioItem;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsPlayContentItem;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsPlayRemoteItem;
import com.willblaschko.android.alexa.interfaces.context.ContextUtil;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsMediaNextCommandItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsMediaPauseCommandItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsMediaPlayCommandItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsMediaPreviousCommandItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsReplaceAllItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsReplaceEnqueuedItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsStopItem;
import com.willblaschko.android.alexa.interfaces.speaker.AvsAdjustVolumeItem;
import com.willblaschko.android.alexa.interfaces.speaker.AvsSetMuteItem;
import com.willblaschko.android.alexa.interfaces.speaker.AvsSetVolumeItem;
import com.willblaschko.android.alexa.interfaces.speaker.SpeakerUtil;
import com.willblaschko.android.alexa.interfaces.speechrecognizer.AvsExpectSpeechItem;
import com.willblaschko.android.alexa.interfaces.speechrecognizer.AvsStopCaptureItem;
import com.willblaschko.android.alexa.interfaces.speechsynthesizer.AvsSpeakItem;
import com.willblaschko.android.alexa.interfaces.system.AvsResetUserInactivityItem;
import com.willblaschko.android.alexa.interfaces.system.AvsSetEndPointItem;
import com.willblaschko.android.alexa.interfaces.system.AvsUnableExecuteItem;

import java.util.LinkedHashMap;

/**
 * Created by ggec on 2017/3/31.
 */

public class AvsHandleHelper {
    private static final String TAG = "com.ggec.voice.assistservice.AvsHandleHelper";
    private static AvsHandleHelper sAvsHandleHelper;
    private volatile LinkedHashMap<String, AvsItem> avsQueue = new LinkedHashMap<>();
    private AlexaAudioPlayer audioPlayer;
    //TODO 這裏把microphone 的控制加上

    private AvsHandleHelper() {
        //instantiate our audio player
        audioPlayer = AlexaAudioPlayer.getInstance(MyApplication.getContext());

        //Remove the current item and check for more items once we've finished playing
        audioPlayer.addCallback(alexaAudioPlayerCallback);
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
        boolean checkAfter = (avsQueue.size() == 0);
        if (response != null) {
            //if we have a clear queue item in the list, we need to clear the current queue before proceeding
            //iterate backwards to avoid changing our array positions and getting all the nasty errors that come
            //from doing that
            for (int i = 0; i < response.size(); i++) {
                addAvsItemToQueue(response.get(i));
            }
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "Adding " + response.size() + " items to our queue, " + " old is: " + avsQueue.size());
                for (int i = 0; i < response.size(); i++) {
                    Log.i(TAG, "\tAdding: " + response.get(i).getToken());
                }
            }
        }
        if (checkAfter) {
            checkQueue();
        }
    }

    public void handleAvsItem(AvsItem response) {
        boolean checkAfter = (avsQueue.size() == 0);
        addAvsItemToQueue(response);
        if (checkAfter) {
            checkQueue();
        }
    }

    private void addAvsItemToQueue(AvsItem response){
        if(response == null) return;

        if(response instanceof AvsAlertStopItem){ //FIXME 这个需要专门处理
            Log.d(TAG, "stop alarm right now :"+response.messageID);
            avsQueue.remove(response.messageID);
            audioPlayer.release();
            checkQueue();
            return;
        }

        if(processAvsItemImmediately(response)){
            return;
        }
        //if we have a clear queue item in the list, we need to clear the current queue before proceeding
        //iterate backwards to avoid changing our array positions and getting all the nasty errors that come
        //from doing that
        if (response instanceof AvsReplaceAllItem || response instanceof AvsReplaceEnqueuedItem) {
            //clear our queue
            avsQueue.clear();
            Log.w(TAG, " handle clear queue ! AvsReplaceAllItem or AvsReplaceEnqueuedItem");
            return;
        }

        if(TextUtils.isEmpty(response.messageID)){
            //AvsResponseException， 已经在parseResponse 中throw，AvsUnableExecuteItem 已在processAvsItemImmediately处理
            if(BuildConfig.DEBUG) {
                Log.e(TAG, "[important]handleAvsItem messageID is empty! " + response.getClass());
            }
            return;
        }
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "Adding " + response + " items to our queue, " + " old is: " + avsQueue.size());
        }
        avsQueue.put(response.messageID, response);
    }

    /**
     * Check our current queue of items, and if we have more to parse (once we've reached a play or listen callback) then proceed to the
     * next item in our list.
     * <p>
     * We're handling the AvsReplaceAllItem in handleResponse() because it needs to clear everything currently in the queue, before
     * the new items are added to the list, it should have no function here.
     */
    private synchronized void checkQueue() {

        //if we're out of things, hang up the phone and move on
        if (avsQueue.size() == 0) {
//            setState(STATE_FINISHED);
            return;
        }

        String key = getFirstKey();
        final AvsItem current = avsQueue.get(key);

        Log.d(TAG, "Item type " + current.getClass().getName());

        if (current instanceof AvsPlayRemoteItem) {
            //play a URL
            if (!audioPlayer.isPlaying()) {
                audioPlayer.playItem((AvsPlayRemoteItem) current);
            }
        } else if (current instanceof AvsPlayContentItem) {
            //play a URL
            if (!audioPlayer.isPlaying()) {
                audioPlayer.playItem((AvsPlayContentItem) current);
            }
        } else if (current instanceof AvsSpeakItem) {
            //play a sound file
            if (!audioPlayer.isPlaying()) {
                audioPlayer.playItem((AvsSpeakItem) current);
            }
//            setState(STATE_SPEAKING);
        } else if (current instanceof AvsStopItem) {
            //stop our play
            audioPlayer.stop();
            avsQueue.remove(key);
        } else if (current instanceof AvsReplaceAllItem) {
            //clear all items
            //mAvsItemQueue.clear();
            audioPlayer.stop();
            avsQueue.remove(key);
        } else if (current instanceof AvsReplaceEnqueuedItem) {
            //clear all items
            //mAvsItemQueue.clear();

            avsQueue.remove(key);
        } else if (current instanceof AvsExpectSpeechItem) {
            //listen for user input
            audioPlayer.stop();
            avsQueue.clear();
            startListening(((AvsExpectSpeechItem) current).getTimeoutInMiliseconds());
        } else if (current instanceof AvsMediaPlayCommandItem) {
            //fake a hardware "play" press
//            sendMediaButton(this, KeyEvent.KEYCODE_MEDIA_PLAY);
            Log.i(TAG, "Media play command issued");
            avsQueue.remove(key);
            checkQueue();
        } else if (current instanceof AvsMediaPauseCommandItem) {
            //fake a hardware "pause" press
//            sendMediaButton(this, KeyEvent.KEYCODE_MEDIA_PAUSE);
            Log.i(TAG, "Media pause command issued");
            avsQueue.remove(key);
            checkQueue();
        } else if (current instanceof AvsMediaNextCommandItem) {
            //fake a hardware "next" press
//            sendMediaButton(this, KeyEvent.KEYCODE_MEDIA_NEXT);
            Log.i(TAG, "Media next command issued");
            avsQueue.remove(key);
            checkQueue();
        } else if (current instanceof AvsMediaPreviousCommandItem) {
            //fake a hardware "previous" press
//            sendMediaButton(this, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
            Log.i(TAG, "Media previous command issued");
            avsQueue.remove(key);
            checkQueue();
        }  else if (current instanceof AvsSetAlertItem) {
            AvsSetAlertItem setAlertItem = (AvsSetAlertItem) current;
            Intent it = new Intent(MyApplication.getContext(), BgProcessIntentService.class);
            it.putExtra(BgProcessIntentService.EXTRA_CMD, new BackGroundProcessServiceControlCommand(BackGroundProcessServiceControlCommand.BEGIN_ALARM));
            boolean setSuccess = SetAlertHelper.setAlert(MyApplication.getContext(), setAlertItem, it);
            if (setSuccess) {
                SetAlertHelper.putAlert(MyApplication.getContext(), setAlertItem);
                SetAlertHelper.sendSetAlertSucceeded(AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID)
                        , current.getToken(), new ImplAsyncCallback("SetAlertSucceeded"));
            } else {
                SetAlertHelper.sendSetAlertFailed(AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID)
                        , current.getToken(), new ImplAsyncCallback("SetAlertFail"));
            }
            avsQueue.remove(key);
            checkQueue();
        } else if (current instanceof AvsDeleteAlertItem) {
            AvsDeleteAlertItem deleteAlertItem = (AvsDeleteAlertItem) current;
            Intent it = new Intent(MyApplication.getContext(), BgProcessIntentService.class);
            it.putExtra(BgProcessIntentService.EXTRA_CMD, new BackGroundProcessServiceControlCommand(BackGroundProcessServiceControlCommand.STOP_ALARM));
            SetAlertHelper.cancelOrDeleteAlert(MyApplication.getContext(), deleteAlertItem.getMessageID(), it);
            SetAlertHelper.sendDeleteAlertSucceeded(AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID)
                    , deleteAlertItem.getToken()
                    , new ImplAsyncCallback("DeleteAlertSucceeded"));
            avsQueue.remove(key);
            checkQueue();
        } else if(current instanceof AvsAlertPlayItem){
            if (!audioPlayer.isPlaying()) {
                audioPlayer.playItem((AvsAlertPlayItem) current);
            }
        } else {
            avsQueue.remove(key);
            checkQueue();
            Log.w(TAG, "pop a unhandle item !" + current.getClass());
        }
    }

    private synchronized boolean processAvsItemImmediately(AvsItem current) {
//        TODO 后续还要添加其他处理
        if (current instanceof AvsSetVolumeItem) {
            //set our volume
            SpeakerUtil.setVolume(MyApplication.getContext()
                    , AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID)
                    , ((AvsSetVolumeItem) current).getVolume()
                    , false
                    , new ImplAsyncCallback("setVolume"));
        } else if (current instanceof AvsAdjustVolumeItem) {
            //adjust the volume
            SpeakerUtil.setVolume(MyApplication.getContext()
                    , AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID)
                    , ((AvsAdjustVolumeItem) current).getAdjustment()
                    , true
                    , new ImplAsyncCallback("AdjustVolume")
            );
        } else if (current instanceof AvsSetMuteItem) {
            //mute/unmute the device
            SpeakerUtil.setMute(MyApplication.getContext()
                    , AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID)
                    , ((AvsSetMuteItem) current).isMute()
                    , new ImplAsyncCallback("setMute")
            );
        } else if(current instanceof AvsUnableExecuteItem){
            AvsUnableExecuteItem item = (AvsUnableExecuteItem) current;
            AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID);
            alexaManager.sendEvent(Event
                            .createExceptionEncounteredEvent(ContextUtil.getContextList(MyApplication.getContext())
                                    , item.unparsedDirective
                                    , item.type
                                    , item.message)
                    , null);
        } else if(current instanceof AvsResetUserInactivityItem){
            AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID);
            alexaManager.resetUserInactivityTime();
        } else if(current instanceof AvsSetEndPointItem){
            avsQueue.clear();
            AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID);
            alexaManager.setEndPoint(((AvsSetEndPointItem) current).endPoint);
            Log.w(TAG, " handle clear queue ! AvsSetEndPointItem");
        } else if(current instanceof AvsStopCaptureItem){
//            AlexaManager alexaManager = AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID);
//            alexaManager.cancelAudioRequest(); //FIXME 不应该是cancel request，应该是停止录音，但是现在用的都是close talk，在client这边判断语音是否结束，因此不需要。
            avsQueue.remove(current.messageID);
            Log.w(TAG, "handle AvsStopCaptureItem");
        } else {
            return false;
        }
        Log.d(TAG, "processAvsItemImmediately type:" + current.getClass());
        return true;
    }

    private void setState(final int state) {

    }

    private String getFirstKey() {
        String out = null;
        for (String key : avsQueue.keySet()) {
            out = (key);
            break;
        }
        return out;
    }


    //Our callback that deals with removing played items in our media player and then checking to see if more items exist
    private AlexaAudioPlayer.Callback alexaAudioPlayerCallback = new AlexaAudioPlayer.Callback() {

        private boolean almostDoneFired = false; // FIXME 这两个标志貌似会无法复位，需要检查
        private boolean playbackStartedFired = false;

        @Override
        public void playerPrepared(AvsItem pendingItem) {
            almostDoneFired = false;
            playbackStartedFired = false;
        }

        @Override
        public void playerProgress(AvsItem item, long offsetInMilliseconds, float percent) {
            if (BuildConfig.DEBUG) {
                //Log.i(TAG, "Player percent: " + percent);
            }
            if (item instanceof AvsPlayContentItem || item == null) {
                return;
            }
            if (!playbackStartedFired) {
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "PlaybackStarted " + item.getToken() + " fired: " + percent);
                }
                playbackStartedFired = true;
                if(!(item instanceof AvsAlertPlayItem)) sendPlaybackStartedEvent(item);
            }
            if (!almostDoneFired && percent > .8f) {
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "AlmostDone " + item.getToken() + " fired: " + percent);
                }
                almostDoneFired = true;
                if (item instanceof AvsPlayAudioItem) {
                    sendPlaybackNearlyFinishedEvent((AvsPlayAudioItem) item, offsetInMilliseconds);
                }
            }
        }

        @Override
        public void itemComplete(AvsItem completedItem) {
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "Complete " + completedItem.getToken() + " fired");
            }

            almostDoneFired = false;
            playbackStartedFired = false;
            avsQueue.remove(completedItem.messageID);
            checkQueue();
            if (completedItem instanceof AvsPlayContentItem) {
                return;
            }

            if(!(completedItem instanceof AvsAlertPlayItem)) sendPlaybackFinishedEvent(completedItem);
        }

        @Override
        public boolean playerError(AvsItem item, int what, int extra) {
            itemComplete(item);
            return true;
        }

        @Override
        public void dataError(AvsItem item, Exception e) {
            e.printStackTrace();
        }


    };

    /**
     * Send an event back to Alexa that we're nearly done with our current playback event, this should supply us with the next item
     * https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/audioplayer#PlaybackNearlyFinished Event
     */
    private void sendPlaybackNearlyFinishedEvent(AvsPlayAudioItem item, long offsetInMilliseconds) {
        if (item != null) {
            AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID)
                    .sendPlaybackNearlyFinishedEvent(item, offsetInMilliseconds, new ImplAsyncCallback("PlaybackNearlyFinished"));
            Log.i(TAG, "Sending PlaybackNearlyFinishedEvent");
        }
    }

    /**
     * Send an event back to Alexa that we're starting a speech event
     * https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/audioplayer#PlaybackNearlyFinished Event
     */
    private void sendPlaybackStartedEvent(AvsItem item) {
        AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID).sendPlaybackStartedEvent(item, null);
        Log.i(TAG, "Sending SpeechStartedEvent");
    }

    /**
     * Send an event back to Alexa that we're done with our current speech event, this should supply us with the next item
     * https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/audioplayer#PlaybackNearlyFinished Event
     */
    private void sendPlaybackFinishedEvent(AvsItem item) {
        if (item != null) {
            AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID).sendPlaybackFinishedEvent(item, null);
            Log.i(TAG, "Sending PlaybackFinishedEvent");
        }
    }

    private void startListening(long waitMicMillseconds) {
        Intent it = new Intent(MyApplication.getContext(), BgProcessIntentService.class);
        BackGroundProcessServiceControlCommand cmd = new BackGroundProcessServiceControlCommand(1);
        cmd.waitMicDelayMillSecond = waitMicMillseconds;
        it.putExtra(BgProcessIntentService.EXTRA_CMD, cmd);
        MyApplication.getContext().startService(it);
    }

}
