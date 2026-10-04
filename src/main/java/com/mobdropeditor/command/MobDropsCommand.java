package com.mobdropeditor.command;

import com.mobdropeditor.config.MessageManager;
import com.mobdropeditor.drops.DropRule;
import com.mobdropeditor.gui.MainMobMenu;
import com.mobdropeditor.gui.MobDropMenu;
import com.mobdropeditor.item.ItemData;
import com.mobdropeditor.item.ItemSerializer;
import com.mobdropeditor.mob.MobDefinition;
import com.mobdropeditor.mob.MobRegistry;
import com.mobdropeditor.platform.ItemAdapter;
import com.mobdropeditor.platform.scheduler.SchedulerAdapter;
import com.mobdropeditor.storage.Storage;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

public class MobDropsCommand implements CommandExecutor {
    private final MobRegistry mobRegistry;
    private final Storage storage;
    private final MessageManager messageManager;
    private final SchedulerAdapter schedulerAdapter;
    private final ItemAdapter itemAdapter;
    private final Runnable reloadCallback;

    public MobDropsCommand(MobRegistry mobRegistry, Storage storage, MessageManager messageManager, SchedulerAdapter schedulerAdapter, ItemAdapter itemAdapter, Runnable reloadCallback) {
        this.mobRegistry = mobRegistry;
        this.storage = storage;
        this.messageManager = messageManager;
        this.schedulerAdapter = schedulerAdapter;
        this.itemAdapter = itemAdapter;
        this.reloadCallback = reloadCallback;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("open")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(messageManager.colorize("&cApenas jogadores podem abrir a GUI."));
                return true;
            }
            Player player = (Player) sender;
            if (!player.hasPermission("mobdrops.admin") && !player.hasPermission("mobdrops.view")) {
                player.sendMessage(messageManager.getMessage("no-permission"));
                return true;
            }

            if (args.length >= 2) {
                String mobKey = args[1];
                Optional<MobDefinition> mobOpt = mobRegistry.getMob(mobKey);
                if (mobOpt.isPresent()) {
                    MainMobMenu mainMenu = new MainMobMenu(mobRegistry, storage, messageManager, schedulerAdapter);
                    MobDropMenu mobMenu = new MobDropMenu(mobOpt.get(), mobRegistry, storage, messageManager, schedulerAdapter, mainMenu);
                    mobMenu.open(player);
                    return true;
                }
            }

