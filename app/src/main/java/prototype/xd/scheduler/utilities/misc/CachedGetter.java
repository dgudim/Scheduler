package prototype.xd.scheduler.utilities.misc;

import androidx.annotation.Nullable;

public class CachedGetter<T> {
    
    private final ParameterGetter<T> parameterGetter;
    
    private T value;
    private boolean valid;
    
    public CachedGetter(@Nullable ParameterGetter<T> parameterGetter) {
        this.parameterGetter = parameterGetter;
    }
    
    public void invalidate() {
        valid = false;
    }
    
    public T get(T previousValue) {
        if (!valid) {
            if (parameterGetter == null) {
                return previousValue;
            }
            value = parameterGetter.get(previousValue);
            valid = true;
        }
        return value;
    }
    
    public T get() {
        return get(null);
    }
    
}
