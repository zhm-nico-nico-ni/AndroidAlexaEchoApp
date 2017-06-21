package com.willblaschko.android.alexa.interfaces.response;

import com.ggec.voice.toollibrary.DiskLruCache;
import com.ggec.voice.toollibrary.log.Log;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.willblaschko.android.alexa.DishLruCacheHelper;
import com.willblaschko.android.alexa.callbacks.AsyncCallback;
import com.willblaschko.android.alexa.data.Directive;
import com.willblaschko.android.alexa.interfaces.AvsException;
import com.willblaschko.android.alexa.interfaces.AvsItem;
import com.willblaschko.android.alexa.interfaces.AvsResponse;
import com.willblaschko.android.alexa.interfaces.errors.AvsResponseException;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsMediaNextCommandItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsMediaPauseCommandItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsMediaPlayCommandItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsMediaPreviousCommandItem;
import com.willblaschko.android.alexa.interfaces.speechrecognizer.AvsExpectSpeechItem;
import com.willblaschko.android.alexa.interfaces.system.AvsUnableExecuteItem;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okio.Buffer;

import static okhttp3.internal.Util.UTF_8;

/**
 * Static helper class to parse incoming responses from the Alexa server and generate a corresponding
 * {@link AvsResponse} item with all the directives matched to their audio streams.
 *
 * @author will on 5/21/2016.
 */
public class ResponseParser {

    public static final String TAG = "ResponseParser";

    private static final Pattern PATTERN = Pattern.compile("<(.*?)>");

    /**
     * Get the AvsItem associated with a Alexa API post/get, this will contain a list of {@link AvsItem} directives,
     * if applicable.
     *
     * Includes hacky work around for PausePrompt items suggested by Eric@Amazon
     * @see <a href="https://forums.developer.amazon.com/questions/28021/response-about-the-shopping-list.html">Forum Discussion</a>
     *
     * @param stream the input stream as a result of our  OkHttp post/get calls
     * @param boundary the boundary we're using to separate the multiparts
     * @param callback
     * @return the parsed AvsResponse
     * @throws IOException
     */
    public static AvsResponse parseResponse(byte[] stream, String boundary, boolean checkBoundary, AsyncCallback<AvsResponse, Exception> callback) throws IOException, IllegalStateException, AvsException{
        byte[] bytes = stream;

//        Log.d(TAG, "raw:"+responseString);
        if (checkBoundary) {
            boolean eq = MyMultiPartResponseParser.arrayequals(stream, new byte[]{0x0D, 0x0A}, 2 );
            String responseString = eq ? new String(stream, 2, stream.length-2) : string(bytes);
            final String responseTrim = responseString.trim();

            final String testBoundary = "--" + boundary;
            if (!StringUtils.isEmpty(responseTrim) && StringUtils.endsWith(responseTrim, testBoundary) && !StringUtils.startsWith(responseTrim, testBoundary)) {
                responseString = "--" + boundary + "\r\n" + responseString;
                bytes = responseString.getBytes();
                return parseResponse3(new ByteArrayInputStream(bytes), boundary, callback);
            } else if(eq) {
                return parseResponse3(new ByteArrayInputStream(bytes, 2, stream.length-2), boundary, callback);
            }
        }

        return parseResponse3(new ByteArrayInputStream(bytes), boundary, callback);
    }

    private static final String string(byte[] bytes) {
        return new String(bytes, UTF_8);
    }

    /**
     * Parse our directive using Gson into an object
     * @param directive the string representation of our JSON object
     * @return the reflected directive
     */
    private static Directive getDirective(String directive){
        Gson gson = new Gson();
        Directive.DirectiveWrapper wrapper = gson.fromJson(directive, Directive.DirectiveWrapper.class);
        if (wrapper.getDirective() == null) {
            return gson.fromJson(directive, Directive.class);
        }
        return wrapper.getDirective();
    }


