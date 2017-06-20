package com.willblaschko.android.alexa.interfaces.response;

import android.net.Uri;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Predicate;
import com.google.android.exoplayer2.util.Util;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * Created by ggec on 2017/6/16.
 */

public class MyDataSource implements DataSource {

    public static final DataSource.Factory FACTORY = new DataSource.Factory() {

        @Override
        public DataSource createDataSource() {
            return new MyDataSource("11", null, new TransferListener<MyDataSource>() {
                long totalTransfer;

                @Override
                public void onTransferStart(MyDataSource source, DataSpec dataSpec) {
                    Log.d(TAG, "onTransferStart " + dataSpec.toString());
                }

                @Override
                public void onBytesTransferred(MyDataSource source, int bytesTransferred) {
                    totalTransfer += bytesTransferred;
//                    Log.d(TAG, "onBytesTransferred "+bytesTransferred );
                }

                @Override
                public void onTransferEnd(MyDataSource source) {
                    Log.d(TAG, "onTransferEnd " + "   " + totalTransfer);
                }
            });
        }

    };

    /**
     * The default connection timeout, in milliseconds.
     */
    public static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 8 * 1000;
    /**
     * The default read timeout, in milliseconds.
     */
    public static final int DEFAULT_READ_TIMEOUT_MILLIS = 8 * 1000;

    private static final String TAG = "MyDataSource";
    private static final long MAX_BYTES_TO_DRAIN = 2048;
    private static final Pattern CONTENT_RANGE_HEADER =
            Pattern.compile("^bytes (\\d+)-(\\d+)/(\\d+)$");
    private static final AtomicReference<byte[]> skipBufferReference = new AtomicReference<>();

    private final boolean allowCrossProtocolRedirects;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private final String userAgent;
    private final Predicate<String> contentTypePredicate;
    private final HttpDataSource.RequestProperties defaultRequestProperties;
    private final HttpDataSource.RequestProperties requestProperties;
    private final TransferListener<? super MyDataSource> listener;

    private DataSpec dataSpec;


    private boolean opened;

    private long bytesToSkip;
    private long bytesToRead;

    private long bytesSkipped;
    private RandomAccessFile randomAccessFile;
    private String mFilePath;

    /**
     * @param userAgent            The User-Agent string that should be used.
     * @param contentTypePredicate An optional {@link Predicate}. If a content type is rejected by the
     *                             predicate then a {@link HttpDataSource.InvalidContentTypeException} is thrown from
     *                             {@link #open(DataSpec)}.
     */
    public MyDataSource(String userAgent, Predicate<String> contentTypePredicate) {
        this(userAgent, contentTypePredicate, null);
    }

    /**
     * @param userAgent            The User-Agent string that should be used.
     * @param contentTypePredicate An optional {@link Predicate}. If a content type is rejected by the
     *                             predicate then a {@link HttpDataSource.InvalidContentTypeException} is thrown from
     *                             {@link #open(DataSpec)}.
     * @param listener             An optional listener.
     */
    public MyDataSource(String userAgent, Predicate<String> contentTypePredicate,
                        TransferListener<? super MyDataSource> listener) {
        this(userAgent, contentTypePredicate, listener, DEFAULT_CONNECT_TIMEOUT_MILLIS,
                DEFAULT_READ_TIMEOUT_MILLIS);
    }

    /**
     * @param userAgent            The User-Agent string that should be used.
     * @param contentTypePredicate An optional {@link Predicate}. If a content type is rejected by the
     *                             predicate then a {@link HttpDataSource.InvalidContentTypeException} is thrown from
     *                             {@link #open(DataSpec)}.
     * @param listener             An optional listener.
     * @param connectTimeoutMillis The connection timeout, in milliseconds. A timeout of zero is
     *                             interpreted as an infinite timeout.
     * @param readTimeoutMillis    The read timeout, in milliseconds. A timeout of zero is interpreted
     *                             as an infinite timeout.
     */
    public MyDataSource(String userAgent, Predicate<String> contentTypePredicate,
                        TransferListener<? super MyDataSource> listener, int connectTimeoutMillis,
                        int readTimeoutMillis) {
        this(userAgent, contentTypePredicate, listener, connectTimeoutMillis, readTimeoutMillis, false,
                null);
    }

