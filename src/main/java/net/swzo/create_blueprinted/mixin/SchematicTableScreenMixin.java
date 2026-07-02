package net.swzo.create_blueprinted.mixin;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.content.schematics.table.SchematicTableMenu;
import com.simibubi.create.content.schematics.table.SchematicTableScreen;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.swzo.create_blueprinted.SchematicImageHandler;
import net.swzo.create_blueprinted.api.SchematicRenderSettings;
import net.swzo.create_blueprinted.api.SchematicRenderSettings.Orientation;
import net.swzo.create_blueprinted.gui.RenderButton;
import net.swzo.create_blueprinted.util.SchematicUtils;
import org.example.test.createschematicpreview.client.PreviewScreenAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Mixin(SchematicTableScreen.class)
public abstract class SchematicTableScreenMixin extends AbstractSimiContainerScreen<SchematicTableMenu> implements PreviewScreenAccess {

    @Shadow private ScrollInput schematicsArea;
    @Shadow private IconButton refreshButton;
    @Shadow private List<Rect2i> extraAreas;

    @SuppressWarnings("FieldCanBeLocal")
    @Unique private IconButton cb$renderButton;

    @Unique private boolean cb$shiftWasDownOnInit = false;

    protected SchematicTableScreenMixin(SchematicTableMenu container, Inventory inv, Component title) {
        super(container, inv, title);
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void onInitHead(CallbackInfo ci) {
        cb$shiftWasDownOnInit = Screen.hasShiftDown();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInitTail(CallbackInfo ci) {
        int renderButtonX = refreshButton.getX();
        int renderButtonY = refreshButton.getY() - refreshButton.getHeight() - 4;

        cb$renderButton = new RenderButton(renderButtonX, renderButtonY);
        cb$renderButton.withCallback(() -> {
            var previewPanel = (SchematicPreviewAccessor) createschematicpreview$getPanel();
            if (previewPanel == null || schematicsArea == null) return;

            Optional<String> fileName = SchematicUtils.getSchematicNameFromIndex(schematicsArea.getState());
            if (fileName.isEmpty()) return;

            var orientation = new Orientation(previewPanel.yaw(), previewPanel.pitch());
            boolean shiftActive = Screen.hasShiftDown() && !cb$shiftWasDownOnInit;
            int imageWidth = shiftActive ? 2048 : 1024;
            var settingsbuilder = SchematicRenderSettings.builder()
                    .imageWidth(imageWidth)
                    .orientation(orientation);

            Player player = Minecraft.getInstance().player;
            CommandSourceStack source = Objects.requireNonNull(player).createCommandSourceStack();
            new SchematicImageHandler(source, fileName.get(), settingsbuilder).export();
        });
        this.addRenderableWidget(cb$renderButton);

        List<Rect2i> newExtraAreas = new ArrayList<>(this.extraAreas);
        newExtraAreas.add(new Rect2i(renderButtonX, renderButtonY, cb$renderButton.getWidth(), cb$renderButton.getHeight()));
        this.extraAreas = ImmutableList.copyOf(newExtraAreas);
    }
}