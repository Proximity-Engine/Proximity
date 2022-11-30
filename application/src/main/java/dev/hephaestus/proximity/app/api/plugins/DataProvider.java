package dev.hephaestus.proximity.app.api.plugins;

import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.app.api.logging.Log;
import dev.hephaestus.proximity.json.api.JsonElement;
import javafx.scene.layout.Pane;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.function.Consumer;

public interface DataProvider<D extends RenderJob> {
    String getId();

    Class<D> getDataClass();

    Pane createHeaderElement();

    DataWidget<D> createDataEntryElement(Context context);

    default BufferedImage crop(BufferedImage image) throws IOException {
        return image;
    }

    default void addMenuItems(Context context, Consumer<ImportHandler> menuConsumer) {

    }

    interface Context {
        Log log();

        /**
         * @return the serialized contents of the data row, to use in initialization
         */
        JsonElement data();
    }
}
