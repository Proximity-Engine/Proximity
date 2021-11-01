package dev.hephaestus.proximity.util;

import dev.hephaestus.proximity.api.json.JsonElement;
import dev.hephaestus.proximity.api.json.JsonNull;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.json5.JsonReader;

import java.io.IOException;
import java.nio.file.FileSystems;

public class ParsingUtil {
    public static Result<JsonElement> parseStringValue(@Nullable String value) {
        if (value == null) return Result.of(JsonNull.INSTANCE);

        if (!value.startsWith("[") && !value.startsWith("{") && !(value.startsWith("\"") && value.endsWith("\"")) && !value.equals("false") && !value.equals("true") && !value.equals("null")) {
            value = '"' + value + '"';
        }

        try {
            return Result.of(JsonElement.parseElement(JsonReader.json(value)));
        } catch (IOException e) {
            return Result.error(e.getMessage());
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
