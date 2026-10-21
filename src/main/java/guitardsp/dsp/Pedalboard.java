package guitardsp.dsp;

import guitardsp.effects.Effect;
import java.util.ArrayList;
import java.util.List;

public class Pedalboard {
    private final List<Effect> chain = new ArrayList<>();

    public void addEffect(Effect e){
        chain.add(e);
    }

    public void process(float[] buffer, int offset, int length){
        for (Effect e : chain){
            e.process(buffer, offset, length);
        }
    }
}