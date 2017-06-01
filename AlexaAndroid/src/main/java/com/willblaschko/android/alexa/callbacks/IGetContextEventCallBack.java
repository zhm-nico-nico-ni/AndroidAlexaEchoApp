package com.willblaschko.android.alexa.callbacks;

import com.willblaschko.android.alexa.data.Event;
import com.willblaschko.android.alexa.data.message.request.speechrecognizer.Initiator;

import java.util.List;

/**
 * Created by ggec on 2017/4/28.
 */

public interface IGetContextEventCallBack {
    List<Event> getContextEvent();

    Initiator getInitiator();
}
