package net.aquadc.delegapter;

import java.util.ArrayList;
import java.util.Collection;

/**
 * ArrayList with public removeRange().
 */
public /*effectively-internal*/ class RrAL<E> extends ArrayList<E> {
    private RrAL() { super(); }
    private RrAL(int initialCapacity) { super(initialCapacity); }
    public RrAL(Collection<? extends E> copyFrom) { super(copyFrom); }
    static <E> RrAL<E> create(int initialCapacity) {
        return initialCapacity < 0 ? new RrAL<>() : new RrAL<>(initialCapacity);
    }

    @Override public void removeRange(int fromIndex, int toIndex) {
        super.removeRange(fromIndex, toIndex);
    }
}
