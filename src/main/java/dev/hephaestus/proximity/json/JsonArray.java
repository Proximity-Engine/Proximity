package dev.hephaestus.proximity.json;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.jetbrains.annotations.NotNull;
import org.quiltmc.json5.JsonReader;
import org.quiltmc.json5.JsonToken;
import org.quiltmc.json5.JsonWriter;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.UnaryOperator;

public final class JsonArray extends JsonElement implements List<JsonElement>, ProxyArray {
    private final List<JsonElement> elements;

    public JsonArray() {
        elements = new ArrayList<>();
    }

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

    public boolean contains(String s) {
        for (JsonElement element : this) {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString() && element.getAsJsonPrimitive().getAsString().equalsIgnoreCase(s)) {
                return true;
            }
        }

        return false;
    }

    public void add(String string) {
        elements.add(string == null ? JsonNull.INSTANCE : new JsonPrimitive(string));
    }

    public boolean add(JsonElement element) {
        if (element == null) {
            element = JsonNull.INSTANCE;
        }

        return elements.add(element);
    }

    public JsonElement set(int index, JsonElement element) {
        return elements.set(index, element);
    }

    @Override
    public void add(int index, JsonElement element) {
        if (element == null) {
            element = JsonNull.INSTANCE;
        }

        elements.add(index, element);
    }

    public JsonElement remove(int index) {
        return elements.remove(index);
    }

    public int size() {
        return elements.size();
    }

    public @NotNull Iterator<JsonElement> iterator() {
        return elements.iterator();
    }

    public JsonElement get(int i) {
        return elements.get(i);
    }

    @Override
    public Number getAsNumber() {
        if (elements.size() == 1) {
            return elements.get(0).getAsNumber();
        }
        throw new IllegalStateException();
    }

    @Override
    public String getAsString() {
        if (elements.size() == 1) {
            return elements.get(0).getAsString();
        }
        throw new IllegalStateException();
    }

    @Override
    public double getAsDouble() {
        if (elements.size() == 1) {
            return elements.get(0).getAsDouble();
        }
        throw new IllegalStateException();
    }

    @Override
    public BigDecimal getAsBigDecimal() {
        if (elements.size() == 1) {
            return elements.get(0).getAsBigDecimal();
        }
        throw new IllegalStateException();
    }

    @Override
    public BigInteger getAsBigInteger() {
        if (elements.size() == 1) {
            return elements.get(0).getAsBigInteger();
        }
        throw new IllegalStateException();
    }

    @Override
    public float getAsFloat() {
        if (elements.size() == 1) {
            return elements.get(0).getAsFloat();
        }
        throw new IllegalStateException();
    }

    @Override
    public long getAsLong() {
        if (elements.size() == 1) {
            return elements.get(0).getAsLong();
        }
        throw new IllegalStateException();
    }

    @Override
    public int getAsInt() {
        if (elements.size() == 1) {
            return elements.get(0).getAsInt();
        }
        throw new IllegalStateException();
    }

    @Override
    public byte getAsByte() {
        if (elements.size() == 1) {
            return elements.get(0).getAsByte();
        }
        throw new IllegalStateException();
    }

    @Override
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
    public boolean getAsBoolean() {
        if (elements.size() == 1) {
            return elements.get(0).getAsBoolean();
        }
        throw new IllegalStateException();
    }

    @Override
    public boolean equals(Object o) {
        return (o == this) || (o instanceof JsonArray && ((JsonArray) o).elements.equals(elements));
    }

    @Override
    public int hashCode() {
        return elements.hashCode();
    }

    @Override
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return elements.contains(o);
    }

    @Override
    public Object @NotNull [] toArray() {
        return elements.toArray();
    }

    @Override
    public <T> T @NotNull [] toArray(T @NotNull [] a) {
        //noinspection SuspiciousToArrayCall
        return elements.toArray(a);
    }

    @Override
    public boolean remove(Object o) {
        return elements.remove(o);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return elements.containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends JsonElement> c) {
        return elements.addAll(c);
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends JsonElement> c) {
        return elements.addAll(index, c);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return elements.removeAll(c);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return elements.retainAll(c);
    }

    @Override
    public void replaceAll(UnaryOperator<JsonElement> operator) {
        elements.replaceAll(operator);
    }

    @Override
    public void sort(Comparator<? super JsonElement> c) {
        elements.sort(c);
    }

    @Override
    public void clear() {
        elements.clear();
    }

    @Override
    public int indexOf(Object o) {
        return elements.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return elements.lastIndexOf(o);
    }

    @Override
    public @NotNull ListIterator<JsonElement> listIterator() {
        return elements.listIterator();
    }

    @Override
    public @NotNull ListIterator<JsonElement> listIterator(int index) {
        return elements.listIterator(index);
    }

    @Override
    public @NotNull List<JsonElement> subList(int fromIndex, int toIndex) {
        return elements.subList(fromIndex, toIndex);
    }

    @Override
    public Spliterator<JsonElement> spliterator() {
        return elements.spliterator();
    }

    @Override
    public Object get(long index) {
        return this.elements.get((int) index);
    }

    @Override
    public void set(long index, Value value) {
        // TODO: Support writes
    }

    @Override
    public long getSize() {
        return this.elements.size();
    }
}
