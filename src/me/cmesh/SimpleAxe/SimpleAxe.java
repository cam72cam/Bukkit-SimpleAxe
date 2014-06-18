package me.cmesh.SimpleAxe;

import org.bukkit.plugin.java.JavaPlugin;

public class SimpleAxe extends JavaPlugin {
	
	private AxeListener listener; 
	
	public SimpleAxe () {
		listener = new AxeListener();
	}
	
	public void onEnable() {
		this.getServer().getPluginManager().registerEvents(listener, this);
	}
	
	public void onDisable() {
		
	}
}
