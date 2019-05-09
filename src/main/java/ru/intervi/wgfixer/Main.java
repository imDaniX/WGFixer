package ru.intervi.wgfixer;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.entity.Player;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import java.util.UUID;

public class Main extends JavaPlugin implements Listener {
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

	/*
		TODO:
		Добавить поддержку указания мира.
		В WorldGuard есть аргумент -w, который позволяет редактировать регион в другом мире.
		Например /rg addowner spawn -w world_the_end Vasya_Pupkin2001
	*/
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onCommand(PlayerCommandPreprocessEvent event) {
		Player player = event.getPlayer();
		String[] cmd = event.getMessage().toLowerCase().split(" ");
		if (
				cmd.length < 4
				|| 
				!(((cmd[0].equals("/rg") || cmd[0].equals("/region") || cmd[0].equals("/regions") || cmd[0].equals("/worldguard:rg") || cmd[0].equals("/worldguard:region") || cmd[0].equals("/worldguard:regions"))
				&&
				(cmd[1].equals("removeowner") || cmd[1].equals("ro") || cmd[1].equals("removemember") || cmd[1].equals("rm") || cmd[1].equals("remmember") || cmd[1].equals("removemem") || cmd[1].equals("remmem") || cmd[1].equals("addowner") || cmd[1].equals("ao") || cmd[1].equals("addmember") || cmd[1].equals("am") || cmd[1].equals("addmem")))
				&&
				!cmd[3].equals("-a"))
			) return;
		User essUser = ess.getOfflineUser(cmd[3]);
		if (essUser == null) { 
			player.sendMessage(ChatColor.RED + "Игрока " + cmd[3] + " ещё небыло на сервере!");
			event.setCancelled(true);
			return;
		}
		UUID uuid = essUser.getConfigUUID();
		if (Bukkit.getPlayer(uuid) != null)
			return;
		ProtectedRegion rg = wg.getPlatform().getRegionContainer().get(BukkitAdapter.adapt(player.getWorld())).getRegion(cmd[2]);
		if (rg == null) {
			player.sendMessage(ChatColor.RED + "Неизвестный регион " + cmd[2] + ".");
			return;
		}
		switch (cmd[1]) {
			case "removeowner": case "ro": {
				if (!canAffect(rg, "removeowner", player))
					return;
				rg.getOwners().removePlayer(uuid);
				player.sendMessage(ChatColor.GREEN + cmd[3] + " удалён из владельцев " + cmd[2] + ".");
				break;
			}
			case "removemember": case "rm": case "remmember": case "removemem": case"remmem": {
				if (!canAffect(rg, "removemember", player))
					return;
				rg.getMembers().removePlayer(uuid);
				player.sendMessage(ChatColor.GREEN + cmd[3] + " удалён из участников " + cmd[2] + ".");
				break;
			}
			case "addowner": case "ao": {
				if (!canAffect(rg, "addowner", player))
					return;
				rg.getOwners().addPlayer(uuid);
				player.sendMessage(ChatColor.GREEN + cmd[3] + " добавлен во владельцы " + cmd[2] + ".");
				break;
			}
			case "addmember": case "am": case "addmem": {
				if (!canAffect(rg, "addmember", player))
					return;
				rg.getMembers().addPlayer(uuid);
				player.sendMessage(ChatColor.GREEN + cmd[3] + " добавлен в участники " + cmd[2] + ".");
				break;
			}
			default: return;
		}
		if(!saveChanges(player.getWorld()))
			player.sendMessage(ChatColor.RED+"При попытке сохранения была найдена ошибка! Попробуйте позже.");
		event.setCancelled(true);
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
		return (region.getOwners().contains(player.getUniqueId()) && player.hasPermission("worldguard.region."+action+".own.*"))
				||
				player.hasPermission("worldguard.region."+action+".*")
				||
				(region.getMembers().contains(player.getUniqueId()) && player.hasPermission("worldguard.region."+action+".member.*"));
	}
}