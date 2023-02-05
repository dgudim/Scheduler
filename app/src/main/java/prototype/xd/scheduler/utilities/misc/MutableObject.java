package prototype.xd.scheduler.utilities.misc;

public class MutableObject<T> {
    
    T obj;
    
    public T get() {
        return obj;
    }
    
    public void set(T obj) {
        this.obj = obj;
    }
}
