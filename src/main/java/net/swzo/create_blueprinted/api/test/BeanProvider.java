package net.swzo.create_blueprinted.api.test;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.swzo.create_blueprinted.api.ShareProvider;
import net.swzo.create_blueprinted.render.SchematicRenderSettings;
import org.jetbrains.annotations.Nullable;

import java.net.URL;

import static net.swzo.create_blueprinted.CreateBlueprinted.rl;

public class BeanProvider implements ShareProvider {
    @Override
    public ResourceLocation id() {
        return rl("beanz");
    }

    @Override
    public Component destinationName() {
        return Component.literal("Beans on toast!");
    }

    @Override
    public String destinationUrl() {
        return "https://beanz.can";
    }

    @Override
    public boolean beforeBake(ResourceLocation handlerId, String schematicName, SchematicRenderSettings renderSettings) {
        return true;
    }

    @Override
    public boolean silenceMessages() {
        return true;
    }
}
