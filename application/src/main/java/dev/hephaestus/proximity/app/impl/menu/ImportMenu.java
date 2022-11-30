package dev.hephaestus.proximity.app.impl.menu;

import dev.hephaestus.proximity.app.api.logging.Log;
import dev.hephaestus.proximity.app.api.plugins.DataProvider;
import dev.hephaestus.proximity.app.impl.Initializable;
import dev.hephaestus.proximity.app.impl.Proximity;
import dev.hephaestus.proximity.json.api.JsonElement;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

public class ImportMenu extends Menu implements Initializable {
    public void initialize() {
        DataProvider<?> provider = Proximity.getDataProvider();

        DataProvider.Context context = new DataProvider.Context() {
            @Override
            public Log log() {
                return Proximity.deriveLogger(Proximity.getDataProviderPluginID());
            }

            @Override
            public JsonElement data() {
                return null;
            }
        };

        provider.addMenuItems(context, importHandler -> {
            MenuItem item = new MenuItem(importHandler.getText());

            item.setOnAction(event -> importHandler.createDataWidgets(context));

            this.getItems().add(item);
        });

    }
}
