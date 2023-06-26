package me.cometkaizo.origins.util;

import net.minecraft.client.gui.FontRenderer;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class StringUtils {

    public static List<String> createLines(String text, FontRenderer fontRenderer, int width) {
        String textLeft = text;
        List<String> lines = new ArrayList<>();

        while (!textLeft.isEmpty()) {
            String line = fontRenderer.trimStringToWidth(textLeft, width);
            textLeft = textLeft.substring(line.length());

            if (!line.endsWith(" ") && !textLeft.startsWith(" ") && line.contains(" ") && !textLeft.isEmpty()) {
                String truncatedWord = line.substring(line.lastIndexOf(" "));
                line = line.substring(0, line.lastIndexOf(" "));
                textLeft = truncatedWord + textLeft;
            }

            textLeft = textLeft.trim();
            lines.add(line.trim());
        }

        return lines;
    }

}
