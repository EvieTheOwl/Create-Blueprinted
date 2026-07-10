package net.swzo.create_blueprinted.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.swzo.create_blueprinted.api.ShareProvider;
import net.swzo.create_blueprinted.api.ShareProviderRegistry;
import net.swzo.create_blueprinted.util.UIHelpers;

import static net.swzo.create_blueprinted.CreateBlueprinted.translatable;
import static net.swzo.create_blueprinted.config.CreateBlueprintedConfig.CONFIG;

public class ShareButton extends SmallIconButton {

    private static final Component BUTTON_TITLE = translatable("gui.schematic_table.share_button.title")
            .withColor(UIHelpers.DARK_GREEN_TEXT_COLOR);

    private static final Component SHIFT_RES_HINT = translatable("gui.schematic_table.render_button.res.hint",
            Component.literal("Shift").withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.DARK_GRAY);

    private final ShareProvider shareProvider;

    public ShareButton(int x, int y) {
        super(x, y, CBGuiTextures.SHARE_ICON);
        this.shareProvider = ShareProviderRegistry.getMainProvider().orElseThrow(() ->
                new RuntimeException("Schematic image share button cannot be initialized without a share provider"));
    }

    @Override
    public void doRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.toolTip.clear();
        this.toolTip.add(BUTTON_TITLE);

        boolean hasShiftDown = Screen.hasShiftDown();
        String defaultWidth = CONFIG.defaultWidth.get().toString();
        String altWidth = CONFIG.alternateWidth.get().toString();
        MutableComponent resolutionComponent = translatable("gui.schematic_table.render_button.res").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(hasShiftDown ? altWidth : defaultWidth)
                        .withColor(UIHelpers.LIGHT_GREEN_TEXT_COLOR));

        if (!hasShiftDown) resolutionComponent.append(SHIFT_RES_HINT);
        this.toolTip.add(resolutionComponent);
        this.toolTip.add(Component.literal(" "));

        MutableComponent nameComponent = shareProvider.destinationName().plainCopy();
        if (CONFIG.enableImageSharing.get()) {
            this.toolTip.add(translatable("gui.schematic_table.share_button.override_name")
                    .append(nameComponent.withColor(UIHelpers.LIGHT_GREEN_TEXT_COLOR)).withStyle(ChatFormatting.GRAY));
            this.toolTip.add(translatable("gui.schematic_table.share_button.override_url",
                    Component.literal(shareProvider.destinationUrl())
                            .withColor(UIHelpers.LIGHT_GREEN_TEXT_COLOR)).withStyle(ChatFormatting.GRAY));
            this.toolTip.add(translatable("gui.schematic_table.share_button.override_id",
                    shareProvider.id().toString()).withStyle(ChatFormatting.DARK_GRAY));
        } else
            this.toolTip.add(translatable("gui.schematic_table.share_button.disabled"));

        super.doRender(graphics, mouseX, mouseY, partialTicks);
    }
}
