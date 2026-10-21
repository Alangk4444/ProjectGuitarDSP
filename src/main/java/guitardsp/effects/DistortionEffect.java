package guitardsp.effects;

public class DistortionEffect implements Effect {
    private float gain = 2.0f;

    @Override
    public void process(float[] buffer, int offset, int length) { //buffer = audio samples
        for (int i = offset; i < offset + length; i++) {     // offset = index of buffer array to start processing
            float x = buffer[i] * gain;                     // length =  //how many samples to process from offset
            buffer[i] = Math.max(-1f, Math.min(1f, x)); //hard clip
        }


    }

    @Override
    public String getName(){
        return "Distortion";
    }

    public void setGain(float gain){
        this.gain = gain;
    }
}
