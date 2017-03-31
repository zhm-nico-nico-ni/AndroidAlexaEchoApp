package com.ggec.voice.assistservice;

import android.util.Log;

import com.willblaschko.android.alexa.AlexaManager;
import com.willblaschko.android.alexa.audioplayer.AlexaAudioPlayer;
import com.willblaschko.android.alexa.callbacks.AsyncCallback;
import com.willblaschko.android.alexa.interfaces.AvsItem;
import com.willblaschko.android.alexa.interfaces.AvsResponse;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsPlayAudioItem;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsPlayContentItem;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsPlayRemoteItem;
import com.willblaschko.android.alexa.interfaces.errors.AvsResponseException;
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
import com.willblaschko.android.alexa.interfaces.speechrecognizer.AvsExpectSpeechItem;
import com.willblaschko.android.alexa.interfaces.speechsynthesizer.AvsSpeakItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ggec on 2017/3/31.
 */

public class AvsHandleHelper {
    private static final String TAG = "com.ggec.voice.assistservice.AvsHandleHelper";
    private static AvsHandleHelper sAvsHandleHelper;
    private List<AvsItem> avsQueue = new ArrayList<>();
    private AlexaAudioPlayer audioPlayer;

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
    public void handleResponse(AvsResponse response) {
        boolean checkAfter = (avsQueue.size() == 0);
        if (response != null) {
            //if we have a clear queue item in the list, we need to clear the current queue before proceeding
            //iterate backwards to avoid changing our array positions and getting all the nasty errors that come
            //from doing that
            for (int i = response.size() - 1; i >= 0; i--) {
                if (response.get(i) instanceof AvsReplaceAllItem || response.get(i) instanceof AvsReplaceEnqueuedItem) {
                    //clear our queue
                    avsQueue.clear();
                    //remove item
                    response.remove(i);
                }
            }
            Log.i(TAG, "Adding " + response.size() + " items to our queue");
            if (BuildConfig.DEBUG) {
                for (int i = 0; i < response.size(); i++) {
                    Log.i(TAG, "\tAdding: " + response.get(i).getToken());
                }
            }
            avsQueue.addAll(response);
        }
        if (checkAfter) {
            checkQueue();
        }
    }

