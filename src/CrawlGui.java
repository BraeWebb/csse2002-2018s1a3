import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;

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
    private final EventHandler[] BUTTON_CALLBACKS = {
            (event -> look()),
            (event -> look()),
            (event -> look()),
            (event -> look()),
            (event -> look()),
            (event -> look()),
    };

    private TextArea output;

    private Cartographer map;
    private Room currentRoom;
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
            buttons[i].setOnAction(BUTTON_CALLBACKS[i - DIRECTION_COUNT]);
            buttonLocation = BUTTON_POSITIONS[i];
            actionButtons.add(buttons[i], buttonLocation.x, buttonLocation.y);
        }
    }

    private void display(String message) {
        output.setText(output.getText() + "\n" + message);
    }

    private String ask(String title, String question) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setGraphic(null);

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()){
            return result.get();
        }
        return null;
    }

    private void move(String direction) {
        Room nextRoom = currentRoom.getExits().get(direction);
        if (nextRoom == null) {
            display("No door that way");
            return;
        }
        if (!currentRoom.leave(player)) {
            display("Something prevents you from leaving");
            return;
        }
        nextRoom.enter(player);
        currentRoom = nextRoom;
        display("You enter");
        map.update();
    }

    private void look() {
        display(currentRoom.getDescription() + " - you see:");
        for (Thing thing : currentRoom.getContents()) {
            display(" " + thing.getShortDescription());
        }
        display("You are carrying:");
        int worth = 0;
        for (Thing thing : player.getContents()) {
            display(" " + thing.getShortDescription());
            if (thing instanceof Lootable) {
                worth += ((Lootable) thing).getValue();
            }
        }
        display("worth " + worth + " in total");
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
        currentRoom = (Room) data[1];
        currentRoom.enter(player);

        BorderPane window = new BorderPane();

        VBox buttonPane = new VBox();
        directionButtons = new GridPane();
        actionButtons = new GridPane();
        buttonPane.getChildren().add(directionButtons);
        buttonPane.getChildren().add(actionButtons);

        loadButtons();

        window.setRight(buttonPane);

        output = new TextArea("You find yourself in "
                + currentRoom.getDescription());
        output.setEditable(false);
        window.setBottom(output);

        map = new Cartographer(currentRoom);
        map.update();
        window.setCenter(map);

        primaryStage.setScene(new Scene(window));
        primaryStage.show();
    }
}
