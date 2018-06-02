import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;

import java.util.List;
import java.util.Optional;

public class CrawlActions {

    private TextArea output;

    private Player player;
    private Room startRoom;
    private Room currentRoom;

    public CrawlActions(Player player, Room startRoom, TextArea output) {
        this.output = output;
        this.player = player;
        this.startRoom = startRoom;
        this.currentRoom = startRoom;
    }

    public void setRoom(Room nextRoom) {
        currentRoom = nextRoom;
    }

    public Room getRoom() {
        return currentRoom;
    }

    private void display(String message) {
        output.appendText("\n" + message);
    }

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

    public void look() {
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

    public void examine() {
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

    public void drop() {
        String item = ask("Item to drop?");
        Thing thing = player.drop(item);
        if (thing != null) {
            currentRoom.enter(thing);
        }
    }

    public void take() {
        String item = ask("Take what?");
        Thing thing = find(item, currentRoom.getContents(), true, false);
        if (thing instanceof Mob && ((Mob) thing).isAlive()) {
            return;
        }
        if (!currentRoom.leave(thing)) {
            return;
        }
        player.add(thing);
    }

    public void fight() {
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
    }

    public void save() {
        String file = ask("Save filename?");
        if (MapIO.saveMap(startRoom, file)) {
            display("Saved");
        } else {
            display("Unable to save");
        }
    }

    private Thing find(String description, List<Thing> contents) {
        return find(description, contents, false, false);
    }

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
}
