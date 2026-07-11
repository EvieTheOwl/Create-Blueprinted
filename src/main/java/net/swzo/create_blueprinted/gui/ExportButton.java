package net.swzo.create_blueprinted.gui;

import dev.titlo10.createschematicpreview.CSPConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.swzo.create_blueprinted.util.UIHelpers;

import java.util.List;

import static net.swzo.create_blueprinted.CreateBlueprinted.translatable;
import static net.swzo.create_blueprinted.CreateBlueprintedConfig.CONFIG;

public class ExportButton extends SmallIconButton {

    private static final Component BUTTON_TITLE = translatable("gui.schematic_table.export_button.title")
            .withColor(UIHelpers.DARK_BLUE_TEXT_COLOR);

    private static final Component SHIFT_RES_HINT = translatable("gui.schematic_table.render_button.res.hint",
            Component.literal("Shift").withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.DARK_GRAY);
    private static final Component CTRL_DIR_HINT = translatable("gui.schematic_table.render_button.dir.hint",
            Component.literal("Ctrl").withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.DARK_GRAY);

    private static final Component LEFT = translatable("gui.schematic_table.render_button.dir.left")
            .withColor(UIHelpers.LIGHT_BLUE_TEXT_COLOR);
    private static final Component RIGHT = translatable("gui.schematic_table.render_button.dir.right")
            .withColor(UIHelpers.LIGHT_BLUE_TEXT_COLOR);

    private static final List<Component> OTHER_HINTS = List.of(
            Component.literal(" "),
            translatable("gui.schematic_table.export_button.save_hint").withStyle(ChatFormatting.GRAY),
            translatable("gui.schematic_table.export_button.chat_hint").withStyle(ChatFormatting.GRAY),
            translatable("gui.schematic_table.export_button.chat_hint_visibility").withStyle(ChatFormatting.DARK_GRAY)
    );

    public ExportButton(int x, int y) {
        super(x, y, CBGuiTextures.SAVE_ICON);
    }

    @Override
    public void doRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.toolTip.clear();
        this.toolTip.add(BUTTON_TITLE);

        boolean hasShiftDown = Screen.hasShiftDown();
        String defaultWidth = CONFIG.defaultWidth.get().toString();
        String altWidth = CONFIG.alternateWidth.get().toString();

        MutableComponent resolutionMessage = translatable("gui.schematic_table.render_button.res").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(hasShiftDown ? altWidth : defaultWidth)
                .withColor(UIHelpers.LIGHT_BLUE_TEXT_COLOR));
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
        this.toolTip.addAll(OTHER_HINTS);
        super.doRender(graphics, mouseX, mouseY, partialTicks);
    }
}
