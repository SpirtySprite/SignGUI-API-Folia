package me.kirug.signgui;

import me.kirug.anvilgui.AnvilGUI;
import org.bukkit.plugin.java.JavaPlugin;

public class SignGuiPlugin extends JavaPlugin {

    private static SignGuiPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        SignGUI.init(this);
        AnvilGUI.init(this);
        getLogger().info("SignGuiAPI enabled.");
    }

    @Override
    public void onDisable() {
        SignGUI.shutdown();
    }

    public static SignGuiPlugin getInstance() {
        return instance;
    }
}
