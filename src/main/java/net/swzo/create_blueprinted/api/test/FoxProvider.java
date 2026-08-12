package net.swzo.create_blueprinted.api.test;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.swzo.create_blueprinted.api.ShareProvider;
import net.swzo.create_blueprinted.render.SchematicRenderSettings;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.List;
import java.util.Optional;

import static net.swzo.create_blueprinted.CreateBlueprinted.rl;

public class FoxProvider implements ShareProvider {
    @Override
    public ResourceLocation id() {
        return rl("foxy");
    }

    @Override
    public Component destinationName() {
        return Component.literal("Planet fox");
    }

    @Override
    public String destinationUrl() {
        return "https://foxyfox.com";
    }

    @Override
    public @Nullable URL onRender(ResourceLocation handlerId, String schematicName, SchematicRenderSettings renderSettings, byte[] imageByteArray) {
        return null;
    }

    @Override
    public List<Component> extras() {
        return List.of(Component.literal("Foxes are cute and mysterious").withStyle(ChatFormatting.GOLD),
                Component.empty(), Component.literal("beeeeeep!").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    @Override
    public boolean hideTooltipHints() {
        return true;
    }

    @Override
    public boolean includeSchematicData() {
        return true;
    }

    @Override
    public int priority() {
        return 5;
    }
}
