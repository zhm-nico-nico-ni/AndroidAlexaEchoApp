package com.willblaschko.android.alexa.interfaces.system;

import com.ggec.voice.toollibrary.log.Log;
import com.willblaschko.android.alexa.callbacks.AsyncCallback;
import com.willblaschko.android.alexa.connection.ClientUtil;
import com.willblaschko.android.alexa.interfaces.AvsResponse;
import com.willblaschko.android.alexa.interfaces.SendEvent;
import com.willblaschko.android.alexa.interfaces.errors.AvsResponseException;
import com.willblaschko.android.alexa.interfaces.response.ResponseParser;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.Buffer;
import okio.BufferedSource;

/**
 * Open Down Channel {@link com.willblaschko.android.alexa.data.Event} to open a persistent connection with the Alexa server. Currently doesn't seem to work as expected.
 *
 * {@link com.willblaschko.android.alexa.data.Event}
 *
 * @author will on 5/21/2016.
 */
public class OpenDownchannel extends SendEvent {

    private static final String TAG = "OpenDownchannel";
    private Call currentCall;
    private OkHttpClient client;
    private String url;
    private final AsyncCallback<AvsResponse, Exception> callback;
    private boolean isStop;

    public OpenDownchannel(final String url, final AsyncCallback<AvsResponse, Exception> callback) {
        this.callback = callback;
        this.url = url;
        this.client = ClientUtil.getHttp2Client().newBuilder().connectionPool(new ConnectionPool(1,1,TimeUnit.HOURS)).readTimeout(60, TimeUnit.MINUTES).build();
    }

    /**
     * Open the connection
     * @param accessToken
     * @return true if canceled externally
     * @throws IOException
     */
    public boolean connect(String accessToken) throws IOException {
        isStop = false;

        final Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + accessToken)
                .build();

        Response response = null;
        try {
            currentCall = client.newCall(request);
            response = currentCall.execute();
            final String boundary = getBoundary(response);
            BufferedSource source = response.body().source();
            Buffer buffer = new Buffer();
            Log.d(TAG, "on response 0 :"+boundary);
            if (callback != null) {
                callback.start();
            }
            while (!source.exhausted()) {
                long all = source.read(buffer, 8192);
                Log.d(TAG, "on response 1. " +all);
                AvsResponse val = new AvsResponse();

                try {
                    val = ResponseParser.parseResponse(buffer.readByteArray(), boundary, true);
                } catch (AvsResponseException ex){
                    if(ex.isUnAuthorized()) {
                        onError(callback, ex);
                        break;
                    } else {
                        Log.e(TAG, "on response parseResponse error: " + ex.getMessage(), ex);
                    }
                } catch (Exception exp) {
                    Log.e(TAG, "on response parseResponse error: " + exp.getMessage(), exp);
                }

                if (callback != null) {
                    callback.success(val);
                }
            }
        } catch (IOException e) {
            onError(callback, e);
        } finally {
            if (response != null) {
                response.close();
            }
            Log.d(TAG, "on response 5");
        }

        return isStop;
    }

    public void closeConnection(boolean stop) {
        isStop = stop;
        if (currentCall != null && !currentCall.isCanceled()) {
            currentCall.cancel();
        }
    }

    private void onError(final AsyncCallback<AvsResponse, Exception> callback, Exception e) {
        if (callback != null) {
            callback.failure(e);
            callback.complete();
        }
    }

    @Override
    @NotNull
    protected String getEvent() {
        return "";
    }
}
