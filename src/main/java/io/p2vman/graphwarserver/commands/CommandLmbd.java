package io.p2vman.graphwarserver.commands;

@FunctionalInterface
public interface CommandLmbd {
    int handle(CommandContext ctx, IArguments arguments, String command);
}
