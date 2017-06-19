package com.willblaschko.android.alexa.interfaces.response;

import android.support.annotation.NonNull;

import org.apache.commons.fileupload.MultipartStream;
import org.apache.commons.lang3.ArrayUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import okio.Buffer;

/**
 * Created by ggec on 2017/6/15.
 */

public class MyMultiPartResponseParser {
    /**
     * The Carriage Return ASCII character value.
     */
    public static final byte CR = 0x0D;

    /**
     * The Line Feed ASCII character value.
     */
    public static final byte LF = 0x0A;

    /**
     * The dash (-) ASCII character value.
     */
    public static final byte DASH = 0x2D;

    /**
     * A byte sequence that marks the end of <code>header-part</code>
     * (<code>CRLFCRLF</code>).
     */
    protected static final byte[] HEADER_SEPARATOR = {CR, LF, CR, LF};

    /**
     * A byte sequence that that follows a delimiter that will be
     * followed by an encapsulation (<code>CRLF</code>).
     */
    protected static final byte[] FIELD_SEPARATOR = {CR, LF};

    /**
     * A byte sequence that that follows a delimiter of the last
     * encapsulation in the stream (<code>--</code>).
     */
    protected static final byte[] STREAM_TERMINATOR = {DASH, DASH};

    /**
     * A byte sequence that precedes a boundary (<code>CRLF--</code>).
     */
    protected static final byte[] BOUNDARY_PREFIX = {CR, LF, DASH, DASH};

    /**
     * The maximum length of <code>header-part</code> that will be
     * processed (10 kilobytes = 10240 bytes.).
     */
    public static final int HEADER_PART_SIZE_MAX = 10240;


    /**
     * The input stream from which data is read.
     */
    private final InputStream input;

//    private final String mBoundary;
    private final byte[] BodyEndBoundary;
    private final byte[] mBoundaryArray;

    /**
     * The content encoding to use when reading headers.
     */
    private String headerEncoding;

    public MyMultiPartResponseParser(InputStream input, @NonNull String boundary) {
        this.input = input;
//        mBoundary = boundary;
        mBoundaryArray = boundary.getBytes();
        BodyEndBoundary = append(BOUNDARY_PREFIX, mBoundaryArray);
    }

    /**
     * 第一次使用这个
     * @return true if equal
     */
    public boolean checkBeginBoundary() throws IOException { // FIXME 如果这里不相等，那么这一段的byte就会丢失
        byte[] raw = new byte[2 + mBoundaryArray.length + 2];
        if (-1 == input.read(raw, 0, raw.length)) {
            throw new IOException("No more data is available");
        }
        return Arrays.equals(raw, append(STREAM_TERMINATOR , mBoundaryArray ,FIELD_SEPARATOR));
    }

    /**
     * 第一次使用 {@link #checkBeginBoundary()}
     * 在application/octet-stream 前可以继续使用这个
     * @return true if equal
     */
    public boolean checkHasBoundary() throws IOException {
        byte[] raw = new byte[mBoundaryArray.length + 2];
        if (-1 == input.read(raw, 0, raw.length)) {
            throw new IOException("No more data is available");
        }
        if(arrayequals(raw, mBoundaryArray, mBoundaryArray.length)){
            byte[] end = new byte[]{raw[raw.length-2], raw[raw.length-1]};

            if(Arrays.equals(end, FIELD_SEPARATOR)) {
                return true;
            } else {
                return false;
            }
        }

        return false;
    }

    public String readHeader() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        int i = 0;
        int size = 0;
        while (i < HEADER_SEPARATOR.length) {
            byte b = readByte();
            if (++size > HEADER_PART_SIZE_MAX) {
                throw new MultipartStream.MalformedStreamException(String.format("Header section has more than %s bytes (maybe it is not properly terminated)",
                        HEADER_PART_SIZE_MAX));
            }
            if (b == HEADER_SEPARATOR[i]) {
                i++;
            } else {
                i = 0;
            }
            baos.write(b);
        }
        String headers = null;
        if (headerEncoding != null) {
            try {
                headers = baos.toString(headerEncoding);
            } catch (UnsupportedEncodingException e) {
                // Fall back to platform default if specified encoding is not
                // supported.
                headers = baos.toString();
            }
        } else {
            headers = baos.toString();
        }

