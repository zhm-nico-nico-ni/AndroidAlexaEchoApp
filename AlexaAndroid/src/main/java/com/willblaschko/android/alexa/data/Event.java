package com.willblaschko.android.alexa.data;

import com.google.gson.Gson;
import com.willblaschko.android.alexa.AVSAPIConstants;
import com.willblaschko.android.alexa.data.message.Payload;
import com.willblaschko.android.alexa.data.message.PayloadFactory;
import com.willblaschko.android.alexa.data.message.request.audioplayer.PlaybackError;

import java.util.ArrayList;
import java.util.List;

import static com.willblaschko.android.alexa.utility.Util.getUuid;

/**
 * A catch-all Event to classify return responses from the Amazon Alexa v20160207 API
 * Will handle calls to:
 * <a href="https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/speechrecognizer">Speech Recognizer</a>
 * <a href="https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/alerts">Alerts</a>
 * <a href="https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/audioplayer">Audio Player</a>
 * <a href="https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/playbackcontroller">Playback Controller</a>
 * <a href="https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/speaker">Speaker</a>
 * <a href="https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/speechsynthesizer">Speech Synthesizer</a>
 * <a href="https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/system">System</a>
 *
 * @author wblaschko on 5/6/16.
 */
public class Event {

    Header header;
    Payload payload;

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public Payload getPayload() {
        return  payload;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }

    public static class EventWrapper{
        Event event;
        List<Event> context;

        public Event getEvent() {
            return event;
        }

        public List<Event> getContext() {
            return context;
        }

        public String toJson(){
            return new Gson().toJson(this)+"\n";
        }
    }

    public static class Builder{
        Event event = new Event();
        Payload payload = new Payload();
        Header header = new Header();
        List<Event> context = new ArrayList<>();

        public Builder(){
            event.setPayload(payload);
            event.setHeader(header);
        }

        public EventWrapper build(){
            EventWrapper wrapper = new EventWrapper();
            wrapper.event = event;

            if (context != null && !context.isEmpty() && !(context.size() == 1 && context.get(0) == null)) {
                wrapper.context = context;
            }

            return wrapper;
        }

        public String toJson(){
            return build().toJson();
        }

        public Builder setContext(List<Event> context) {
            if (context == null) {
                return this;
            }
            this.context = context;
            return this;
        }

        public Builder setHeaderNamespace(String namespace){
            header.namespace = namespace;
            return this;
        }

        public Builder setHeaderName(String name){
            header.name = name;
            return this;
        }

        public Builder setHeaderMessageId(String messageId){
            header.messageId = messageId;
            return this;
        }

        public Builder setHeaderDialogRequestId(String dialogRequestId){
            header.dialogRequestId = dialogRequestId;
            return this;
        }

        public Builder setPayload(Payload payload){
            this.payload = payload;
            event.setPayload(payload);
            return this;
        }

    }

    public static String getSpeechRecognizerEvent(String profile, List<Event> events){
        Builder builder = new Builder();
        builder.setHeaderNamespace("SpeechRecognizer")
                .setHeaderName("Recognize")
                .setHeaderMessageId(getUuid())
                .setHeaderDialogRequestId(getUuid())
                .setPayload(PayloadFactory
                        .createSpeechRecognizerPayload(profile, //"CLOSE_TALK", "NEAR_FIELD", "FAR_FIELD"
                                "AUDIO_L16_RATE_16000_CHANNELS_1"))
                .setContext(events)
                ;
        return builder.toJson();
    }

    /** https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/speaker#volumechanged
     *   The VolumeChanged event must be sent to AVS when:
     *  1) A SetVolume or AdjustVolume directive is received and processed to indicate that the speaker volume on your product has been adjusted/changed.
     *  2) Volume is locally adjusted to indicate that the speaker volume on your product has been adjusted/changed.
     * @param volume	The absolute volume level scaled from 0 (min) to 100 (max).
     *                  Accepted values: Any long value between 0 and 100	long
     * @param isMute A boolean value is used to mute/unmute a product's speaker. The value is TRUE when the speaker is muted,
     *               and FALSE when unmuted.
     *               Accepted values: TRUE or FALSE
     * @return
     */
    public static String getVolumeChangedEvent(long volume, boolean isMute){
        if(volume<0 || volume>100) throw new IllegalArgumentException("Any long value must between 0 and 100 ,current:"+volume);
        Builder builder = new Builder();
        builder.setHeaderNamespace("Speaker")
                .setHeaderName(AVSAPIConstants.Speaker.Events.VolumeChanged.NAME)
                .setHeaderMessageId(getUuid())
                .setPayload(PayloadFactory.createVolumeChangedPayload(isMute, volume))
                ;
        return builder.toJson();
    }

