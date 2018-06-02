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
    private final EventHandler[] BUTTON_CALLBACKS = {
            (event -> look()),
            (event -> examine()),
            (event -> drop()),
            (event -> take()),
            (event -> fight()),
            (event -> save()),
    };

    private TextArea output;

    private Cartographer map;
    private Room startRoom;
    private Room currentRoom;
    private Player player;

    /**
     * Load all the buttons into the appropriate grid and add their
     * action event handlers.
     */
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

    /**
     * Log a message to the text area output of the application.
     *
     * @param message The message to append to the output
     */
    private void display(String message) {
        output.appendText("\n" + message);
    }

    /**
     * Display a popup dialog asking the user for input and return the input.
     *
     * @param question Title of the popup dialog.
     * @return The string the user entered or null if there was no entry.
     */
    private String ask(String question) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(question);
        dialog.setHeaderText(null);
        dialog.setGraphic(null);

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()){
            return result.get();
        }
        return null;
    }

    /**
     * Move the player from the current room to the room in the given direction.
     *
     * Fail and display errors if there is no room in that direction or
     * the player is prevented from leaving the current room.
     *
     * @param direction The direction to walk. One of North, South, East or West
     */
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
        display("You enter " + currentRoom.getDescription());
        map.update();
    }

    /**
     * Display, using the display method, the surroundings of the player
     * and the contents of the player's inventory.
     */
    private void look() {
        display(currentRoom.getDescription() + " - you see:");
        for (Thing thing : currentRoom.getContents()) {
            display(" " + thing.getShortDescription());
        }
        display("You are carrying:");
        double worth = 0;
        for (Thing thing : player.getContents()) {
            display(" " + thing.getShortDescription());
            if (thing instanceof Lootable) {
                worth += ((Lootable) thing).getValue();
            }
        }
        display(String.format("worth %.1f in total", worth));
    }

    /**
     * Ask the user to enter the short description of a thing to examine.
     * Examine the first Thing it can find by displaying the long description.
     */
    private void examine() {
        String item = ask("Examine what?");
        Thing thing = find(item, player.getContents());
        if (thing != null) {
            display(thing.getDescription());
            return;
        }
        thing = find(item, currentRoom.getContents());
        if (thing != null) {
            display(thing.getDescription());
            return;
        }
        display("Nothing found with that name");
    }

    /**
     * Ask the user to enter the short description of a Thing from the players
     * inventory to drop into the current room.
     */
    private void drop() {
        String item = ask("Item to drop?");
        Thing thing = player.drop(item);
        if (thing != null) {
            currentRoom.enter(thing);
        }
        map.update();
    }

    /**
     * Ask the user to enter the short description of a Thing from the current
     * room to place into the players inventory.
     */
    private void take() {
        String item = ask("Take what?");
        Thing thing = find(item, currentRoom.getContents(), true, false);
        if (thing instanceof Mob && ((Mob) thing).isAlive()) {
            return;
        }
        if (!currentRoom.leave(thing)) {
            return;
        }
        player.add(thing);
        map.update();
    }

    /**
     * Ask the user to enter the short description of a critter to fight and
     * begin a fight with that critter if it is found.
     *
     * If a critter with a matching description cannot be found, fail silently.
     */
    private void fight() {
        String item = ask("Fight what?");
        Thing thing = find(item, currentRoom.getContents(), false, true);
        Critter critter = (Critter) thing;

        if (critter == null) {
            return;
        }

        if (critter.isAlive()) {
            player.fight(critter);
        }
        if (critter.isAlive()) {
            display("Game over");
        } else {
            display("You won");
        }
        map.update();
    }

    /**
     * Ask the user to enter a filename to save to and save the current map.
     */
    private void save() {
        String file = ask("Save filename?");
        if (MapIO.saveMap(startRoom, file)) {
            display("Saved");
        } else {
            display("Unable to save");
        }
    }

    /**
     * Find and return a Thing in a list of Thing's based on their short
     * description.
     *
     * @param description The Thing short description to search for
     * @param contents The list of Thing's to search
     * @return The thing that was found or null
     */
    private Thing find(String description, List<Thing> contents) {
        return find(description, contents, false, false);
    }

    /**
     * Find and return a Thing in a list of Thing's based on their short
     * description.
     *
     * @param description The Thing short description to search for
     * @param contents The list of Thing's to search
     * @param skip True iff instances of Player's should be ignored
     * @param critter True iff only instances of Critter should be considered
     * @return The thing that was found or null
     */
    private Thing find(String description, List<Thing> contents,
                       boolean skip, boolean critter) {
        for (Thing thing : contents) {
            if (skip && thing instanceof Player) {
                continue;
            }
            if (critter && !(thing instanceof Critter)) {
                continue;
            }
            if (thing.getShortDescription().equals(description)) {
                return thing;
            }
        }
        return null;
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
        startRoom = (Room) data[1];
        currentRoom = startRoom;
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
