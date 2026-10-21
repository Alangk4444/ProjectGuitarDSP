package guitardsp.effects;

public interface Effect {
    void process(float[] buffer, int offset, int length);
    String getName();
}
