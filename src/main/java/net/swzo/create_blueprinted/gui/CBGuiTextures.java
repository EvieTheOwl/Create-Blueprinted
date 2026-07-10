package net.swzo.create_blueprinted.gui;

import net.createmod.catnip.gui.TextureSheetSegment;
import net.createmod.catnip.gui.element.ScreenElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import static net.swzo.create_blueprinted.CreateBlueprinted.rl;

/*
 * MIT License
 *
 * Copyright (c) 2019 simibubi
 * Modified by EvieTheOwl
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 *         to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 */
public enum CBGuiTextures implements ScreenElement, TextureSheetSegment {

    SHARE_ICON("share_icon", 0, 0, 13, 13, 13),
    SAVE_ICON("save_icon", 0, 0, 13, 13, 13),
    REFRESH_ICON("refresh_icon", 0, 0, 13, 13, 13),
    SMALL_BUTTON( "small_buttons", 0, 0, 15, 15, 64),
    SMALL_BUTTON_HOVER("small_buttons", 16, 0, 15, 15, 64),
    SMALL_BUTTON_DOWN("small_buttons", 0, 16, 15, 15, 64),
    SMALL_BUTTON_GREEN("small_buttons", 32, 0, 15, 15, 64),
    SMALL_BUTTON_DISABLED("small_buttons", 16, 16, 15, 15, 64);

    public final ResourceLocation location;
    private final int width, height, startX, startY, textureWidth, textureHeight;

    CBGuiTextures(String path, int startX, int startY, int width, int height, int textureSize) {
        this(rl("textures/gui/" + path + ".png"), startX, startY, width, height, textureSize);
    }

    CBGuiTextures(String namespace, String path, int startX, int startY, int width, int height, int textureSize) {
        this(rl(namespace, "textures/gui/" + path + ".png"), startX, startY, width, height, textureSize);
    }

    CBGuiTextures(ResourceLocation location, int startX, int startY, int width, int height, int textureSize) {
        this.location = location;
        this.width = width;
        this.height = height;
        this.startX = startX;
        this.startY = startY;
        this.textureWidth = textureSize;
        this.textureHeight = textureSize;
    }

    @Override
    public @NotNull ResourceLocation getLocation() {
        return location;
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y) {
        graphics.blit(location, x, y, startX, startY, width, height, textureWidth, textureHeight);
    }

    @Override
    public int getStartX() {
        return startX;
    }

    @Override
    public int getStartY() {
        return startY;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    public int getTextureWidth() {
        return textureWidth;
    }

    public int getTextureHeight() {
        return textureHeight;
    }
}

