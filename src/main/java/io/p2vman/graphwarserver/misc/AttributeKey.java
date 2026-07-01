package io.p2vman.graphwarserver.misc;

public final class AttributeKey<T> {
    private final String name;

    private AttributeKey(String name) {
        this.name = name;
    }

    public static <T> AttributeKey<T> valueOf(String name) {
        return new AttributeKey<>(name);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AttributeKey<?> key && name.equals(key.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}