package dev.hephaestus.proximity.utils;

import java.util.Collection;
import java.util.Iterator;

public class UnmodifiableIterator<T> implements Iterator<T> {
    private final Iterator<? extends T> itr;

    public UnmodifiableIterator(Iterator<? extends T> itr) {
        this.itr = itr;
    }

    public UnmodifiableIterator(Collection<? extends T> collection) {
        this.itr = collection.iterator();
    }

    @Override
    public boolean hasNext() {
        return this.itr.hasNext();
    }

    @Override
    public T next() {
        return this.itr.next();
    }
}
