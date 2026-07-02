package net.swzo.create_blueprinted.util;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.schematics.SchematicItem;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.swzo.create_blueprinted.CreateBlueprinted;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class SchematicUtils {

    public static StructureTemplate loadTemplateFromSchematic(String schematicName) {
        Minecraft client = Minecraft.getInstance();
        String owner = Objects.requireNonNull(client.player).getGameProfile().getName();
        ItemStack blueprint = AllItems.SCHEMATIC.asStack();

        blueprint.set(AllDataComponents.SCHEMATIC_OWNER, owner);
        blueprint.set(AllDataComponents.SCHEMATIC_FILE, schematicName);
        return SchematicItem.loadSchematic(client.level, blueprint);
    }

    /**
     * Gets a stream of all loaded schematics by name.
     *
     * @return Stream of schematic names
     */
    public static Stream<String> getAllSchematicNames() {
        return CreateClient.SCHEMATIC_SENDER.getAvailableSchematics().stream().map(Component::getString);
    }

    public static Optional<String> getSchematicNameFromIndex(int schematicIndex) {
        List<Component> availableSchematics = CreateClient.SCHEMATIC_SENDER.getAvailableSchematics();

        int numAvailableSchematics = availableSchematics.size();
        if (schematicIndex < 0 || schematicIndex >= numAvailableSchematics) {
            CreateBlueprinted.LOGGER.warn("Unable to retrieve a loaded schematic from the following index: {}. " +
                    "Expected range: 0 to {}", schematicIndex, numAvailableSchematics - 1);
            return Optional.empty();
        }
        return Optional.of(availableSchematics.get(schematicIndex).getString());
    }

    public static String sanitizeFileName(String schematicFileName) {
        if (schematicFileName.endsWith(".nbt"))
            schematicFileName = schematicFileName.substring(0, schematicFileName.length() - 4);
        return schematicFileName;
    }
}
