package net.swzo.create_blueprinted;

import com.mojang.logging.LogUtils;
import net.createmod.catnip.config.ui.BaseConfigScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.swzo.create_blueprinted.command.CBSchematicCommands;
import net.swzo.create_blueprinted.util.DebugTimer;
import net.swzo.create_blueprinted.util.UIHelpers;
import org.slf4j.Logger;

import static net.swzo.create_blueprinted.config.CreateBlueprintedConfig.CONFIG;

@Mod(value = CreateBlueprinted.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(Dist.CLIENT)
public class CreateBlueprinted {

    public static final String MOD_ID = "create_blueprinted";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DebugTimer DEBUG_TIMER = new DebugTimer();

    public CreateBlueprinted(IEventBus __, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, CONFIG.specification);
        container.registerExtensionPoint(IConfigScreenFactory.class, (_container, screen)
                -> new BaseConfigScreen(screen, _container.getModId()));
    }

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static ResourceLocation rl(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    public static MutableComponent translatable(String path, Object... args) {
        return Component.translatable(MOD_ID + "." + path, args);
    }

    public static MutableComponent translatableError(String path, Object... args) {
        return Component.translatable(MOD_ID + ".error." + path, args).withColor(UIHelpers.ERROR_DARKER);
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CBSchematicCommands.register(event.getDispatcher());
    }
}