    /**
     * Get the content id from the return headers from the AVS server
     * @param headers the return headers from the AVS server
     * @return a string form of our content id
     */
    private static String getCID(String headers) throws IOException {
        final String contentString = "Content-ID:";
        BufferedReader reader = new BufferedReader(new StringReader(headers));
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            if (line.startsWith(contentString)) {
                return line.substring(contentString.length()).trim();
            }
        }
        return null;
    }

    /**
     * Check if the response is JSON (a validity check)
     * @param headers the return headers from the AVS server
     * @return true if headers state the response is JSON, false otherwise
     */
    private static boolean isJson(String headers) {
        return headers.contains("application/json");
    }


    public static AvsResponse parseResponse3(InputStream stream, String boundary, final AsyncCallback<AvsResponse, Exception> callback) throws IOException, IllegalStateException, AvsException {
        long start = System.currentTimeMillis();

        final AvsResponse response = new AvsResponse();
        MyMultiPartResponseParser multipartStream = new MyMultiPartResponseParser(stream, boundary);

        final AtomicBoolean hasHandleDirectives = new AtomicBoolean(false);
        final List<Directive> directives = new ArrayList<>();
        boolean nextPart = multipartStream.checkBeginBoundary();
        if (nextPart) {
            Log.d(TAG, "Found initial boundary: true " +callback);

            int count = 0;
            while (nextPart) {
                long nanoT1 = System.currentTimeMillis();
                String headers;
                try {
                    headers = multipartStream.readHeader();// process headers
                } catch (IOException exp) {
                    break;
                }
                Log.d(TAG, "count:" + count + " readHeader use: " + (System.currentTimeMillis() - nanoT1));

                if (isJson(headers))  {
                    nanoT1 = System.currentTimeMillis();
                    String directive = multipartStream.readStringBody();
                    Log.d(TAG, "count:" + count + " readBodyData use: " + (System.currentTimeMillis() - nanoT1) +" "+ directive);
                    // get the json directive
                    directives.add(getDirective(directive));

                    try {
                        nextPart = multipartStream.checkHasBoundary();
                    } catch (IOException exp) {
                        break;
                    }

                } else {
                    Log.d(TAG, "begin  read audio binary");
                    nanoT1 = System.currentTimeMillis();
                    String contentId = getCID(headers);
                    if (contentId != null) {
                        Matcher matcher = PATTERN.matcher(contentId);
                        if (matcher.find()) {
                            String filePath = matcher.group(1);
                            DiskLruCache.Editor editor = DishLruCacheHelper.getHelper().edit(filePath);
                            if (editor != null) {
                                final RandomAccessFile finalWriteFile = editor.newRandomAccessFile(0);
                                SingleFileLockHelper.getHelper().put(filePath);
                                final Buffer temp = new Buffer();
                                try {
                                    multipartStream.readOctetStream(new MyMultiPartResponseParser.IReadOctetStreamCallBack() {
                                        int total = 0;

                                        @Override
                                        public void onData(byte[] array) {
                                            temp.write(array);
                                            if (temp.size() >= 4 * 1024) {
                                                try {
                                                    finalWriteFile.write(temp.readByteArray());
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                            total += array.length;
                                            if (total >= 4 * 1024 && !hasHandleDirectives.get() && callback != null) {
                                                hasHandleDirectives.set(true);
                                                response.addOtherAvsResponse(sss(directives));
                                                callback.handle(response);
                                            }
                                        }
                                    });

                                    if (temp.size() > 0) {
                                        finalWriteFile.write(temp.readByteArray());
                                    }
                                    editor.commit();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    finalWriteFile.close();
                                    editor.abort();
                                    throw e;
                                } finally {
                                    SingleFileLockHelper.getHelper().removeWritingFlag(filePath);
                                }
                                Log.d(TAG, "count:" + count + " put date to audio use: " + (System.currentTimeMillis() - nanoT1));
                            }
                        } else {
                            Log.w(TAG, "breaking:");
                            break;
                        }
                    }


                    // get the audio data
                    //convert our multipart into byte data
                    nextPart = multipartStream.hasBoundary();
                }

                count++;

            }
        } else {
            Log.e(TAG, "parseResponse2 should not into here!!!!!!!!");
        }
        if(!hasHandleDirectives.getAndSet(true)){
            response.addOtherAvsResponse(sss(directives));
        }

        Log.d(TAG, "Parsing response took: " + (System.currentTimeMillis() - start) + " size is " + response.size());

        return response;
    }

    public static AvsResponse parseResponseFail(String raw) throws IOException, IllegalStateException, AvsException{
        List<Directive> directives = new ArrayList<>();
        try {
            Directive directive = getDirective(raw);
            if(directive.isTypeException()){
                AvsResponseException exception = new AvsResponseException(directive);
                Log.e(TAG, "AvsResponseException -> " + exception.getMessage());
                throw exception;
            }else {
                directives.add(directive);
            }
        }catch (JsonParseException e) {
            e.printStackTrace();
            throw new AvsException("Response from Alexa server malformed. ");
        }
        return sss(directives);
    }

    private static AvsResponse sss(List<Directive> directives) {
        AvsResponse response = new AvsResponse();

        for (Directive directive: directives) {
//            Log.d(TAG, "Parsing directive type: "+directive.getHeaderNameSpace()+":"+directive.getHeaderName()); //这个有点消耗性能

            AvsItem item = DirectiveParseHelper.parseDirective(directive, response);
            if(item instanceof AvsExpectSpeechItem){
                response.continueWakeWordDetect = false;
            }

            if(item==null) {
                if (directive.isTypeMediaPlay()) {
                    item = new AvsMediaPlayCommandItem(directive.getPayload().getToken(), directive.getHeaderMessageId());
                } else if (directive.isTypeMediaPause()) {
                    item = new AvsMediaPauseCommandItem(directive.getPayload().getToken(), directive.getHeaderMessageId());
                } else if (directive.isTypeMediaNext()) {
                    item = new AvsMediaNextCommandItem(directive.getPayload().getToken(), directive.getHeaderMessageId());
                } else if (directive.isTypeMediaPrevious()) {
                    item = new AvsMediaPreviousCommandItem(directive.getPayload().getToken(), directive.getHeaderMessageId());
                } else {
                    String directiveString = new Gson().toJson(directive);
                    String msg = "Unknown type found -> " + directive.getHeaderNameSpace() + ":" + directive.getHeaderName();
                    Log.e(TAG, msg+ "\n directive:"+directiveString);
                    item = new AvsUnableExecuteItem(directiveString, "UNSUPPORTED_OPERATION", msg);
                }
            }

            response.add(item);
        }
        return response;
    }
}
