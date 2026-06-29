package io.p2vman.graphwarserver.util;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.regex.Pattern;

public class TextBuilder {
    private static final Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
    private final Object2ObjectMap<String, Object> variables;
    private String text = null;
    public TextBuilder() {
        this.variables = new Object2ObjectOpenHashMap<>();
    }

    public TextBuilder setVariable(String name, Object obj) {
        this.variables.put(name, obj);
        return this;
    }

    public TextBuilder setText(String text) {
        this.text = text;
        return this;
    }

    public String build() {
        if (text != null) {
            return pattern.matcher(text).replaceAll(matchResult -> {
                String key = matchResult.group(1);
                if (variables.containsKey(key)) {
                    return variables.get(key).toString();
                }

                return "";
            });
        }
        return "";
    }
}
