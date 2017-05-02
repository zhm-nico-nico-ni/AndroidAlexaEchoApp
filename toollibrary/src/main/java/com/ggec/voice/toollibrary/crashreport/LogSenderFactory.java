package com.ggec.voice.toollibrary.crashreport;

import android.content.Context;

import org.acra.config.ACRAConfiguration;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderFactory;

/**
 * Created by Administrator on 2016/5/27.
 */
public class LogSenderFactory implements ReportSenderFactory {
    public static final String TAG = "LogSenderFactory";

    @Override
    public ReportSender create(Context context, ACRAConfiguration config) {
        return new LogSender(context.getApplicationContext());
    }
}