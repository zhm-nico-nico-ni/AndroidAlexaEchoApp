package com.willblaschko.android.alexa.interfaces.alerts;

import com.willblaschko.android.alexa.interfaces.AvsItem;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * An AVS Item to handle setting alerts on the device
 * <p>
 * {@link com.willblaschko.android.alexa.data.Directive} response item type parsed so we can properly
 * deal with the incoming commands from the Alexa server.
 */
public class AvsSetAlertItem extends AvsItem {
    private String type;
    private String scheduledTime;
    private transient int timeId;

    public static final String TIMER = "TIMER";
    public static final String ALARM = "ALARM";

    /**
     * Create a new AVSItem directive for an alert
     *
     * @param token         the alert identifier
     * @param type          the alert type
     * @param scheduledTime the alert time
     */
    public AvsSetAlertItem(String token, String type, String scheduledTime, String messageId) {
        super(token, messageId);
        this.type = type;
        this.scheduledTime = scheduledTime;
        timeId = (int) (System.currentTimeMillis() & 0xFFFFFFFFL);
    }

    public String getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(String scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isTimer() {
        return type.equals(TIMER);
    }

    public boolean isAlarm() {
        return type.equals(ALARM);
    }

    public String getMessageId() {
        return messageID;
    }

    public int getTimeId(){
        return timeId;
    }

    public String toJsonString(){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("type", type);
            jsonObject.put("scheduledTime", scheduledTime);
            jsonObject.put("token", getToken());
            jsonObject.put("messageId", messageID);
            jsonObject.put("timeId", timeId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }

    public static AvsSetAlertItem create(String json){
        try {
            JSONObject jsonObject = new JSONObject(json);
            AvsSetAlertItem item = new AvsSetAlertItem(
                    jsonObject.getString("token")
                    ,jsonObject.getString("type")
                    ,jsonObject.getString("scheduledTime")
                    ,jsonObject.getString("messageId"));
            item.timeId = jsonObject.getInt("timeId");

            return item;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

    }
}
