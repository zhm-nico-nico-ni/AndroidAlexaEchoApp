package com.ggec.voice.assistservice;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.ggec.voice.assistservice.data.BackGroundProcessServiceControlCommand;
import com.ggec.voice.assistservice.log.Log;
import com.willblaschko.android.alexa.BroadCast;

import java.io.File;
import java.io.IOException;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

/**
 * Created by ggec on 2017/3/29.
 */

public class AssistService extends Service implements RecognitionListener {
    private static final String TAG = "AssistService";
    private static final String KWS_SEARCH = "wakeup";

    /* Keyword we are looking for to activate menu */
    private static final String KEYPHRASE = "oh mighty computer";//oh mighty computer

    private SpeechRecognizer recognizer;
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.w(TAG, "onReceive RECEIVE_START_WAKE_WORD_LISTENER");
            switchSearch(KWS_SEARCH);
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        startForeground(21940, new Notification.Builder(this)
                .setContentText("GGEC Assist Service")
                .build());

        try {
            Assets assets = new Assets(this);
            File assetDir = assets.syncAssets();
            setupRecognizer(assetDir);
            switchSearch(KWS_SEARCH);
            registerReceiver(receiver, new IntentFilter(BroadCast.RECEIVE_START_WAKE_WORD_LISTENER));
        } catch (IOException e) {

        }
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them

        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))

//                .setRawLogDir(assetsDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)

                .getRecognizer();
        recognizer.addListener(this);

        /** In your application you might not need to add all those searches.
         * They are added here for demonstration. You can leave just one.
         */

        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);

//        // Create grammar-based search for selection between demos
//        File menuGrammar = new File(assetsDir, "menu.gram");
//        recognizer.addGrammarSearch(MENU_SEARCH, menuGrammar);
//
//        // Create grammar-based search for digit recognition
//        File digitsGrammar = new File(assetsDir, "digits.gram");
//        recognizer.addGrammarSearch(DIGITS_SEARCH, digitsGrammar);
//
//        // Create language model search
//        File languageModel = new File(assetsDir, "weather.dmp");
//        recognizer.addNgramSearch(FORECAST_SEARCH, languageModel);
//
//        // Phonetic search
//        File phoneticModel = new File(assetsDir, "en-phone.dmp");
//        recognizer.addAllphoneSearch(PHONE_SEARCH, phoneticModel);
    }

    private void switchSearch(String searchName) {
        recognizer.stop();

        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
        if (searchName.equals(KWS_SEARCH)) {
            recognizer.startListening(searchName);
        }
//            else
//            recognizer.startListening(searchName, 10000);

    }

    @Override
    public void onBeginningOfSpeech() {
        // nothing
    }

    /**
     * We stop recognizer here to get a final result
     */
    @Override
    public void onEndOfSpeech() {
        if (!recognizer.getSearchName().equals(KWS_SEARCH)) {
            switchSearch(KWS_SEARCH);
        }
    }

    /**
     * In partial result we get quick updates about current hypothesis. In
     * keyword spotting mode we can react here, in other modes we need to wait
     * for final result in onResult.
     */
    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        String text = hypothesis != null ? hypothesis.getHypstr() : null;
        if (KEYPHRASE.equals(text)) {
            recognizer.stop();
            Log.w(TAG, "onPartialResult detect:" + text);
            startService(
                    BackGroundProcessServiceControlCommand.createIntentByType(this,
                            BackGroundProcessServiceControlCommand.START_VOICE_RECORD)
            );
        } else {
//            Log.d(TAG, "onPartialResult " + text);
        }
    }

    /**
     * This callback is called when we stop the recognizer.
     */
    @Override
    public void onResult(Hypothesis hypothesis) {
//        if (hypothesis != null) {
//            String text = hypothesis.getHypstr();
//            Log.w(TAG, "detect:"+text);
//        }
    }

    @Override
    public void onError(Exception e) {
        Log.e(TAG, "onError ", e);
    }

    @Override
    public void onTimeout() {

    }
}
