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

    // Amount of direction buttons
    private static final int DIRECTION_COUNT = 4;

    // Labels of the buttons to load
    private static final String[] BUTTONS = {
            "North", "West", "East", "South",
            "Look", "Examine", "Drop", "Take", "Fight", "Save"
    };
    // Location in the grid of the buttons to load
    private static final Pair[] BUTTON_POSITIONS = {
            new Pair(1, 0), new Pair(0, 1), new Pair(2, 1),
            new Pair(1, 2), new Pair(0, 3), new Pair(1, 3),
            new Pair(0, 4), new Pair(1, 4), new Pair(0, 5),
            new Pair(0, 6)
    };
    // Event handlers to call when buttons are pressed
    private final EventHandler[] BUTTON_CALLBACKS = {
            (event -> look()),
            (event -> examine()),
            (event -> drop()),
            (event -> take()),
            (event -> fight()),
            (event -> save()),
    };

    // Array of direction and action buttons
    private Button[] buttons = new Button[10];
    // Output area where messages should be displayed
    private TextArea output;
    // Map of the current game level
    private Cartographer map;

    // Room that a level start from
    private Room startRoom;
    // Room the player is currently in
    private Room currentRoom;
    // The player of the current game
    private Player player;

    /**
     * Create a new grid pane and load direction buttons into the frame with
     * their action event handlers.
     *
     * @return The grid the buttons are stored into
     */
    private GridPane loadDirectionButtons() {
        GridPane directionButtons = new GridPane();
        Pair buttonLocation;

        // Create buttons and assign callbacks
        for (int i = 0; i < DIRECTION_COUNT; i++) {
            buttons[i] = new Button(BUTTONS[i]);
            // Assign a button callback
            buttons[i].setOnAction((event) ->
                    move(((Button) event.getSource()).getText()));
            // Add button to the grid
            buttonLocation = BUTTON_POSITIONS[i];
            directionButtons.add(buttons[i], buttonLocation.x, buttonLocation.y);
        }

        return directionButtons;
    }

    /**
     * Create a new grid pane and load action buttons into the frame with
     * their action event handlers.
     *
     * @return The grid the buttons are stored into
     */
    private GridPane loadActionButtons() {
        GridPane actionButtons = new GridPane();
        Pair buttonLocation;

        // Create buttons and assign callbacks
        for (int i = DIRECTION_COUNT; i < BUTTONS.length; i++) {
            buttons[i] = new Button(BUTTONS[i]);
            // Assign a button callback
            buttons[i].setOnAction(BUTTON_CALLBACKS[i - DIRECTION_COUNT]);
            // Add button to the grid
            buttonLocation = BUTTON_POSITIONS[i];
            actionButtons.add(buttons[i], buttonLocation.x, buttonLocation.y);
        }
        return actionButtons;
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
        // Make a dialog input window
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(question);
        dialog.setHeaderText(null);
        dialog.setGraphic(null);

        // Ask and wait for input
        Optional<String> result = dialog.showAndWait();
        // Return input if given
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
        // Check a room in the direction exists
        if (nextRoom == null) {
            display("No door that way");
            return;
        }
        // Attempt to leave the current room
        if (!currentRoom.leave(player)) {
            display("Something prevents you from leaving");
            return;
        }

        // Move to the next room
        nextRoom.enter(player);
        currentRoom = nextRoom;
        display("You enter " + currentRoom.getDescription());

        // Update the display
        map.update();
    }

    /**
     * Display, using the display method, the surroundings of the player
     * and the contents of the player's inventory.
     */
    private void look() {
        display(currentRoom.getDescription() + " - you see:");
        // Display what items are found in the current room
        for (Thing thing : currentRoom.getContents()) {
            display(" " + thing.getShortDescription());
        }
        display("You are carrying:");
        // Display what items are found in the current room
        // and calculate the total worth of the items
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
        // Attempt to examine an item in the players inventory
        Thing thing = find(item, player.getContents());
        if (thing != null) {
            display(thing.getDescription());
            return;
        }
        // Attempt to examine an item in the current room
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
        // Drop an item from the player
        Thing thing = player.drop(item);
        if (thing != null) {
            // Add the item to the current room
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
        // Find an item to take
        Thing thing = find(item, currentRoom.getContents(), true, false);

        // Ensure that the thing can be taken and can leave
        if (thing instanceof Mob && ((Mob) thing).isAlive()) {
            return;
        }
        if (!currentRoom.leave(thing)) {
            return;
        }

        // Collect the thing into the players inventory
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
        // Find an item to fight
        Thing thing = find(item, currentRoom.getContents(), false, true);
        Critter critter = (Critter) thing;

        // Check critter can be fought with
        if (critter == null || !critter.isAlive()) {
            return;
        }

        player.fight(critter);

        // Display appropriate message
        if (critter.isAlive()) {
            display("Game over");
            // Disable buttons when the game is over
            for (Button button : buttons) {
                button.setDisable(true);
            }
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
        // Attempt to save from the start room to the given filename
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
            // Skip over players
            if (skip && thing instanceof Player) {
                continue;
            }
            // Only count critters if critter flag is true
            if (critter && !(thing instanceof Critter)) {
                continue;
            }
            // Return a matching thing
            if (thing.getShortDescription().equals(description)) {
                return thing;
            }
        }
        return null;
    }

    /**
     * Launch the JavaFX Application.
     *
     * @param args Command line arguments passed to the program
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Start the JavaFX interface by loading in the appropriate components.
     *
     * @param primaryStage The primary stage for this application,
     *                     onto which the application scene can be set
     */
    @Override
    public void start(Stage primaryStage) {
        List<String> parameters = getParameters().getRaw();

        // Ensure a map filename has been provided
        if (parameters.size() != 1) {
            System.err.println("Usage: java CrawlGui mapname");
            System.exit(1);
        }

        // Attempt to load the given filename
        Object[] data = MapIO.loadMap(parameters.get(0));

        // Ensure the map can be successfully loaded
        if (data == null) {
            System.err.println("Unable to load file");
            System.exit(2);
        }

        // Load appropriate data from the file into variables
        player = (Player) data[0];
        currentRoom = startRoom = (Room) data[1];
        currentRoom.enter(player);

        BorderPane window = new BorderPane();

        // Create the buttons pane on the right side
        VBox buttonPane = new VBox();
        buttonPane.getChildren().add(loadDirectionButtons());
        buttonPane.getChildren().add(loadActionButtons());
        window.setRight(buttonPane);

        // Create the output text area on the bottom side
        output = new TextArea("You find yourself in "
                + currentRoom.getDescription());
        output.setEditable(false);
        window.setBottom(output);

        // Create a map view into the center of the window
        map = new Cartographer(currentRoom);
        map.update();
        window.setCenter(map);

        // Load and show the JavaFx scene
        primaryStage.setScene(new Scene(window));
        primaryStage.setTitle("Crawl - Explore");
        primaryStage.show();
    }
}
