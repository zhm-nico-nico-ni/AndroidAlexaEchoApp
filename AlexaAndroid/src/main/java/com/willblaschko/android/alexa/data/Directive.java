package com.willblaschko.android.alexa.data;

import android.text.TextUtils;

import com.willblaschko.android.alexa.data.message.directive.audio.ProgressReport;

/**
 * A catch-all Directive to classify return responses from the Amazon Alexa v20160207 API
 * Will handle calls to:
 * <a href="https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/speechrecognizer">Speech Recognizer</a>
 * <a href="https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/alerts">Alerts</a>
 * <a href="https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/audioplayer">Audio Player</a>
 * <a href="https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/playbackcontroller">Playback Controller</a>
 * <a href="https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/speaker">Speaker</a>
 * <a href="https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/speechsynthesizer">Speech Synthesizer</a>
 * <a href="https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/system">System</a>
 *
 *
 * @author wblaschko on 5/6/16.
 */
public class Directive {
    private Header header;
    private Payload payload;

    private static final String TYPE_MEDIA_PLAY = "PlayCommandIssued";
    private static final String TYPE_MEDIA_PAUSE = "PauseCommandIssued";
    private static final String TYPE_MEDIA_NEXT = "NextCommandIssued";
    private static final String TYPE_MEDIA_PREVIOUS = "PreviousCommandIssue";
    private static final String TYPE_EXCEPTION = "Exception";

    private static final String PLAY_BEHAVIOR_REPLACE_ALL = "REPLACE_ALL";
    private static final String PLAY_BEHAVIOR_ENQUEUE = "ENQUEUE";
    private static final String PLAY_BEHAVIOR_REPLACE_ENQUEUED = "REPLACE_ENQUEUED";

    //DIRECTIVE TYPES


    public boolean isTypeMediaPlay(){
        return TextUtils.equals(header.getName(), TYPE_MEDIA_PLAY);
    }

    public boolean isTypeMediaPause(){
        return TextUtils.equals(header.getName(), TYPE_MEDIA_PAUSE);
    }

    public boolean isTypeMediaNext(){
        return TextUtils.equals(header.getName(), TYPE_MEDIA_NEXT);
    }

    public boolean isTypeMediaPrevious(){
        return TextUtils.equals(header.getName(), TYPE_MEDIA_PREVIOUS);
    }

    public boolean isTypeException(){
        return TextUtils.equals(header.getName(), TYPE_EXCEPTION);
    }


    //PLAY BEHAVIORS

    public boolean isPlayBehaviorReplaceAll(){
        return TextUtils.equals(payload.getPlayBehavior(), PLAY_BEHAVIOR_REPLACE_ALL);
    }
//    public boolean isPlayBehaviorEnqueue(){ 默认就是这个，不需要专门处理
//        return TextUtils.equals(payload.getPlayBehavior(), PLAY_BEHAVIOR_ENQUEUE);
//    }
    public boolean isPlayBehaviorReplaceEnqueued(){
        return TextUtils.equals(payload.getPlayBehavior(), PLAY_BEHAVIOR_REPLACE_ENQUEUED);
    }

    public String getHeaderName() {
        return header.getName();
    }

    public String getHeaderNameSpace(){
        return header.getNamespace();
    }

    public String getHeaderMessageId() {
        return header.getMessageId();
    }

    public String getHeaderDialogRequestId() {
        return header.getDialogRequestId();
    }

    public Payload getPayload() {
        return payload;
    }

    public static class Payload{

        String initiator;
        String url;
        String format;
        String token;
        String type;
        String scheduledTime;
        String playBehavior;
        AudioItem audioItem;
        long volume;
        boolean mute;
        long timeoutInMilliseconds;
        String description;
        String code;
        String endpoint;
        public String clearBehavior;

        public String getUrl() {
            return url;
        }

        public String getFormat() {
            return format;
        }

        public String getToken() {
            if(token == null){
                //sometimes we need to return the stream tokens, not the top level tokens
                if(audioItem != null && audioItem.getStream() != null){
                    return audioItem.getStream().getToken();
                }
            }
            return token;
        }

        public String getType() {
            return type;
        }

        public String getScheduledTime() {
            return scheduledTime;
        }

        public String getPlayBehavior() {
            return playBehavior;
        }

        public AudioItem getAudioItem() {
            return audioItem;
        }

        public long getVolume() {
            return volume;
        }

        public boolean isMute(){
            return mute;
        }

        public long getTimeoutInMilliseconds(){ return timeoutInMilliseconds; }

        public String getDescription() {
            return description;
        }

        public String getCode() {
            return code;
        }

        public String getEndpoint(){
            return endpoint;
        }

        public String getInitiator() {return initiator;}
    }

    public static class AudioItem{
        String audioItemId;
        Stream stream;

        public String getAudioItemId() {
            return audioItemId;
        }

        public Stream getStream() {
            return stream;
        }
    }
    public static class Stream{
        String url;
        String streamFormat;
        long offsetInMilliseconds;
        String expiryTime;
        String token;
        String expectedPreviousToken;
        public ProgressReport progressReport;

        public String getUrl() {
            return url;
        }

        public String getStreamFormat() {
            return streamFormat;
        }

        public long getOffsetInMilliseconds() {
            return offsetInMilliseconds;
        }

        public String getExpiryTime() {
            return expiryTime;
        }

        public String getToken() {
            return token;
        }

        public String getExpectedPreviousToken() {
            return expectedPreviousToken;
        }
    }



    public static class DirectiveWrapper{
        Directive directive;
        public Directive getDirective(){
            return directive;
        }
    }
}
