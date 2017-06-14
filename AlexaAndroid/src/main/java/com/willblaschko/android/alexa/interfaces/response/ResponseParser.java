package com.willblaschko.android.alexa.interfaces.response;

import com.ggec.voice.toollibrary.log.Log;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
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

import org.apache.commons.fileupload.MultipartStream;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     * @return the parsed AvsResponse
     * @throws IOException
     */
    public static AvsResponse parseResponse(byte[] stream, String boundary, boolean checkBoundary) throws IOException, IllegalStateException, AvsException{
        long start = System.currentTimeMillis();

        List<Directive> directives = new ArrayList<>();
        HashMap<String, byte[]> audio = new HashMap<>();

        byte[] bytes = stream;
        String responseString = string(bytes);
//        Log.d(TAG, "raw:"+responseString);
        if (checkBoundary) {
            Log.d(TAG, ""+responseString);
            final String responseTrim = responseString.trim();
            final String testBoundary = "--" + boundary;
            if (!StringUtils.isEmpty(responseTrim) && StringUtils.endsWith(responseTrim, testBoundary) && !StringUtils.startsWith(responseTrim, testBoundary)) {
                responseString = "--" + boundary + "\r\n" + responseString;
                bytes = responseString.getBytes();
            }
        }

        MultipartStream mpStream = new MultipartStream(new ByteArrayInputStream(bytes), boundary.getBytes(), 2048, null);//FIXME 这个改小点后，要测下与没有问题

        //have to do this otherwise mpStream throws an exception
        if (mpStream.skipPreamble()) {
            Log.d(TAG, "Found initial boundary: true");

            //we have to use the count hack here because otherwise readBoundary() throws an exception
            int count = 0;
            while (count < 1 || mpStream.readBoundary()) {
                String headers;
                try {
                    headers = mpStream.readHeaders();
                } catch (MultipartStream.MalformedStreamException exp) {
                    break;
                }
                ByteArrayOutputStream data = new ByteArrayOutputStream();
                mpStream.readBodyData(data);
                if (!isJson(headers)) {
                    // get the audio data
                    //convert our multipart into byte data
                    String contentId = getCID(headers);
                    if(contentId != null) {
                        Matcher matcher = PATTERN.matcher(contentId);
                        if (matcher.find()) {
                            String currentId = "cid:" + matcher.group(1);
                            audio.put(currentId, data.toByteArray());
                        }
                    }
                } else {
                    // get the json directive
                    String directive = data.toString(Charset.defaultCharset().displayName());
                    Log.d(TAG, ""+directive);
                    directives.add(getDirective(directive));
                }
                count++;
            }

        } else {
            try {
                Directive directive = getDirective(responseString);
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
        }

        AvsResponse response = new AvsResponse();

        for (Directive directive: directives) {

//            Log.d(TAG, "Parsing directive type: "+directive.getHeaderNameSpace()+":"+directive.getHeaderName()); //这个有点消耗性能

            AvsItem item = DirectiveParseHelper.parseDirective(directive, audio, response); //FIXME 根据namespace来区分
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

        Log.d(TAG, "Parsing response took: " + (System.currentTimeMillis() - start) +" size is " + response.size());

        return response;
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
}
