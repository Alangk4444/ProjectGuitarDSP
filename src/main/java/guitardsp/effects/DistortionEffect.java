package guitardsp.effects;

import java.util.List;

public class DistortionEffect implements Effect {
    private static final float SAMPLE_RATE = 44100f;

    private volatile float gain = 4.0f;     // 0..10 on the slider, mapped to 1x..100x (0..40 dB) below
    private volatile float tone = 1.0f;     // 0 = dark, 1 = bright
    private volatile float volume = 1.0f;   // output level

    private float lowpassState = 0f;        // memory for the tone filter (audio thread only)

    @Override
    public void process(float[] buffer, int offset, int length) { //buffer = audio samples
        float g = (float) Math.pow(10.0, gain / 5.0);   // exponential, so each step of the slider sounds even
        float v = volume;

        // tone: one-pole low-pass, cutoff from about 500 Hz (dark) up to 12 kHz (bright)
        float cutoff = (float) (500.0 * Math.pow(24.0, tone));
        float a = 1f - (float) Math.exp(-2.0 * Math.PI * cutoff / SAMPLE_RATE);

        for (int i = offset; i < offset + length; i++) {     // offset = index of buffer array to start processing
            float x = buffer[i] * g;                         // length = how many samples to process from offset
            x = (float) Math.tanh(x);                        // soft clip: rounds the peaks instead of chopping them flat
            lowpassState += a * (x - lowpassState);
            buffer[i] = lowpassState * v;
        }
    }

    @Override
    public String getName(){
        return "Distortion";
    }

    @Override
    public List<Parameter> getParameters() {
        return List.of(
                new Parameter("Gain", 0, 10, () -> gain, v -> gain = (float) v),
                new Parameter("Tone", 0, 1, () -> tone, v -> tone = (float) v),
                new Parameter("Volume", 0, 2, () -> volume, v -> volume = (float) v)
        );
    }

    public void setGain(float gain){
        this.gain = gain;
    }
}
