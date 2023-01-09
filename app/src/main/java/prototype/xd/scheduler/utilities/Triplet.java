package prototype.xd.scheduler.utilities;

import static prototype.xd.scheduler.utilities.GraphicsUtilities.getExpiredUpcomingColor;
import static prototype.xd.scheduler.utilities.GraphicsUtilities.mixColorWithBg;

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
    
}
