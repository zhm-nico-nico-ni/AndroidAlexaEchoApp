package com.ggec.voice.assistservice;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.facebook.stetho.Stetho;
import com.ggec.voice.assistservice.data.BackGroundProcessServiceControlCommand;
import com.ggec.voice.toollibrary.Util;
import com.ggec.voice.toollibrary.crashreport.LogSenderFactory;
import com.ggec.voice.toollibrary.log.DebugFileLogger;
import com.ggec.voice.toollibrary.log.Log;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.config.ACRAConfiguration;
import org.acra.config.ACRAConfigurationException;
import org.acra.config.ConfigurationBuilder;
import org.acra.sender.ReportSenderFactory;

import java.io.File;

import static org.acra.ReportField.ANDROID_VERSION;
import static org.acra.ReportField.APP_VERSION_CODE;
import static org.acra.ReportField.APP_VERSION_NAME;
import static org.acra.ReportField.AVAILABLE_MEM_SIZE;
import static org.acra.ReportField.BRAND;
import static org.acra.ReportField.BUILD;
import static org.acra.ReportField.BUILD_CONFIG;
import static org.acra.ReportField.CUSTOM_DATA;
import static org.acra.ReportField.FILE_PATH;
import static org.acra.ReportField.INITIAL_CONFIGURATION;
import static org.acra.ReportField.INSTALLATION_ID;
import static org.acra.ReportField.IS_SILENT;
import static org.acra.ReportField.LOGCAT;
import static org.acra.ReportField.PACKAGE_NAME;
import static org.acra.ReportField.PHONE_MODEL;
import static org.acra.ReportField.PRODUCT;
import static org.acra.ReportField.REPORT_ID;
import static org.acra.ReportField.SHARED_PREFERENCES;
import static org.acra.ReportField.STACK_TRACE;
import static org.acra.ReportField.TOTAL_MEM_SIZE;
import static org.acra.ReportField.USER_APP_START_DATE;
import static org.acra.ReportField.USER_CRASH_DATE;

/**
 * Created by ggec on 2017/3/29.
 */

public class MyApplication extends Application {

    private static Context sContext;

    @Override
    public void onCreate() {
        super.onCreate();
        final String processName = Util.getMyProcessName(this);
        boolean sIsUIProcess = Util.isUIProcess(processName);

        sContext = this;
//        AppResCopy.copyResFromAssetsToSD(this);
        Stetho.initializeWithDefaults(this);
        MultiDex.install(this);
        startService(BackGroundProcessServiceControlCommand.createIntentByType(this, BackGroundProcessServiceControlCommand.NETWORK_CONNECT));
        startService(BackGroundProcessServiceControlCommand.createIntentByType(this, BackGroundProcessServiceControlCommand.USER_INACTIVITY_REPORT));


        //Log开关
        if (!BuildConfig.DEBUG) {
            Log.LOG_LEVEL = (android.util.Log.WARN);
        } else {
            Log.LOG_LEVEL = (android.util.Log.VERBOSE);
        }

        String tag = sIsUIProcess ? "ui" : "service";

        if (BuildConfig.DEBUG) {
            File logDir = getExternalFilesDir("log");
//            File logDir = new File(Environment.getExternalStorageDirectory(), getPackageName() + "/log/");
            DebugFileLogger.initialize(logDir, tag);
            Log.i("MyApplication", "#### app client ver:" + Util.getPackageVersionName(this) + "-" + Util.getPackageVersionCode(this));
        }

        initACRA();
    }

    public static Context getContext() {
        return sContext;
    }

    private void initACRA() {
        final Class<?>[] list = new Class<?>[]{LogSenderFactory.class};
        final Class<? extends ReportSenderFactory>[] myReportSenderFactoryClasses = (Class<? extends ReportSenderFactory>[]) list;

        ReportField[] reportFields = new ReportField[]{
                REPORT_ID, APP_VERSION_CODE, APP_VERSION_NAME,
                PACKAGE_NAME, FILE_PATH, PHONE_MODEL, BRAND, PRODUCT, ANDROID_VERSION, BUILD, TOTAL_MEM_SIZE,
                AVAILABLE_MEM_SIZE, BUILD_CONFIG, IS_SILENT, STACK_TRACE, INITIAL_CONFIGURATION,
                USER_APP_START_DATE, USER_CRASH_DATE, LOGCAT, INSTALLATION_ID, SHARED_PREFERENCES,
                CUSTOM_DATA
        };

        // Create an ConfigurationBuilder. It is prepopulated with values specified via annotation.
        // Set the ReportSenderfactories on it and create an ACRAConfiguration

        try {
            ACRAConfiguration cfg = new ConfigurationBuilder(this)
                    .setReportSenderFactoryClasses(myReportSenderFactoryClasses)
                    .setCustomReportContent(reportFields)
                    .setBuildConfigClass(BuildConfig.class)
                    .setAdditionalSharedPreferences(new String[]{"app_status", "userinfo"})
                    .setLogcatArguments(new String[]{"-t", "100", "-v", "time"})
                    .build();
            ACRA.init(this, cfg);
        } catch (ACRAConfigurationException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
