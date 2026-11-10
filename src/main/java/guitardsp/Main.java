package guitardsp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.scene.Scene;

import guitardsp.audio.AudioEngine;
import guitardsp.dsp.Pedalboard;
import guitardsp.effects.DistortionEffect;
import guitardsp.ui.controller.MainController;

public class Main extends Application {
    private AudioEngine engine;

    @Override
    public void start(Stage stage) throws Exception {
        // Build pedalboard and audio engine
        Pedalboard board = new Pedalboard();
        board.addEffect(new DistortionEffect()); // first effect
        engine = new AudioEngine(board);

        // Load the UI and give the controller the engine
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/guitardsp/ui/MainView.fxml"));
        Parent root = loader.load();
        MainController controller = loader.getController();
        controller.setEngine(engine);
        controller.setPedalboard(board);

        stage.setScene(new Scene(root));
        stage.setTitle("Guitar DSP Pedalboard");
        stage.show();

        engine.startAsync();
    }

    @Override
    public void stop() {
        engine.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
