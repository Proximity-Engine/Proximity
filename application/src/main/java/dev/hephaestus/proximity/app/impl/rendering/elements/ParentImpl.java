package dev.hephaestus.proximity.app.impl.rendering.elements;

import dev.hephaestus.proximity.app.api.Proximity;
import dev.hephaestus.proximity.app.api.logging.ExceptionUtil;
import dev.hephaestus.proximity.app.api.rendering.Document;
import dev.hephaestus.proximity.app.api.rendering.RenderData;
import dev.hephaestus.proximity.app.api.rendering.elements.Group;
import dev.hephaestus.proximity.app.api.rendering.elements.Image;
import dev.hephaestus.proximity.app.api.rendering.elements.Selector;
import dev.hephaestus.proximity.app.api.rendering.elements.Text;
import dev.hephaestus.proximity.app.api.rendering.elements.Textbox;
import dev.hephaestus.proximity.app.api.rendering.elements.Tree;
import dev.hephaestus.proximity.app.api.rendering.util.ThrowingFunction;
import dev.hephaestus.proximity.app.api.util.Properties;
import javafx.beans.Observable;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;

import java.net.MalformedURLException;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class ParentImpl<D extends RenderData> extends ElementImpl<D> implements Tree {
    protected final ListProperty<ElementImpl<D>> children = Properties.list();

    public ParentImpl(String id, Document<D> document, ParentImpl<D> parent) {
        super(id, document, parent);
    }

    @Override
    protected void getAttributes(Consumer<Observable> attributes) {
        attributes.accept(this.children);
    }

    private <C extends ElementImpl<D>> C createElement(String id, Constructor<D, C> constructor, Consumer<? super C> initializer) {
        C child = constructor.construct(id, this.document, this);

        if (initializer != null) {
            initializer.accept(child);
        }

        this.children.add(child);

        return child;
    }

    private <C extends ElementImpl<D>> C createElement(String id, Constructor<D, C> constructor) {
        return this.createElement(id, constructor, null);
    }

    private <C extends ElementImpl<D>> C createElement(Constructor<D, C> constructor, Consumer<? super C> initializer) {
        return this.createElement(null, constructor, initializer);
    }

    private <C extends ElementImpl<D>> C createElement(Constructor<D, C> constructor) {
        return this.createElement(null, constructor, null);
    }

    @Override
    public final Group group(Consumer<Group> consumer) {
        return this.createElement(GroupImpl::new, consumer);
    }

    @Override
    public Group group(String id, Consumer<Group> consumer) {
        return this.createElement(id, GroupImpl::new, consumer);
    }

    @Override
    public Selector select(Consumer<Selector> consumer) {
        return this.createElement(SelectorImpl::new, consumer);
    }

    @Override
    public Selector select(String id, Consumer<Selector> consumer) {
        return this.createElement(id, SelectorImpl::new, consumer);
    }

    @Override
    public void tree(Consumer<Tree> consumer, Tree.Level... levels) {
        ((ParentImpl<D>) this.select(s -> {})).tree(consumer, levels, 0);
    }

    @Override
    public void tree(String id, Consumer<Tree> consumer, Tree.Level... levels) {
        ((ParentImpl<D>) this.select(id, s -> {})).tree(consumer, levels, 0);
    }

    @Override
    public Group group(ObservableBooleanValue condition, Consumer<Group> consumer) {
        Group group = this.createElement(GroupImpl::new, consumer);

        ((ElementImpl<D>) group).bindVisibility(condition);

        return group;
    }

    @Override
    public Group group(String id, ObservableBooleanValue condition, Consumer<Group> consumer) {
        var group = this.createElement(id, GroupImpl::new, consumer);

        group.bindVisibility(condition);

        return group;
    }

    @Override
    public Selector select(ObservableBooleanValue condition, Consumer<Selector> consumer) {
        var selector = this.createElement(SelectorImpl::new, consumer);

        selector.bindVisibility(condition);

        return selector;
    }

    @Override
    public Selector select(String id, ObservableBooleanValue condition, Consumer<Selector> consumer) {
        var selector = this.createElement(id, SelectorImpl::new, consumer);

        selector.bindVisibility(condition);

        return selector;
    }

    private void tree(Consumer<Tree> consumer, Tree.Level[] levels, int i) {
        Tree.Level level = levels[i];

        for (var branch : level.branches()) {
            if (i == levels.length - 1) {
                this.select(branch.name(), branch.value(), s -> consumer.accept((SelectorImpl<D>) s));
            } else {
                this.select(branch.name(), branch.value(), s -> ((ParentImpl<D>) s).tree(consumer, levels, i + 1));
            }
        }
    }

    @Override
    public void tree(ObservableBooleanValue condition, Consumer<Tree> consumer, Tree.Level... levels) {
        ((ParentImpl<D>) this.select(condition, s -> {})).tree(consumer, levels, 0);
    }

    @Override
    public void tree(String id, ObservableBooleanValue condition, Consumer<Tree> consumer, Tree.Level... levels) {
        ((ParentImpl<D>) this.select(id, condition, s -> {})).tree(consumer, levels, 0);
    }

    @Override
    public Image image(String id) {
        return this.createElement(id, ImageImpl::new);
    }

    @Override
    public Image image(ReadOnlyBooleanProperty property) {
        return this.createElement(property.getName(), ImageImpl::new, image -> {
            image.bindVisibility(property);
        });
    }

    @Override
    public Image image(String id, ObservableBooleanValue condition) {
        return this.createElement(id, ImageImpl::new, image -> {
            image.bindVisibility(condition);
        });
    }

    @Override
    public Image image(String id, ThrowingFunction<Image, Observable, MalformedURLException> consumer) {
        return this.createElement(id, ImageImpl::new, image -> {
            try {
                consumer.apply(image).addListener( o -> {
                    try {
                        consumer.apply(image);
                    } catch (MalformedURLException e) {
                        this.document.getErrors().add(ExceptionUtil.getErrorMessage(e));
                    }
                });
            } catch (MalformedURLException e) {
                Proximity.write(e);
                this.document.getErrors().add(ExceptionUtil.getErrorMessage(e));
            }
        });
    }

    @Override
    public Textbox textbox(String id, Function<Textbox, Observable> consumer) {
        return this.createElement(id, TextboxImpl::new, textbox -> {
            consumer.apply(textbox).addListener(o -> consumer.apply(textbox));
        });
    }

    @Override
    public Text text(String id, Function<Text, Observable> consumer) {
        return this.createElement(id, TextImpl::new, text -> {
            consumer.apply(text).addListener(o -> consumer.apply(text));
        });
    }
}
