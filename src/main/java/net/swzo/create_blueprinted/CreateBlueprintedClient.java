package net.swzo.create_blueprinted;

import net.createmod.catnip.theme.Color;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.swzo.create_blueprinted.api.RenderSchematicImageEvent;
import net.swzo.create_blueprinted.command.CBSchematicCommands;

@EventBusSubscriber(Dist.CLIENT)
public class CreateBlueprintedClient {

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
            CBSchematicCommands.register(event.getDispatcher());
    }

    // TODO - Remove after testing
    @SubscribeEvent
    public static void onSaveSchematicFile(RenderSchematicImageEvent.Post e) {
        String fileName = e.getFileName();
        byte[] imageContents = e.getImageContent();

        boolean setCancelled = false;
        e.setCanceled(setCancelled);
    }


    // TODO - Remove after testing
    @SubscribeEvent
    public static void onSaveSchematicFile(RenderSchematicImageEvent.Pre e) {
        String fileName = e.getFileName();
        e.modifyRenderSettings().imageWidth(256);

        boolean setCancelled = false;
        e.setCanceled(setCancelled);
    }
}