package io.p2vman.graphwarserver.metircs;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import io.p2vman.graphwarserver.Config;
import lombok.Getter;

import java.util.function.Consumer;

public class MetricsClient {
    private final InfluxDBClient client;

    @Getter
    private static MetricsClient instance = null;

    public static void init(Config.Metrics metrics) {
        instance = new MetricsClient(metrics);
    }

    private MetricsClient(Config.Metrics metrics) {
        if (metrics.enable) {
            client = InfluxDBClientFactory.create(
                    metrics.host,
                    metrics.token.toCharArray(),
                    metrics.org,
                    metrics.bucket
            );
        } else {
            client = null;
        }
    }

    public void runWithBlocking(Consumer<WriteApiBlocking> b) {
        if (client != null) {
            b.accept(client.getWriteApiBlocking());
        }
    }

    public void runWithClient(Consumer<InfluxDBClient> b) {
        if (client != null) {
            b.accept(client);
        }
    }
}
