package net.swzo.create_blueprinted;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.common.Mod;
import net.swzo.create_blueprinted.util.DebugTimer;
import net.swzo.create_blueprinted.util.UiHelpers;
import org.slf4j.Logger;

@Mod(CreateBlueprinted.MOD_ID)
public class CreateBlueprinted {

    public static final String MOD_ID = "create_blueprinted";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DebugTimer DEBUG_TIMER = new DebugTimer();

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
    public static MutableComponent translatable(String path, Object... args) {
        return Component.translatable(MOD_ID + "." + path, args);
    }

    public static MutableComponent translatableError(String path, Object... args) {
        return Component.translatable(MOD_ID + ".error." + path, args).withColor(UiHelpers.ERROR_PRIMARY);
    }
}
