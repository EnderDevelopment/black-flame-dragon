package com.enderdevelopment.plugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(new DragonListener(this), this);
    }

    @Override
    public void onDisable() {
        DragonListener listener = DragonListener.instance;
        if (listener != null) {
            listener.restoreAll();
        }
    }
}
