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
        builder.append(getMessage()).append("\n");
        for (StackTraceElement stackTraceElement : getStackTrace()) {
            var c = stackTraceElement.getClassName();
            if (c.equals("io.netty.handler.codec.MessageToMessageDecoder")) break;
            builder.append(c).append(".").append(stackTraceElement.getMethodName()).append("#").append(stackTraceElement.getLineNumber()).append("\n");
        }
        return builder.toString();
    }
}
