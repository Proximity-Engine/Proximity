package dev.hephaestus.proximity.json;

import org.graalvm.polyglot.HostAccess;
import org.jetbrains.annotations.NotNull;
import org.quiltmc.json5.JsonReader;
import org.quiltmc.json5.JsonToken;
import org.quiltmc.json5.JsonWriter;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.UnaryOperator;

public final class JsonArray extends JsonElement implements List<JsonElement> {
    private final List<JsonElement> elements;

    @HostAccess.Export
    public JsonArray() {
        elements = new ArrayList<>();
    }

    @HostAccess.Export
    public JsonArray(int capacity) {
        elements = new ArrayList<>(capacity);
    }

    public JsonArray(Collection<JsonElement> collection) {
        this.elements = new ArrayList<>(collection);
    }

    public static JsonArray parseArray(JsonReader reader) throws IOException {
        reader.beginArray();

        JsonArray array = new JsonArray();

        while (reader.hasNext() && reader.peek() != JsonToken.END_ARRAY) {
            array.add(JsonElement.parseElement(reader));
        }

        reader.endArray();

        return array;
    }

    @Override
    @HostAccess.Export
    public JsonArray deepCopy() {
        if (!elements.isEmpty()) {
            JsonArray result = new JsonArray(elements.size());
            for (JsonElement element : elements) {
                result.add(element.deepCopy());
            }
            return result;
        }
        return new JsonArray();
    }

    @HostAccess.Export
    public boolean contains(String s) {
        for (JsonElement element : this) {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString() && element.getAsJsonPrimitive().getAsString().equalsIgnoreCase(s)) {
                return true;
            }
        }

        return false;
    }

    @HostAccess.Export
    public void add(String string) {
        elements.add(string == null ? JsonNull.INSTANCE : new JsonPrimitive(string));
    }

    @HostAccess.Export
    public boolean add(JsonElement element) {
        if (element == null) {
            element = JsonNull.INSTANCE;
        }

        return elements.add(element);
    }

    @HostAccess.Export
    public JsonElement set(int index, JsonElement element) {
        return elements.set(index, element);
    }

    @Override
    @HostAccess.Export
    public void add(int index, JsonElement element) {
        if (element == null) {
            element = JsonNull.INSTANCE;
        }

        elements.add(index, element);
    }

    @HostAccess.Export
    public JsonElement remove(int index) {
        return elements.remove(index);
    }

    @HostAccess.Export
    public int size() {
        return elements.size();
    }

    @HostAccess.Export
    public @NotNull Iterator<JsonElement> iterator() {
        return elements.iterator();
    }

    @HostAccess.Export
    public JsonElement get(int i) {
        return elements.get(i);
    }

    @Override
    @HostAccess.Export
    public Number getAsNumber() {
        if (elements.size() == 1) {
            return elements.get(0).getAsNumber();
        }
        throw new IllegalStateException();
    }

    @Override
    @HostAccess.Export
    public String getAsString() {
        if (elements.size() == 1) {
            return elements.get(0).getAsString();
        }
        throw new IllegalStateException();
    }

    @Override
    @HostAccess.Export
    public double getAsDouble() {
        if (elements.size() == 1) {
            return elements.get(0).getAsDouble();
        }
        throw new IllegalStateException();
    }

    @Override
    @HostAccess.Export
    public float getAsFloat() {
        if (elements.size() == 1) {
            return elements.get(0).getAsFloat();
        }
        throw new IllegalStateException();
    }

    @Override
    @HostAccess.Export
    public long getAsLong() {
        if (elements.size() == 1) {
            return elements.get(0).getAsLong();
        }
        throw new IllegalStateException();
    }

    @Override
    @HostAccess.Export
    public int getAsInt() {
        if (elements.size() == 1) {
            return elements.get(0).getAsInt();
        }
        throw new IllegalStateException();
    }

    @Override
    @HostAccess.Export
    public byte getAsByte() {
        if (elements.size() == 1) {
            return elements.get(0).getAsByte();
        }
        throw new IllegalStateException();
    }

    @Override
    @HostAccess.Export
    public short getAsShort() {
        if (elements.size() == 1) {
            return elements.get(0).getAsShort();
        }
        throw new IllegalStateException();
    }

    @Override
    protected void write(JsonWriter writer) throws IOException {
        writer.beginArray();

        for (JsonElement element : this.elements) {
            element.write(writer);
        }

        writer.endArray();
    }

    @Override
    @HostAccess.Export
    public boolean getAsBoolean() {
        if (elements.size() == 1) {
            return elements.get(0).getAsBoolean();
        }
        throw new IllegalStateException();
    }

    @Override
    @HostAccess.Export
    public boolean equals(Object o) {
        return (o == this) || (o instanceof JsonArray && ((JsonArray) o).elements.equals(elements));
    }

    @Override
    @HostAccess.Export
    public int hashCode() {
        return elements.hashCode();
    }

    @Override
    @HostAccess.Export
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    @Override
    @HostAccess.Export
    public boolean contains(Object o) {
        return elements.contains(o);
    }

    @Override
    @HostAccess.Export
    public Object @NotNull [] toArray() {
        return elements.toArray();
    }

    @Override
    @HostAccess.Export
    public <T> T @NotNull [] toArray(T @NotNull [] a) {
        //noinspection SuspiciousToArrayCall
        return elements.toArray(a);
    }

    @Override
    @HostAccess.Export
    public boolean remove(Object o) {
        return elements.remove(o);
    }

    @Override
    @HostAccess.Export
    public boolean containsAll(@NotNull Collection<?> c) {
        return elements.containsAll(c);
    }

    @Override
    @HostAccess.Export
    public boolean addAll(@NotNull Collection<? extends JsonElement> c) {
        return elements.addAll(c);
    }

    @Override
    @HostAccess.Export
    public boolean addAll(int index, @NotNull Collection<? extends JsonElement> c) {
        return elements.addAll(index, c);
    }

    @Override
    @HostAccess.Export
    public boolean removeAll(@NotNull Collection<?> c) {
        return elements.removeAll(c);
    }

    @Override
    @HostAccess.Export
    public boolean retainAll(@NotNull Collection<?> c) {
        return elements.retainAll(c);
    }

    @Override
    @HostAccess.Export
    public void replaceAll(UnaryOperator<JsonElement> operator) {
        elements.replaceAll(operator);
    }

    @Override
    @HostAccess.Export
    public void sort(Comparator<? super JsonElement> c) {
        elements.sort(c);
    }

    @Override
    @HostAccess.Export
    public void clear() {
        elements.clear();
    }

    @Override
    @HostAccess.Export
    public int indexOf(Object o) {
        return elements.indexOf(o);
    }

    @Override
    @HostAccess.Export
    public int lastIndexOf(Object o) {
        return elements.lastIndexOf(o);
    }

    @Override
    @HostAccess.Export
    public @NotNull ListIterator<JsonElement> listIterator() {
        return elements.listIterator();
    }

    @Override
    @HostAccess.Export
    public @NotNull ListIterator<JsonElement> listIterator(int index) {
        return elements.listIterator(index);
    }

    @Override
    @HostAccess.Export
    public @NotNull List<JsonElement> subList(int fromIndex, int toIndex) {
        return elements.subList(fromIndex, toIndex);
    }

    @Override
    @HostAccess.Export
    public Spliterator<JsonElement> spliterator() {
        return elements.spliterator();
    }
}
