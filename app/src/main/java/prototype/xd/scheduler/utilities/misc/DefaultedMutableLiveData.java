package prototype.xd.scheduler.utilities.misc;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

public class DefaultedMutableLiveData<T> extends MutableLiveData<T> {
    
    private final T defaultValue;
    
    public DefaultedMutableLiveData(T defaultValue) {
        this.defaultValue = defaultValue;
    }
    
    public DefaultedMutableLiveData() {
        throw new UnsupportedOperationException("Null initialization is not supported");
    }
   
    @NonNull
    @Override
    public T getValue() {
        T val = super.getValue();
        return val == null ? defaultValue : val;
    }
}
