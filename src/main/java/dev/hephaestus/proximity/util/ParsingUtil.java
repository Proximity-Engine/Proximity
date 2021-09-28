package dev.hephaestus.proximity.util;

import dev.hephaestus.proximity.json.JsonElement;
import dev.hephaestus.proximity.json.JsonNull;
import dev.hephaestus.proximity.json.JsonPrimitive;
import org.jetbrains.annotations.Nullable;

import java.nio.file.FileSystems;

public class ParsingUtil {
    public static JsonElement parseStringValue(@Nullable String value) {
        if (value != null) {
            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
        }

        if (value == null) {
            return JsonNull.INSTANCE;
        } else if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return new JsonPrimitive(Boolean.parseBoolean(value));
        } else {
            return new JsonPrimitive(value);
        }
    }

    public static String getFileLocation(String parentId, String id, String src) {
        String sep = FileSystems.getDefault().getSeparator();
        StringBuilder builder = new StringBuilder(parentId.isEmpty() ? "" : (parentId.replace(".", sep)
                + FileSystems.getDefault().getSeparator()));

        if (src == null || src.isEmpty()) {
            builder.append(id);
        } else {
            String[] split = src.split("/");

            for (int i = 0; i < split.length; i++) {
                String string = split[i];

                if (string.equals("..")) {
                    if (builder.chars().filter(j -> Character.toString(j).equals(sep)).count() > 1) {
                        builder = new StringBuilder(builder.substring(0, builder.length() - 1));
                        builder = new StringBuilder(builder.substring(0, builder.lastIndexOf(sep)));
                    } else {
                        builder = new StringBuilder();
                    }
                } else {
                    builder.append(string);
                }

                if (i < split.length - 1 && !builder.isEmpty()) {
                    builder.append(sep);
                }
            }
        }

        return builder.toString();
    }
}
