package net.swzo.create_blueprinted.mixin;

import com.simibubi.create.CreateClient;
import com.simibubi.create.content.schematics.client.ClientSchematicLoader;
import com.simibubi.create.content.schematics.table.SchematicTableMenu;
import com.simibubi.create.content.schematics.table.SchematicTableScreen;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.titlo10.createschematicpreview.CSPConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.swzo.create_blueprinted.handler.SchematicImageHandler;
import net.swzo.create_blueprinted.render.SchematicRenderSettings;
import net.swzo.create_blueprinted.render.SchematicRenderSettings.Orientation;
import net.swzo.create_blueprinted.api.ShareProviderRegistry;
import net.swzo.create_blueprinted.gui.CBGuiTextures;
import net.swzo.create_blueprinted.gui.ExportButton;
import net.swzo.create_blueprinted.gui.ShareButton;
import net.swzo.create_blueprinted.gui.SmallIconButton;
import net.swzo.create_blueprinted.util.SchematicUtils;
import net.swzo.create_blueprinted.util.UIHelpers;
import dev.titlo10.createschematicpreview.mixin_interfaces.PreviewScreenAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static net.swzo.create_blueprinted.CreateBlueprintedConfig.CONFIG;

@Mixin(value = SchematicTableScreen.class)
public abstract class SchematicTableScreenMixin extends AbstractSimiContainerScreen<SchematicTableMenu> implements PreviewScreenAccess {

    @Unique private final Component cb$availableSchematicsTitle = CreateLang.translateDirect("gui.schematicTable.availableSchematics");

    @Shadow private ScrollInput schematicsArea;

    @Shadow private Label schematicsLabel;
    @Shadow private IconButton refreshButton;
    @SuppressWarnings("FieldCanBeLocal")
    @Unique private IconButton cb$exportButton, cb$shareButton;

    @Unique private boolean cb$shiftWasDownOnInit, cb$ctrlWasDownOnInit;

    protected SchematicTableScreenMixin(SchematicTableMenu container, Inventory inv, Component title) {
        super(container, inv, title);
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void onInitHead(CallbackInfo ci) {
        cb$shiftWasDownOnInit = Screen.hasShiftDown();
        cb$ctrlWasDownOnInit = Screen.hasControlDown();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInitTail(CallbackInfo ci) {
        cb$exportButton = new ExportButton(leftPos + 205, topPos + 1);
        cb$exportButton.withCallback(() ->
                cb$createSchematicImageHandler().ifPresent(SchematicImageHandler::export)
        );
        if (ShareProviderRegistry.hasShareProvider()) {
            cb$shareButton = new ShareButton(leftPos + 205, topPos + 18);
            cb$shareButton.withCallback(() ->
                    cb$createSchematicImageHandler().ifPresent(SchematicImageHandler::share)
            );
            cb$shareButton.active = CONFIG.enableImageSharing.get();
            this.addRenderableWidget(cb$shareButton);
        }
        this.addRenderableWidget(cb$exportButton);
        cb$replaceRefreshButton();
    }

    @Unique
    private void cb$replaceRefreshButton() {
        this.removeWidget(refreshButton);

        int topPos = this.topPos + (ShareProviderRegistry.hasShareProvider() ? 35 : 18);
        refreshButton = new SmallIconButton(leftPos + 205, topPos, CBGuiTextures.REFRESH_ICON);
        refreshButton.withCallback(() -> {
            ClientSchematicLoader schematicSender = CreateClient.SCHEMATIC_SENDER;
            schematicSender.refresh();
            List<Component> availableSchematics1 = schematicSender.getAvailableSchematics();
            removeWidget(schematicsArea);

            if (!availableSchematics1.isEmpty()) {
                schematicsArea = new SelectionScrollInput(leftPos + 45, this.topPos + 21, 139, 18)
                        .forOptions(availableSchematics1)
                        .titled(cb$availableSchematicsTitle.plainCopy())
                        .writingTo(schematicsLabel);
                schematicsArea.onChanged();
                addRenderableWidget(schematicsArea);
            } else {
                schematicsArea = null;
                schematicsLabel.text = CommonComponents.EMPTY;
            }
        });
        this.addRenderableWidget(refreshButton);
    }

    @Unique
    private Optional<SchematicImageHandler> cb$createSchematicImageHandler() {
        var previewPanel = (SchematicPreviewAccessor) csp$getPanel();
        if (previewPanel == null || schematicsArea == null) return Optional.empty();

        Optional<String> fileName = SchematicUtils.getSchematicNameFromIndex(schematicsArea.getState());
        if (fileName.isEmpty()) return Optional.empty();

        boolean ctrlActive = Screen.hasControlDown() && !cb$ctrlWasDownOnInit;
        Orientation orientation;

        if (!CONFIG.usePreviewRotation.get() || !CSPConfig.CONFIG.previewEnabled.get())
            orientation = ctrlActive ? Orientation.ISOMETRIC_LEFT : Orientation.ISOMETRIC_RIGHT;
        else
            orientation = new Orientation(previewPanel.yaw(), previewPanel.pitch());

        boolean shiftActive = Screen.hasShiftDown() && !cb$shiftWasDownOnInit;
        int imageWidth = shiftActive ? CONFIG.alternateWidth.get() : CONFIG.defaultWidth.get();
        var settingsbuilder = SchematicRenderSettings.builder()
                .imageWidth(imageWidth)
                .orientation(orientation);

        Player player = Minecraft.getInstance().player;
        CommandSourceStack source = Objects.requireNonNull(player).createCommandSourceStack();
        Minecraft.getInstance().setScreen(null);

        return Optional.of(new SchematicImageHandler(fileName.get(), source, settingsbuilder));
    }

    @Inject(method = "containerTick", at = @At("TAIL"))
    private void onContainerTickTail(CallbackInfo ci) {
        if (schematicsArea != null && schematicsLabel != null && schematicsLabel.text != null) {
            String originalText = schematicsLabel.text.getString();

            if (!originalText.isEmpty()) {
                int maxWidth = schematicsArea.getWidth() - 5;

                String truncatedText = UIHelpers.truncateString(Minecraft.getInstance().font, originalText, maxWidth);
                schematicsLabel.text = Component.literal(truncatedText);
            }
        }
    }
}