            MainMobMenu menu = new MainMobMenu(mobRegistry, storage, messageManager, schedulerAdapter);
            menu.open(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("mobdrops.admin") && !sender.hasPermission("mobdrops.reload")) {
                sender.sendMessage(messageManager.getMessage("no-permission"));
                return true;
            }
            if (reloadCallback != null) {
                reloadCallback.run();
            }
            sender.sendMessage(messageManager.getMessage("reloaded"));
            return true;
        }

        if (args[0].equalsIgnoreCase("save")) {
            if (!sender.hasPermission("mobdrops.admin") && !sender.hasPermission("mobdrops.edit")) {
                sender.sendMessage(messageManager.getMessage("no-permission"));
                return true;
            }
            sender.sendMessage(messageManager.getMessage("saved"));
            return true;
        }

        if (args[0].equalsIgnoreCase("copy")) {
            if (!sender.hasPermission("mobdrops.admin") && !sender.hasPermission("mobdrops.edit")) {
                sender.sendMessage(messageManager.getMessage("no-permission"));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(messageManager.colorize("&cUso: /mobdrops copy <origem> <destino>"));
                return true;
            }
            String sourceKey = args[1];
            String targetKey = args[2];
            storage.copyConfig(sourceKey, targetKey);
            sender.sendMessage(messageManager.colorize("&aConfiguração copiada de &e" + sourceKey + " &apara &e" + targetKey + "&a!"));
            return true;
        }

        if (args[0].equalsIgnoreCase("reset")) {
            if (!sender.hasPermission("mobdrops.admin") && !sender.hasPermission("mobdrops.edit")) {
                sender.sendMessage(messageManager.getMessage("no-permission"));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(messageManager.colorize("&cUso: /mobdrops reset <mob>"));
                return true;
            }
            String mobKey = args[1];
            storage.resetConfig(mobKey);
            sender.sendMessage(messageManager.colorize("&aConfiguração do mob &e" + mobKey + " &afoi restaurada ao padrão!"));
            return true;
        }

        if (args[0].equalsIgnoreCase("add")) {
            if (!sender.hasPermission("mobdrops.admin") && !sender.hasPermission("mobdrops.edit")) {
                sender.sendMessage(messageManager.getMessage("no-permission"));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(messageManager.colorize("&cUso: /mobdrops add <mob> [chance] [min] [max]"));
                return true;
            }

            String mobKey = args[1];
            double chance = args.length >= 3 ? parseDouble(args[2], 100.0) : 100.0;
            int min = args.length >= 4 ? parseInt(args[3], 1) : 1;
            int max = args.length >= 5 ? parseInt(args[4], 1) : 1;

            ItemData itemData;
            if (sender instanceof Player) {
                Player p = (Player) sender;
                ItemStack hand = itemAdapter.getItemInMainHand(p);
                if (hand != null && hand.getType() != Material.AIR) {
                    itemData = ItemSerializer.serialize(hand);
                } else {
                    itemData = new ItemData("DIAMOND", 1);
                }
            } else {
                itemData = new ItemData("DIAMOND", 1);
            }

            DropRule rule = new DropRule(null, chance, min, max, true, itemData);
            List<DropRule> drops = storage.getDrops(mobKey);
            drops.add(rule);
            storage.saveDrops(mobKey, drops);

            sender.sendMessage(messageManager.getMessage("drop-added").replace("%mob%", mobKey));
            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            if (!sender.hasPermission("mobdrops.admin") && !sender.hasPermission("mobdrops.edit")) {
                sender.sendMessage(messageManager.getMessage("no-permission"));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(messageManager.colorize("&cUso: /mobdrops remove <mob> <drop_id>"));
                return true;
            }

            String mobKey = args[1];
            String dropId = args[2];

            List<DropRule> drops = storage.getDrops(mobKey);
            boolean removed = drops.removeIf(d -> d.getId().equalsIgnoreCase(dropId));
            if (removed) {
                storage.saveDrops(mobKey, drops);
                sender.sendMessage(messageManager.getMessage("drop-removed").replace("%id%", dropId).replace("%mob%", mobKey));
            } else {
                sender.sendMessage(messageManager.colorize("&cDrop ID '" + dropId + "' não encontrado em " + mobKey + "."));
            }
            return true;
        }

        // Default help
        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(messageManager.colorize("&8--- &6MobDropEditor Comandos &8---"));
        sender.sendMessage(messageManager.colorize("&e/mobdrops open [mob] &7- Abre a GUI principal ou do mob"));
        sender.sendMessage(messageManager.colorize("&e/mobdrops add <mob> [chance] [min] [max] &7- Adiciona drop"));
        sender.sendMessage(messageManager.colorize("&e/mobdrops remove <mob> <id> &7- Remove um drop"));
        sender.sendMessage(messageManager.colorize("&e/mobdrops copy <origem> <destino> &7- Copia config"));
        sender.sendMessage(messageManager.colorize("&e/mobdrops reset <mob> &7- Reseta config"));
        sender.sendMessage(messageManager.colorize("&e/mobdrops reload &7- Recarrega configurações"));
        sender.sendMessage(messageManager.colorize("&e/mobdrops save &7- Salva configurações"));
        sender.sendMessage(messageManager.colorize("&e/mobdrops help &7- Exibe esta mensagem"));
    }

    private double parseDouble(String str, double def) {
        try {
            return Double.parseDouble(str);
        } catch (Exception e) {
            return def;
        }
    }

    private int parseInt(String str, int def) {
        try {
            return Integer.parseInt(str);
        } catch (Exception e) {
            return def;
        }
    }
}
