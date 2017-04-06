package com.willblaschko.android.alexa.data;

import com.google.gson.Gson;
import com.willblaschko.android.alexa.data.message.Payload;
import com.willblaschko.android.alexa.data.message.PayloadFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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


    public static class Header{

        String namespace;
        String name;
        String messageId;
        String dialogRequestId;

        public String getNamespace() {
            return namespace;
        }


        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public String getMessageId() {
            return messageId;
        }

        public String getDialogRequestId() {
            return dialogRequestId;
        }

        public void setDialogRequestId(String dialogRequestId) {
            this.dialogRequestId = dialogRequestId;
        }
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

    public static String getSpeechRecognizerEvent(){
        Builder builder = new Builder();
        builder.setHeaderNamespace("SpeechRecognizer")
                .setHeaderName("Recognize")
                .setHeaderMessageId(getUuid())
                .setHeaderDialogRequestId("dialogRequest-321")
                .setPayload(PayloadFactory
                        .createSpeechRecognizerPayload("AUDIO_L16_RATE_16000_CHANNELS_1"
                                ,"NEAR_FIELD"))
                ;
        return builder.toJson();
    }

    public static String getVolumeChangedEvent(long volume, boolean isMute){
        Builder builder = new Builder();
        builder.setHeaderNamespace("Speaker")
                .setHeaderName("VolumeChanged")
                .setHeaderMessageId(getUuid())
                .setPayload(PayloadFactory.createVolumeChangedPayload(isMute, volume))
                ;
        return builder.toJson();
    }
    public static String getMuteEvent(boolean isMute){
        Builder builder = new Builder();
        builder.setHeaderNamespace("Speaker")
                .setHeaderName("VolumeChanged")
                .setHeaderMessageId(getUuid())
                .setPayload(PayloadFactory.createSetMutePayload(isMute));
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


    public static String getPlaybackStartedEvent(String token){
        Builder builder = new Builder();
        builder.setHeaderNamespace("AudioPlayer")
                .setHeaderName("PlaybackStarted")
                .setHeaderMessageId(getUuid())
                .setPayload(PayloadFactory.createPayload(token));
        return builder.toJson();
    }

    public static String getPlaybackFinishedEvent(String token){
        Builder builder = new Builder();
        builder.setHeaderNamespace("AudioPlayer")
                .setHeaderName("PlaybackFinished")
                .setHeaderMessageId(getUuid())
                .setPayload(PayloadFactory.createPayload(token));
        return builder.toJson();
    }


    public static String getSynchronizeStateEvent(){
        Builder builder = new Builder();
        builder.setHeaderNamespace("System")
                .setHeaderName("SynchronizeState")
                .setHeaderMessageId(getUuid());
        return builder.toJson();
    }

    public static String createSystemSynchronizeStateEvent(){
        List<Event> list = new ArrayList<>();
        String token = "";

        Builder playbackEventBuilder = new Builder()
                .setHeaderNamespace("AudioPlayer")
                .setHeaderName("PlaybackState")
                .setPayload(PayloadFactory.createPlaybackStatePayload(token,0, "IDLE"))
                ;
        list.add(playbackEventBuilder.build().event);

        Builder speechSynthesizerEventBuilder = new Builder()
                .setHeaderNamespace("SpeechSynthesizer")
                .setHeaderName("SpeechState")
                .setPayload(PayloadFactory.createSpeechStatePayload(token,0, "FINISHED"))
               ;
        list.add(speechSynthesizerEventBuilder.build().event);

        Builder alertsEventBuilder = new Builder()
                .setHeaderNamespace("Alerts")
                .setHeaderName("AlertsState")
                .setPayload(PayloadFactory.createAlertsStatePayload());
        list.add(alertsEventBuilder.build().event);

        Builder speakerEventBuilder = new Builder()
                .setHeaderNamespace("Speaker")
                .setHeaderName("VolumeState")
                .setPayload(PayloadFactory.createVolumeStatePayload(50, false));
        list.add(speakerEventBuilder.build().event);

        Builder builder = new Builder();
        builder.setHeaderNamespace("System")
                .setHeaderName("SynchronizeState")
                .setHeaderMessageId(getUuid())
                .setContext(list)
        ;

        return builder.toJson();
    }

    private static String getUuid(){
        String uuid = UUID.randomUUID().toString();
        return uuid;
    }
}


