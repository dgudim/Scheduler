package prototype.xd.scheduler.utilities.misc;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

public class NonNullMutableLiveData<T> extends MutableLiveData<T> {
    
    public NonNullMutableLiveData(@NonNull T value) {
        super(value);
    }
    
    public NonNullMutableLiveData() {
        throw new UnsupportedOperationException("Null initialization is not supported");
    }
    
    @SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public T getValue() {
        return super.getValue();
    }
}
