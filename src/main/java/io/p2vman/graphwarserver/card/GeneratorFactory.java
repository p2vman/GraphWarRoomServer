package io.p2vman.graphwarserver.card;

import io.p2vman.graphwarserver.BasicServer;

import java.util.Random;

public interface GeneratorFactory {
    CardGenerator create(Random random, BasicServer server);
}
