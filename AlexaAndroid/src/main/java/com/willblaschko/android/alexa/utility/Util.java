package com.willblaschko.android.alexa.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.Extractor;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.willblaschko.android.alexa.datepersisted.MultiprocessSharedPreferences;
import com.willblaschko.android.alexa.interfaces.response.MyDataSource;

import java.util.UUID;

/**
 * A collection of utility functions.
 *
 * @author wblaschko on 8/13/15.
 */
public class Util {
    private static SharedPreferences mPreferences;

    /**
     * Show an authorization toast on the main thread to make sure the user sees it
     * @param context local context
     * @param message the message to show the user
     */
    public static void showAuthToast(final Context context, final String message){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast authToast = Toast.makeText(context, message, Toast.LENGTH_LONG);
                authToast.show();
            }
        });
    }


    /**
     * Get our default shared preferences
     * @param context local/application context
     * @return default shared preferences
     */
    public static SharedPreferences getPreferences(Context context) {
        if (mPreferences == null) {
            mPreferences = MultiprocessSharedPreferences.getSharedPreferences(context, "PREF_DATA", Context.MODE_PRIVATE);
        }
        return mPreferences;
    }

    public static String getUuid(){
        return UUID.randomUUID().toString();
    }

    public static MediaSource buildMediaSource(Context mContext, Uri uri, String overrideExtension) {
        if ("cid".equals(uri.getScheme())){
            return new ExtractorMediaSource(uri,
                    new DefaultDataSourceFactory(mContext, null, MyDataSource.FACTORY)
                    , new ExtractorsFactory() {
                public Extractor[] createExtractors() {
                    return new Extractor[]{new Mp3Extractor(Mp3Extractor.FLAG_DISABLE_ID3_METADATA)};
                }
            },
                    null, null);
        }


        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(mContext, "GGEC");
        int type = TextUtils.isEmpty(overrideExtension) ? com.google.android.exoplayer2.util.Util.inferContentType(uri)
                : com.google.android.exoplayer2.util.Util.inferContentType("." + overrideExtension);
        switch (type) {
            case C.TYPE_SS:
                return new SsMediaSource(uri, dataSourceFactory,
                        new DefaultSsChunkSource.Factory(dataSourceFactory), null, null);
            case C.TYPE_DASH:
                return new DashMediaSource(uri, dataSourceFactory,
                        new DefaultDashChunkSource.Factory(dataSourceFactory), null, null);
            case C.TYPE_HLS:
                return new HlsMediaSource(uri, dataSourceFactory, null, null);
            case C.TYPE_OTHER:
                return new ExtractorMediaSource(uri, dataSourceFactory, new DefaultExtractorsFactory(),
                        null, null);
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }
}
