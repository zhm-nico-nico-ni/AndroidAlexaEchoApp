package com.willblaschko.android.alexa.connection;

import android.os.Build;

import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.ggec.voice.toollibrary.log.Log;

import java.security.KeyStore;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.TlsVersion;

/**
 * Create a singleton OkHttp client that, hopefully, will someday be able to make sure all connections are valid according to AVS's strict
 * security policy--this will hopefully fix the Connection Reset By Peer issue.
 *
 * Created by willb_000 on 6/26/2016.
 */
public class ClientUtil {

    private static volatile OkHttpClient mHttp1Client;
    private static volatile OkHttpClient mHttp2Client;

    public static synchronized OkHttpClient getHttp2Client(){
        if(mHttp2Client == null) {
            OkHttpClient.Builder client = new OkHttpClient.Builder();
            if (Build.VERSION.SDK_INT >= 16 && Build.VERSION.SDK_INT < 25) {
                try {

                    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    trustManagerFactory.init((KeyStore) null);
                    TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
                    if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                        throw new IllegalStateException("Unexpected default trust managers:"
                                + Arrays.toString(trustManagers));
                    }

                    X509TrustManager trustManager = (X509TrustManager) trustManagers[0];

                    SSLContext sc = SSLContext.getInstance("TLSv1.2");
                    sc.init(null, null, null);

                    String[] enabled = sc.getSocketFactory().getDefaultCipherSuites();
                    String[] supported = sc.getSocketFactory().getSupportedCipherSuites();
//                    Log.d("OkHttpTLSCompat", "enable " + Arrays.toString(enabled));
                    Log.d("OkHttpTLSCompat", "supported " + Arrays.toString(supported));
                    client.sslSocketFactory(new Tls12SocketFactory(sc.getSocketFactory()), trustManager);

                    ConnectionSpec cs = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                            .tlsVersions(TlsVersion.TLS_1_2)
                            .cipherSuites(
                                    CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256 // work
                                    , CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384 // work
                                    /*
                                    CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256, // 已测试，无效
                                    CipherSuite.TLS_DHE_RSA_WITH_AES_128_CBC_SHA, // 已测试，无效
                                    CipherSuite.TLS_RSA_WITH_AES_128_CBC_SHA,// 已测试，无效
                                    CipherSuite.TLS_RSA_WITH_3DES_EDE_CBC_SHA*/// 已测试，无效
                            )
                            .build();


                    client.connectionSpecs(Collections.singletonList(cs));
                    client.protocols(Arrays.asList(Protocol.HTTP_1_1, Protocol.HTTP_2));
                } catch (Exception exc) {
                    Log.e("OkHttpTLSCompat", "Error while setting TLS 1.2", exc);
                }
            }

            if(true){
//                client.addNetworkInterceptor(new StethoInterceptor());
//                HttpLoggingInterceptor logger = new HttpLoggingInterceptor();
//                logger.setLevel(HttpLoggingInterceptor.Level.BASIC);
//                 client.addNetworkInterceptor(logger);
            }
            mHttp2Client = client
                    .readTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)

//                    .addInterceptor(new RetryInterceptor())
                    .build();
        }

        return mHttp2Client;
    }

    public static synchronized OkHttpClient getHttp1Client(){
        if(mHttp1Client == null) {
            OkHttpClient.Builder client = new OkHttpClient.Builder();
            if (Build.VERSION.SDK_INT >= 16 && Build.VERSION.SDK_INT <= 19) {
                try {
                    SSLContext sc = SSLContext.getInstance("TLSv1.2");
                    sc.init(null, null, null);
                    client.sslSocketFactory(new TLSSocketFactory(sc.getSocketFactory()), TLSSocketFactory.getTrustManger());

                } catch (Exception exc) {
                    Log.e("OkHttpTLSCompat", "Error while setting TLS 1.2", exc);
                }
            }
            mHttp1Client = client
                    .readTimeout(8000, TimeUnit.MILLISECONDS)
                    .writeTimeout(8000, TimeUnit.MILLISECONDS)
                    .connectTimeout(10000, TimeUnit.MILLISECONDS)
                    .retryOnConnectionFailure(true)

                    .addNetworkInterceptor(new StethoInterceptor())
                    .build();
            Log.d("zhm", "new http1 client");
        }
        return mHttp1Client;
    }

}
