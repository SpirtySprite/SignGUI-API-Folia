# SignGuiAPI

A simple, lightweight, and optimized API for creating Sign GUIs on Minecraft servers (Paper/Folia) 1.21+ using ProtocolLib.

## Features
*   ✅ **Folia & Paper Support**: Built to work on modern multi-threaded server environments.
*   ✅ **ProtocolLib Powered**: Uses ProtocolLib for reliable packet handling and maximum compatibility.
*   ✅ **Optimized**: Automatic memory management with cleanup on player disconnect.
*   ✅ **Flexible**: Supports all sign types (Oak, Spruce, etc.) via `Material`.

## Prerequisites
*   **ProtocolLib** 5.1.0+ must be installed on your server.
*   **Java 21** or higher.

## Installation

### Deployment (Runtime)
⚠️ **Note**: This project is a **Spigot Plugin**. 
To use it on your server:
1.  Build the plugin (`mvn clean package`).
2.  Place the generated `.jar` file in your server's **`plugins/`** folder.
3.  Ensure **ProtocolLib** is also present in the `plugins/` directory.

### Dependency Configuration (JitPack)
The easiest way to use this API is via [JitPack](https://jitpack.io/#SpirtySprite/SignGUI-API-Folia).

#### 1. Add the Repository
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

#### 2. Add the Dependency
```xml
<dependency>
    <groupId>com.github.SpirtySprite</groupId>
    <artifactId>SignGUI-API-Folia</artifactId>
    <version>v1.0.0</version>
    <scope>provided</scope>
</dependency>
```

### Advanced: Shading (No Plugin Install Required)
If you want to bundle SignGuiAPI directly into your plugin (so users don't have to install the SignGuiAPI.jar separately), use the Maven Shade Plugin:

1.  Change the dependency scope to `compile` instead of `provided`.
2.  Add the Shade Plugin to your `pom.xml`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.5.3</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>shade</goal>
            </goals>
            <configuration>
                <relocations>
                    <relocation>
                        <pattern>me.kirug.signgui</pattern>
                        <shadedPattern>your.package.signgui</shadedPattern>
                    </relocation>
                </relocations>
            </configuration>
        </execution>
    </executions>
</plugin>
```
> [!IMPORTANT]
> Always **relocate** the package to avoid conflicts with other plugins using the same API.

## Usage

### 1. Initialization
You must initialize the API in your main plugin's `onEnable()` method:

```java
@Override
public void onEnable() {
    // Initializes the API and registers necessary listeners
    SignGUI.init(this);
}
```

### 2. Simple Example
Open a default (Oak) GUI and retrieve all lines:

```java
SignGUI.builder()
    .setLines("Line 1", "Line 2", "", "")
    .setHandler((player, lines) -> {
        player.sendMessage("You wrote: " + Arrays.toString(lines));
    })
    .build()
    .open(player);
```

### 3. Changing the Sign Type
You can customize the sign's appearance using `.type(Material)`:

```java
SignGUI.builder()
    .type(Material.DARK_OAK_SIGN)
    .setLines("Initial", "Text", "Goes", "Here")
    .setHandler((player, lines) -> { ... })
    .build()
    .open(player);
```

### 4. Handling a Single Line
To capture a specific piece of information (e.g., a code or name), use the single-line helper:

```java
SignGUI.builder()
    .type(Material.SPRUCE_SIGN)
    .setLine(0, "Enter code:")
    .setHandler(0, (player, result) -> {
        // 'result' contains only the text from line 0
        player.sendMessage("Received code: " + result);
    })
    .build()
    .open(player);
```