    /**
     * @param userAgent                   The User-Agent string that should be used.
     * @param contentTypePredicate        An optional {@link Predicate}. If a content type is rejected by the
     *                                    predicate then a {@link HttpDataSource.InvalidContentTypeException} is thrown from
     *                                    {@link #open(DataSpec)}.
     * @param listener                    An optional listener.
     * @param connectTimeoutMillis        The connection timeout, in milliseconds. A timeout of zero is
     *                                    interpreted as an infinite timeout. Pass {@link #DEFAULT_CONNECT_TIMEOUT_MILLIS} to use
     *                                    the default value.
     * @param readTimeoutMillis           The read timeout, in milliseconds. A timeout of zero is interpreted
     *                                    as an infinite timeout. Pass {@link #DEFAULT_READ_TIMEOUT_MILLIS} to use the default value.
     * @param allowCrossProtocolRedirects Whether cross-protocol redirects (i.e. redirects from HTTP
     *                                    to HTTPS and vice versa) are enabled.
     * @param defaultRequestProperties    The default request properties to be sent to the server as
     *                                    HTTP headers or {@code null} if not required.
     */
    public MyDataSource(String userAgent, Predicate<String> contentTypePredicate,
                        TransferListener<? super MyDataSource> listener, int connectTimeoutMillis,
                        int readTimeoutMillis, boolean allowCrossProtocolRedirects,
                        HttpDataSource.RequestProperties defaultRequestProperties) {
        this.userAgent = Assertions.checkNotEmpty(userAgent);
        this.contentTypePredicate = contentTypePredicate;
        this.listener = listener;
        this.requestProperties = new HttpDataSource.RequestProperties();
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.allowCrossProtocolRedirects = allowCrossProtocolRedirects;
        this.defaultRequestProperties = defaultRequestProperties;
    }

    @Override
    public Uri getUri() {
        return mFilePath == null ? null : Uri.parse(mFilePath);
    }

