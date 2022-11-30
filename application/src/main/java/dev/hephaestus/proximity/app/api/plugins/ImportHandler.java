package dev.hephaestus.proximity.app.api.plugins;

public interface ImportHandler {
    String getText();

    void createDataWidgets(DataProvider.Context context);
}
