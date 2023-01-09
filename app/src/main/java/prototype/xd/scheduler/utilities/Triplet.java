package prototype.xd.scheduler.utilities;

import static prototype.xd.scheduler.utilities.GraphicsUtilities.getExpiredUpcomingColor;

import java.util.function.BiConsumer;

public class Triplet<T> {
    public T upcoming;
    public T current;
    public T expired;
    
    public <R> void applyTo(Triplet<R> other, BiConsumer<T, R> consumer) {
        consumer.accept(upcoming, other.upcoming);
        consumer.accept(current, other.current);
        consumer.accept(expired, other.expired);
    }
    
    public static class ColorTriplet extends Triplet<Integer> {
        
        public Integer getUpcomingMixed(int currentColorOverride) {
            return getExpiredUpcomingColor(currentColorOverride, upcoming);
        }
        
        public Integer getUpcomingMixed() {
            return getExpiredUpcomingColor(current, upcoming);
        }
        
        public Integer getExpiredMixed(int currentColorOverride) {
            return getExpiredUpcomingColor(currentColorOverride, expired);
        }
        
        public Integer getExpiredMixed() {
            return getExpiredUpcomingColor(current, expired);
        }
        
    }
    
}
