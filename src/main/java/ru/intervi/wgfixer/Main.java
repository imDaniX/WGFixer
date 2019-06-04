package ru.intervi.wgfixer;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class Main extends JavaPlugin implements Listener {
	private final UUID ZERO_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
	private WorldGuard wg;
	private Essentials ess;

	private boolean essMode=false;

	private CustomConfig cfg;

	@Override
	public void onEnable() {
		PluginManager manager = Bukkit.getPluginManager();
		if (!Bukkit.getOnlineMode()) {
			manager.registerEvents(this, this);
			cfg = new CustomConfig(this, "config");
			if(manager.isPluginEnabled("Essentials")) {
				ess = (Essentials) manager.getPlugin("Essentials");
				essMode = true;
			}
			essMode=essMode&&cfg.essMode();
			wg = WorldGuard.getInstance();
			return;
		}
		getLogger().info("This server uses online-mode=true. Plugin will be disabled.");
		manager.disablePlugin(this);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onCommand(PlayerCommandPreprocessEvent event) {
		Player player = event.getPlayer();
		String[] cmd = event.getMessage().toLowerCase().split(" ");
		// Является ли введенная команда командой WG
		if (
				cmd.length < 4
				||
				!((cmd[0].equals("/rg") || cmd[0].equals("/region") || cmd[0].equals("/regions") || cmd[0].equals("/worldguard:rg") || cmd[0].equals("/worldguard:region") || cmd[0].equals("/worldguard:regions")))
			) return;
		// Редактируются ли сейчас участники или владельцы региона
		String action = getAction(cmd[1]);
		if(action == null)
			return;

		// Если use_names true, то просто добавляем -n
		if(cfg.useNames()) {
			StringBuilder builder = new StringBuilder(cmd[0]).append(" ").append(cmd[1]).append(" -n");
			for(int i = 2; i < cmd.length; i++)
				builder.append(" ").append(cmd[i]);
			event.setMessage(builder.toString());
			return;
		}

		// Заглушка для слишком большого кол-ва аргументов
		if(cmd.length > 6) {
			player.sendMessage(cfg.getMessage(Message.TOO_MANY_ARGS));
			event.setCancelled(true);
			return;
		}
		// Отдельно запишем название региона, ник и мир
		String region = null, name = null;
		World world = null;
		// Пропарсим команду начиная с третьего аргумента
		for(int i = 2; i < cmd.length; i++) {
			if(cmd[i].equals("-a") || cmd[i].equals("-n"))
				return;
			if(cmd[i].equals("-w")) {
				if(world != null) {
					player.sendMessage(cfg.getMessage(Message.WORLD_ALREADY_CHOSEN, world.getName()));
					event.setCancelled(true);
					return;
				}
				// Определяем мир, так что двигаем на один пункт дальше
				i++;
				if(cmd.length < i+1) {
					player.sendMessage(cfg.getMessage(Message.WORLD_NO_ARG));
					event.setCancelled(true);
					return;
				}
				world = Bukkit.getWorld(cmd[i]);
				if(world == null) {
					player.sendMessage(cfg.getMessage(Message.WORLD_UNKNOWN, cmd[i]));
					event.setCancelled(true);
					return;
				}
				continue;
			}
			if(region == null)
				region = cmd[i];
			else
				name = cmd[i];
		}
		// Проверяем наличие названия региона
		if(region == null) {
			player.sendMessage(cfg.getMessage(Message.REGION_NO_ARG));
			event.setCancelled(true);
			return;
		}
		// Проверяем наличие ника - он обязательно должен быть, т.к. -a нет
		if(name == null) {
			player.sendMessage(cfg.getMessage(Message.NAME_NO_ARG));
			event.setCancelled(true);
			return;
		}
		// Если -w не указан - берем мир от игрока
		if(world == null)
			world=player.getWorld();
		// Ищем игрока среди онлайна и оффлайна
		UUID uuid = getUniqueId(name);
		if(uuid == null) {
			player.sendMessage(cfg.getMessage(Message.NAME_UNKNOWN, name));
			event.setCancelled(true);
			return;
		}
		if(uuid.equals(ZERO_UUID))
			return;
		// Отменяем ивент, т.к. передавать команду в управление WG больше не требуется
		event.setCancelled(true);
		// Проверяем наличие региона
		ProtectedRegion wgRegion = wg.getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world)).getRegion(region);
		if (wgRegion == null) {
			player.sendMessage(cfg.getMessage(Message.REGION_UNKNOWN, region));
			return;
		}
		// Проверяем, может ли игрок творить свои дела
		if (!canAffect(wgRegion, action, player)) {
			player.sendMessage(cfg.getMessage(Message.REGION_NO_PERMISSION));
			return;
		}
		// Выполняем требуемое действие
		switch (action) {
			case "removeowner": {
				wgRegion.getOwners().removePlayer(uuid);
				player.sendMessage(cfg.getMessage(Message.ACTION_REMOVEOWNER, name, region));
				break;
			}
			case "removemember": {
				wgRegion.getMembers().removePlayer(uuid);
				player.sendMessage(cfg.getMessage(Message.ACTION_REMOVEMEMBER, name, region));
				break;
			}
			case "addowner": {
				wgRegion.getOwners().addPlayer(uuid);
				player.sendMessage(cfg.getMessage(Message.ACTION_ADDOWNER, name, region));
				break;
			}
			case "addmember": {
				wgRegion.getMembers().addPlayer(uuid);
				player.sendMessage(cfg.getMessage(Message.ACTION_ADDMEMBER, name, region));
				break;
			}
		}
		// Сохраняем изменения
		if(!saveChanges(world))
			player.sendMessage(cfg.getMessage(Message.SAVE_FAIL));
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(sender.hasPermission("wgfix.command")) {
			if(cfg.reloadConfig()) {
				essMode = cfg.essMode() && Bukkit.getPluginManager().isPluginEnabled("Essentials");
				sender.sendMessage(cfg.getMessage(Message.RELOAD_SUCCESS));
			} else sender.sendMessage(ChatColor.DARK_RED + "Error");
		} else sender.sendMessage(cfg.getMessage(Message.NO_PERMISSION));
		return true;
	}

	/**
	 * @return ZERO_UUID если игрок онлайн и ignoreOnline true, нормальный UUID если игрок оффлайн, null если не найден
	 */
	private UUID getUniqueId(String name) {
		for(Player player : Bukkit.getServer().getOnlinePlayers())
			if(name.equalsIgnoreCase(player.getName()))
				return cfg.ignoreOnline()?ZERO_UUID:player.getUniqueId();
		if(essMode) {
			User essUser = ess.getOfflineUser(name);
			return essUser == null ? null : essUser.getConfigUUID();
		} else {
			for(OfflinePlayer offPlayer : Bukkit.getOfflinePlayers())
				if(name.equalsIgnoreCase(offPlayer.getName()))
					return offPlayer.getUniqueId();
			return null;
		}
	}

	private String getAction(String arg) {
		switch (arg) {
			case "removeowner": case "ro": return "removeowner";
			case "removemember": case "rm": case "remmember": case "removemem": case "remmem": return "removemember";
			case "addowner": case "ao": return "addowner";
			case "addmember": case "am": case "addmem": return "addmember";
			default: return null;
		}
	}

	private boolean saveChanges(World world) {
		try {
			wg.getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world)).saveChanges();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private boolean canAffect(ProtectedRegion region, String action, Player player) {
		String id = region.getId().toLowerCase();
		return (region.getOwners().contains(player.getUniqueId()) && player.hasPermission("worldguard.region." + action + ".own." + id))
				||
				player.hasPermission("worldguard.region." + action + "." + id)
				||
				(region.getMembers().contains(player.getUniqueId()) && player.hasPermission("worldguard.region." + action + ".member." + id));
	}
}
