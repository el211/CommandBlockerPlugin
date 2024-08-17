package fr.elias.commandBlocker;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class CommandBlockerPlugin extends JavaPlugin implements Listener {

    private FileConfiguration config;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        getLogger().info("Configuration loaded. Regions: " + Objects.requireNonNull(config.getConfigurationSection("regions")).getKeys(false));
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("CommandBlockerPlugin has been enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("CommandBlockerPlugin has been disabled.");
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String[] commandParts = event.getMessage().toLowerCase().substring(1).split(" "); // Split into command and arguments
        String command = commandParts[0]; // Get the base command
        String pluginCommand = commandParts.length > 1 ? commandParts[0] + " " + commandParts[1] : commandParts[0]; // e.g., "cmi warp"
        org.bukkit.World bukkitWorld = player.getWorld();

        getLogger().info("Player " + player.getName() + " issued command: " + event.getMessage());

        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(bukkitWorld));

        if (regionManager != null) {
            com.sk89q.worldedit.util.Location worldEditLocation = BukkitAdapter.adapt(player.getLocation());
            ApplicableRegionSet regions = regionManager.getApplicableRegions(worldEditLocation.toVector().toBlockPoint());

            getLogger().info("Regions applicable to player: " + regions.getRegions().toString());

            for (ProtectedRegion region : regions) {
                getLogger().info("Player is in region: " + region.getId());

                if (config.contains("regions." + region.getId())) {
                    List<String> blockedCommands = config.getStringList("regions." + region.getId());
                    getLogger().info("Blocked commands in this region: " + blockedCommands);

                    // Check if the command or its plugin-specific variants are blocked
                    if (isCommandBlocked(command, pluginCommand, blockedCommands)) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.RED + "You cannot use this command in this area.");
                        getLogger().info("Command blocked: " + event.getMessage());
                        return;
                    } else {
                        getLogger().info("Command not blocked: " + event.getMessage());
                    }
                }
            }
        }
    }

    private boolean isCommandBlocked(String command, String pluginCommand, List<String> blockedCommands) {
        Set<String> blockedSet = blockedCommands.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        // Exact match check
        if (blockedSet.contains(command) || blockedSet.contains(pluginCommand)) {
            return true;
        }

        // Wildcard match check (e.g., "cmi*")
        for (String blocked : blockedSet) {
            if (blocked.endsWith("*")) {
                String baseCommand = blocked.substring(0, blocked.length() - 1);
                if (command.startsWith(baseCommand)) {
                    return true;
                }
            }
        }
        return false;
    }
}
