package com.willblaschko.android.alexa.audioplayer;

import android.text.TextUtils;

import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.ggec.voice.toollibrary.log.Log;
import com.willblaschko.android.alexa.audioplayer.pls.Playlist;
import com.willblaschko.android.alexa.audioplayer.pls.PlaylistParser;
import com.willblaschko.android.alexa.connection.ClientUtil;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsPlayRemoteItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by ggec on 2017/5/15.
 */

public class MyAVSAudioParser {
    private final static String TAG = "MyAVSAudioParser";
    private static final Pattern FILE_PATTERN = Pattern.compile("pls|m3u", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLAY_LIST_PATTERN = Pattern.compile("audio/x-scpls|audio/x-mpegurl", Pattern.CASE_INSENSITIVE);
    private final static int MAX_REDIRECT_TIMES = 3;

    private AvsPlayRemoteItem mAvsPlayRemoteItem;
    private Call mAvsRemoteCall;
    private int redirectCount;

    private OkHttpClient okHttpClient;
    ;

    public MyAVSAudioParser(AvsPlayRemoteItem playItem) {
        mAvsPlayRemoteItem = playItem;
    }

    public void cancelRequest() {
        if (mAvsRemoteCall != null) {
            if (!mAvsRemoteCall.isCanceled()) {
                mAvsRemoteCall.cancel();
            }
            mAvsRemoteCall = null;
        }
    }

    public boolean isExecuted() {
        return mAvsRemoteCall != null && mAvsRemoteCall.isExecuted();
    }

    public boolean isCanceled(){
        return mAvsRemoteCall == null || mAvsRemoteCall.isCanceled();
    }

    private OkHttpClient getHttpClient() {
        if (okHttpClient == null) {
            okHttpClient = new OkHttpClient.Builder()
                    .readTimeout(8, TimeUnit.SECONDS)
                    .writeTimeout(8, TimeUnit.SECONDS)
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .addNetworkInterceptor(new StethoInterceptor())
                    .build();
        }
        return okHttpClient;
    }

    public String request(String url) throws IOException {
        redirectCount = MAX_REDIRECT_TIMES;
        return requestImpl(url);
    }

    private String requestImpl(String url) throws IOException {
        final HttpUrl urll = HttpUrl.parse(url);
        OkHttpClient okHttpClient;
        if (urll.isHttps()) {
            okHttpClient = ClientUtil.getHttp2Client();
        } else {
            okHttpClient = getHttpClient();
        }

        mAvsRemoteCall = okHttpClient
                .newCall(new Request.Builder()
                        .url(urll)
                        .build());

        Log.d(TAG, "requestImpl:"+url);
        return parseResponse(mAvsRemoteCall.execute());
    }

    private String parseResponse(Response response) throws IOException {
        String playUri = response.request().url().toString();
        if (PLAY_LIST_PATTERN.matcher(response.header("Content-Type")).find()) {
            // audio/x-mpegurl 是声明文件了，解析
            BufferedReader bufferedReader = new BufferedReader(response.body().charStream());
            String responseFirstLine = bufferedReader.readLine();
            if ("[playlist]".equalsIgnoreCase(responseFirstLine)) {
                // 是.pls
                Playlist playlist = PlaylistParser.parse(bufferedReader);
                Log.d(TAG, "parse pls finish," + playlist.toString());
                if (!playlist.getTracks().isEmpty()) {
                    playUri = playlist.getTracks().get(0).getFile();
                    mAvsPlayRemoteItem.setConvertUrl(playUri);
                } else {
                    Log.w(TAG, "can not parse pls:"+ playUri) ;
                }

            } else if ("#EXTM3U".equalsIgnoreCase(responseFirstLine)) {
                Log.d(TAG, "parse finish, is HLS, " + playUri);
                mAvsPlayRemoteItem.setConvertUrl(playUri);
                mAvsPlayRemoteItem.extension = ".m3u8";
            } else {
                // 可能是m3u 或者是m3u8

                if (FILE_PATTERN.matcher(responseFirstLine).find()) {
                    if (redirectCount > 0) {
                        redirectCount--;
                        return requestImpl(responseFirstLine);
                    } else {
                        Log.w(TAG, "do redirect to much ," + MAX_REDIRECT_TIMES);
                        return null;
                    }
                } else {
                    String bodyString = responseFirstLine;
                    Log.i(TAG, "get remote result: " + bodyString);
                    HttpUrl maybeUrl = HttpUrl.parse(bodyString.trim());
                    if (maybeUrl != null && !TextUtils.isEmpty(maybeUrl.scheme()) && maybeUrl.scheme().contains("http")) {
                        mAvsPlayRemoteItem.setConvertUrl(bodyString.trim());
                        playUri = mAvsPlayRemoteItem.getConvertUrl();
                    } else {
                        playUri = mAvsPlayRemoteItem.getUrl();
                    }
                }
            }

        } else {
            mAvsPlayRemoteItem.setConvertUrl(playUri);
        }

        return playUri;
    }
}
