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

### Dependency Configuration
To use this API in your project, add the compiled JAR as a system dependency or install it to your local Maven repository.

#### Option 1: Using a "libs" folder
Create a `libs` directory at your project's root, place the `.jar` inside, and add this to your `pom.xml`:

```xml
<dependency>
    <groupId>me.kirug</groupId>
    <artifactId>SignGuiAPI</artifactId>
    <version>1.0-SNAPSHOT</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/libs/SignGuiAPI-1.0-SNAPSHOT.jar</systemPath>
</dependency>
```

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
