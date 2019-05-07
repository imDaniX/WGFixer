package ru.intervi.wgfixer;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.entity.Player;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import java.util.UUID;

public class Main extends JavaPlugin implements Listener {
	private WorldGuardPlugin wg = null;
	
	@Override
	public void onEnable() {
		if (!Bukkit.getOnlineMode()) {
			getServer().getPluginManager().registerEvents(this, this);
			wg = getWorldGuard();
		}
	}
	
	private WorldGuardPlugin getWorldGuard() { //получить WorldGuard
	    Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");
	 
	    // WorldGuard may not be loaded
	    if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
	        return null; // Maybe you want throw an exception instead
	    }
	 
	    return (WorldGuardPlugin) plugin;
	}
	
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onCommand(PlayerCommandPreprocessEvent event) {
		String msg[] = event.getMessage().toLowerCase().split(" ");
		if (msg.length < 4 || (!msg[0].equals("rg") || !msg[0].equals("region"))) return;
		UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + msg[3]).getBytes());
		if (Bukkit.getOfflinePlayer(uuid).isOnline()) return;
		Player player = event.getPlayer();
		ProtectedRegion rg = wg.getRegionManager(player.getWorld()).getRegion(msg[2]);
		if (rg == null) {
			player.sendMessage("invalid region " + msg[2]);
			return;
		}
		if (msg[1].equals("removeowner")) {
			if (
					!(rg.getOwners().contains(player.getUniqueId()) && (player.hasPermission("worldguard.region.removeowner.own.*") || player.hasPermission("worldguard.region.removeowner.*")))
					||
					!(rg.getMembers().contains(player.getUniqueId()) && player.hasPermission("worldguard.region.removeowner.member.*"))
				) return;
			rg.getOwners().removePlayer(uuid);
			player.sendMessage("owner " + msg[3] + " removing from " + msg[2]);
			try {
				if (!wg.getRegionManager(player.getWorld()).saveChanges()) player.sendMessage("error, not saved");
			} catch(Exception e) {e.printStackTrace();}
			event.setCancelled(true);
		} else if (msg[1].equals("removemember")) {
			if (
					!(rg.getOwners().contains(player.getUniqueId()) && (player.hasPermission("worldguard.region.removemember.own.*") || player.hasPermission("worldguard.region.removemember.*")))
					||
					!(rg.getMembers().contains(player.getUniqueId()) && player.hasPermission("worldguard.region.removemember.member.*"))
				) return;
			rg.getMembers().removePlayer(uuid);
			player.sendMessage("member " + msg[3] + " removing from " + msg[2]);
			try {
				if (!wg.getRegionManager(player.getWorld()).saveChanges()) player.sendMessage("error, not saved");
			} catch(Exception e) {e.printStackTrace();}
			event.setCancelled(true);
		}
	}
}