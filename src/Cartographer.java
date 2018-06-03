import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


/**
 * The map of a dungeon adventure game.
 */
public class Cartographer extends Canvas {

    // Size (in pixels) of the rooms drawn to the map
    private static final int ROOM_SIZE = 50;
    private static final int ROOM_WIDTH = ROOM_SIZE / 2;
    // Width (in pixels) of the doors in the map
    private static final int DOOR_WIDTH = 5;

    // Canvas graphics renderer
    private GraphicsContext graphics;

    // Map cardinal directions to positions exits in that direction should be
    // drawn relative to the rooms position
    private Map<String, int[]> exitPositions = new HashMap<>();

    // Map of the current level
    private BoundsMapper map;
    // Offsets to account for rooms in negative coordinates
    private int xOffset;
    private int yOffset;

    /**
     * Construct a new Cartographer representing all rooms expanding from the
     * starting room.
     *
     * @param start The room to start drawing from
     */
    public Cartographer(Room start) {
        super();

        // Load the positions exits should be drawn
        exitPositions.put("North", new int[]{ROOM_WIDTH, -DOOR_WIDTH,
                ROOM_WIDTH, DOOR_WIDTH});
        exitPositions.put("East", new int[]{ROOM_SIZE - DOOR_WIDTH,
                ROOM_WIDTH, ROOM_SIZE + DOOR_WIDTH, ROOM_WIDTH});
        exitPositions.put("West", new int[]{-DOOR_WIDTH, ROOM_WIDTH,
                DOOR_WIDTH, ROOM_WIDTH});
        exitPositions.put("South", new int[]{ROOM_WIDTH,
                ROOM_SIZE - DOOR_WIDTH, ROOM_WIDTH,
                ROOM_SIZE + DOOR_WIDTH});

        graphics = getGraphicsContext2D();

        // Map out the map from the starting room
        map = new BoundsMapper(start);
        map.walk();

        // Calculate and set the width and height of the canvas
        int width = (map.xMax - map.xMin + 1) * ROOM_SIZE;
        int height = (map.yMax - map.yMin + 1) * ROOM_SIZE;
        setWidth(width);
        setHeight(height);

        // Calculate the offset to account for negative coordinates
        xOffset = Math.abs(map.xMin) * ROOM_SIZE;
        yOffset = Math.abs(map.yMin) * ROOM_SIZE;
    }

    /**
     * Draw the representation of a Room to a JavaFX graphics context.
     *
     * @param room The room to draw to the graphics context
     * @param location The location relative to the canvas to draw the room
     */
    private void drawRoom(Room room, Pair location) {
        // Calculate the pixel position the room should be drawn
        int startX = (location.x * ROOM_SIZE) + xOffset;
        int startY = (location.y * ROOM_SIZE) + yOffset;

        // Draw a rectangle representing the room
        graphics.strokeRect(startX, startY, ROOM_SIZE, ROOM_SIZE);

        // Draw the doors for each room exit
        for (String direction : room.getExits().keySet()) {
            int[] position = exitPositions.get(direction);
            if (position == null) {
                continue;
            }
            graphics.strokeLine(position[0] + startX, position[1] + startY,
                    position[2] + startX, position[3] + startY);
        }

        // Draw representations for each of the items in the room
        for (Thing thing : room.getContents()) {
            // Draw a player representation
            if (thing instanceof Player) {
                graphics.strokeText("@", startX + 4, startY + 12);
            }
            // Draw a treasure representation
            if (thing instanceof Treasure) {
                graphics.strokeText("$", startX + 4 + ROOM_WIDTH,
                        startY + 12);
            }
            // Draw a critter representation
            if (thing instanceof Critter) {
                Critter critter = (Critter) thing;
                if (critter.isAlive()) {
                    // Draw an alive critter
                    graphics.strokeText("M", startX + 4,
                            startY + 12 + ROOM_WIDTH);
                } else {
                    // Draw a dead critter
                    graphics.strokeText("m", startX + 4 + ROOM_WIDTH,
                            startY + 12 + ROOM_WIDTH);
                }
            }
        }
    }

    /**
     * Redraw the JavaFX display of the map.
     */
    public void update() {
        // Clear the previous map view
        graphics.clearRect(0, 0, getWidth(), getHeight());

        // Draw all the rooms in the map
        for (Entry<Room, Pair> entry : map.coords.entrySet()) {
            drawRoom(entry.getKey(), entry.getValue());
        }
    }

}
