package dev.hephaestus.proximity.app.impl;

public enum ResizeMode {
    /**
     * Resizes the content to cover the entire destination area without maintaining its original aspect ratio.
     */
    SCALE,

    /**
     * Resizes the content being rendered to cover the entire destination area while maintaining the contents original
     * aspect ratio, possibly cropping portions of content that would fall outside of the destination area.
     */
    FILL,

    /**
     * Resizes the content being rendered to fit inside of the destination area while maintaining the contents original
     * aspect ratio, possibly leaving parts of destination area unfilled.
     */
    FIT
}
