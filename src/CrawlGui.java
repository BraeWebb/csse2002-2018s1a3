import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;

/**
 * JavaFX Application for a game where players travel through a map collecting
 * treasure and fighting monsters.
 *
 * @author Brae Webb
 */
public class CrawlGui extends Application {

    private GridPane directionButtons;
    private GridPane actionButtons;
    private Button[] buttons = new Button[10];
    private static final int DIRECTION_COUNT = 4;
    private static final String[] BUTTONS = {
            "North", "West", "East", "South",
            "Look", "Examine", "Drop", "Take", "Fight", "Save"
    };
    private static final Pair[] BUTTON_POSITIONS = {
            new Pair(1, 0), new Pair(0, 1), new Pair(2, 1),
            new Pair(1, 2), new Pair(0, 3), new Pair(1, 3),
            new Pair(0, 4), new Pair(1, 4), new Pair(0, 5),
            new Pair(0, 6)
    };
    private EventHandler[] buttonCallbacks;
    private CrawlActions actions;

    private TextArea output;

    private Cartographer map;
    private Player player;

    private void loadButtons() {
        Pair buttonLocation;
        for (int i = 0; i < DIRECTION_COUNT; i++) {
            buttons[i] = new Button(BUTTONS[i]);
            buttons[i].setOnAction((event) ->
                    move(((Button) event.getSource()).getText()));
            buttonLocation = BUTTON_POSITIONS[i];
            directionButtons.add(buttons[i], buttonLocation.x, buttonLocation.y);
        }
        for (int i = DIRECTION_COUNT; i < BUTTONS.length; i++) {
            buttons[i] = new Button(BUTTONS[i]);
            buttons[i].setOnAction(buttonCallbacks[i - DIRECTION_COUNT]);
            buttonLocation = BUTTON_POSITIONS[i];
            actionButtons.add(buttons[i], buttonLocation.x, buttonLocation.y);
        }
    }

    private void display(String message) {
        output.appendText("\n" + message);
    }

    private void move(String direction) {
        Room nextRoom = actions.getRoom().getExits().get(direction);
        if (nextRoom == null) {
            display("No door that way");
            return;
        }
        if (!actions.getRoom().leave(player)) {
            display("Something prevents you from leaving");
            return;
        }
        nextRoom.enter(player);
        actions.setRoom(nextRoom);
        display("You enter");
        map.update();
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

        Object[] data = MapIO.loadMap(paramters.get(0));

        if (data == null) {
            System.err.println("Unable to load file");
            System.exit(2);
        }

        player = (Player) data[0];
        Room startRoom = (Room) data[1];
        startRoom.enter(player);

        output = new TextArea("You find yourself in "
                + startRoom.getDescription());
        output.setEditable(false);

        actions = new CrawlActions(player, startRoom, output);

        buttonCallbacks = new EventHandler[]{
                (event -> actions.look()),
                (event -> actions.examine()),
                (event -> actions.drop()),
                (event -> actions.take()),
                (event -> actions.fight()),
                (event -> actions.save()),
        };

        BorderPane window = new BorderPane();

        VBox buttonPane = new VBox();
        directionButtons = new GridPane();
        actionButtons = new GridPane();
        buttonPane.getChildren().add(directionButtons);
        buttonPane.getChildren().add(actionButtons);

        loadButtons();

        window.setRight(buttonPane);

        window.setBottom(output);
        window.setBottom(output);

        map = new Cartographer(startRoom);
        map.update();
        window.setCenter(map);

        primaryStage.setScene(new Scene(window));
        primaryStage.show();
    }
}
