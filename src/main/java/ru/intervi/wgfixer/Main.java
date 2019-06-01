package ru.intervi.wgfixer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;
import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.UUID;

public class Main extends JavaPlugin implements Listener {
	private final UUID ZERO_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
	private WorldGuard wg;
	private Essentials ess;

	@Override
	public void onEnable() {
		if (!Bukkit.getOnlineMode()) {
			getServer().getPluginManager().registerEvents(this, this);
			ess = (Essentials) Bukkit.getServer().getPluginManager().getPlugin("Essentials");
			wg = WorldGuard.getInstance();
			return;
		}
		getLogger().info("This server uses online-mode=true. Plugin will be disabled.");
		Bukkit.getPluginManager().disablePlugin(this);
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
		// Заглушка для слишком большого кол-ва аргументов
		if(cmd.length > 6) {
			player.sendMessage(ChatColor.RED + "В введенной вами команде слишком много аргументов.");
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
					player.sendMessage(ChatColor.RED + "Вы уже указали мир.");
					event.setCancelled(true);
					return;
				}
				// Определяем мир, так что двигаем на один пункт дальше
				i++;
				if(cmd.length < i+1) {
					player.sendMessage(ChatColor.RED + "Вы не указали мир, в котором хотите редактировать регион.");
					event.setCancelled(true);
					return;
				}
				world = Bukkit.getWorld(cmd[i]);
				if(world == null) {
					player.sendMessage(ChatColor.RED + "Вы указали несуществующий мир.");
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
			player.sendMessage(ChatColor.RED + "Вы не указали название региона.");
			event.setCancelled(true);
			return;
		}
		// Проверяем наличие ника - он обязательно должен быть, т.к. -a нет
		if(name == null) {
			player.sendMessage(ChatColor.RED + "Вы не указали ник игрока.");
			event.setCancelled(true);
			return;
		}
		// Если -w не указан - берем мир от игрока
		if(world == null)
			world=player.getWorld();
		// Ищем игрока среди онлайна и оффлайна
		UUID uuid = getUniqueId(name);
		if(uuid == null) {
			player.sendMessage(ChatColor.RED + "Игрока " + name + " ещё не было на сервере.");
			event.setCancelled(true);
			return;
		}
		if(uuid.equals(ZERO_UUID))
			return;
		// Отменяем ивент, т.к. передавать команду в управление WG больше не требуется
		event.setCancelled(true);
		// Проверяем наличие региона
		ProtectedRegion rg = wg.getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world)).getRegion(region);
		if (rg == null) {
			player.sendMessage(ChatColor.RED + "Неизвестный регион " + region + ".");
			return;
		}
		// Проверяем, может ли игрок творить свои дела
		if (!canAffect(rg, action, player)) {
			player.sendMessage(ChatColor.RED + "У вас недостаточно прав, чтобы сделать это.");
			return;
		}
		// Выполняем требуемое действие
		switch (action) {
			case "removeowner": {
				rg.getOwners().removePlayer(uuid);
				player.sendMessage(ChatColor.GREEN + name + " удалён из владельцев " + region + ".");
				break;
			}
			case "removemember": {
				rg.getMembers().removePlayer(uuid);
				player.sendMessage(ChatColor.GREEN + name + " удалён из участников " + region + ".");
				break;
			}
			case "addowner": {
				rg.getOwners().addPlayer(uuid);
				player.sendMessage(ChatColor.GREEN + name + " добавлен во владельцы " + region + ".");
				break;
			}
			case "addmember": {
				rg.getMembers().addPlayer(uuid);
				player.sendMessage(ChatColor.GREEN + name + " добавлен в участники " + region + ".");
				break;
			}
		}
		// Сохраняем изменения
		if(!saveChanges(world))
			player.sendMessage(ChatColor.RED+"При попытке сохранения была найдена ошибка! Попробуйте позже.");
	}

	// Вообще, это можно сделать и без Essentials. Здесь скорей вопрос оптимизации
	/**
	 * @return ZERO_UUID если игрок онлайн, нормальный UUID если игрок оффлайн, null если не найден
	 */
	private UUID getUniqueId(String name) {
		for(Player player : Bukkit.getServer().getOnlinePlayers())
			if(name.equalsIgnoreCase(player.getName()))
				return ZERO_UUID;
		User essUser = ess.getOfflineUser(name);
		return essUser == null ? null : essUser.getConfigUUID();
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
