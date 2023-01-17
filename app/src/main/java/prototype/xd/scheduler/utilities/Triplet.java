package prototype.xd.scheduler.utilities;

import static prototype.xd.scheduler.utilities.GraphicsUtilities.getExpiredUpcomingColor;
import static prototype.xd.scheduler.utilities.GraphicsUtilities.mixColorWithBg;

import java.util.function.BiConsumer;

import kotlin.jvm.functions.Function3;

public class Triplet<T> {
    public T upcoming;
    public T current;
    public T expired;
    
    enum Type {
        UPCOMING, CURRENT, EXPIRED
    }
    
    public <R> void applyTo(Triplet<R> other, BiConsumer<T, R> consumer) {
        consumer.accept(upcoming, other.upcoming);
        consumer.accept(current, other.current);
        consumer.accept(expired, other.expired);
    }
    
    public void setByType(Type type, T newValue) {
        switch (type) {
            case UPCOMING:
                upcoming = newValue;
                break;
            case CURRENT:
                current = newValue;
                break;
            case EXPIRED:
                expired = newValue;
                break;
        }
    }
    
    public boolean has(T value) {
        return upcoming.equals(value) || current.equals(value) || expired.equals(value);
    }
    
    public static class ColorTriplet extends Triplet<Integer> {
        
        public int getCurrentMixed(int surfaceColor, int adaptiveColorBalance) {
            return mixColorWithBg(current, surfaceColor, adaptiveColorBalance);
        }
        
        public int getUpcomingMixed(int surfaceColor, int adaptiveColorBalance) {
            return mixColorWithBg(getExpiredUpcomingColor(current, upcoming), surfaceColor, adaptiveColorBalance);
        }
        
        public int getExpiredMixed(int surfaceColor, int adaptiveColorBalance) {
            return mixColorWithBg(getExpiredUpcomingColor(current, expired), surfaceColor, adaptiveColorBalance);
        }
        
        public int getUpcoming() {
            return getExpiredUpcomingColor(current, upcoming);
        }
        
        public int getExpired() {
            return getExpiredUpcomingColor(current, expired);
        }
        
    }
    
    public static class DefaultedValueTriplet<T, D extends Keys.DefaultedValue<T>> extends Triplet<D> {
    
        // aliases
        public final D UPCOMING; // NOSONAR, this is an alias
        public final D CURRENT; // NOSONAR
        public final D EXPIRED; // NOSONAR
        
        DefaultedValueTriplet(Function3<String, T, Type, D> supplier,
                              String upcomingKey, T upcomingDefaultValue,
                              String currentKey, T currentDefaultValue,
                              String expiredKey, T expiredDefaultValue) {
            UPCOMING = upcoming = supplier.invoke(upcomingKey, upcomingDefaultValue, Type.UPCOMING);
            CURRENT = current = supplier.invoke(currentKey, currentDefaultValue, Type.CURRENT);
            EXPIRED = expired = supplier.invoke(expiredKey, expiredDefaultValue, Type.EXPIRED);
        }
    }
    
}
