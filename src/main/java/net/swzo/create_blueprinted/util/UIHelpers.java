package net.swzo.create_blueprinted.util;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;

import static net.swzo.create_blueprinted.CreateBlueprinted.translatable;

public class UIHelpers {

    public static final int DARK_BLUE_TEXT_COLOR = 0x528fde;
    public static final int DARK_GREEN_TEXT_COLOR = 0x72b975;
    public static final int LIGHT_BLUE_TEXT_COLOR = 0x94b5dd;
    public static final int LIGHT_GREEN_TEXT_COLOR = 0x8dd58f;
    public static final int ERROR_DARKER = 0xda4a54;
    public static final int ERROR_PRIMARY = 0xe4656e;

    public static String truncateString(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String ellipsis = "...";
        int textWidth = maxWidth - font.width(ellipsis);
        return font.plainSubstrByWidth(text, textWidth) + ellipsis;
    }

    public static <T extends Enum<T>> MutableComponent enumToTranslatable(String prefix, T enumInput) {
        return translatable(prefix + enumInput.name().toLowerCase(Locale.ENGLISH));
    }
}
