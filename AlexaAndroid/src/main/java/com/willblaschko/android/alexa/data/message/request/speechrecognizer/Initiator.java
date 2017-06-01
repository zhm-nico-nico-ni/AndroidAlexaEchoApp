package com.willblaschko.android.alexa.data.message.request.speechrecognizer;


import com.google.gson.Gson;

/**
 * {@link "https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/streaming-requirements-for-cloud-based-wake-word-verification"}
 * "initiator": {
 *      "type": "{{STRING}}",
 *      "payload": {
 *          "wakeWordIndices": {
 *              "startIndexInSamples": {{LONG}},
 *              "endIndexInSamples": {{LONG}}
 *          }
 *      }
 * }
 */

public class Initiator {
    String type;
    Payload payload;

    public Initiator(String type, long startIndexInSamples, long endIndexInSamples){
        this.type = type;

        payload = new Payload();
        payload.wakeWordIndices = new WakeWordIndices();
        payload.wakeWordIndices.startIndexInSamples = startIndexInSamples;
        payload.wakeWordIndices.endIndexInSamples = endIndexInSamples;
    }

    public long getEndIndexInSamples(){
        return payload.wakeWordIndices.endIndexInSamples;
    }

    private class Payload{
        WakeWordIndices wakeWordIndices;
    }

    private class WakeWordIndices{
        public long startIndexInSamples;
        public long endIndexInSamples;
    }

    public String toJson(){
        return new Gson().toJson(this);
    }

}