    @Override
    public long open(DataSpec dataSpec) throws HttpDataSource.HttpDataSourceException {
        this.dataSpec = dataSpec;
        this.bytesSkipped = 0;

        try {
            randomAccessFile = new RandomAccessFile(dataSpec.uri.getPath(), "r");
            mFilePath = dataSpec.uri.getPath();
        } catch (FileNotFoundException e) {
            throw new HttpDataSource.HttpDataSourceException("Unable to connect to " + dataSpec.uri.toString(), e,
                    dataSpec, HttpDataSource.HttpDataSourceException.TYPE_OPEN);
        }

        bytesToSkip = dataSpec.position != 0 ? dataSpec.position : 0;

        // Determine the length of the data to be read, after skipping.
        if (!dataSpec.isFlagSet(DataSpec.FLAG_ALLOW_GZIP)) {
            if (dataSpec.length != C.LENGTH_UNSET) {
                bytesToRead = dataSpec.length;
            } else {
                bytesToRead = C.LENGTH_UNSET;
//                long contentLength = C.LENGTH_UNSET;//getContentLength(connection);
//                bytesToRead = contentLength != C.LENGTH_UNSET ? (contentLength - bytesToSkip)
//                        : C.LENGTH_UNSET;
            }
        } else {
            // Gzip is enabled. If the server opts to use gzip then the content length in the response
            // will be that of the compressed data, which isn't what we want. Furthermore, there isn't a
            // reliable way to determine whether the gzip was used or not. Always use the dataSpec length
            // in this case.
            bytesToRead = dataSpec.length;
        }

        opened = true;
        if (listener != null) {
            listener.onTransferStart(this, dataSpec);
        }

        return bytesToRead;
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws HttpDataSource.HttpDataSourceException {
        try {
            skipInternal();
            return readInternal(buffer, offset, readLength);
        } catch (IOException e) {
            throw new HttpDataSource.HttpDataSourceException(e, dataSpec, HttpDataSource.HttpDataSourceException.TYPE_READ);
        }
    }

    @Override
    public void close() throws HttpDataSource.HttpDataSourceException {
        try {
            if (randomAccessFile != null) {
                try {
                    randomAccessFile.close();
                } catch (IOException e) {
                    throw new HttpDataSource.HttpDataSourceException(e, dataSpec, HttpDataSource.HttpDataSourceException.TYPE_CLOSE);
                }
            }
        } finally {
            randomAccessFile = null;
            closeConnectionQuietly();
            if (opened) {
                opened = false;
                if (listener != null) {
                    listener.onTransferEnd(this);
                }
            }
        }
    }

    /**
     * Skips any bytes that need skipping. Else does nothing.
     * <p>
     * This implementation is based roughly on {@code libcore.io.Streams.skipByReading()}.
     *
     * @throws InterruptedIOException If the thread is interrupted during the operation.
     * @throws EOFException           If the end of the input stream is reached before the bytes are skipped.
     */
    private void skipInternal() throws IOException {
        if (bytesSkipped == bytesToSkip) {
            return;
        }

        // Acquire the shared skip buffer.
        byte[] skipBuffer = skipBufferReference.getAndSet(null);
        if (skipBuffer == null) {
            skipBuffer = new byte[4096];
        }

        while (bytesSkipped != bytesToSkip) {
            int readLength = (int) Math.min(bytesToSkip - bytesSkipped, skipBuffer.length);
            int read = randomAccessFile.read(skipBuffer, 0, readLength);
            if (Thread.interrupted()) {
                throw new InterruptedIOException();
            }
            if (read == -1) {
                throw new EOFException();
            }
            bytesSkipped += read;
            if (listener != null) {
                listener.onBytesTransferred(this, read);
            }
        }

        // Release the shared skip buffer.
        skipBufferReference.set(skipBuffer);
    }

    /**
     * Reads up to {@code length} bytes of data and stores them into {@code buffer}, starting at
     * index {@code offset}.
     * <p>
     * This method blocks until at least one byte of data can be read, the end of the opened range is
     * detected, or an exception is thrown.
     *
     * @param buffer     The buffer into which the read data should be stored.
     * @param offset     The start offset into {@code buffer} at which data should be written.
     * @param readLength The maximum number of bytes to read.
     * @return The number of bytes read, or {@link C#RESULT_END_OF_INPUT} if the end of the opened
     * range is reached.
     * @throws IOException If an error occurs reading from the source.
     */
    private int readInternal(byte[] buffer, int offset, int readLength) throws IOException {
        if (readLength == 0) {
            return 0;
        }
//        if (bytesToRead != C.LENGTH_UNSET) {
//            long bytesRemaining = bytesToRead - bytesRead;
//            if (bytesRemaining == 0) {
//                return C.RESULT_END_OF_INPUT;
//            }
//            readLength = (int) Math.min(readLength, bytesRemaining);
//        }

        int read = randomAccessFile.read(buffer, offset, readLength);
        if (read < readLength) {
            boolean isFileAlreadyLock = true;
            do {
                boolean notFinish = SingleFileLockHelper.getHelper().getIsWriting(mFilePath);
                if (notFinish) {
                    try {
                        new CountDownLatch(1).await(80, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (read >= 0) {
                        int r = randomAccessFile.read(buffer,
                                offset + read, readLength - read);
                        if(r > 0) read += r;
                    } else if (read < 0) {
                        read = randomAccessFile.read(buffer, offset, readLength);
                    }
                } else {
                    long filePointer = randomAccessFile.getFilePointer();
                    long fileLength = randomAccessFile.length();
                    Log.d(TAG, "get lock " + filePointer + "   " + fileLength + " read:" + read);
                    if (filePointer < fileLength) {
                        int needRead = read <= 0 ? readLength : readLength - read;
                        int remaining = (int) (fileLength - filePointer);
                        if (remaining >= needRead) {
                            if (read >= 0) {
                                read += randomAccessFile.read(buffer, offset + read, readLength - read);
                            } else {
                                read = randomAccessFile.read(buffer, offset, readLength);
                            }
                        } else if (remaining == 0){

                        } else if(remaining < needRead) {
                            if (read >= 0) {
                                read += randomAccessFile.read(buffer, offset + read, remaining);
                            } else {
                                read = randomAccessFile.read(buffer, offset, remaining);
                            }

                        }

                        Log.d(TAG, "get lock break");
//                        isFileAlreadyLock = false;
                        break;
                    }

                    if (bytesToRead != C.LENGTH_UNSET) {
                        // End of stream reached having not read sufficient data.
                        throw new EOFException();
                    }
                    return C.RESULT_END_OF_INPUT;
                }

            } while (isFileAlreadyLock && read < readLength);


        }

        if(read < readLength){
            Log.w(TAG, "read < readLength " + read +"    "+ readLength);
        }
        if (listener != null) {
            listener.onBytesTransferred(this, read);
        }
        return read;
    }

    /**
     * On platform API levels 19 and 20, okhttp's implementation of {@link InputStream#close} can
     * block for a long time if the stream has a lot of data remaining. Call this method before
     * closing the input stream to make a best effort to cause the input stream to encounter an
     * unexpected end of input, working around this issue. On other platform API levels, the method
     * does nothing.
     *
     * @param connection     The connection whose {@link InputStream} should be terminated.
     * @param bytesRemaining The number of bytes remaining to be read from the input stream if its
     *                       length is known. {@link C#LENGTH_UNSET} otherwise.
     */
    private static void maybeTerminateInputStream(RandomAccessFile connection, long bytesRemaining) {
        if (Util.SDK_INT != 19 && Util.SDK_INT != 20) {
            return;
        }

        try {
            if (bytesRemaining == C.LENGTH_UNSET) {
                // If the input stream has already ended, do nothing. The socket may be re-used.
                if (connection.read() == -1) {
                    return;
                }
            } else if (bytesRemaining <= MAX_BYTES_TO_DRAIN) {
                // There isn't much data left. Prefer to allow it to drain, which may allow the socket to be
                // re-used.
                return;
            }
            String className = connection.getClass().getName();
            if (className.equals("com.android.okhttp.internal.http.HttpTransport$ChunkedInputStream")
                    || className.equals(
                    "com.android.okhttp.internal.http.HttpTransport$FixedLengthInputStream")) {
                Class<?> superclass = connection.getClass().getSuperclass();
                Method unexpectedEndOfInput = superclass.getDeclaredMethod("unexpectedEndOfInput");
                unexpectedEndOfInput.setAccessible(true);
                unexpectedEndOfInput.invoke(connection);
            }
        } catch (Exception e) {
            // If an IOException then the connection didn't ever have an input stream, or it was closed
            // already. If another type of exception then something went wrong, most likely the device
            // isn't using okhttp.
        }
    }


    /**
     * Closes the current connection quietly, if there is one.
     */
    private void closeConnectionQuietly() {
        if (randomAccessFile != null) {
            try {
                randomAccessFile.close();
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error while disconnecting", e);
            }
            randomAccessFile = null;
        }
    }
}
