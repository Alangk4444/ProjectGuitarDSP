package guitardsp.effects;

import java.util.List;

public interface Effect {
    void process(float[] buffer, int offset, int length);
    String getName();

    /** The settings the UI should show for this pedal. */
    default List<Parameter> getParameters() {
        return List.of();
    }
}
