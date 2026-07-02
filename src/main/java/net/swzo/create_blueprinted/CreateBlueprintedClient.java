package net.swzo.create_blueprinted;

import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.createmod.catnip.theme.Color;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.swzo.create_blueprinted.api.RenderSchematicImageEvent;
import net.swzo.create_blueprinted.command.CBSchematicCommands;

import javax.swing.*;

@EventBusSubscriber(Dist.CLIENT)
public class CreateBlueprintedClient {

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
            CBSchematicCommands.register(event.getDispatcher());
    }
}