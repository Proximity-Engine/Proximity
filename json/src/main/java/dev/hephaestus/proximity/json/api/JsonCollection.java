package dev.hephaestus.proximity.json.api;

public interface JsonCollection<T extends JsonElement> {
    T mutableCopy();
}
