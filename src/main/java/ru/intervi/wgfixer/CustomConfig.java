package ru.intervi.wgfixer;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class CustomConfig {

	private YamlConfiguration yml;
	private File file;

	private Map<Message, String> messages;
	private boolean names, online, essentials, respect;
	
	public CustomConfig(JavaPlugin plg, String name) {
		file = new File(plg.getDataFolder(), name + ".yml");
		file.getParentFile().mkdirs();
		if (!file.exists())
			plg.saveResource(name + ".yml", false);
		yml = YamlConfiguration.loadConfiguration(file);
		loadData();
	}
	
	public boolean reloadConfig() {
		try {
			yml = YamlConfiguration.loadConfiguration(file);
			loadData();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private void loadData() {
		messages = new HashMap<>();
		for(Message msg:Message.values())
			messages.put(msg, clr(yml.getString(msg.getLocation(), ChatColor.DARK_RED + "Error")));
		names = yml.getBoolean("settings.use_names");
		online = yml.getBoolean("settings.ignore_online");
		essentials = yml.getBoolean("settings.essentials");
		respect = yml.getBoolean("settings.respect_cancelled");
	}

	public String getMessage(Message msg) {
		return messages.get(msg);
	}

	public String getMessage(Message msg, String... phs) {
		int i = 0;
		String message = messages.get(msg);
		for(String ph:msg.getPlaceholders())
			message = message.replace(ph, phs[i++]);
		return message;
	}


	public boolean useNames() {
		return names;
	}

	public boolean ignoreOnline() {
		return online;
	}

	public boolean essMode() {
		return essentials;
	}

	public boolean respectCancelled() {
		return respect;
	}

	private String clr(String s) {
		return ChatColor.translateAlternateColorCodes('&', s);
	}
}
