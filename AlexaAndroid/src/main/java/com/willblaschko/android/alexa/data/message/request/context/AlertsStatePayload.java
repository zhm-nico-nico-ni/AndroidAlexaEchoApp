package com.willblaschko.android.alexa.data.message.request.context;

import com.willblaschko.android.alexa.data.message.Payload;
import com.willblaschko.android.alexa.interfaces.alerts.AvsSetAlertItem;

import java.util.List;

public final class AlertsStatePayload extends Payload {
    public List<AvsSetAlertItem> allAlerts;
    public List<AvsSetAlertItem> activeAlerts;
//    private final List<Alert> allAlerts;
//    private final List<Alert> activeAlerts;
//
//    public AlertsStatePayload(List<Alert> all, List<Alert> active) {
//        this.allAlerts = all;
//        this.activeAlerts = active;
//    }
//
//    public List<Alert> getAllAlerts() {
//        return allAlerts;
//    }
//
//    public List<Alert> getActiveAlerts() {
//        return activeAlerts;
//    }


}