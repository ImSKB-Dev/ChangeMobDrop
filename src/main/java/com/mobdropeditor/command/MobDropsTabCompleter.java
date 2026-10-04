package com.mobdropeditor.command;

import com.mobdropeditor.mob.MobDefinition;
import com.mobdropeditor.mob.MobRegistry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MobDropsTabCompleter implements TabCompleter {
    private final MobRegistry mobRegistry;

    public MobDropsTabCompleter(MobRegistry mobRegistry) {
        this.mobRegistry = mobRegistry;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("open", "add", "remove", "copy", "reset", "reload", "save", "help");
            return subCommands.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && Arrays.asList("open", "add", "remove", "copy", "reset").contains(args[0].toLowerCase())) {
            return mobRegistry.getAllMobs().stream()
                    .map(MobDefinition::getKey)
                    .filter(k -> k.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("copy")) {
            return mobRegistry.getAllMobs().stream()
                    .map(MobDefinition::getKey)
                    .filter(k -> k.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
