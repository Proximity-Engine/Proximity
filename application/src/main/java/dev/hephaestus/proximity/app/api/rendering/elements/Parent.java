package dev.hephaestus.proximity.app.api.rendering.elements;

import dev.hephaestus.proximity.app.api.rendering.util.ThrowingFunction;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;

import java.net.MalformedURLException;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Parent {
    Group group(Consumer<Group> consumer);
    Group group(String id, Consumer<Group> consumer);
    Selector select(Consumer<Selector> consumer);
    Selector select(String id, Consumer<Selector> consumer);

    void tree(Consumer<Tree> consumer, Tree.Level... levels);
    void tree(String id, Consumer<Tree> consumer, Tree.Level... levels);

    Group group(ObservableBooleanValue condition, Consumer<Group> consumer);
    Group group(String id, ObservableBooleanValue condition, Consumer<Group> consumer);
    Selector select(ObservableBooleanValue condition, Consumer<Selector> consumer);
    Selector select(String id, ObservableBooleanValue condition, Consumer<Selector> consumer);

    void tree(ObservableBooleanValue condition, Consumer<Tree> consumer, Tree.Level... levels);
    void tree(String id, ObservableBooleanValue condition, Consumer<Tree> consumer, Tree.Level... levels);

    Image image(String id);
    Image image(ReadOnlyBooleanProperty property);
    Image image(String id, ObservableBooleanValue condition);
    Image image(String id, ThrowingFunction<Image, Observable, MalformedURLException> consumer);

    Textbox textbox(String id, Function<Textbox, Observable> consumer);
    Text text(String id, Function<Text, Observable> consumer);
}
