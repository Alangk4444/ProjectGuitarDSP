package guitardsp.dsp;

import guitardsp.effects.Effect;
import java.util.ArrayList;
import java.util.List;

public class Pedalboard {
    // The audio thread reads this list while the UI thread changes it, so every change
    // builds a new immutable list and swaps it in.
    private volatile List<Effect> chain = List.of();

    public void addEffect(Effect e){
        insert(Integer.MAX_VALUE, e);
    }

    public synchronized void insert(int index, Effect e){
        List<Effect> copy = new ArrayList<>(chain);
        copy.add(Math.max(0, Math.min(index, copy.size())), e);
        chain = List.copyOf(copy);
    }

    public synchronized void remove(int index){
        if (index < 0 || index >= chain.size()) return;
        List<Effect> copy = new ArrayList<>(chain);
        copy.remove(index);
        chain = List.copyOf(copy);
    }

    /** Moves the effect at 'from' so it lands at insertion position 'to' (a position in the list before the move). */
    public synchronized void move(int from, int to){
        if (from < 0 || from >= chain.size()) return;
        List<Effect> copy = new ArrayList<>(chain);
        Effect e = copy.remove(from);
        if (to > from) to--;
        copy.add(Math.max(0, Math.min(to, copy.size())), e);
        chain = List.copyOf(copy);
    }

    public List<Effect> getEffects(){
        return chain;
    }

    // effects are applied in list order
    public void process(float[] buffer, int offset, int length){
        for (Effect e : chain){
            e.process(buffer, offset, length);
        }
    }
}
