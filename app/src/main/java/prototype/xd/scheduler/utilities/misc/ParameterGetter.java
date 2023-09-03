package prototype.xd.scheduler.utilities.misc;

@FunctionalInterface
public interface ParameterGetter<T> { // NOSONAR, will be confusing if replaced by UnaryOperator<T>
    T get(T previousValue);
    
    default T get() {
        return get(null);
    }
}
