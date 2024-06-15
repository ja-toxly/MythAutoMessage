package com.mythicalshop.mythautomessage;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarStyle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MythAutoMessage extends JavaPlugin {

    private List<MessageTask> messageTasks = new ArrayList<>();
    private int currentIndex = 0;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadMessages();
        getCommand("automessage").setExecutor(this);
    }

    @Override
    public void onDisable() {
        for (MessageTask task : messageTasks) {
            task.cancel();
        }
        messageTasks.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("automessage")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("myth.automessage.reload")) {
                    reloadConfig();
                    for (MessageTask task : messageTasks) {
                        task.cancel();
                    }
                    messageTasks.clear();
                    loadMessages();
                    sender.sendMessage(ChatColor.GREEN + "MythAutoMessage reloaded!");
                } else {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                }
                return true;
            }
        }
        return false;
    }

    private void loadMessages() {
        FileConfiguration config = getConfig();
        Set<String> keys = config.getKeys(false);

        for (String key : keys) {
            long delay = config.getLong(key + ".delay") * 20;
            String type = config.getString(key + ".type");
            List<String> messages = config.getStringList(key + ".message");
            String color = config.getString(key + ".color", "WHITE");
            String subtitle = config.getString(key + ".subtitle", "");
            long displayTime = config.getLong(key + ".display-time", 10) * 20;
            List<String> worlds = config.getStringList(key + ".worlds");
            boolean global = config.getBoolean(key + ".global", false);

            if (global) {
                worlds = null; // Null means global
            }

            MessageTask task = new MessageTask(type, messages, subtitle, BarColor.valueOf(color), delay, displayTime, worlds);
            task.runTaskTimer(this, delay, delay);
            messageTasks.add(task);
        }
    }

    private class MessageTask extends BukkitRunnable {
        private final String type;
        private final List<String> messages;
        private final String subtitle;
        private final BarColor color;
        private final long delay;
        private final long displayTime;
        private final List<String> worlds;

        public MessageTask(String type, List<String> messages, String subtitle, BarColor color, long delay, long displayTime, List<String> worlds) {
            this.type = type;
            this.messages = messages;
            this.subtitle = subtitle;
            this.color = color;
            this.delay = delay;
            this.displayTime = displayTime;
            this.worlds = worlds;
        }

        @Override
        public void run() {
            switch (type.toUpperCase()) {
                case "CHAT":
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (canSendToWorld(player.getWorld().getName())) {
                            for (String message : messages) {
                                player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
                            }
                        }
                    }
                    break;
                case "BOSSBAR":
                    BossBar bossBar = Bukkit.createBossBar(ChatColor.translateAlternateColorCodes('&', messages.get(0)), color, BarStyle.SOLID);
                    bossBar.setVisible(true);
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (canSendToWorld(player.getWorld().getName())) {
                            bossBar.addPlayer(player);
                        }
                    }
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            bossBar.setVisible(false);
                            bossBar.removeAll();
                        }
                    }.runTaskLater(MythAutoMessage.this, displayTime);
                    break;
                case "TITLE":
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (canSendToWorld(player.getWorld().getName())) {
                            player.sendTitle(ChatColor.translateAlternateColorCodes('&', messages.get(0)), ChatColor.translateAlternateColorCodes('&', subtitle), 10, 70, 20);
                        }
                    }
                    break;
                case "ACTIONBAR":
                    new BukkitRunnable() {
                        private long elapsed = 0;

                        @Override
                        public void run() {
                            if (elapsed >= displayTime) {
                                cancel();
                                return;
                            }
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                if (canSendToWorld(player.getWorld().getName())) {
                                    player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', messages.get(0))));
                                }
                            }
                            elapsed += 10;
                        }
                    }.runTaskTimer(MythAutoMessage.this, 0, 10); // 10 ticks = 0.5 second
                    break;
            }
        }

        private boolean canSendToWorld(String worldName) {
            if (worlds == null) {
                return true; // Global message
            } else {
                return worlds.contains(worldName);
            }
        }
    }
}