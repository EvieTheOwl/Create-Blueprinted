package net.swzo.create_blueprinted.gui;

import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.swzo.create_blueprinted.util.UiHelpers;

import java.util.List;

import static net.swzo.create_blueprinted.CreateBlueprinted.translatable;

public class RenderButton extends IconButton {

    private static final Component BUTTON_TITLE = translatable("gui.schematic_table.render_button.title")
            .withColor(UiHelpers.DARK_BLUE_TEXT_COLOR);

    private static final Component SHIFT_RES_HINT = translatable("gui.schematic_table.render_button.res.hint",
            Component.literal("Shift").withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.DARK_GRAY);

    private static final List<Component> HINTS = List.of(
            Component.literal(" "),
            translatable("gui.schematic_table.render_button.save_hint").withStyle(ChatFormatting.GRAY),
            translatable("gui.schematic_table.render_button.chat_hint").withStyle(ChatFormatting.GRAY),
            translatable("gui.schematic_table.render_button.chat_hint_visibility").withStyle(ChatFormatting.DARK_GRAY)
    );

    public RenderButton(int x, int y) {
        super(x, y, AllIcons.I_CONFIG_SAVE);
    }

    @Override
    public void doRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.toolTip.clear();
        this.toolTip.add(BUTTON_TITLE);

        boolean hasShiftDown = Screen.hasShiftDown();
        MutableComponent resolutionComponent = translatable("gui.schematic_table.render_button.res").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(hasShiftDown ? "2048" : "1024")
                .withColor(UiHelpers.LIGHT_BLUE_TEXT_COLOR));

        if (!hasShiftDown) resolutionComponent.append(SHIFT_RES_HINT);

        this.toolTip.add(resolutionComponent);
        this.toolTip.addAll(HINTS);
        super.doRender(graphics, mouseX, mouseY, partialTicks);
    }
}
