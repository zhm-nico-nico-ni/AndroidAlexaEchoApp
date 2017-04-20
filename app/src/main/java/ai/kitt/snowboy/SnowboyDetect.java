
package ai.kitt.snowboy;

public class SnowboyDetect {
    private transient long swigCPtr;
    protected transient boolean swigCMemOwn;

    protected SnowboyDetect(long cPtr, boolean cMemoryOwn) {
        swigCMemOwn = cMemoryOwn;
        swigCPtr = cPtr;
    }

    protected static long getCPtr(SnowboyDetect obj) {
        return (obj == null) ? 0 : obj.swigCPtr;
    }

    protected void finalize() {
        delete();
    }

    public synchronized void delete() {
        if (swigCPtr != 0) {
            if (swigCMemOwn) {
                swigCMemOwn = false;
                snowboyJNI.delete_SnowboyDetect(swigCPtr);
            }
            swigCPtr = 0;
        }
    }

    public SnowboyDetect(String resource_filename, String model_str) {
        this(snowboyJNI.new_SnowboyDetect(resource_filename, model_str), true);
    }

    public boolean Reset() {
        return snowboyJNI.SnowboyDetect_Reset(swigCPtr, this);
    }

    public int RunDetection(String data) {
        return snowboyJNI.SnowboyDetect_RunDetection__SWIG_0(swigCPtr, this, data);
    }

    public int RunDetection(float[] data, int array_length) {
        return snowboyJNI.SnowboyDetect_RunDetection__SWIG_1(swigCPtr, this, data, array_length);
    }

    public int RunDetection(short[] data, int array_length) {
        return snowboyJNI.SnowboyDetect_RunDetection__SWIG_2(swigCPtr, this, data, array_length);
    }

    public int RunDetection(int[] data, int array_length) {
        return snowboyJNI.SnowboyDetect_RunDetection__SWIG_3(swigCPtr, this, data, array_length);
    }

    public void SetSensitivity(String sensitivity_str) {
        snowboyJNI.SnowboyDetect_SetSensitivity(swigCPtr, this, sensitivity_str);
    }

    public String GetSensitivity() {
        return snowboyJNI.SnowboyDetect_GetSensitivity(swigCPtr, this);
    }

    public void SetAudioGain(float audio_gain) {
        snowboyJNI.SnowboyDetect_SetAudioGain(swigCPtr, this, audio_gain);
    }

    public void UpdateModel() {
        snowboyJNI.SnowboyDetect_UpdateModel(swigCPtr, this);
    }

    public int NumHotwords() {
        return snowboyJNI.SnowboyDetect_NumHotwords(swigCPtr, this);
    }

    public void ApplyFrontend(boolean apply_frontend) {
        snowboyJNI.SnowboyDetect_ApplyFrontend(swigCPtr, this, apply_frontend);
    }

    public int SampleRate() {
        return snowboyJNI.SnowboyDetect_SampleRate(swigCPtr, this);
    }

    public int NumChannels() {
        return snowboyJNI.SnowboyDetect_NumChannels(swigCPtr, this);
    }

    public int BitsPerSample() {
        return snowboyJNI.SnowboyDetect_BitsPerSample(swigCPtr, this);
    }

}