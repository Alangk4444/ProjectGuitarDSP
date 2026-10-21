package guitardsp.audio;

import guitardsp.dsp.Pedalboard;
import javax.sound.sampled.*;

public class AudioEngine {
    private final Pedalboard pedalboard;

    public AudioEngine(Pedalboard pedalboard){
        this.pedalboard = pedalboard;
    }

    public void start() throws Exception{
        AudioFormat format = new AudioFormat(44100, 16, 1, true, false);

        TargetDataLine input = AudioSystem.getTargetDataLine(format);
        input.open(format);
        input.start();

        SourceDataLine output = AudioSystem.getSourceDataLine(format);
        output.open(format);
        output.start();

        byte[] buffer = new byte[2048]; //raw bytes from audio interface
        float[] floatBuffer = new float[1024];    //each audio sample is 2 bytes

        //loop that runs forever
        while(true){
            int bytesRead = input.read(buffer, 0, buffer.length);

            //converting the bytes to floats
            for (int i = 0, j = 0; i < bytesRead; i +=2, j++){
                //buffer[i] = low byte, buffer[i+1] = high byte
                //buffer[i] & 0xff (255 in deecimal) to get lower byte
                //buffer[i+1] << 8 is a logical shift left by a byte
                //bit wise or | merges them together

                //16-bit audio ranges from -32768 to 32767
                //divide by 32768 so it ranges from -1.0f to 1.0f

                int sample = (buffer[i] & 0xff) | (buffer[i + 1] << 8);
                floatBuffer[j] = sample / 32768f;
            }

            //applying effects
            pedalboard.process(floatBuffer, 0, floatBuffer.length);

            //converting floats back to bytes
            for(int i = 0, j = 0; i < floatBuffer.length; j++, i +=2){
                int sample = (int) (floatBuffer[j] * 32767);
                buffer [i] = (byte) (sample & 0xff);
                buffer [i+1] = (byte) ((sample>>8) & 0xff);
            }

            output.write(buffer, 0, bytesRead);
        }

    }
}
