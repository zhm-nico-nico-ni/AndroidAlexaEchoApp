package com.ggec.voice.assistservice.wakeword.sphinx;

import android.util.Log;

import com.ggec.voice.assistservice.MyApplication;
import com.ggec.voice.assistservice.audio.SingleAudioRecord;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import edu.cmu.pocketsphinx.Config;
import edu.cmu.pocketsphinx.Decoder;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;

public class SpeechRecognizer {
    protected static final String TAG = SpeechRecognizer.class.getSimpleName();
    private final Decoder decoder;
    private final int sampleRate;
    private final Collection<RecognitionListener> listeners = new HashSet();
    private int bufferSize;
    private Thread recognizerThread;

    public SpeechRecognizer(Config config) throws IOException {
        this.decoder = new Decoder(config);
        this.sampleRate = (int)this.decoder.getConfig().getFloat("-samprate");
        this.bufferSize = SingleAudioRecord.getInstance().getBufferSizeInBytes();
    }

    public void addListener(RecognitionListener listener) {
        synchronized(this.listeners) {
            this.listeners.add(listener);
        }
    }

    public void removeListener(RecognitionListener listener) {
        synchronized(this.listeners) {
            this.listeners.remove(listener);
        }
    }

    public boolean startListening(String searchName) {
        if(null != this.recognizerThread) {
            return false;
        } else {
            Log.i(TAG, String.format("Start recognition \"%s\"", searchName));
            this.decoder.setSearch(searchName);
            this.recognizerThread = new SpeechRecognizer.RecognizerThread();
            this.recognizerThread.start();
            return true;
        }
    }

    public boolean startListening(String searchName, int timeout) {
        if(null != this.recognizerThread) {
            return false;
        } else {
            Log.i(TAG, String.format("Start recognition \"%s\"", searchName));
            this.decoder.setSearch(searchName);
            this.recognizerThread = new SpeechRecognizer.RecognizerThread(timeout);
            this.recognizerThread.start();
            return true;
        }
    }

    private boolean stopRecognizerThread() {
        if(null == this.recognizerThread) {
            return false;
        } else {
            try {
                this.recognizerThread.interrupt();
                this.recognizerThread.join();
            } catch (InterruptedException var2) {
                Thread.currentThread().interrupt();
            }

            this.recognizerThread = null;
            return true;
        }
    }

    public boolean stop() {
        boolean result = this.stopRecognizerThread();
        if(result) {
            Log.i(TAG, "Stop recognition");
            Hypothesis hypothesis = this.decoder.hyp();
            MyApplication.mainHandler.post(new SpeechRecognizer.ResultEvent(hypothesis, true));
        }

        return result;
    }

    public boolean cancel() {
        boolean result = this.stopRecognizerThread();
        if(result) {
            Log.i(TAG, "Cancel recognition");
        }

        return result;
    }

    public Decoder getDecoder() {
        return this.decoder;
    }

//    public void shutdown() {
//        this.recorder.release();
//    }

    public String getSearchName() {
        return this.decoder.getSearch();
    }

//    public void addFsgSearch(String searchName, FsgModel fsgModel) {
//        this.decoder.setFsg(searchName, fsgModel);
//    }
//
//    public void addGrammarSearch(String name, File file) {
//        Log.i(TAG, String.format("Load JSGF %s", new Object[]{file}));
//        this.decoder.setJsgfFile(name, file.getPath());
//    }
//
//    public void addGrammarSearch(String name, String jsgfString) {
//        this.decoder.setJsgfString(name, jsgfString);
//    }
//
//    public void addNgramSearch(String name, File file) {
//        Log.i(TAG, String.format("Load N-gram model %s", new Object[]{file}));
//        this.decoder.setLmFile(name, file.getPath());
//    }

    public void addKeyphraseSearch(String name, String phrase) {
        this.decoder.setKeyphrase(name, phrase);
    }

//    public void addKeywordSearch(String name, File file) {
//        this.decoder.setKws(name, file.getPath());
//    }

//    public void addAllphoneSearch(String name, File file) {
//        this.decoder.setAllphoneFile(name, file.getPath());
//    }

    private class TimeoutEvent extends SpeechRecognizer.RecognitionEvent {
        private TimeoutEvent() {
            super();
        }

        protected void execute(RecognitionListener listener) {
            listener.onTimeout();
        }
    }

    private class OnErrorEvent extends SpeechRecognizer.RecognitionEvent {
        private final Exception exception;

