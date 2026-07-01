package io.p2vman.graphwarserver.card;

import io.p2vman.graphwarserver.Avatar;
import io.p2vman.graphwarserver.BasicServer;
import io.p2vman.graphwarserver.Team;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.*;

public class LineCardGenerator implements CardGenerator
{
    public static final int CIRCLE_MEAN_RADIUS = 40;
    public static final int CIRCLE_STANDARD_DEVIATION = 25;
    public static final int NUM_CIRCLES_MEAN_VALUE = 15;
    public static final int NUM_CIRCLES_STANDARD_DEVIATION = 7;

    public static final int SOLDIER_RADIUS = 7;
    public static final int SOLDIER_SELECTION_RADIUS = 15;

    public static final int PLANE_LENGTH = 770;
    public static final int PLANE_HEIGHT = 450;

    private final Random random;

    public LineCardGenerator(Random random, BasicServer server)
    {
        this.random = random;

    }

    @Override
    public Vector3i[] generateCircles()
    {
        int numCircles = (int) (random.nextGaussian() * NUM_CIRCLES_STANDARD_DEVIATION + NUM_CIRCLES_MEAN_VALUE);
        if (numCircles < 1) {
            numCircles = 1;
        }

        List<Vector3i> circlesList = new ArrayList<>();
        for (int i = 0; i < numCircles; i++) {
            int z = 0;
            do {
                z = (int) (random.nextGaussian() * CIRCLE_STANDARD_DEVIATION + CIRCLE_MEAN_RADIUS);
            } while (z < 0);

            circlesList.add(new Vector3i(
                    random.nextInt(PLANE_LENGTH),
                    random.nextInt(PLANE_HEIGHT),
                    z
            ));
        }

        int centerX = PLANE_LENGTH / 2;
        int centerY = PLANE_HEIGHT / 2;

        int currentY = 0;
        while (currentY < PLANE_HEIGHT) {
            int radius = 7 + random.nextInt(12);
            circlesList.add(new Vector3i(centerX, currentY, radius));

            int step = (radius * 2) - 4;
            currentY += step;
        }

        int currentX = 0;
        while (currentX < PLANE_LENGTH) {
            int radius = 7 + random.nextInt(12);
            circlesList.add(new Vector3i(currentX, centerY, radius));

            int step = (radius * 2) - 4;
            currentX += step;
        }
        return circlesList.toArray(new Vector3i[0]);
    }

    @Override
    public Vector2i[] generateSoldiers(Vector3i[] circles, Collection<Avatar> players)
    {
        List<Vector2i> soldiers = new ArrayList<>();

        for (Avatar player : players) {
            for (int i = 0; i < player.getNum_soldiers(); i++) {
                soldiers.add(generateSoldier(soldiers, circles, player.getTeam()));
            }
        }

        Vector2i[] soldiersPos = new Vector2i[soldiers.size()];

        ListIterator<Vector2i> sitr = soldiers.listIterator();
        int i = 0;
        while (sitr.hasNext())
        {
            Vector2i tempSoldier = sitr.next();

            soldiersPos[i] = new Vector2i(tempSoldier.x, tempSoldier.y);

            i++;
        }

        return soldiersPos;
    }

    private Vector2i generateSoldier(List<Vector2i> soldiers, Vector3i[] circles, Team team)
    {
        Vector2i soldier;

        int xMin, xMax;

        if (team == Team.LEFT)
        {
            xMin = 0;
            xMax = PLANE_LENGTH / 2;
        }
        else
        {
            xMin = PLANE_LENGTH / 2;
            xMax = PLANE_LENGTH;
        }

        do
        {
            int x = xMin + random.nextInt(xMax - xMin - 2 * SOLDIER_RADIUS);
            int y = SOLDIER_RADIUS + random.nextInt(PLANE_HEIGHT - 2 * SOLDIER_RADIUS);

            soldier = new Vector2i(x, y);
        }
        while (!testSoldier(soldier, soldiers, circles));

        return soldier;
    }

    private boolean testSoldier(Vector2i soldier, List<Vector2i> soldiers, Vector3i[] circles)
    {
        for (Vector2i temp : soldiers)
        {
            if (Math.abs(soldier.x - temp.x) < 20 &&
                    Math.abs(soldier.y - temp.y) < 20)
            {
                return false;
            }
        }

        for (Vector3i c : circles)
        {
            double dx = soldier.x - c.x;
            double dy = soldier.y - c.y;
            double dist = Math.sqrt(dx * dx + dy * dy);

            if (dist < c.z + SOLDIER_SELECTION_RADIUS)
            {
                return false;
            }
        }

        return true;
    }
}