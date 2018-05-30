package org.hopto.electronrush.minecraft.plugins.resourceVerse.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.hopto.electronrush.minecraft.plugins.resourceVerse.ResourceVerse;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.destination.WorldDestination;
import com.onarandombox.MultiversePortals.event.MVPortalEvent;

public final class MVPortalListener implements Listener {
	
	ResourceVerse rv;
	
	public MVPortalListener(ResourceVerse rv) {
		this.rv = rv;
	}
	
	@EventHandler
	public void onMVPortalEvent(MVPortalEvent event) {
		if(event.getSendingPortal().getName().toLowerCase().startsWith(rv.getConfig().getString("portalPrefix").toLowerCase())) {
			event.setCancelled(true);
			if(event.getTeleportee().hasPermission("ResourceVerse.allowed")) {
				MultiverseCore mcore = rv.getMVCore();
				WorldDestination destination = new WorldDestination();
				destination.setDestination(mcore, "w:" + rv.getActiveWorld().getName());
				mcore.getSafeTTeleporter().safelyTeleport(rv.getServer().getConsoleSender(), event.getTeleportee(),destination);
			}else {
				event.getTeleportee().sendMessage("You do not have permission to access the resource world.");
			}
		}
	}
}