    /** https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/speaker#mutechanged
     * The MuteChanged event must be sent to AVS when:
     * 1) A SetMute directive is received and processed to indicate that the mute status of the product’s speaker has changed.
     * 2) Your product is muted/unmuted locally to indicate that the mute status of the product’s speaker has changed.
     * @param isMuted
     * @return
     */
    public static String getMuteChangeEvent(boolean isMuted, long volume){
        Builder builder = new Builder();
        builder.setHeaderNamespace("Speaker")
                .setHeaderName(AVSAPIConstants.Speaker.Events.MuteChanged.NAME)
                .setHeaderMessageId(getUuid())
                .setPayload(PayloadFactory.createSetMutePayload(isMuted, volume));
        return builder.toJson();
    }

    public static String getExpectSpeechTimedOutEvent(){
        Builder builder = new Builder();
        builder.setHeaderNamespace("SpeechRecognizer")
                .setHeaderName("ExpectSpeechTimedOut")
                .setHeaderMessageId(getUuid());
        return builder.toJson();
    }

    public static String getSpeechNearlyFinishedEvent(String token, long offsetInMilliseconds){
        Builder builder = new Builder();
        builder.setHeaderNamespace("SpeechSynthesizer")
                .setHeaderName("PlaybackNearlyFinished")
                .setHeaderMessageId(getUuid())
                .setPayload(PayloadFactory
                        .createAudioPlayerPayload(token, offsetInMilliseconds))
                ;
        return builder.toJson();
    }

    public static String getPlaybackNearlyFinishedEvent(String token, long offsetInMilliseconds){
        Builder builder = new Builder();
        builder.setHeaderNamespace("AudioPlayer")
                .setHeaderName("PlaybackNearlyFinished")
                .setHeaderMessageId(getUuid())
                .setPayload(PayloadFactory
                        .createAudioPlayerPayload(token, offsetInMilliseconds))
        ;

        return builder.toJson();
    }

    public static String getSetAlertSucceededEvent(String token) {
        return getAlertEvent(token, "SetAlertSucceeded");
    }

    public static String getSetAlertFailedEvent(String token) {
        return getAlertEvent(token, "SetAlertFailed");
    }

    public static String getDeleteAlertSucceededEvent(String token) {
        return getAlertEvent(token, "DeleteAlertSucceeded");
    }

    public static String getDeleteAlertFailedEvent(String token) {
        return getAlertEvent(token, "DeleteAlertFailed");
    }

    public static String getAlertStartedEvent(String token) {
        return getAlertEvent(token, "AlertStarted");
    }

    public static String getAlertStoppedEvent(String token) {
        return getAlertEvent(token, "AlertStopped");
    }

    public static String getAlertEnteredForegroundEvent(String token) {
        return getAlertEvent(token, "AlertEnteredForeground");
    }

    public static String getAlertEnteredBackgroundEvent(String token) {
        return getAlertEvent(token, "AlertEnteredBackground");
    }

    private static String getAlertEvent(String token, String type) {
        Builder builder = new Builder();
        builder.setHeaderNamespace("Alerts")
                .setHeaderName(type)
                .setHeaderMessageId(getUuid())
                .setPayload(PayloadFactory.createPayload(token));
        return builder.toJson();
    }

    public static String getSpeechStartedEvent(String token){
        Builder builder = new Builder();
        builder.setHeaderNamespace("SpeechSynthesizer")
                .setHeaderName("SpeechStarted")
                .setHeaderMessageId(getUuid())
                .setPayload(PayloadFactory.createPayload(token));
        return builder.toJson();
    }

    public static String getSpeechFinishedEvent(String token){
        Builder builder = new Builder();
        builder.setHeaderNamespace("SpeechSynthesizer")
                .setHeaderName("SpeechFinished")
                .setHeaderMessageId(getUuid())
                .setPayload(PayloadFactory.createPayload(token));
        return builder.toJson();
    }


    public static String getPlaybackStartedEvent(String directiveToken, long offset){
        Builder builder = new Builder();
        builder.setHeaderNamespace("AudioPlayer")
                .setHeaderName("PlaybackStarted")
                .setHeaderMessageId(getUuid())
                .setPayload(PayloadFactory.createAudioPlayerPayload(directiveToken, offset));
        return builder.toJson();
    }

    public static String getPlaybackStoppedEvent(String directiveToken, long offset){
        Builder builder = new Builder();
        builder.setHeaderNamespace(AVSAPIConstants.AudioPlayer.NAMESPACE)
                .setHeaderName(AVSAPIConstants.AudioPlayer.Events.PlaybackStopped.NAME)
                .setHeaderMessageId(getUuid())
                .setPayload(PayloadFactory.createAudioPlayerPayload(directiveToken, offset));
        return builder.toJson();
    }

