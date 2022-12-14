package prototype.xd.scheduler.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

// a list that has a method for cleaning up after removing an entry, useful for TodoListEntries and Groups
public abstract class BaseCleanupList<T> extends ArrayList<T> {
    
    protected BaseCleanupList() {
        super();
    }
    
    protected BaseCleanupList(int initialCapacity) {
        super(initialCapacity);
    }
    
    protected abstract @Nullable
    T handleOldEntry(@Nullable T oldEntry);
    
    @Override
    public T set(int index, T element) {
        return handleOldEntry(super.set(index, element));
    }
    
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
    
    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public boolean retainAll(@NonNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public boolean removeIf(@NonNull Predicate<? super T> filter) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void replaceAll(@NonNull UnaryOperator<T> operator) {
        throw new UnsupportedOperationException();
    }
}
