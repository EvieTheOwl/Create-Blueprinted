package net.swzo.create_blueprinted.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.AllKeys;
import com.simibubi.create.foundation.gui.widget.IconButton;
import net.createmod.catnip.gui.element.ScreenElement;
import net.minecraft.client.gui.GuiGraphics;

public class SmallIconButton extends IconButton {

    public SmallIconButton(int x, int y, ScreenElement icon) {
        super(x, y, 15, 15, icon);
    }

    @Override
    public void doRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;

        isHovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;

        CBGuiTextures button = !active ? CBGuiTextures.SMALL_BUTTON_DISABLED
                : isHovered && AllKeys.isMouseButtonDown(0) ? CBGuiTextures.SMALL_BUTTON_DOWN
                  : isHovered ? CBGuiTextures.SMALL_BUTTON_HOVER
                    : green ? CBGuiTextures.SMALL_BUTTON_GREEN : CBGuiTextures.SMALL_BUTTON;

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        drawBg(graphics, button);

        icon.render(graphics, getX() + 1, getY() + 1);
    }

    protected void drawBg(GuiGraphics graphics, CBGuiTextures button) {
        graphics.blit(button.location, getX(), getY(), button.getStartX(), button.getStartY(), button.getWidth(),
                button.getHeight(), button.getTextureWidth(), button.getTextureHeight());
    }
}
