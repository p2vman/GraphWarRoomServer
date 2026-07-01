package io.p2vman.graphwarserver.commands;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectList;
import lombok.NonNull;

import java.util.Arrays;

public class Dispatcher {
    private final Object2ObjectMap<String, CommandLmbd> commands;
    public Dispatcher() {
        commands = new Object2ObjectOpenHashMap<>();
    }

    public void register(String name, @NonNull CommandLmbd lmbd) {
        if (commands.containsKey(name)) return;
        commands.put(name, lmbd);
    }

    public int handle(String input, @NonNull CommandContext ctx) throws CommandException {
        if (input == null || input.trim().isEmpty()) {
            throw new CommandException("Input string cannot be empty");
        }
        String[] tokens = input.trim().split("\\s+");
        String command = tokens[0];
        String[] rawArgs = Arrays.copyOfRange(tokens, 1, tokens.length);
        if (!commands.containsKey(command)) throw new CommandException.CommandNotFoundException(command);
        return commands.get(command).handle(ctx, new ArgumentsImpl(ObjectList.of(rawArgs)), command);
    }

    public boolean handleCommand(@NonNull String input, @NonNull CommandContext ctx) {
        try {
            if (input.startsWith("-")) {
                ctx.sendMessage("on this server the command prefix has been changed from '-' to '/'");
            }
            if (input.startsWith("/")) {
                handle(input.substring(1), ctx);
                return true;
            }
        } catch (CommandException e) {
            ctx.sendMessage(e.build());
            return true;
        }
        return false;
    }
}
