package dev.hephaestus.proximity.util;

public final record Option<T>(T value, T defaultValue) {
    public static final String COUNT = "count";
    public static final String SET_CODE = "set";
    public static final String USE_OFFICIAL_ART = "use_official_art";
    public static final String ARTIST = "artist";


    public Option<T> derive(T value) {
        return new Option<>(value, this.defaultValue);
    }
}
