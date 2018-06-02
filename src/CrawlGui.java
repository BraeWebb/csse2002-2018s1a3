import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

public class CrawlGui extends Application {

    private GridPane directionButtons;
    private GridPane actionButtons;
    private Button[] buttons = new Button[10];
    private static final int DIRECTION_COUNT = 4;
    private static final String[] BUTTONS = {
            "North", "East", "South", "West",
            "Look", "Examine", "Drop", "Take", "Fight", "Save"
    };
    private static final Pair[] BUTTON_POSITIONS = {
            new Pair(1, 0), new Pair(0, 1), new Pair(2, 1),
            new Pair(1, 2), new Pair(0, 3), new Pair(1, 3),
            new Pair(0, 4), new Pair(1, 4), new Pair(0, 5),
            new Pair(0, 6)
    };

    private TextArea output;

    private void loadButtons() {
        Pair buttonLocation;
        for (int i = 0; i < DIRECTION_COUNT; i++) {
            buttons[i] = new Button(BUTTONS[i]);
            buttonLocation = BUTTON_POSITIONS[i];
            directionButtons.add(buttons[i], buttonLocation.x, buttonLocation.y);
        }
        for (int i = DIRECTION_COUNT; i < BUTTONS.length; i++) {
            buttons[i] = new Button(BUTTONS[i]);
            buttonLocation = BUTTON_POSITIONS[i];
            actionButtons.add(buttons[i], buttonLocation.x, buttonLocation.y);
        }
    }

    private void display(String message) {
        output.setText(output.getText() + "\n" + message);
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        List<String> paramters = getParameters().getRaw();

        if (paramters.size() != 1) {
            System.err.println("Usage: java CrawlGui mapname");
            System.exit(1);
        }

        Object[] map = MapIO.loadMap(paramters.get(0));

        if (map == null) {
            System.err.println("Unable to load file");
            System.exit(2);
        }

        Player player = (Player) map[0];
        Room room = (Room) map[1];

        BorderPane window = new BorderPane();

        VBox buttonPane = new VBox();
        directionButtons = new GridPane();
        actionButtons = new GridPane();
        buttonPane.getChildren().add(directionButtons);
        buttonPane.getChildren().add(actionButtons);

        loadButtons();

        window.setRight(buttonPane);

        output = new TextArea("You find yourself in " + room.getDescription());
        output.setEditable(false);
        window.setBottom(output);

        primaryStage.setScene(new Scene(window));
        primaryStage.show();
    }
}