        return headers;
    }

    public String readStringBody() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Buffer temp = new Buffer();

        int i = 0;
        while (i < BOUNDARY_PREFIX.length) {
            byte b = readByte();
            if (b == BOUNDARY_PREFIX[i]) {
                i++;
                temp.writeByte(b);
            } else {
                i = 0;
                if(temp.size() > 0){
                    baos.write(temp.readByteArray());
                }
                baos.write(b);
            }
        }
        String body = null;
        if (headerEncoding != null) {
            try {
                body = baos.toString(headerEncoding);
            } catch (UnsupportedEncodingException e) {
                // Fall back to platform default if specified encoding is not
                // supported.
                body = baos.toString();
            }
        } else {
            body = baos.toString();
        }

        return body;
    }

    public ByteArrayOutputStream readOctetStreamBody() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Buffer temp = new Buffer();

        int i = 0;
        while (i < BodyEndBoundary.length) {
            byte b = readByte();

            if (b == BodyEndBoundary[i]) {
                i++;
                temp.writeByte(b);
            } else {
                i = 0;
                if(temp.size()>0){
                    baos.write(temp.readByteArray());
                }
                baos.write(b);
            }
        }

        byte[] raw = new byte[2];
        if (-1 == input.read(raw, 0, raw.length)) {
            throw new IOException("No more data is available");
        } else if(Arrays.equals(STREAM_TERMINATOR, raw)) {
            hasBoundary.set(false);
        } else if(Arrays.equals(FIELD_SEPARATOR, raw)){
            hasBoundary.set(true);
        }

        return baos;
    }

    public void readOctetStream(IReadOctetStreamCallBack callBack) throws IOException {
        Buffer temp = new Buffer();

        int i = 0;
        while (i < BodyEndBoundary.length) {
            byte b = readByte();

            if (b == BodyEndBoundary[i]) {
                i++;
                temp.writeByte(b);
            } else {
                i = 0;
                if(temp.size()>0){
                    callBack.onData(temp.readByteArray());
                }
                callBack.onData(new byte[]{b});
            }
        }

        byte[] raw = new byte[2];
        if (-1 == input.read(raw, 0, raw.length)) {
            throw new IOException("No more data is available");
        } else if(Arrays.equals(STREAM_TERMINATOR, raw)) {
            hasBoundary.set(false);
        } else if(Arrays.equals(FIELD_SEPARATOR, raw)){
            hasBoundary.set(true);
        }

    }

    AtomicBoolean hasBoundary = new AtomicBoolean(false);

    public boolean hasBoundary(){
        return hasBoundary.getAndSet(false);
    }

    private byte readByte() throws IOException {
        byte[] b = new byte[1];
        int tail = input.read(b, 0, 1);
        if (tail == -1) {
            // No more data available.
            throw new IOException("No more data is available");
        }
        return b[0];
    }

    /**
     * Compares <code>count</code> first bytes in the arrays
     * <code>a</code> and <code>b</code>.
     *
     * @param a     The first array to compare.
     * @param b     The second array to compare.
     * @param count How many bytes should be compared.
     *
     * @return <code>true</code> if <code>count</code> first bytes in arrays
     *         <code>a</code> and <code>b</code> are equal.
     */
    public static boolean arrayequals(byte[] a,
                                      byte[] b,
                                      int count) {
        for (int i = 0; i < count; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    public static byte[] append(byte[]... array){
        int size= 0;
        ArrayList<Byte> result = new ArrayList<>();

        for(byte[] a: array){
            for (byte b : a){
                result.add(b);
                size++;
            }
        }

        return ArrayUtils.toPrimitive(result.toArray(new Byte[size]));
    }


    public interface IReadOctetStreamCallBack{
        void onData(byte[] array);
    }
}
