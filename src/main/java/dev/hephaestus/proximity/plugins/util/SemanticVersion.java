package dev.hephaestus.proximity.plugins.util;

import org.jetbrains.annotations.NotNull;

public class SemanticVersion implements Comparable<SemanticVersion> {
    private final String version;
    public final int[] numbers;

    private SemanticVersion(@NotNull String version, int[] numbers) {
        this.version = version;
        this.numbers = numbers;
    }

    public static SemanticVersion parse(String s) {
        String[] split = s.split("-")[0].split("\\.");
        int[] numbers = new int[split.length];

        for (int i = 0; i < split.length; i++) {
            numbers[i] = Integer.parseInt(split[i]);
        }

        return new SemanticVersion(s, numbers);
    }

    @Override
    public int compareTo(@NotNull SemanticVersion another) {
        int maxLength = Math.max(this.numbers.length, another.numbers.length);

        for (int i = 0; i < maxLength; i++) {
            int left = i < this.numbers.length ? this.numbers[i] : 0;
            int right = i < another.numbers.length ? another.numbers[i] : 0;

            if (left != right) {
                return left < right ? -1 : 1;
            }
        }

        return 0;
    }

    @Override
    public String toString() {
        return this.version;
    }
}