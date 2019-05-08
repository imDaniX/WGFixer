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
		String[] msg = event.getMessage().toLowerCase().split(" ");
		if (
				msg.length < 4 
				|| 
				!(((msg[0].equals("/rg") || msg[0].equals("/region") || msg[0].equals("/regions") || msg[0].equals("/worldguard:rg") || msg[0].equals("/worldguard:region") || msg[0].equals("/worldguard:regions")) 
				&&
				(msg[1].equals("removeowner") || msg[1].equals("ro") || msg[1].equals("removemember") || msg[1].equals("rm") || msg[1].equals("remmember") || msg[1].equals("removemem") || msg[1].equals("remmem") || msg[1].equals("addowner") || msg[1].equals("ao") || msg[1].equals("addmember") || msg[1].equals("am") || msg[1].equals("addmem"))) 
				&&
				!msg[3].equals("-a"))
			) return;
		User essUser = ess.getOfflineUser(msg[3]);
		if (essUser == null) { 
			player.sendMessage("Игрока " + msg[3] + " ещё небыло на сервере!");
			event.setCancelled(true);
			return;
		}
		UUID uuid = essUser.getConfigUUID();
		if (Bukkit.getOfflinePlayer(uuid).isOnline())
			return;
		ProtectedRegion rg = wg.getPlatform().getRegionContainer().get(BukkitAdapter.adapt(player.getWorld())).getRegion(msg[2]);
		if (rg == null) {
			player.sendMessage(ChatColor.RED + "Неизвестный регион " + msg[2]);
			return;
		}
		switch (msg[1]) {
			case "removeowner": case "ro": {
				if (!canAffect(rg, "removeowner", player))
					return;
				rg.getOwners().removePlayer(uuid);
				player.sendMessage(ChatColor.GREEN + msg[3] + " удалён из владельцев " + msg[2]);
				break;
			}
			case "removemember": case "rm": case "remmember": case "removemem": case"remmem": {
				if (!canAffect(rg, "removemember", player))
					return;
				rg.getMembers().removePlayer(uuid);
				player.sendMessage(ChatColor.GREEN + msg[3] + " удалён из участников " + msg[2]);
				break;
			}
			case "addowner": case "ao": {
				if (!canAffect(rg, "addowner", player))
					return;
				rg.getOwners().addPlayer(uuid);
				player.sendMessage(ChatColor.GREEN + msg[3] + " добавлен во владельцы " + msg[2]);
				break;
			}
			case "addmember": case "am": case "addmem": {
				if (!canAffect(rg, "addmember", player))
					return;
				rg.getMembers().addPlayer(uuid);
				player.sendMessage(ChatColor.GREEN + msg[3] + " добавлен в участники " + msg[2]);
				break;
			}
		}
		if(!saveChanges(player.getWorld()))
			player.sendMessage(ChatColor.RED+"Error, not saved");
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