    /**
     * Check our current queue of items, and if we have more to parse (once we've reached a play or listen callback) then proceed to the
     * next item in our list.
     * <p>
     * We're handling the AvsReplaceAllItem in handleResponse() because it needs to clear everything currently in the queue, before
     * the new items are added to the list, it should have no function here.
     */
    private void checkQueue() {

        //if we're out of things, hang up the phone and move on
        if (avsQueue.size() == 0) {
//            setState(STATE_FINISHED);
            return;
        }

        final AvsItem current = avsQueue.get(0);

        Log.i(TAG, "Item type " + current.getClass().getName());

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
            avsQueue.remove(current);
        } else if (current instanceof AvsReplaceAllItem) {
            //clear all items
            //mAvsItemQueue.clear();
            audioPlayer.stop();
            avsQueue.remove(current);
        } else if (current instanceof AvsReplaceEnqueuedItem) {
            //clear all items
            //mAvsItemQueue.clear();
            avsQueue.remove(current);
        } else if (current instanceof AvsExpectSpeechItem) {

            //listen for user input
            audioPlayer.stop();
            avsQueue.clear();
//            startListening();
        } else if (current instanceof AvsSetVolumeItem) {
            //set our volume
//            setVolume(((AvsSetVolumeItem) current).getVolume());
            avsQueue.remove(current);
        } else if (current instanceof AvsAdjustVolumeItem) {
            //adjust the volume
//            adjustVolume(((AvsAdjustVolumeItem) current).getAdjustment());
            avsQueue.remove(current);
        } else if (current instanceof AvsSetMuteItem) {
            //mute/unmute the device
//            setMute(((AvsSetMuteItem) current).isMute());
            avsQueue.remove(current);
        } else if (current instanceof AvsMediaPlayCommandItem) {
            //fake a hardware "play" press
//            sendMediaButton(this, KeyEvent.KEYCODE_MEDIA_PLAY);
            Log.i(TAG, "Media play command issued");
            avsQueue.remove(current);
        } else if (current instanceof AvsMediaPauseCommandItem) {
            //fake a hardware "pause" press
//            sendMediaButton(this, KeyEvent.KEYCODE_MEDIA_PAUSE);
            Log.i(TAG, "Media pause command issued");
            avsQueue.remove(current);
        } else if (current instanceof AvsMediaNextCommandItem) {
            //fake a hardware "next" press
//            sendMediaButton(this, KeyEvent.KEYCODE_MEDIA_NEXT);
            Log.i(TAG, "Media next command issued");
            avsQueue.remove(current);
        } else if (current instanceof AvsMediaPreviousCommandItem) {
            //fake a hardware "previous" press
//            sendMediaButton(this, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
            Log.i(TAG, "Media previous command issued");
            avsQueue.remove(current);
        } else if (current instanceof AvsResponseException) {
            AvsResponseException exception = (AvsResponseException) current;
            Log.e(TAG, "AvsResponseException -> " + exception.getDirective().getPayload().getCode()
                    + ": " + exception.getDirective().getPayload().getDescription());

            avsQueue.remove(current);
            checkQueue();
        }
    }

    private void setState(final int state) {

    }


    //Our callback that deals with removing played items in our media player and then checking to see if more items exist
    private AlexaAudioPlayer.Callback alexaAudioPlayerCallback = new AlexaAudioPlayer.Callback() {

        private boolean almostDoneFired = false;
        private boolean playbackStartedFired = false;

        @Override
        public void playerPrepared(AvsItem pendingItem) {

        }

        @Override
        public void playerProgress(AvsItem item, long offsetInMilliseconds, float percent) {
            if(BuildConfig.DEBUG) {
                //Log.i(TAG, "Player percent: " + percent);
            }
            if(item instanceof AvsPlayContentItem || item == null){
                return;
            }
            if(!playbackStartedFired){
                if(BuildConfig.DEBUG) {
                    Log.i(TAG, "PlaybackStarted " + item.getToken() + " fired: " + percent);
                }
                playbackStartedFired = true;
                sendPlaybackStartedEvent(item);
            }
            if(!almostDoneFired && percent > .8f){
                if(BuildConfig.DEBUG) {
                    Log.i(TAG, "AlmostDone " + item.getToken() + " fired: " + percent);
                }
                almostDoneFired = true;
                if(item instanceof AvsPlayAudioItem) {
                    sendPlaybackNearlyFinishedEvent((AvsPlayAudioItem) item, offsetInMilliseconds);
                }
            }
        }

        @Override
        public void itemComplete(AvsItem completedItem) {
            almostDoneFired = false;
            playbackStartedFired = false;
            avsQueue.remove(completedItem);
            checkQueue();
            if(completedItem instanceof AvsPlayContentItem || completedItem == null){
                return;
            }
            if(BuildConfig.DEBUG) {
                Log.i(TAG, "Complete " + completedItem.getToken() + " fired");
            }
            sendPlaybackFinishedEvent(completedItem);
        }

        @Override
        public boolean playerError(AvsItem item, int what, int extra) {
            return false;
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
    private void sendPlaybackNearlyFinishedEvent(AvsPlayAudioItem item, long offsetInMilliseconds){
        if (item != null) {
            AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID)
                    .sendPlaybackNearlyFinishedEvent(item, offsetInMilliseconds, requestCallback);
            Log.i(TAG, "Sending PlaybackNearlyFinishedEvent");
        }
    }

    /**
     * Send an event back to Alexa that we're starting a speech event
     * https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/audioplayer#PlaybackNearlyFinished Event
     */
    private void sendPlaybackStartedEvent(AvsItem item){
        AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID).sendPlaybackStartedEvent(item, null);
        Log.i(TAG, "Sending SpeechStartedEvent");
    }

    /**
     * Send an event back to Alexa that we're done with our current speech event, this should supply us with the next item
     * https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/audioplayer#PlaybackNearlyFinished Event
     */
    private void sendPlaybackFinishedEvent(AvsItem item){
        if (item != null) {
            AlexaManager.getInstance(MyApplication.getContext(), BuildConfig.PRODUCT_ID).sendPlaybackFinishedEvent(item, null);
            Log.i(TAG, "Sending PlaybackFinishedEvent");
        }
    }

    private AsyncCallback<AvsResponse, Exception> requestCallback = new AsyncCallback<AvsResponse, Exception>() {
        public long startTime;

        @Override
        public void start() {
            startTime = System.currentTimeMillis();
            com.ggec.voice.assistservice.log.Log.i(TAG, "Event Start");
//                setState(STATE_PROCESSING);
        }

        @Override
        public void success(AvsResponse result) {
            com.ggec.voice.assistservice.log.Log.i(TAG, "Event Success " + result);
                handleResponse(result);
        }

        @Override
        public void failure(Exception error) {
            com.ggec.voice.assistservice.log.Log.e(TAG, "Event Error", error);
//                setState(STATE_FINISHED);
        }

        @Override
        public void complete() {
            long totalTime = System.currentTimeMillis() - startTime;
            com.ggec.voice.assistservice.log.Log.i(TAG, "Event Complete, " +"Total request time: "+totalTime+" miliseconds");
        }
    };
}
