package me.kirug.signgui;


import org.bukkit.plugin.java.JavaPlugin;

public class SignGuiPlugin extends JavaPlugin {

    private static SignGuiPlugin instance;

    @Override
    public void onLoad() {
        // ProtocolLib is loaded automatically as a plugin dependency
    }

    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize the SignGUI API with this plugin instance
        SignGUI.init(this);
        
        getCommand("signinput").setExecutor(new TestCommand());
        getCommand("menutest").setExecutor(new MenuTestCommand(this));
        getLogger().info("SignGuiAPI has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("SignGuiAPI has been disabled!");
    }

    public static SignGuiPlugin getInstance() {
        return instance;
    }
}
