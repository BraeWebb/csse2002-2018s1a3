import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Cartographer extends Canvas {

    private GraphicsContext graphics;
    private Room start;

    private static final int ROOM_SIZE = 50;
    private static final int STROKE_WIDTH = 5;

    private Map<String, int[]> exitPositions = new HashMap<>();

    private int xOffset;
    private int yOffset;

    public Cartographer(Room start) {
        super();

        exitPositions.put("North", new int[]{ROOM_SIZE/2, -STROKE_WIDTH,
                ROOM_SIZE/2, STROKE_WIDTH});
        exitPositions.put("East", new int[]{ROOM_SIZE-STROKE_WIDTH, ROOM_SIZE/2,
                ROOM_SIZE+STROKE_WIDTH, ROOM_SIZE/2});
        exitPositions.put("West", new int[]{-STROKE_WIDTH, ROOM_SIZE/2,
                STROKE_WIDTH, ROOM_SIZE/2});
        exitPositions.put("South", new int[]{ROOM_SIZE/2, ROOM_SIZE-STROKE_WIDTH,
                ROOM_SIZE/2, ROOM_SIZE+STROKE_WIDTH});

        graphics = getGraphicsContext2D();
        this.start = start;
    }

    public void drawRoom(Room room, Pair location) {
        int startX = (location.x * ROOM_SIZE) + xOffset;
        int startY = (location.y * ROOM_SIZE) + yOffset;
        graphics.strokeRect(startX, startY, ROOM_SIZE, ROOM_SIZE);

        for (String direction : room.getExits().keySet()) {
            int[] position = exitPositions.get(direction);
            if (position == null) {
                continue;
            }
            graphics.strokeLine(position[0] + startX, position[1] + startY,
                    position[2] + startX, position[3] + startY);
        }

        for (Thing thing : room.getContents()) {
            if (thing instanceof Player) {
                graphics.strokeText("@", startX + 4, startY + 12);
            }
            if (thing instanceof Treasure) {
                graphics.strokeText("$", startX + 4 + ROOM_SIZE/2, startY + 12);
            }
            if (thing instanceof Critter) {
                Critter critter = (Critter) thing;
                if (critter.isAlive()) {
                    graphics.strokeText("M", startX + 4, startY + 12 + ROOM_SIZE / 2);
                } else {
                    graphics.strokeText("m", startX + 4 + ROOM_SIZE/2, startY + 12 + ROOM_SIZE / 2);
                }
            }
        }
    }

    public void update() {
        graphics = getGraphicsContext2D();
        BoundsMapper map = new BoundsMapper(start);
        map.walk();

        int width = (map.xMax - map.xMin + 1) * ROOM_SIZE;
        int height = (map.yMax - map.yMin + 1) * ROOM_SIZE;

        setWidth(width);
        setHeight(height);

        graphics.clearRect(0, 0, getWidth(), getHeight());

        xOffset = Math.abs(map.xMin) * ROOM_SIZE;
        yOffset = Math.abs(map.yMin) * ROOM_SIZE;

        for (Entry<Room, Pair> entry : map.coords.entrySet()) {
            drawRoom(entry.getKey(), entry.getValue());
        }
    }

}
