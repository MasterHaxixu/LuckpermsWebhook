package com.masterhaxixu.luckpermswebhook;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import com.masterhaxixu.luckpermswebhook.listeners.LuckpermsListener;

import dev.faststats.bukkit.BukkitMetrics;
import dev.faststats.core.ErrorTracker;
import dev.faststats.core.Metrics;
import net.luckperms.api.LuckPerms;

/**
 * Hello world!
 *
 */
public class Main extends JavaPlugin {

    private final Map<String, String> embeds = new HashMap<>();
    private final List<String> eventTypes = Arrays.asList(
            "LogBroadcastEvent",
            "LogBroadcastPermissionEvent",
            "LogBroadcastParentEvent");
    private LuckpermsListener luckpermsListener;
    public static final ErrorTracker ERROR_TRACKER = ErrorTracker.contextAware();
    private final Metrics metrics = BukkitMetrics.factory()
            .token("22f89ca898749b5dce9fae10139d4596")
            .errorTracker(ERROR_TRACKER)
            .create(this);

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadEmbeds();
        checkEnabled();
        initLuckperms();
        initMetrics();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("LuckpermsWebhook has been disabled!");
        shutdownMetrics();

        if (luckpermsListener != null) {
            luckpermsListener = null;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("luckpermswebhook")) {
            return false;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("luckpermswebhook.reload")) {
                sender.sendMessage("You don't have permission to do that.");
                return true;
            }

            reloadConfig();
            reloadEmbeds();
            checkEnabled();
            sender.sendMessage("LuckpermsWebhook reloaded.");
            return true;
        }

        sender.sendMessage("Usage: /" + label + " reload");
        return true;
    }

    public void reloadEmbeds() {
        embeds.clear();
        for (String fileName : eventTypes) {
            File file = new File(this.getDataFolder(), "embeds/" + fileName + ".json");
            if (!file.exists()) {
                this.saveResource("embeds/" + fileName + ".json", false);
            }
            try {
                String content = new String(Files.readAllBytes(file.toPath()));
                embeds.put(fileName, content);
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Failed to load embed template: " + file.getName(), e);
            }
        }
    }

    public void checkEnabled() {
        if (this.getConfig().getBoolean("webhooks.luckperms.enabled")) {
            getLogger().info("LuckPerms webhook is enabled. Initializing...");
        } else {
            getLogger().info("LuckPerms webhook is disabled in the config. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);

        }
    }

    public void initLuckperms() {
        LuckPerms luckPerms = getServer().getServicesManager().load(LuckPerms.class);
        if (luckPerms == null) {
            getLogger().severe("LuckPerms not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
        } else {
            luckpermsListener = new LuckpermsListener(this, luckPerms);
        }
    }

    public void initMetrics() {
        metrics.ready();
    }

    public void shutdownMetrics() {
        metrics.shutdown();
    }

    public Map<String, String> getEmbeds() {
        return embeds;
    }

}
