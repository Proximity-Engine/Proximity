package dev.hephaestus.proximity.app.impl.rendering.elements;

import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.app.api.rendering.elements.Tree;
import dev.hephaestus.proximity.app.impl.rendering.DocumentImpl;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

public class TreeImpl<D extends RenderJob> implements Tree<D>, Iterable<Pair<String, Predicate<D>>> {
    private final DocumentImpl<D> document;
    private final List<Pair<String, Predicate<D>>> branches = new ArrayList<>();

    public TreeImpl(DocumentImpl<D> document) {
        this.document = document;
    }

    public String getBranch() {
        for (var pair : this.branches) {
            if (pair.getValue().test(this.document.getData())) {
                return pair.getKey();
            }
        }

        return null;
    }

    @Override
    public void branch(String id, Predicate<D> visibilityPredicate) {
        this.branches.add(new Pair<>(id, visibilityPredicate));
    }

    @Override
    public void branch(String id) {
        this.branches.add(new Pair<>(id, card -> true));
    }

    @NotNull
    @Override
    public Iterator<Pair<String, Predicate<D>>> iterator() {
        return this.branches.iterator();
    }
}
