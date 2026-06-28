package io.p2vman.graphwarserver.card;

import io.p2vman.graphwarserver.Avatar;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.Collection;

public interface CardGenerator
{
    Vector3i[] generateCircles();
    Vector2i[] generateSoldiers(Vector3i[] circles, Collection<Avatar> players);
}
