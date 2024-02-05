package prototype.xd.scheduler.utilities.misc;

import androidx.annotation.Nullable;

import java.util.Objects;

public class CachedGetter<T> implements Ephemeral {
    
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
    
    public boolean getBool() {
        return Objects.equals(get(null), Boolean.TRUE);
    }
    
}
