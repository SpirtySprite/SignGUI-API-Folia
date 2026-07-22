package me.kirug.gui.internal;

import me.kirug.gui.internal.reflect.Reflect;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

// Folia needs per-entity scheduling; older Paper/Spigot (no Entity#getScheduler before 1.19.4)
// falls back to the main-thread Bukkit scheduler. The Folia branch only runs where the method
// exists, so 1.16.5 never tries to link it.
public final class Schedulers {

    private static final boolean FOLIA =
            Reflect.tryClass("io.papermc.paper.threadedregions.RegionizedServer") != null;

    private Schedulers() {
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    public static void forEntity(Plugin plugin, Entity entity, Runnable task) {
        if (plugin == null || !plugin.isEnabled()) {
            return;
        }
        try {
            if (FOLIA) {
                entity.getScheduler().run(plugin, scheduled -> task.run(), null);
            } else {
                Bukkit.getScheduler().runTask(plugin, task);
            }
        } catch (Throwable ignored) {
            // Plugin disabling mid-flight, or the entity is gone.
        }
    }

    public static void forEntityLater(Plugin plugin, Entity entity, Runnable task, long delayTicks) {
        if (plugin == null || !plugin.isEnabled()) {
            return;
        }
        long delay = Math.max(1L, delayTicks);
        try {
            if (FOLIA) {
                entity.getScheduler().runDelayed(plugin, scheduled -> task.run(), null, delay);
            } else {
                Bukkit.getScheduler().runTaskLater(plugin, task, delay);
            }
        } catch (Throwable ignored) {
            // Plugin disabling mid-flight, or the entity is gone.
        }
    }
}
