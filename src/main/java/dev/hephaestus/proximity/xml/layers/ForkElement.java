package dev.hephaestus.proximity.xml.layers;

import dev.hephaestus.proximity.cards.predicates.CardPredicate;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.templates.Template;
import dev.hephaestus.proximity.templates.layers.Group;
import dev.hephaestus.proximity.templates.layers.Layer;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.util.XMLUtil;
import dev.hephaestus.proximity.xml.Properties;
import org.w3c.dom.Element;

import java.util.*;

public class ForkElement extends LayerElement<Group> {
    private List<LayerElement<?>> children;
    private Map<String, List<CardPredicate>> branches;

    public ForkElement(Element element) {
        super(element);
    }

    @Override
    protected Result<LayerElement<Group>> parseLayer(Context context, Properties properties) {
        this.children = new ArrayList<>();
        this.branches = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();

        XMLUtil.iterate(this.getElement(), (e, i) -> {
            Factory<LayerElement<?>> factory = getFactory(e.getTagName());

            if (factory != null) {
                factory.create(e).parse(context, properties)
                        .ifPresent(this.children::add)
                        .ifError(errors::add);
            }
        });


        XMLUtil.iterate(this.getElement(), "branches", (child, i) -> XMLUtil.iterate(child, (branch, j) -> {
            int conditionsCount = XMLUtil.iterate(branch, (predicate, k) -> {
                Result<CardPredicate> r = XMLUtil.parsePredicate(predicate, context.definedPredicateGetter());

                if (r.isError()) {
                    errors.add(r.getError());
                } else {
                    this.branches.computeIfAbsent(branch.getAttribute("id"), key -> new ArrayList<>())
                            .add(r.get());
                }
            });

            if (conditionsCount == 0) {
                this.branches.put(branch.getAttribute("id"), Collections.singletonList(c -> Result.of(true)));
            }
        }));

        List<CardPredicate> branchPredicates = new ArrayList<>();

        for (var branch : this.branches.values()) {
            branchPredicates.add(new CardPredicate.And(branch));
        }

        this.addPredicate(new CardPredicate.Or(branchPredicates));

        if (errors.isEmpty()) {
            return Result.of(this);
        } else {
            return Result.error("Error(s) while parsing Fork: \n\t%s", String.join("\n\t%s", errors));
        }
    }

    @Override
    public Result<LayerElement<Group>> createFactoryImmediately(Template template) {
        ArrayList<LayerElement<?>> children = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (LayerElement<?> factoryFactory : this.children) {
            factoryFactory.createFactory(template)
                    .ifPresent(children::add)
                    .ifError(errors::add);
        }

        if (!errors.isEmpty()) {
            return Result.error("Error creating child factories for layer %s:\n\t%s", this.getId(), String.join("\n\t"));
        }

        this.children = children;

        return Result.of(this);
    }

    @Override
    public Result<Group> createLayer(String parentId, JsonObject card) {
        String branch = null;

        for (var entry : this.branches.entrySet()) {
            boolean pass = true;

            for (CardPredicate p : entry.getValue()) {
                Result<Boolean> r = p.test(card);

                pass &= (!r.isError()) && r.get();
            }

            if (pass) {
                branch = entry.getKey();
                break;
            }
        }

        if (branch == null) {
            return Result.error("No branches of Fork '%s' match card '%s'", Layer.id(parentId, this.getId()), card.getAsString("name"));
        }

        List<Layer> layers = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        String id = this.getId() == null || this.getId().isEmpty()
                ? branch
                : this.getId() + "." + branch;

        for (LayerElement<?> factory : this.children) {
            factory.createLayer(Layer.id(parentId, id), card)
                    .ifError(errors::add)
                    .ifPresent(layers::add);
        }

        if (errors.isEmpty()) {
            return Result.of(new Group(parentId, id, this.getX(), this.getY(), layers));
        } else {
            return Result.error("Error creating child factories for layer %s:\n\t%s", Layer.id(parentId, id), String.join("\n\t%s", errors));
        }
    }
}
