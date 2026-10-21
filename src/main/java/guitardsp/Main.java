package guitardsp;

import javafx.application.Application;
import javafx.stage.Stage;

import guitardsp.audio.AudioEngine;
import guitardsp.dsp.Pedalboard;
import guitardsp.effects.DistortionEffect;

public class Main extends Application {

    @Override
    public void start(Stage stage) {

        // Temporary UI until you build MainView
        stage.setTitle("Guitar DSP Pedalboard");
        stage.show();

        // Build pedalboard
        Pedalboard board = new Pedalboard();
        board.addEffect(new DistortionEffect()); // first effect

        // Start audio engine on background thread
        AudioEngine engine = new AudioEngine(board);

        Thread audioThread = new Thread(() -> {
            try {
                engine.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        audioThread.setDaemon(true); // stops when app closes
        audioThread.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
