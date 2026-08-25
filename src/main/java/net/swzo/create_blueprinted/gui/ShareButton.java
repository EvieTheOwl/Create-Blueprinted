package net.swzo.create_blueprinted.gui;

import dev.titlo10.createschematicpreview.CSPConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.swzo.create_blueprinted.CreateBlueprinted;
import net.swzo.create_blueprinted.api.ShareProvider;
import net.swzo.create_blueprinted.command.CBSchematicCommands;
import net.swzo.create_blueprinted.impl.ShareProviderRegistryImpl;
import net.swzo.create_blueprinted.util.UIHelpers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import static net.swzo.create_blueprinted.CreateBlueprinted.translatable;
import static net.swzo.create_blueprinted.CreateBlueprintedConfig.CONFIG;
import static net.swzo.create_blueprinted.impl.ShareProviderRegistryImpl.*;

public class ShareButton extends SmallIconButton {

    private static final Component BUTTON_TITLE = translatable("gui.schematic_table.share_button.title")
            .withColor(UIHelpers.DARK_GREEN_TEXT_COLOR);

    private static final Component SHIFT_RES_HINT = translatable("gui.schematic_table.render_button.res.hint",
            Component.literal("Shift").withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.DARK_GRAY);
    private static final Component CTRL_DIR_HINT = translatable("gui.schematic_table.render_button.dir.hint",
            Component.literal("Ctrl").withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.DARK_GRAY);

    private static final Component LEFT = translatable("gui.schematic_table.render_button.dir.left")
            .withColor(UIHelpers.LIGHT_GREEN_TEXT_COLOR);
    private static final Component RIGHT = translatable("gui.schematic_table.render_button.dir.right")
            .withColor(UIHelpers.LIGHT_GREEN_TEXT_COLOR);

    private ShareProvider shareProvider;

    public ShareButton(int x, int y) {
        super(x, y, CBGuiTextures.SHARE_ICON);
        this.shareProvider = getActiveShareProvider().orElseThrow(() ->
                new RuntimeException("Schematic image share button cannot be initialized without a share provider"));
    }

    @Override
    public void doRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.toolTip.clear();
        this.toolTip.add(BUTTON_TITLE);

        boolean hasShiftDown = Screen.hasShiftDown();
        String defaultWidth = CONFIG.defaultWidth.get().toString();
        String altWidth = CONFIG.alternateWidth.get().toString();

        @SuppressWarnings("OptionalGetWithoutIsPresent")
        boolean hideTooltipHints = getActiveShareProvider().get().hideTooltipHints();
        if (!hideTooltipHints) {
            MutableComponent resolutionMessage = translatable("gui.schematic_table.render_button.res").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(hasShiftDown ? altWidth : defaultWidth)
                            .withColor(UIHelpers.LIGHT_GREEN_TEXT_COLOR));

            if (!hasShiftDown) resolutionMessage.append(SHIFT_RES_HINT);

            boolean hasCtrlDown = Screen.hasControlDown();
            MutableComponent directionMessage = null;

            if (!CONFIG.usePreviewRotation.get() || !CSPConfig.CONFIG.previewEnabled.get()) {
                directionMessage = translatable("gui.schematic_table.render_button.dir").withStyle(ChatFormatting.GRAY)
                        .append(hasCtrlDown ? LEFT : RIGHT);
                if (!hasCtrlDown) directionMessage.append(CTRL_DIR_HINT);
            }

            this.toolTip.add(resolutionMessage);
            if (directionMessage != null) this.toolTip.add(directionMessage);
            this.toolTip.add(Component.literal(" "));
        }

        MutableComponent nameComponent = shareProvider.destinationName().plainCopy();
        if (CONFIG.enableImageSharing.get()) {
            this.toolTip.add(translatable("gui.schematic_table.share_button.override_name_" +
                    (shareProvider.includeSchematicData() ? "schematic" : "image"))
                    .append(nameComponent.withColor(UIHelpers.LIGHT_GREEN_TEXT_COLOR)).withStyle(ChatFormatting.GRAY));
            this.toolTip.add(translatable("gui.schematic_table.share_button.override_url",
                    Component.literal(shareProvider.destinationUrl())
                            .withColor(UIHelpers.LIGHT_GREEN_TEXT_COLOR)).withStyle(ChatFormatting.GRAY));

            if (!shareProvider.extras().isEmpty())
                this.toolTip.addAll(shareProvider.extras());

            if (getAllProviders().size() > 1)
                this.toolTip.add(translatable("gui.schematic_table.share_button.click_hint",
                    shareProvider.id().toString()).withStyle(ChatFormatting.DARK_GRAY));
        } else
            this.toolTip.add(translatable("gui.schematic_table.share_button.disabled")
                    .withStyle(ChatFormatting.GRAY));

        super.doRender(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int buttonIdx) {
        // Right-clicked. More than 1 provider available.
        if (buttonIdx == 1 && getAllProviders().size() > 1) {
            @SuppressWarnings("OptionalGetWithoutIsPresent")
            ResourceLocation activeProviderId = getActiveShareProvider().get().id();
            List<ShareProvider> providers = new ArrayList<>(getAllProviders());
            ListIterator<ShareProvider> iterator = providers.listIterator();

            while (iterator.hasNext()){
                ShareProvider provider = iterator.next();

                if (provider.id().equals(activeProviderId)) {
                    this.shareProvider = iterator.hasNext() ? iterator.next() : providers.getFirst();
                    CreateBlueprinted.State.activeShareProvider.set(this.shareProvider.id().toString());
                    CBSchematicCommands.resetQueuedFile();
                    break;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, buttonIdx);
    }
}
