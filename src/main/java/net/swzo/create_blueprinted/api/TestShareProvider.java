package net.swzo.create_blueprinted.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.net.URL;

import static net.swzo.create_blueprinted.CreateBlueprinted.rl;

public class TestShareProvider implements ShareProvider {
    @Override
    public ResourceLocation id() {
        return rl("testing");
    }

    @Override
    public Component destinationName() {
        return Component.literal("beep beep");
    }

    @Override
    public String destinationUrl() {
        return "https://testing.com";
    }

    @Override
    public @Nullable URL handle(String fileName, byte[] imageByteArray) {
        return null;
    }
}
