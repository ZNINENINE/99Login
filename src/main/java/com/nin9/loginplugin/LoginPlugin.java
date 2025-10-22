package com.nin9.loginplugin;

import org.bukkit.plugin.java.JavaPlugin;

public class LoginPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info("99Login habilitado!");
    }

    @Override
    public void onDisable() {
        getLogger().info("99Login desabilitado.");
    }
}
