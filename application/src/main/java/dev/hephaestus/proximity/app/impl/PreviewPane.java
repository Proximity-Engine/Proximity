package dev.hephaestus.proximity.app.impl;

import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.app.api.Template;
import dev.hephaestus.proximity.app.api.logging.ExceptionUtil;
import dev.hephaestus.proximity.app.api.logging.Log;
import dev.hephaestus.proximity.app.api.plugins.DataWidget;
import dev.hephaestus.proximity.app.api.rendering.Canvas;
import dev.hephaestus.proximity.app.api.rendering.ImageRenderer;
import dev.hephaestus.proximity.app.api.util.Task;
import dev.hephaestus.proximity.app.impl.rendering.DocumentImpl;
import dev.hephaestus.proximity.app.impl.rendering.PreviewImageRenderer;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Background;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.Function;
import java.util.function.Supplier;

public class PreviewPane extends VBox {
    private final MenuItem crop = new MenuItem("Disable Cropping");
    private final MenuItem copyPreview = new MenuItem("Copy (Preview)");
    private final MenuItem copy = new MenuItem("Copy (Print)");
    private final ContextMenu menu = new ContextMenu(this.crop, this.copyPreview, this.copy);
    private final StackPane preview = new StackPane();
    private final StackPane renderingShade = new StackPane();
    private final Spinner spinner = new Spinner();
    private final StackPane previewArea = new StackPane(new Rectangle(), this.preview, this.renderingShade, this.spinner);

    private final Cache cache = new Cache(5);
    private final BooleanProperty isRendering = new SimpleBooleanProperty();

    private final Task copyPreviewTask = new CopyTask("copy-preview", Proximity.log(), true);
    private final Task copyTask = new CopyTask("copy", Proximity.log(), false);
    private final Task render = new RenderTask("render", Proximity.log());

    private DataWidget.Entry<?> mostRecentlyDrawn;
    private boolean cropPreview = true;

    public PreviewPane() {
        super();

        this.setPrefWidth(350);
        this.setBackground(Appearance.SIDEBAR_EXPANDED);

        this.crop.setOnAction(event -> {
            if (this.cropPreview) {
                this.cropPreview = false;
                this.crop.setText("Enable Cropping");
            } else {
                this.cropPreview = true;
                this.crop.setText("Disable Cropping");
            }

            this.render.run();
        });

        this.copyPreview.setOnAction(event -> {
            this.copyPreviewTask.run();
        });

        this.copy.setOnAction(event -> {
            this.copyTask.run();
        });

        this.getChildren().addAll(this.previewArea);

        this.renderingShade.visibleProperty().bind(this.isRendering);
        this.renderingShade.setBackground(Background.fill(Color.color(0, 0, 0, 0.5)));

        this.spinner.visibleProperty().bind(this.isRendering);

        this.previewArea.setOnContextMenuRequested(ev -> {
            this.menu.show(this.previewArea, ev.getScreenX(), ev.getScreenY());
        });
    }

    private class CopyTask extends Task {
        private final boolean crop;

        public CopyTask(String name, Log log, boolean crop) {
            super(name, log);
            this.crop = crop;
        }

        @Override
        protected Builder<?> addSteps(Builder<Void> builder) {
            return builder.then((Supplier<DocumentImpl<?>>) this::startCopy)
                    .then(this::doRender)
                    .then(this::doCrop)
                    .then(this::toToolkitImage)
                    .then(this::copyToClipboard);
        }

        private <D extends RenderJob<?>> DocumentImpl<?> startCopy() {
            return PreviewPane.this.mostRecentlyDrawn.document().getValue();
        }

        private Canvas doRender(DocumentImpl<?> document) {
            Template<?> template = document.getTemplate();
            ImageRenderer renderer = new ImageRenderer();
            Canvas canvas = renderer.createCanvas(template.getWidth(), template.getHeight(), template.getDPI());

            try {
                renderer.render(document, canvas);
                return canvas;
            } catch (IOException e) {
                return this.interrupt(e);
            }
        }

        private BufferedImage doCrop(Canvas canvas) {
            if (this.crop) {
                try {
                    return Proximity.getDataProvider().crop(canvas.getImage());
                } catch (IOException e) {
                    return this.interrupt(e);
                }
            } else {
                return canvas.getImage();
            }
        }

        private Image toToolkitImage(BufferedImage image) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            try {
                ImageIO.write(image, "png", stream);

                return new Image(new ByteArrayInputStream(stream.toByteArray()));
            } catch (IOException e) {
                return this.interrupt(e);
            }
        }

