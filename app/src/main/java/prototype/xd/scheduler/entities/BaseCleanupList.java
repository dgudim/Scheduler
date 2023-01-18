package prototype.xd.scheduler.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * A list that has a method for cleaning up after removing an entry, useful for TodoEntries and Groups
 *
 * @param <T> type to store
 */
public abstract class BaseCleanupList<T> extends ArrayList<T> {
    
    private static final long serialVersionUID = -7450793963107268853L;
    
    BaseCleanupList() {
    
    }
    
    protected BaseCleanupList(int initialCapacity) {
        super(initialCapacity);
    }
    
    /**
     * Do stuff before removing entry from list
     *
     * @param oldEntry entry to be removed
     * @return the same entry for chaining
     */
    @Nullable
    protected abstract T handleOldEntry(@Nullable T oldEntry);
    
    @Nullable
    @Override
    public T set(int index, T element) {
        return handleOldEntry(super.set(index, element));
    }
    
    @Nullable
    @Override
    public T remove(int index) {
        return handleOldEntry(super.remove(index));
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public boolean remove(@Nullable Object o) {
        boolean removed = super.remove(o);
        if (removed) {
            handleOldEntry((T) o);
        }
        return removed;
    }
    
    @Override
    public void clear() {
        for (T entry : this) {
            handleOldEntry(entry);
        }
        super.clear();
    }
    
    // too lazy to implement
    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        throw new UnsupportedOperationException("removeAll is not supported");
    }
    
    @Override
    public boolean retainAll(@NonNull Collection<?> c) {
        throw new UnsupportedOperationException("retainAll is not supported");
    }
    
    @Override
    public boolean removeIf(@NonNull Predicate<? super T> filter) {
        throw new UnsupportedOperationException("removeIf is not supported");
    }
    
    @Override
    public void replaceAll(@NonNull UnaryOperator<T> operator) {
        throw new UnsupportedOperationException("replaceAll not supported");
    }
}
