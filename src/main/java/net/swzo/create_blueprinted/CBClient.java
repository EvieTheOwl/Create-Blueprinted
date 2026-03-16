package net.swzo.create_blueprinted;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.swzo.create_blueprinted.command.RenderSchemCommand;

public class CBClient {
    public static void onCtorClient(IEventBus modEventBus, IEventBus neoEventBus, ModContainer container) {
        neoEventBus.register(CBClient.class);
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
            RenderSchemCommand.register(event.getDispatcher());
    }
}