        private void copyToClipboard(Image image) {
            Platform.runLater(() -> {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content = new ClipboardContent();

                content.putImage(image);

                clipboard.setContent(content);
            });
        }
    }

    private class RenderTask extends Task {
        private final ThreadLocal<DataWidget.Entry<?>> widget = ThreadLocal.withInitial(() -> PreviewPane.this.mostRecentlyDrawn);
        private final ThreadLocal<DocumentImpl<?>> document = new ThreadLocal<>();
        public RenderTask(String name, Log log) {
            super(name, log);
        }

        @Override
        protected Builder<?> addSteps(Builder<Void> builder) {
            return builder.then((Supplier<RenderResult>) this::clearPreviewPane)
                    .then((Function<RenderResult, RenderResult>) this::assemble)
                    .then((Function<RenderResult, RenderResult>) this::render)
                    .then(this::setPreview);
        }

        private <D extends RenderJob<?>> RenderResult clearPreviewPane() {
            //noinspection unchecked
            var widget = (DataWidget.Entry<D>) this.widget.get();
            Template<D> template = widget.template().getValue();

            if (template == null) {
                Platform.runLater(() -> PreviewPane.this.preview.getChildren().clear());
                return this.interrupt();
            }

            Platform.runLater(() -> {
                double r = ((double) template.getWidth()) / template.getHeight();
                PreviewPane.this.previewArea.getChildren().set(0, new Rectangle(
                        PreviewPane.this.getWidth(),
                        PreviewPane.this.getWidth() / r,
                        Color.TRANSPARENT)
                );
            });

            return PreviewPane.this.cache.get(widget);
        }

        private <D extends RenderJob<?>> RenderResult assemble(RenderResult result) {
            if (result.original == null && !PreviewPane.this.cropPreview || result.cropped == null && PreviewPane.this.cropPreview) {
                Platform.runLater(() -> PreviewPane.this.isRendering.set(true));

                //noinspection unchecked
                DataWidget.Entry<D> entry = (DataWidget.Entry<D>) this.widget.get();
                D job = entry.getValue();
                Template<D> template = entry.template().getValue();

                this.document.set(entry.document().getValue());
            }

            return result;
        }


        private <D extends RenderJob<?>> RenderResult render(RenderResult result) {
            DocumentImpl<?> document = this.document.get();
            Template<?> template = document.getTemplate();
            double rW = PreviewPane.this.getWidth() / template.getWidth();
            double rH = PreviewPane.this.getHeight() / template.getHeight();
            double r = Math.min(rW, rH);

            int width = (int) (template.getWidth() * r);
            int height = (int) (template.getHeight() * r);

            PreviewImageRenderer renderer = new PreviewImageRenderer(width, height);
            Canvas canvas = renderer.createCanvas(template.getWidth(), template.getHeight(), template.getDPI());

            try {
                renderer.render(document, canvas);

                BufferedImage bufferedImage = canvas.getImage();

                if (PreviewPane.this.cropPreview) {
                    bufferedImage = Proximity.getDataProvider().crop(bufferedImage);

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();

                    ImageIO.write(bufferedImage, "png", stream);

                    result.cropped = new Image(new ByteArrayInputStream(stream.toByteArray()));
                } else {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();

                    ImageIO.write(bufferedImage, "png", stream);

                    result.original = new Image(new ByteArrayInputStream(stream.toByteArray()));
                }

            } catch (Exception e) {
                if (!(e instanceof RuntimeException)) {
                    this.widget.get().getWidget().getErrorProperty().add(ExceptionUtil.getErrorMessage(e));
                }

                this.interrupt(e);
            }

            return result;
        }

        private void setPreview(RenderResult result) {
            DataWidget.Entry<?> widget = this.widget.get();

            Platform.runLater(() -> {
                if (Proximity.isSelected(widget)) {
                    Image image = PreviewPane.this.cropPreview ? result.cropped : result.original;
                    ImageView imageView = new ImageView(image);
                    ObservableList<Node> children = PreviewPane.this.preview.getChildren();
                    StackPane stackPane = new StackPane(imageView);

                    if (!PreviewPane.this.cropPreview) {
                        stackPane.getChildren().add(0, new Rectangle(image.getWidth(), image.getHeight(), Color.GREY));
                    }

                    children.addAll(stackPane);

                    if (children.size() > 1) {
                        children.remove(0, children.size() - 1);
                    }
                }

                PreviewPane.this.isRendering.set(false);
            });
        }

        @Override
        protected void doFinally() {
            if (Thread.currentThread().isInterrupted()) {
                PreviewPane.this.isRendering.set(false);
            }
        }
    }

    public <D extends RenderJob<?>> void render(DataWidget.Entry<D> widget) {
        if (!Proximity.isPaused() && this.mostRecentlyDrawn == null || this.mostRecentlyDrawn != widget) {
            this.mostRecentlyDrawn = widget;
            this.render.run();
        }
    }

    public void clear() {
        this.preview.getChildren().clear();
        this.mostRecentlyDrawn = null;
    }

    public void rerender(DataWidget.Entry<?> entry) {
        if (entry == this.mostRecentlyDrawn) {
            this.cache.remove(entry);
            this.render.run();
        }
    }

    private static final class RenderResult {
        private Image original;
        private Image cropped;
    }

    private static final class Cache {
        private final DataWidget.Entry<?>[] entries;
        private final RenderResult[] results;

        private int p = 0;

        private Cache(int size) {
            this.entries = new DataWidget.Entry[size];
            this.results = new RenderResult[size];
        }

        public RenderResult get(DataWidget.Entry<?> entry) {
            for (int i = 0, cacheLength = this.entries.length; i < cacheLength; i++) {
                if (this.entries[i] == entry) return results[i];
            }

            return this.cache(entry, new RenderResult());
        }

        public RenderResult cache(DataWidget.Entry<?> entry, RenderResult result) {
            this.entries[this.p] = entry;
            this.results[this.p] = result;

            this.p = (this.p + 1) % this.entries.length;

            return result;
        }

        public void remove(DataWidget.Entry<?> entry) {
            for (int i = 0, cacheLength = this.entries.length; i < cacheLength; i++) {
                if (this.entries[i] == entry) {
                    this.entries[i] = null;
                    this.results[i] = null;
                }
            }
        }
    }
}
