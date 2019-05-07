package ru.intervi.wgfixer;

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
	private WorldGuard wg = null;
	private Essentials ess = (Essentials) Bukkit.getServer().getPluginManager().getPlugin("Essentials");
	@Override
	public void onEnable() {
		if (!Bukkit.getOnlineMode()) {
			getServer().getPluginManager().registerEvents(this, this);
			wg = WorldGuard.getInstance();
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onCommand(PlayerCommandPreprocessEvent event) {
		Player player = event.getPlayer();
		String msg[] = event.getMessage().toLowerCase().split(" ");
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
		}
		else {
		UUID uuid = essUser.getConfigUUID();
		if (Bukkit.getOfflinePlayer(uuid).isOnline()) return;
		ProtectedRegion rg = wg.getPlatform().getRegionContainer().get(BukkitAdapter.adapt(player.getWorld())).getRegion(msg[2]);
		if (rg == null) {
			player.sendMessage(ChatColor.RED.toString() + "Неизвестный регион " + msg[2]);
			return;
		}
		if (msg[1].equals("removeowner") || msg[1].equals("ro")){
			if (
					!((rg.getOwners().contains(player.getUniqueId()) && player.hasPermission("worldguard.region.removeowner.own.*"))
					|| 
					player.hasPermission("worldguard.region.removeowner.*")
					||
					(rg.getMembers().contains(player.getUniqueId()) && player.hasPermission("worldguard.region.removeowner.member.*")))
				) return;
			rg.getOwners().removePlayer(uuid);
			player.sendMessage(ChatColor.GREEN.toString() + msg[3] + " удалён из владельцев " + msg[2]);
			try {
				if (!wg.getPlatform().getRegionContainer().get(BukkitAdapter.adapt(player.getWorld())).saveChanges()) player.sendMessage("error, not saved");
			} catch(Exception e) {e.printStackTrace();}
			event.setCancelled(true);
		} else if (msg[1].equals("removemember") || msg[1].equals("rm") || msg[1].equals("remmember") || msg[1].equals("removemem") || msg[1].equals("remmem")) {
			if (
					!((rg.getOwners().contains(player.getUniqueId()) && player.hasPermission("worldguard.region.removemember.own.*"))
					|| 
					player.hasPermission("worldguard.region.removemember.*")
					||
					(rg.getMembers().contains(player.getUniqueId()) && player.hasPermission("worldguard.region.removemember.member.*")))
				) return;
			rg.getMembers().removePlayer(uuid);
			player.sendMessage(ChatColor.GREEN.toString() + msg[3] + " удалён из участников " + msg[2]);
			try {
				if (!wg.getPlatform().getRegionContainer().get(BukkitAdapter.adapt(player.getWorld())).saveChanges()) player.sendMessage("error, not saved");
			} catch(Exception e) {e.printStackTrace();}
			event.setCancelled(true);
		} else if (msg[1].equals("addowner") || msg[1].equals("ao")) {
			if (
					!((rg.getOwners().contains(player.getUniqueId()) && player.hasPermission("worldguard.region.addowner.own.*"))
					|| 
					player.hasPermission("worldguard.region.addowner.*")
					||
					(rg.getMembers().contains(player.getUniqueId()) && player.hasPermission("worldguard.region.addowner.member.*")))
				) return;
			rg.getOwners().addPlayer(uuid);
			player.sendMessage(ChatColor.GREEN.toString() + msg[3] + " добавлен во владельцы " + msg[2]);
			try {
				if (!wg.getPlatform().getRegionContainer().get(BukkitAdapter.adapt(player.getWorld())).saveChanges()) player.sendMessage("error, not saved");
			} catch(Exception e) {e.printStackTrace();}
			event.setCancelled(true);
		} else if (msg[1].equals("addmember") || msg[1].equals("am") || msg[1].equals("addmem")) {
			if (
					!((rg.getOwners().contains(player.getUniqueId()) && player.hasPermission("worldguard.region.addmember.own.*"))
					|| 
					player.hasPermission("worldguard.region.addmember.*")
					||
					(rg.getMembers().contains(player.getUniqueId()) && player.hasPermission("worldguard.region.addmember.member.*"))) 
				) return;
			rg.getMembers().addPlayer(uuid);
			player.sendMessage(ChatColor.GREEN.toString() + msg[3] + " добавлен в участники " + msg[2]);
			try {
				if (!wg.getPlatform().getRegionContainer().get(BukkitAdapter.adapt(player.getWorld())).saveChanges()) player.sendMessage("error, not saved");
			} catch(Exception e) {e.printStackTrace();}
			event.setCancelled(true);
			}
		}
	}
}