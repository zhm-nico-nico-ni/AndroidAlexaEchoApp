package com.willblaschko.android.alexa.connection;

import com.ggec.voice.toollibrary.log.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.CipherSuite;
import okhttp3.TlsVersion;

public class TLSSocketFactory extends SSLSocketFactory {
    private final static String TLS_v1_1 = TlsVersion.TLS_1_1.javaName();
    private final static String TLS_v1_2 = TlsVersion.TLS_1_2.javaName();

    private SSLSocketFactory internalSSLSocketFactory;

    public TLSSocketFactory(SSLSocketFactory delegate) throws KeyManagementException, NoSuchAlgorithmException {
        internalSSLSocketFactory = delegate;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return internalSSLSocketFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return internalSSLSocketFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(address, port, localAddress, localPort));
    }

    /*
     * Utility methods
     */

    private static Socket enableTLSOnSocket(Socket socket) {
        if (socket instanceof SSLSocket) { // skip the fix if server doesn't provide there TLS version
            SSLSocket sslSocket = (SSLSocket) socket;
            boolean isTlsServer =isTLSServerEnabled(sslSocket);
            Log.i(Log.TAG_APP,"is tls enable:"+isTlsServer+ " getEnabledProtocols:" + Arrays.toString(sslSocket.getEnabledProtocols())
                    +"\ngetSupportedCipherSuites:"+Arrays.toString(sslSocket.getSupportedCipherSuites()));
            if(isTlsServer){
                sslSocket.setEnabledProtocols(new String[]{"TLSv1.2"});
                sslSocket.setEnabledCipherSuites(new String[]{CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256.javaName()});
                Log.i(Log.TAG_APP,"2 getEnabledProtocols:" + Arrays.toString(sslSocket.getEnabledProtocols()));
            }
        }
        return socket;
    }

    private static boolean isTLSServerEnabled(SSLSocket sslSocket) {
        Log.i(Log.TAG_APP, "__prova__ :: " + Arrays.toString(sslSocket.getSupportedProtocols()));
        for (String protocol : sslSocket.getSupportedProtocols()) {
            if (protocol.equals(TLS_v1_1) || protocol.equals(TLS_v1_2)) {
                return true;
            }
        }
        return false;
    }

    public static X509TrustManager getTrustManger(){
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
    }
}