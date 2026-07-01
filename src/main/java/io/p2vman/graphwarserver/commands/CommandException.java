package io.p2vman.graphwarserver.commands;

public class CommandException extends RuntimeException {
    public CommandException(String message) {
        super(message);
    }

    public static class CommandNotFoundException extends CommandException {
        public CommandNotFoundException(String command) {
            super(command);
        }
    }

    public String build() {
        StringBuilder builder = new StringBuilder();
        builder.append(getMessage());
        return builder.toString();
    }
}
