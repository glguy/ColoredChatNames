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

/**
 * Simple plug-in for replacing player's names in chat messages with their "display names" automatically
 *
 * @author Eric Mertens
 *
 */
public final class ColoredChatNames extends JavaPlugin {

	private static final String DISABLED_USERS_CONFIG_SECTION = "disabled-users";
	private static final String PLAYER_NOT_FOUND = ChatColor.RED + "Player not found";
	private static final String NO_TOGGLE_PERMISSION = ChatColor.RED + "You don't have permission to change other players.";
	private static final String CONSOLE_MUST_SPECIFY_PLAYER = ChatColor.RED + "You must specify a player when running this from the console.";
	private static final String TOO_MANY_ARGUMENTS = ChatColor.RED + "Too many arguments.";
	private static final String TOGGLE_COMMAND = "coloredchatnames";
	private static final String TOGGLE_OTHER_PERMISSION = "coloredchatnames.toggle.other";
	private static final String COLORED_PERMISSION = "coloredchatnames";
	private static final String FORMATTING_CHARACTER = "\u00A7";

	/**
	 * Set of users who will not have automatic coloring even if they have permission for it.
	 */
	private final Set<String> disabledUsers = new HashSet<String>();

	/**
	 * Listener for asynchronous chat message. When appropriate the messages are colored.
	 */
	final Listener listener = new Listener() {
		@EventHandler(ignoreCancelled=true)
		public void onAsyncPlayerChat(final AsyncPlayerChatEvent event) {

			String msg = event.getMessage();
			if (msg.contains(FORMATTING_CHARACTER)) return; // Don't auto-format a chat with manual formatting

			final Player player = event.getPlayer();
			if (!playerEnabled(player)) return;

			for (final Player online : getServer().getOnlinePlayers()) {
				// Perform a case-insensitive match on the whole-word
				msg = msg.replaceAll("(?i)\\b"+online.getName()+"\\b", online.getDisplayName());
			}

			event.setMessage(msg);
		}
	};

	/**
	 * Listen for the command to toggle player's preferences about their chat messages being colored.
	 */
	@Override
	public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {

		final boolean commandHandled;

		if (command.getName().equalsIgnoreCase(TOGGLE_COMMAND)) {
			// Determine target
			final Player target = determineTarget(sender, args);
			if (target != null) {
				// Determine the effect of the toggle
				final String targetName = target.getName();
				final boolean newSetting = togglePlayerStatus(targetName);
				final String changeMsg = newSetting ? "disabled" : "enabled";
				sender.sendMessage(ChatColor.GREEN + "Colored name chat " + changeMsg + " for " + targetName);

				updateConfig();
			}

			commandHandled = true;
		} else {
			commandHandled = false;
		}

		return commandHandled;
	}

	private Player determineTarget(final CommandSender sender, final String[] args) {
		final Player target;

		switch (args.length) {
		case 0: // Player unspecified - target self
			target = sender instanceof Player ? (Player)sender : null;
			if (target == null) {
				sender.sendMessage(CONSOLE_MUST_SPECIFY_PLAYER);
			}
			break;
		case 1: // Player specified
			if (sender.hasPermission(TOGGLE_OTHER_PERMISSION)) {
				target = getServer().getPlayer(args[0]);
				if (target == null) {
					sender.sendMessage(PLAYER_NOT_FOUND);
				}
			} else {
				sender.sendMessage(NO_TOGGLE_PERMISSION);
				target = null;
			}
			break;
		default: // Player over-specified
			sender.sendMessage(TOO_MANY_ARGUMENTS);
			target = null;
		}

		return target;
	}

	/**
	 *
	 * @param targetName Player's name to toggle
	 * @return New preference: true if player is now disabled, false if player is now enabled
	 */
	private boolean togglePlayerStatus(final String targetName) {
		final boolean oldDisabled = disabledUsers.contains(targetName);
		if (oldDisabled) {
			disabledUsers.remove(targetName);
		} else {
			disabledUsers.add(targetName);
		}
		return !oldDisabled;
	}

	/**
	 * Register for chat events and load plugin state from configuration files
	 */
	@Override
	public void onEnable() {

		// Saves the config only if one does not exist
		saveDefaultConfig();

		// Start listening for chat events
		getServer().getPluginManager().registerEvents(listener, this);

		loadConfig();
	}

	/**
	 * Restore plug-in state from configuration file.
	 */
	private void loadConfig() {
		disabledUsers.clear();

		final List<?> configDisabledUsers = getConfig().getList(DISABLED_USERS_CONFIG_SECTION, new ArrayList<String>());
		for (final Object disabledUser : configDisabledUsers) {
			if (disabledUser instanceof String) {
				disabledUsers.add((String)disabledUser);
			}
		}
	}

	/**
	 * Store plug-in state to configuration file
	 */
	private void updateConfig() {
		// Update config file
		final FileConfiguration config = getConfig();
		config.set(DISABLED_USERS_CONFIG_SECTION, disabledUsers.toArray());
		saveConfig();
	}

	/**
	 * Test if player's chat messages should be colored
	 * @param player Player who is sending the chat message
	 * @return true when the player's messages should be colored
	 */
	private boolean playerEnabled(final Player player) {
		return player.hasPermission(COLORED_PERMISSION)
				&& !disabledUsers.contains(player.getName());
	}
}
