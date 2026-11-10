package guitardsp.ui.controller;

import guitardsp.audio.AudioEngine;
import guitardsp.dsp.Pedalboard;
import guitardsp.effects.Effect;
import guitardsp.effects.Parameter;
import guitardsp.effects.PedalCatalog;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class MainController {
    private static final String MIX_ALL = "All channels (mix)";

    @FXML private StackPane settingsOverlay;
    @FXML private ComboBox<String> inputBox;
    @FXML private ComboBox<String> channelBox;
    @FXML private ComboBox<String> outputBox;
    @FXML private Slider bufferSlider;
    @FXML private Label bufferLabel;

    @FXML private StackPane boardArea;
    @FXML private HBox boardBox;
    @FXML private VBox paletteBox;
    @FXML private Label boardHint;
    @FXML private Slider masterSlider;
    @FXML private Label masterLabel;

    private static final String CARD_STYLE =
            "-fx-background-color: #3a1a10; -fx-background-radius: 8; -fx-border-color: #c98a6b; "
            + "-fx-border-radius: 8; -fx-padding: 10 14 10 14;";

    private AudioEngine engine;
    private Pedalboard board;
    private final List<Node> placedCards = new ArrayList<>();   // pedal cards on the board, in chain order

    public void setEngine(AudioEngine engine){
        this.engine = engine;
        masterSlider.setValue(engine.getMasterVolume() * 100);
    }

    public void setPedalboard(Pedalboard board){
        this.board = board;
        refreshBoard();
    }

    @FXML
    private void initialize(){
        // clicking the dark area outside the panel closes settings
        settingsOverlay.setOnMouseClicked(e -> {
            if (e.getTarget() == settingsOverlay) closeSettings();
        });

        // right-hand list: one draggable card per pedal in the catalog
        for (String name : PedalCatalog.names()) {
            Label card = new Label(name);
            card.setMaxWidth(Double.MAX_VALUE);
            card.setStyle(CARD_STYLE + " -fx-text-fill: white; -fx-cursor: hand;");
            card.setOnDragDetected(e -> startDrag(card, card, "new:" + name, TransferMode.COPY, e));
            paletteBox.getChildren().add(card);
        }

        // the centre board accepts drops
        boardArea.setOnDragOver(this::onBoardDragOver);
        boardArea.setOnDragDropped(this::onBoardDragDropped);

        // master volume: slider is a percentage, 100 = unchanged
        masterSlider.valueProperty().addListener((obs, old, val) -> {
            masterLabel.setText(String.format("%d%%", Math.round(val.doubleValue())));
            if (engine != null) engine.setMasterVolume(val.floatValue() / 100f);
        });

        // slider value is log2 of the buffer size in samples (6 -> 64 ... 11 -> 2048)
        bufferSlider.valueProperty().addListener((obs, old, val) -> updateBufferLabel());

        // when a different input device is picked, list that device's channels
        inputBox.valueProperty().addListener((obs, old, device) -> {
            if (device != null) fillChannels(device, device.equals(engine.getInputDevice()) ? engine.getInputChannel() : -1);
        });
    }

    // source = node the drag starts from, view = node whose picture is shown under the cursor
    private void startDrag(Node source, Node view, String payload, TransferMode mode, javafx.scene.input.MouseEvent e){
        Dragboard db = source.startDragAndDrop(mode);
        ClipboardContent content = new ClipboardContent();
        content.putString(payload);
        db.setContent(content);
        db.setDragView(view.snapshot(null, null));
        e.consume();
    }

    private void onBoardDragOver(DragEvent e){
        if (e.getDragboard().hasString()) e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
        e.consume();
    }

    private void onBoardDragDropped(DragEvent e){
        Dragboard db = e.getDragboard();
        boolean done = false;

        if (board != null && db.hasString()) {
            String payload = db.getString();
            int index = dropIndex(e.getSceneX());

            if (payload.startsWith("new:")) {
                Effect effect = PedalCatalog.create(payload.substring(4));
                if (effect != null) {
                    board.insert(index, effect);
                    done = true;
                }
            } else if (payload.startsWith("move:")) {
                board.move(Integer.parseInt(payload.substring(5)), index);
                done = true;
            }
            refreshBoard();
        }

        e.setDropCompleted(done);
        e.consume();
    }

    // the dropped pedal goes after every card whose centre is to the left of the cursor
    private int dropIndex(double sceneX){
        int index = 0;
        for (Node card : placedCards) {
            var bounds = card.localToScene(card.getBoundsInLocal());
            double centreX = (bounds.getMinX() + bounds.getMaxX()) / 2;
            if (centreX < sceneX) index++;
        }
        return index;
    }

    // rebuilds the centre board from the pedalboard's current order
    private void refreshBoard(){
        boardBox.getChildren().clear();
        placedCards.clear();
        if (board == null) return;

        List<Effect> effects = board.getEffects();
        for (int i = 0; i < effects.size(); i++) {
            final int index = i;

            Label name = new Label(effects.get(i).getName());
            name.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

            Button remove = new Button("✕");
            remove.setStyle("-fx-background-color: transparent; -fx-text-fill: #e8c9b8; -fx-cursor: hand;");
            remove.setOnAction(e -> {
                board.remove(index);
                refreshBoard();
            });

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox header = new HBox(8, name, spacer, remove);
            header.setAlignment(Pos.CENTER);
            header.setStyle("-fx-cursor: hand;");

            VBox card = new VBox(6, header);
            card.setMinWidth(210);
            card.setStyle(CARD_STYLE);
            // only the header starts a drag, so the sliders below stay usable
            header.setOnDragDetected(e -> startDrag(header, card, "move:" + index, TransferMode.MOVE, e));

            // one slider per adjustable setting of this pedal
            for (Parameter p : effects.get(i).getParameters()) {
                Label label = new Label(p.getName());
                label.setMinWidth(55);
                label.setStyle("-fx-text-fill: white;");

                Slider slider = new Slider(p.getMin(), p.getMax(), p.get());
                slider.setPrefWidth(100);

                Label value = new Label(String.format("%.2f", p.get()));
                value.setMinWidth(36);
                value.setStyle("-fx-text-fill: #e8c9b8;");

                slider.valueProperty().addListener((obs, old, val) -> {
                    p.set(val.doubleValue());
                    value.setText(String.format("%.2f", val.doubleValue()));
                });

                HBox row = new HBox(6, label, slider, value);
                row.setAlignment(Pos.CENTER_LEFT);
                card.getChildren().add(row);
            }

            if (i > 0) {
                Label arrow = new Label("➜");
                arrow.setStyle("-fx-text-fill: #e8c9b8; -fx-font-size: 16px;");
                boardBox.getChildren().add(arrow);
            }
            boardBox.getChildren().add(card);
            placedCards.add(card);
        }
        boardHint.setVisible(effects.isEmpty());
    }

    // channelBox items: "All channels (mix)", "Channel 1", "Channel 2", ...
    private void fillChannels(String device, int selectedChannel){
        int count = AudioEngine.getInputChannelCount(device);
        channelBox.getItems().clear();
        channelBox.getItems().add(MIX_ALL);
        for (int i = 1; i <= count; i++) channelBox.getItems().add("Channel " + i);
        int index = (selectedChannel >= 0 && selectedChannel < count) ? selectedChannel + 1 : 0;
        channelBox.getSelectionModel().select(index);
    }

    @FXML
    public void settings(ActionEvent e){
        inputBox.getItems().setAll(AudioEngine.getInputDevices());
        outputBox.getItems().setAll(AudioEngine.getOutputDevices());
        outputBox.setValue(engine.getOutputDevice());

        inputBox.setValue(null);   // makes sure the listener fires even if the device is unchanged
        inputBox.setValue(engine.getInputDevice());

        bufferSlider.setValue(Math.log(engine.getBufferSamples()) / Math.log(2));
        updateBufferLabel();
        settingsOverlay.setVisible(true);
    }

    private int sliderSamples(){
        return 1 << (int) Math.round(bufferSlider.getValue());
    }

    private void updateBufferLabel(){
        int samples = sliderSamples();
        double ms = samples * 1000.0 / 44100;
        bufferLabel.setText(String.format("Buffer: %d samples (%.1f ms per block)", samples, ms));
    }

    @FXML
    private void applySettings(){
        int channel = channelBox.getSelectionModel().getSelectedIndex() - 1;   // -1 = mix all, 0 = channel 1, ...
        engine.setDevices(inputBox.getValue(), channel, outputBox.getValue());
        engine.setBufferSamples(sliderSamples());
        engine.restart();
        closeSettings();
    }

    @FXML
    private void closeSettings(){
        settingsOverlay.setVisible(false);
    }
}
