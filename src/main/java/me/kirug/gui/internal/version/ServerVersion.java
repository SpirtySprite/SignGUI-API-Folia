package me.kirug.gui.internal.version;

import org.bukkit.Bukkit;

// Figures out which MC version we're on and how the runtime is mapped, so the
// reflection layer knows which class names to try and which quirks to expect.
public final class ServerVersion {

    private static final ServerVersion CURRENT = detect();

    private final int minor;
    private final int patch;
    private final String craftbukkitTag;
    private final boolean mojangMapped;

    private ServerVersion(int minor, int patch, String craftbukkitTag, boolean mojangMapped) {
        this.minor = minor;
        this.patch = patch;
        this.craftbukkitTag = craftbukkitTag;
        this.mojangMapped = mojangMapped;
    }

    public static ServerVersion current() {
        return CURRENT;
    }

    public int minor() {
        return minor;
    }

    public int patch() {
        return patch;
    }

    public String craftbukkitTag() {
        return craftbukkitTag;
    }

    public boolean mojangMapped() {
        return mojangMapped;
    }

    // Signs went two-sided in 1.20; the open/update packets carry a front-text flag from then on.
    public boolean twoSidedSigns() {
        return minor >= 20;
    }

    public boolean legacyVersionedPackage() {
        return minor <= 16;
    }

    public boolean isAtLeast(int minMinor, int minPatch) {
        return minor > minMinor || (minor == minMinor && patch >= minPatch);
    }

    private static ServerVersion detect() {
        int minor = 21;
        int patch = -1;

        // getBukkitVersion() looks like "1.16.5-R0.1-SNAPSHOT".
        String bukkitVersion = Bukkit.getBukkitVersion();
        try {
            String number = bukkitVersion.split("-", 2)[0];
            String[] parts = number.split("\\.");
            if (parts.length >= 2) {
                minor = Integer.parseInt(parts[1]);
            }
            if (parts.length >= 3) {
                patch = Integer.parseInt(parts[2]);
            }
        } catch (RuntimeException ignored) {
            // Defaults are fine; class resolution tries both name sets anyway.
        }

        // Only 1.16.x still relocates CraftBukkit into a versioned package.
        String tag = "";
        if (minor <= 16) {
            String pkg = Bukkit.getServer().getClass().getPackage().getName();
            int marker = pkg.lastIndexOf('.');
            if (marker != -1 && marker + 1 < pkg.length()) {
                tag = pkg.substring(marker + 1);
            }
        }

        // Paper flipped to Mojang mappings in 1.20.5.
        boolean mojang = minor > 20 || (minor == 20 && patch >= 5);

        return new ServerVersion(minor, patch, tag, mojang);
    }

    @Override
    public String toString() {
        return "1." + minor + (patch >= 0 ? "." + patch : "")
                + (mojangMapped ? " (mojang)" : " (spigot)")
                + (craftbukkitTag.isEmpty() ? "" : " [" + craftbukkitTag + "]");
    }
}
