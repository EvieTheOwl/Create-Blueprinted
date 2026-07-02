# Create: Blueprinted

Originally made for the [Brassworks SMP](https://brassworks.opnsoc.org/), this mod is free and open for anyone to use!

Create: Blueprinted allows you to render your Create mod schematics into high-resolution PNG images straight from the game.
___
## Dependencies
Version 2+ requires [Create Schematic Preview](https://modrinth.com/mod/create-schematic-preview). 
This is used to preview schematics and select their orientation (rotation) before rendering.
___
## Features
* **High-Resolution Renders:** Render your `.nbt` schematics to crisp PNGs directly from the Schematic Table UI or a command.
* **Adjust Orientation:** Use the schematic preview to pick a unique orientation for your image or enter precise angles from a command.
* **Adjustable Resolution:** Choose your output width, from a quick preview, all the way up to 8192px posters.
* **Anti-Aliasing:** Built-in supersampling smooths out jagged edges. It's on by default, and you can tune or disable it.
* **Fluids Included:** Water, lava, and waterlogged blocks are drawn in the render instead of being skipped.
* **Background Processing:** Most operations happen in the background and wont interrupt your game. Live progress is displayed in the action bar.
* **Schematic Table Bug Fix:** Prevents long schematic names overflowing the scroll input box bounds.
___
## Usage
### In-Game UI
You can render schematics directly from the Schematic Table. Just select your schematic and click the new render button!

Hold **Shift** while clicking to render at 2048px (instead of the default 1024px).
Rotate the schematic preview to select a unique orientation for your image.

![Render Button in UI](showcase/ui.png)
![Example Rendered Output](showcase/render.png)
- *Rendered Build by [LiukRast](https://liukrast.net/)*

### Commands
If you need more control over the output, you can use the built-in command:
```bash
/schematic [export/share] <filename> [width] [orientation] [antialiasing]
```
* **export** - Save image to a PNG file within the `./schematics` folder.
* **share** - Copy the image to your clipboard. This functionality can be overriden by addon mods to send to other platforms like Discord.
* **width** — Total output width in pixels (64–8192).
* **orientation** — The yaw & pitch of the image. See: [Rotation - Minecraft Wiki](https://minecraft.wiki/w/Argument_types#rotation) for more info.
* **antialiasing** — Supersampling factor: `1` = off, `2`–`4` = progressively smoother edges (defaults to `2`).

## Developers
To add Blueprinted as a dependency add the following to your `build.gradle` file:
```gradle
repositories {
    maven { url = "https://api.modrinth.com/maven" }
}

dependencies {
    compileOnly "maven.modrinth:create-blueprinted:${blueprinted_version}+mc${minecraft_version}-neoforge"
    localRuntime "maven.modrinth:create-blueprinted:${blueprinted_version}+mc${minecraft_version}-neoforge"
}
```
Make sure to define `blueprinted_version` and `minecraft_version` within the `gradle.properties` file.

### Events
A couple events are provided which you can utilize in your mod:
- `RenderSchematicEvent.Pre` - Fired before a schematic is rendered. Includes the `SchematicLevel` which represents the content that is about to be rendered. 
- `RenderSchematicEvent.Post` - Fired after a schematic is rendered and before it is about to be exported or shared. includes a byte array of the rendered PNG image.

### Examples

```java
@SubscribeEvent
public static void beforeRenderImage(RenderSchematicImageEvent.Pre e) {
    // Get the file name and level content (read only)
    String fileName = e.getFileName();
    SchematicLevel level = e.getImageContent();

    // Set the images width to 256 pixels and apply a purple background
    e.modifyRenderSettings().imageWidth(256).backgroundColor(Color.PURPLE);
    // Cancel the image render event. Useful if you want to implement your own rendering logic.
    e.setCanceled(true);
}

@SubscribeEvent
public static void onExportOrShareImage(RenderSchematicImageEvent.Post e) {
    // The player is choosing to share a file instead of exporting it to a file
    boolean isSharing = e.getAction() == RenderSchematicImageEvent.Action.SHARE;
    // You can now implement your own custom image sharing logic
    if (isSharing) shareImageToBob(e.getFileName(), e.getImageContent());
}
```

## License
This project is licensed under MIT
