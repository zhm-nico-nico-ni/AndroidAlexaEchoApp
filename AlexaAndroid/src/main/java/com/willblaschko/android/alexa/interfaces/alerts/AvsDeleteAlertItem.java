package com.willblaschko.android.alexa.interfaces.alerts;

import com.willblaschko.android.alexa.interfaces.AvsItem;

public class AvsDeleteAlertItem extends AvsItem {

    public final String dialogRequestId;
    public AvsDeleteAlertItem(String token, String messageId, String dialogRequestId) {
        super(token, messageId);
        this.dialogRequestId = dialogRequestId;
    }

}
