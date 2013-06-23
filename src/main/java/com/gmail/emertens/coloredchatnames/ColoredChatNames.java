package com.gmail.emertens.coloredchatnames;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class ColoredChatNames extends JavaPlugin implements Listener {

	private static final String TOGGLE_OTHER_PERMISSION = "coloredchatnames.toggle.other";
	private static final String COLORED_PERMISSION = "coloredchatnames";
	private final Set<String> preferences = new HashSet<String>();

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {

		final Player player;

		if (!(sender instanceof Player)) {
			player = null;
		} else {
			player = (Player) sender;
		}

		if (command.getName().equalsIgnoreCase("coloredchatnames")) {

			// Determine target
			final Player target;
			switch (args.length) {
			case 0: // Unspecified
				if (player == null) {
					sender.sendMessage(ChatColor.RED
							+ "You must specify a player when running this from the console.");
					return true;
				} else {
					target = player;
				}
				break;
			case 1: // Specified
				if (sender.hasPermission(TOGGLE_OTHER_PERMISSION)) {
					target = getServer().getPlayer(args[0]);
				} else {
					sender.sendMessage(ChatColor.RED + "You don't have permission to change other players.");
					return true;
				}
				break;
			default: // Over-specified
				return false;
			}

			// Determine if a player could be found
			if (target == null) {
				sender.sendMessage(ChatColor.RED + "Player not found");
				return true;
			}

			// Determine the effect of the toggle
			final String targetName = target.getName();
			boolean oldDisabled = preferences.contains(targetName);
			if (oldDisabled) {
				preferences.remove(targetName);
			} else {
				preferences.add(targetName);
			}

			// Notify sender
			final String changeMsg = oldDisabled ? "enabled" : "disabled";
			sender.sendMessage(ChatColor.GREEN + "Colored name chat " + changeMsg + " for " + targetName);

			// Update config file
			final FileConfiguration config = getConfig();
			config.set("disabled-users", preferences.toArray());
			saveConfig();

			return true;
		}

		return false;
	}

	@Override
	public void onEnable() {

		// Saves the config only if one does not exist
		saveDefaultConfig();

		getServer().getPluginManager().registerEvents(this, this);

		final FileConfiguration config = getConfig();
		@SuppressWarnings("unchecked")
		final List<String> disabledUsers = (List<String>) config.getList("disabled-users", new ArrayList<String>());
		preferences.clear();
		preferences.addAll(disabledUsers);
	}

	private boolean playerEnabled(Player player) {
		return player.hasPermission(COLORED_PERMISSION) && !preferences.contains(player.getName());
	}

	@EventHandler(ignoreCancelled=true)
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		final Player[] players = getServer().getOnlinePlayers();

		String msg = event.getMessage();
		if (msg.contains("\u00A7")) return; // Don't auto-format a chat with manual formatting

		final Player player = event.getPlayer();
		if (!playerEnabled(player)) return;

		for (Player online : players) {
			// Perform a case-insensitive match on the whole-word
			msg = msg.replaceAll("(?i)\\b"+online.getName()+"\\b", online.getDisplayName());
		}

		event.setMessage(msg);
	}
}
