package net.swzo.create_blueprinted.config;

import net.createmod.catnip.config.ConfigBase;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

@EventBusSubscriber(Dist.CLIENT)
public class CreateBlueprintedConfig extends ConfigBase {

    public static final CreateBlueprintedConfig CONFIG = register(CreateBlueprintedConfig::new);

    public final ConfigBool enableImageSharing = b(true, "enableImageSharing",
            "Enable image sharing to a remote server if another mod provides this functionality.");

    public final ConfigBool usePreviewRotation = b(true, "usePreviewRotation",
            "If a schematic preview is available use its rotation when rendering an image.");

    public final ConfigInt defaultWidth = i(1024, 64, 8192, "defaultWidth",
            "Default width used whenever a player saves or shares a schematic image using a button.");

    public final ConfigInt alternateWidth = i(2048, 64, 8192, "alternateWidth",
            "Width used whenever a player holds SHIFT before saving or sharing a schematic image using a button.");

    public final ConfigInt defaultAntialiasing = i(2, 1, 4, "defaultAntialiasing",
            "Default antialiasing factor for a schematic Image.");

    public final OrientationConfig orientationConfig = nested(0, OrientationConfig::new,
            "Image Orientation");

    @Override
    public @NotNull String getName() {
        return "client";
    }

    private static <T extends ConfigBase> T register(Supplier<T> factory) {
        Pair<T, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(builder -> {
            T config = factory.get();
            config.registerAll(builder);
            return config;
        });
        T config = specPair.getLeft();
        config.specification = specPair.getRight();
        return config;
    }

    @SubscribeEvent
    public static void onLoad(ModConfigEvent.Loading e) {
        if (CONFIG.specification == e.getConfig().getSpec()) CONFIG.onLoad();
    }

    @SubscribeEvent
    public static void onReload(ModConfigEvent.Reloading e) {
        if (CONFIG.specification == e.getConfig().getSpec()) CONFIG.onLoad();
    }

}