    /**
     * https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/audioplayer#playbackfinished
     * This event is not sent when:
     * Playback is stopped (either locally or as the result of a Stop directive)
     * Navigating between streams (next/previous)
     *
     * For each URL that AVS sends,
     * it expects no more than one PlaybackFinished event.
     * If you receive a playlist URL (composed of multiple URLs) only send one PlaybackFinished event
     *
     * @param directiveToken
     * @param offset
     * @return
     */
    public static String getPlaybackFinishedEvent(String directiveToken, long offset){
        Builder builder = new Builder();
        builder.setHeaderNamespace(AVSAPIConstants.AudioPlayer.NAMESPACE)
                .setHeaderName(AVSAPIConstants.AudioPlayer.Events.PlaybackFinished.NAME)
                .setHeaderMessageId(getUuid())
                .setPayload(PayloadFactory.createAudioPlayerPayload(directiveToken, offset));
        return builder.toJson();
    }

    public static String getPlaybackFailEvent(String directiveToken, long offset, String playerActivity, PlaybackError error){
        Builder builder = new Builder();
        builder.setHeaderNamespace(AVSAPIConstants.AudioPlayer.NAMESPACE)
                .setHeaderName(AVSAPIConstants.AudioPlayer.Events.PlaybackFailed.NAME)
                .setHeaderMessageId(getUuid())
                .setPayload(PayloadFactory.createPlaybackFailPayload(directiveToken, offset, playerActivity, error));
        return builder.toJson();
    }

    public static String getPlaybackStutterStartedEvent(String directiveToken, long offset){
        Builder builder = new Builder();
        builder.setHeaderNamespace(AVSAPIConstants.AudioPlayer.NAMESPACE)
                .setHeaderName(AVSAPIConstants.AudioPlayer.Events.PlaybackStutterStarted.NAME)
                .setHeaderMessageId(getUuid())
                .setPayload(PayloadFactory.createAudioPlayerPayload(directiveToken, offset));
        return builder.toJson();
    }
    public static String getPlaybackStutterFinishEvent(String directiveToken, long offset, long stutterDurationInMilliseconds){
        Builder builder = new Builder();
        builder.setHeaderNamespace(AVSAPIConstants.AudioPlayer.NAMESPACE)
                .setHeaderName(AVSAPIConstants.AudioPlayer.Events.PlaybackStutterFinished.NAME)
                .setHeaderMessageId(getUuid())
                .setPayload(PayloadFactory.createPlaybackStutterFinishedPayload(directiveToken, offset, stutterDurationInMilliseconds));
        return builder.toJson();
    }

    public static String getPlaybackQueueClearedEvent(){
        Builder builder = new Builder();
        builder.setHeaderNamespace(AVSAPIConstants.AudioPlayer.NAMESPACE)
                .setHeaderName(AVSAPIConstants.AudioPlayer.Events.PlaybackQueueCleared.NAME)
                .setHeaderMessageId(getUuid());
        return builder.toJson();
    }

    public static String getPlaybackResumedEvent(String directiveToken, long offset){
        Builder builder = new Builder();
        builder.setHeaderNamespace(AVSAPIConstants.AudioPlayer.NAMESPACE)
                .setHeaderName(AVSAPIConstants.AudioPlayer.Events.PlaybackResumed.NAME)
                .setHeaderMessageId(getUuid())
                .setPayload(PayloadFactory.createAudioPlayerPayload(directiveToken, offset));
        return builder.toJson();
    }

    public static String getPlaybackPausedEvent(String directiveToken, long offset){
        Builder builder = new Builder();
        builder.setHeaderNamespace(AVSAPIConstants.AudioPlayer.NAMESPACE)
                .setHeaderName(AVSAPIConstants.AudioPlayer.Events.PlaybackPaused.NAME)
                .setHeaderMessageId(getUuid())
                .setPayload(PayloadFactory.createAudioPlayerPayload(directiveToken, offset));
        return builder.toJson();
    }

    public static String createSystemSynchronizeStateEvent(List<Event> contextList){

        Event.Builder builder = new Event.Builder();
        builder.setHeaderNamespace("System")
                .setHeaderName("SynchronizeState")
                .setHeaderMessageId(getUuid())
                .setContext(contextList);

        return builder.toJson();
    }

    public static String createExceptionEncounteredEvent(List<Event> contextList, String unparsedDirective, String type, String msg){

        Event.Builder builder = new Event.Builder();
        builder.setHeaderNamespace("System")
                .setHeaderName(AVSAPIConstants.System.Events.ExceptionEncountered.NAME)
                .setHeaderMessageId(getUuid())
                .setContext(contextList)
                .setPayload(PayloadFactory.createExceptionEncounteredPayload(unparsedDirective, type, msg));

        return builder.toJson();
    }

    /**
     *
     * @param inactiveTimeInSeconds 单位是秒 ！！！
     * @return
     */
    public static String createUserInactivityReportEvent(long inactiveTimeInSeconds){ // TODO send this per hour
        Event.Builder builder = new Event.Builder();
        builder.setHeaderNamespace(AVSAPIConstants.System.NAMESPACE)
                .setHeaderName(AVSAPIConstants.System.Events.UserInactivityReport.NAME)
                .setHeaderMessageId(getUuid())
                .setPayload(PayloadFactory.createUserInactivityReportPayload(inactiveTimeInSeconds));

        return builder.toJson();
    }
}


