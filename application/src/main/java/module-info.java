module dev.hephaestus.proximity.app {
    uses dev.hephaestus.proximity.app.api.plugins.DataProvider;
    uses dev.hephaestus.proximity.app.api.Template;

    exports dev.hephaestus.proximity.app.api;
    exports dev.hephaestus.proximity.app.api.controls;
    exports dev.hephaestus.proximity.app.api.exceptions;
    exports dev.hephaestus.proximity.app.api.logging;
    exports dev.hephaestus.proximity.app.api.options;
    exports dev.hephaestus.proximity.app.api.plugins;
    exports dev.hephaestus.proximity.app.api.rendering;
    exports dev.hephaestus.proximity.app.api.rendering.elements;
    exports dev.hephaestus.proximity.app.api.rendering.properties;
    exports dev.hephaestus.proximity.app.api.rendering.util;
    exports dev.hephaestus.proximity.app.api.text;

    exports dev.hephaestus.proximity.app.impl to javafx.graphics, javafx.controls, javafx.fxml;
    exports dev.hephaestus.proximity.app.impl.menu to javafx.graphics, javafx.controls, javafx.fxml;
    exports dev.hephaestus.proximity.app.impl.sidebar to javafx.graphics, javafx.controls, javafx.fxml;
    exports dev.hephaestus.proximity.app.impl.skins to javafx.graphics, javafx.controls, javafx.fxml;
    exports dev.hephaestus.proximity.app.api.util;

    requires java.base;
    requires jdk.crypto.ec;
    requires java.net.http;
    requires dev.hephaestus.proximity.utils;
    requires javafx.graphics;
    requires java.sql;
    requires dev.hephaestus.proximity.json;
    requires org.jetbrains.annotations;
    requires java.desktop;
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.common;

    opens dev.hephaestus.proximity.app.impl to javafx.fxml;
    opens dev.hephaestus.proximity.app.impl.sidebar to javafx.fxml;
}
