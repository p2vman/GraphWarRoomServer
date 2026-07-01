package io.p2vman.graphwarserver;

import io.p2vman.graphwarserver.card.GeneratorFactory;
import io.p2vman.graphwarserver.card.LineCardGenerator;
import io.p2vman.graphwarserver.card.StandardCardGenerator;
import io.p2vman.graphwarserver.registry.Identifier;
import io.p2vman.graphwarserver.registry.Registry;
import io.p2vman.graphwarserver.registry.SimpleRegistry;

public class Registries {
    public static final Registry<GeneratorFactory> CARD_GENERATORS =
            new SimpleRegistry<>(Identifier.of(Identifier.DEFAULT_NAMESPACE, "card_generators"));

    static {
        Registry.register(CARD_GENERATORS, Identifier.tryParse("standard"), StandardCardGenerator::new);
        Registry.register(CARD_GENERATORS, Identifier.tryParse("line"), LineCardGenerator::new);
    }
}
