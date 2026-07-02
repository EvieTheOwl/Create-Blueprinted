package net.swzo.create_blueprinted.util;

import net.minecraft.client.gui.Font;

public class UiHelpers {

    public static final int DARK_BLUE_TEXT_COLOR = 0x528fde;
    public static final int LIGHT_BLUE_TEXT_COLOR = 0x94b5dd;
    public static final int ERROR_PRIMARY = 0xDE525B;

    public static String truncateString(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String ellipsis = "...";
        int textWidth = maxWidth - font.width(ellipsis);
        return font.plainSubstrByWidth(text, textWidth) + ellipsis;
    }
}
