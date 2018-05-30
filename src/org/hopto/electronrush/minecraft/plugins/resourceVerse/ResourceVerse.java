package org.hopto.electronrush.minecraft.plugins.resourceVerse;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Random;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.hopto.electronrush.minecraft.plugins.resourceVerse.listeners.MVPortalListener;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import com.onarandombox.MultiverseCore.destination.WorldDestination;

public class ResourceVerse extends JavaPlugin {
	
	MultiverseCore mcore;
	Logger log;
	FileConfiguration config;
	
	MultiverseWorld worldA;
	MultiverseWorld worldB;
	
	WorldDestination spawnWorldDestination = new WorldDestination();
	
	boolean isAActive = true;
	int refreshHour = 0;
	int refreshPeriod = 1;
	Calendar nextRefresh;
	SimpleDateFormat dateFormat;
	
	@Override
	public void onEnable() {
		log = getLogger();
		log.info("Loading Configuration.");
		saveDefaultConfig();
		config = getConfig();
		log.info("Loaded Configuration.");
		log.info("Loading MultiverseCore.");
		Plugin plugin = getServer().getPluginManager().getPlugin("Multiverse-Core");
		
		if(plugin instanceof MultiverseCore) {
			mcore = (MultiverseCore)plugin;
			log.info("Loaded MultiverseCore.");
		}else {
			log.warning("Error, Incorrect MultiverseCore.");
		}
		
		log.info("Checking for Resource Worlds");
		MultiverseWorld worldA = mcore.getMVWorldManager().getMVWorld(config.getString("resouceWorldAName"));
		if(worldA != null) {
			this.worldA = worldA;
			log.info("Found Resource World A: " + config.getString("resouceWorldAName"));
		}else {
			log.info("Unable to find Resource World A: " + config.getString("resouceWorldAName"));
		}
		
		MultiverseWorld worldB = mcore.getMVWorldManager().getMVWorld(config.getString("resouceWorldBName"));
		if(worldB != null) {
			this.worldB = worldB;
			log.info("Found Resource World B: " + config.getString("resouceWorldBName"));
		}else {
			log.info("Unable to find Resource World B: " + config.getString("resouceWorldBName"));
		}
		
		spawnWorldDestination.setDestination(mcore, "w:" + mcore.getMVWorldManager().getSpawnWorld().getName());
		
		refreshHour = config.getInt("refreshTime");
		refreshPeriod = config.getInt("refreshPeriod");
		
		getServer().getPluginManager().registerEvents(new MVPortalListener(this), this);
		
		Calendar cal = Calendar.getInstance();
		dateFormat = new SimpleDateFormat(config.getString("dateFormat"));
		
		long now = cal.getTimeInMillis();
		
		if(cal.get(Calendar.HOUR_OF_DAY) >= refreshHour) {
			cal.add(Calendar.DATE, 1);
		}
		
		cal.set(Calendar.HOUR_OF_DAY, refreshHour);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		
		long offset = cal.getTimeInMillis() - now;
		long ticks = offset / 50L;
		nextRefresh = cal;
		
		getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			
			@Override
			public void run() {
				nextRefresh.add(Calendar.DATE, refreshPeriod);
				refresh();
			}
			
		},ticks ,(long)refreshPeriod * 172800L);
	}
	
	@Override
	public void onDisable() {
		getServer().getScheduler().cancelTasks(this);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String lable, String[] args) {
		if(lable.equalsIgnoreCase("rvrefresh")) {
			refresh();
			return true;
		}else if(lable.equalsIgnoreCase("rvexit")){
			if(sender instanceof Player) {
				mcore.getSafeTTeleporter().safelyTeleport(getServer().getConsoleSender(), (Player)sender, spawnWorldDestination);
				return true;
			}else {
				sender.sendMessage("You have to be a player to use this command.");
				return false;
			}
		}else if(lable.equalsIgnoreCase("rvnext")){
			Date next = nextRefresh.getTime();
			sender.sendMessage("The next refresh is schduled at " + dateFormat.format(next) + ".");
			return true;
		}else {
			return false;
		}
	}
	
	Location findSpawn(MultiverseWorld world,Location spawn) {
		World CBWorld = world.getCBWorld();
		Random rng = new Random();
		while(true) {
			spawn.setY(200);
			while(spawn.getBlockY() > 40) {
				Block head = CBWorld.getBlockAt(spawn.add(0, 1, 0));
				Block feet = CBWorld.getBlockAt(spawn);
				Block ground = CBWorld.getBlockAt(spawn.add(0, -1, 0));
				log.info("Checking level " + spawn.getBlockY() + " " + head.getType()+ "|" + feet.getType() + "|" + ground.getType());
				if(head.isEmpty() && feet.isEmpty()){
					if(ground.getType() != Material.LAVA && ground.getType() != Material.AIR) {
						return spawn;
					}
				}
				spawn = spawn.add(0, -1, 0);
			}
			spawn.add(rng.nextDouble()*3, 0, rng.nextDouble()*3);
		}
	}
	
	void refresh() {
		if(isAActive) {
			worldB =regenerateWorld(worldB);
			isAActive = false;
			sendWorldWideMessage(worldA,config.getString("worldDeactivationMessage"));
		}else {
			worldA = regenerateWorld(worldA);
			sendWorldWideMessage(worldB,config.getString("worldDeactivationMessage"));
			isAActive = true;
		}
		sendServerWideMessage(config.getString("worldRefreshMessage"));
	}
	
	MultiverseWorld regenerateWorld(MultiverseWorld world) {
		log.info("Regenerating " + world.getName() + ".");
		mcore.getMVWorldManager().regenWorld(world.getName(), true, true, "");
		world = mcore.getMVWorldManager().getMVWorld(world.getName());
		Location newSpawn = findSpawn(world, world.getSpawnLocation());
		if(newSpawn != null) {
			world.setSpawnLocation(newSpawn);
		}else {
			log.warning("Can't find Spawn in " + world.getName() +".");
		}
		log.info("Done.");
		return world;
	}
	
	
	public static Collection<Player> getPlayersInWorld(MultiverseWorld world){
		return world.getCBWorld().getEntitiesByClass(Player.class);
	}
	
	public static void sendWorldWideMessage(MultiverseWorld world,String message) {
		Collection<Player> players = getPlayersInWorld(world);
		
		for(Player player:players) {
			player.sendMessage(message);
		}
	}
	
	public void sendServerWideMessage(String message) {
		Collection<? extends Player> players =  getServer().getOnlinePlayers();
		for(Player player:players) {
			player.sendMessage(message);
		}
	}
	
	public MultiverseWorld getActiveWorld() {
		if(isAActive) {
			return worldA;
		}else {
			return worldB;
		}
	}
	
	public MultiverseWorld getInactiveWorld() {
		if(isAActive) {
			return worldB;
		}else {
			return worldA;
		}
	}
	
	public MultiverseCore getMVCore() {
		return mcore;
	}
	

}