        OnErrorEvent(Exception exception) {
            super();
            this.exception = exception;
        }

        protected void execute(RecognitionListener listener) {
            listener.onError(this.exception);
        }
    }

    private class ResultEvent extends SpeechRecognizer.RecognitionEvent {
        protected final Hypothesis hypothesis;
        private final boolean finalResult;

        ResultEvent(Hypothesis hypothesis, boolean finalResult) {
            super();
            this.hypothesis = hypothesis;
            this.finalResult = finalResult;
        }

        protected void execute(RecognitionListener listener) {
            if(this.finalResult) {
                listener.onResult(this.hypothesis);
            } else {
                listener.onPartialResult(this.hypothesis);
            }

        }
    }

    private class InSpeechChangeEvent extends SpeechRecognizer.RecognitionEvent {
        private final boolean state;

        InSpeechChangeEvent(boolean state) {
            super();
            this.state = state;
        }

        protected void execute(RecognitionListener listener) {
            if(this.state) {
                listener.onBeginningOfSpeech();
            } else {
                listener.onEndOfSpeech();
            }

        }
    }

    private abstract class RecognitionEvent implements Runnable {
        private RecognitionEvent() {
        }

        public void run() {
            RecognitionListener[] emptyArray = new RecognitionListener[0];
            RecognitionListener[] var2 = (RecognitionListener[])SpeechRecognizer.this.listeners.toArray(emptyArray);
            int var3 = var2.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                RecognitionListener listener = var2[var4];
                this.execute(listener);
            }

        }

        protected abstract void execute(RecognitionListener var1);
    }

    private final class RecognizerThread extends Thread {
        private static final int NO_TIMEOUT = -1;
        private int remainingSamples;
        private int timeoutSamples;

        public RecognizerThread(int timeout) {
            if(timeout != -1) {
                this.timeoutSamples = timeout * SpeechRecognizer.this.sampleRate / 1000;
            } else {
                this.timeoutSamples = -1;
            }

            this.remainingSamples = this.timeoutSamples;
        }

        public RecognizerThread() {
            this(-1);
        }

        public void run() {
            SingleAudioRecord.getInstance().startRecording();
            if(!SingleAudioRecord.getInstance().isRecording()) {
                SingleAudioRecord.getInstance().stop();
                IOException ioe = new IOException("Failed to start recording. Microphone might be already in use.");
                MyApplication.mainHandler.post(SpeechRecognizer.this.new OnErrorEvent(ioe));
            } else {
                Log.d(SpeechRecognizer.TAG, "Starting decoding");
                SpeechRecognizer.this.decoder.startUtt();
                short[] buffer = new short[SpeechRecognizer.this.bufferSize];
                boolean inSpeech = SpeechRecognizer.this.decoder.getInSpeech();
                SingleAudioRecord.getInstance().getAudioRecorder().read(buffer, 0, buffer.length);

                while(!interrupted() && (this.timeoutSamples == -1 || this.remainingSamples > 0)) {
                    int nread = SingleAudioRecord.getInstance().getAudioRecorder().read(buffer, 0, buffer.length);
                    if(-1 == nread) {
                        throw new RuntimeException("error reading audio buffer");
                    }

                    if(nread > 0) {
                        SpeechRecognizer.this.decoder.processRaw(buffer, (long)nread, false, false);
                        if(SpeechRecognizer.this.decoder.getInSpeech() != inSpeech) {
                            inSpeech = SpeechRecognizer.this.decoder.getInSpeech();
                            MyApplication.mainHandler.post(SpeechRecognizer.this.new InSpeechChangeEvent(inSpeech));
                        }

                        if(inSpeech) {
                            this.remainingSamples = this.timeoutSamples;
                        }

                        Hypothesis hypothesis = SpeechRecognizer.this.decoder.hyp();
                        MyApplication.mainHandler.post(SpeechRecognizer.this.new ResultEvent(hypothesis, false));
                    }

                    if(this.timeoutSamples != -1) {
                        this.remainingSamples -= nread;
                    }
                }

                SingleAudioRecord.getInstance().stop();
                SpeechRecognizer.this.decoder.endUtt();
                MyApplication.mainHandler.removeCallbacksAndMessages(null);
                if(this.timeoutSamples != -1 && this.remainingSamples <= 0) {
                    MyApplication.mainHandler.post(SpeechRecognizer.this.new TimeoutEvent());
                }

            }
        }
    }
}