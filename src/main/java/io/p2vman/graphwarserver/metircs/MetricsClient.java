package io.p2vman.graphwarserver.metircs;

import io.p2vman.graphwarserver.Config;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import lombok.Getter;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Consumer;

public class MetricsClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsClient.class);
    private final HttpClient client;
    private final Config.Metrics config;

    @Getter
    private static MetricsClient instance = null;

    public static void init(Config.Metrics metrics) {
        instance = new MetricsClient(metrics);
    }

    private MetricsClient(Config.Metrics metrics) {
        if (metrics.enable) {
             this.client = HttpClient.newHttpClient();
        } else {
            client = null;
        }
        this.config = metrics;
    }


    public void run(Consumer<MetricsClient> consumer) {
        if (client != null) {
            consumer.accept(this);
        }
    }

    public void send(@NonNull PointBuilder... builders) {
        if (builders.length==0) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < builders.length; i++) {
            if (i != 0) {
                builder.append('\n');
            }
            builder.append(builders[i].build());
        }


        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.host).resolve("/api/v2/write?org="+config.org+"&bucket="+config.bucket+"&precision=ns"))
                .header("Authorization", "Token " + config.token)
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(builder.toString()))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenAccept(response -> {
                    if (response.statusCode() != 204) {
                        LOGGER.warn("InfluxDB error code: {}", response.statusCode());
                    }
                })
                .exceptionally(ex -> {
                    LOGGER.error("", ex);
                    return null;
                });
    }

    public static class PointBuilder {
        private final String name;
        private final Object2ObjectMap<String, Object> tags = new Object2ObjectArrayMap<>();
        private final Object2ObjectMap<String, Object> fields = new Object2ObjectArrayMap<>();

        public PointBuilder(String name) {
            this.name = name;
        }

        public PointBuilder addTag(String name, Object data) {
            this.tags.put(name, data);
            return this;
        }

        public PointBuilder addField(String name, Object data) {
            this.fields.put(name, data);
            return this;
        }

        public String build() {
            StringBuilder builder = new StringBuilder();
            builder.append(name);
            for (String s : tags.keySet()) {
                var value = tags.get(s);
                builder.append(",").append(escapeTag(s)).append("=").append(escapeTag(value));
            }
            builder.append(" ");
            boolean tag = true;
            for (String s : fields.keySet()) {
                var value = fields.get(s);
                if (!tag) {
                    builder.append(",");
                }
                tag = false;
                builder.append(escapeTag(s)).append("=");
                if (value instanceof Integer || value instanceof Long) {
                    builder.append(value).append('i');
                } else if (value instanceof Float || value instanceof Double) {
                    builder.append(value);
                } else if (value instanceof Boolean) {
                    builder.append(value);
                } else if (value instanceof String) {
                    builder.append('"')
                            .append(escapeTag(value))
                            .append('"');
                } else {
                    throw new IllegalArgumentException("Unsupported field type: " + value.getClass());
                }
            }

            return builder.toString();
        }

        @Override
        public String toString() {
            return this.build();
        }

        private static String escapeTag(Object value) {
            return value.toString()
                    .replace("\\", "\\\\")
                    .replace(" ", "\\ ")
                    .replace(",", "\\,")
                    .replace("=", "\\=")
                    .replace("\"", "\\\"");
        }
    }
}
