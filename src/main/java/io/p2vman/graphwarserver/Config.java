package io.p2vman.graphwarserver;

import lombok.ToString;

import java.util.List;

@ToString
public class Config {
    public List<Integer> ports;
    public EventLoops eventLops;
    public Tracker tracker;
    public List<Server> servers;
    public Metrics metrics;
    public String[] welcome_messages;

    @ToString
    public static class EventLoops {
        public Loop boos;
        public Loop worker;
    }

    @ToString
    public static class Loop {
        public int threads;
        public String name_pattern;
        public boolean daemon;
    }

    @ToString
    public static class Tracker {
        public String host;
        public int port;
        public boolean auto_reconnect;
    }

    @ToString
    public static class Server {
        public String name;
    }

    @ToString
    public static class Metrics {
        public boolean enable;
        public String host;
        public String token;
        public String org;
        public String bucket;
    }
}