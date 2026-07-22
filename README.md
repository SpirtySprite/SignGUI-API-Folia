# SignGuiAPI

Sign and anvil text input for Paper and Folia, from **1.16.5 to 1.21.x**. No ProtocolLib, no other dependencies — drop it in and it works.

The sign editor is opened with NMS packets built through reflection, and the player's reply is read with a small Netty handler on their connection. The anvil input is plain Bukkit plus `PrepareAnvilEvent`. Everything runs correctly on Folia's threading and falls back to the main thread on older Paper/Spigot.

## Requirements

- Java 21
- Paper or Folia 1.16.5+

That's it. ProtocolLib is no longer needed (it was in 1.x).

## Getting it

Via JitPack:

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>
```

```xml
<dependency>
    <groupId>com.github.SpirtySprite</groupId>
    <artifactId>SignGUI-API-Folia</artifactId>
    <version>2.0.0</version>
    <scope>provided</scope>
</dependency>
```

Keep `provided` if you run the jar as its own plugin. If you'd rather bundle it into your plugin, use `compile` and shade it with a relocation so two plugins can't clash:

```xml
<relocation>
    <pattern>me.kirug</pattern>
    <shadedPattern>your.package.gui</shadedPattern>
</relocation>
```

## Setup

Call `init` once in `onEnable` for whichever APIs you use:

```java
@Override
public void onEnable() {
    SignGUI.init(this);
    AnvilGUI.init(this);
}
```

`SignGUI` also has a `shutdown()` you can call in `onDisable` if you want the connection readers cleaned up explicitly.

## Signs

```java
SignGUI.builder()
    .type(Material.DARK_OAK_SIGN)
    .setLines("Line 1", "Line 2", "", "")
    .setHandler((player, lines) -> player.sendMessage("You wrote: " + Arrays.toString(lines)))
    .build()
    .open(player);
```

Only care about one line?

```java
SignGUI.builder()
    .setLine(0, "Enter code:")
    .setHandler(0, (player, code) -> player.sendMessage("Code: " + code))
    .build()
    .open(player);
```

A throwaway sign is shown a few blocks under the player and reverted the instant they finish, so nothing is ever placed in the world.

## Anvils

```java
AnvilGUI.builder()
    .title("Enter a name")   // shown on Paper; some clients force the default anvil title
    .text("default")         // prefills the box
    .item(Material.PAPER)    // left-slot item, optional
    .disallowEmpty()         // optional: ignore a blank confirm
    .setHandler((player, input) -> player.sendMessage("You typed: " + input))
    .build()
    .open(player);
```

The player types in the rename field and clicks the result slot to confirm. Nothing else in the anvil can be touched.

## A note on versions

Signs became two-sided in 1.20 and the packets changed shape; that's handled for you. The sign code reaches into server internals by reflection, so if some future Minecraft version rearranges the sign packets, that's where a fix would go — it's written to log and bail rather than break anyone's connection. The anvil code is plain Bukkit and doesn't touch any of that.
