package prototype.xd.scheduler.utilities;

import static prototype.xd.scheduler.utilities.GraphicsUtilities.getExpiredUpcomingColor;
import static prototype.xd.scheduler.utilities.GraphicsUtilities.mixColorWithBg;

import androidx.annotation.ColorInt;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;
import java.util.function.BiConsumer;

import kotlin.jvm.functions.Function3;

public class Triplet<T> {
    
    public T upcoming;
    public T current;
    public T expired;
    
    enum Type {
        UPCOMING, CURRENT, EXPIRED
    }
    
    public <R> void applyTo(@NonNull Triplet<R> other, @NonNull BiConsumer<T, R> consumer) {
        consumer.accept(upcoming, other.upcoming);
        consumer.accept(current, other.current);
        consumer.accept(expired, other.expired);
    }
    
    public void setByType(@NonNull Type type, @Nullable T newValue) {
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
    
    public boolean has(@Nullable T value) {
        return Objects.equals(upcoming, value) || Objects.equals(current, value) || Objects.equals(expired, value);
    }
    
    public static class ColorTriplet extends Triplet<Integer> {
        
        public int getCurrentMixed(@ColorInt int surfaceColor, @IntRange(from = 0, to = 10) int adaptiveColorBalance) {
            return mixColorWithBg(current, surfaceColor, adaptiveColorBalance);
        }
        
        public int getUpcomingMixed(@ColorInt int surfaceColor, @IntRange(from = 0, to = 10) int adaptiveColorBalance) {
            return mixColorWithBg(getExpiredUpcomingColor(current, upcoming), surfaceColor, adaptiveColorBalance);
        }
        
        public int getExpiredMixed(@ColorInt int surfaceColor, @IntRange(from = 0, to = 10) int adaptiveColorBalance) {
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
        @NonNull
        public final D UPCOMING; // NOSONAR, this is an alias
        @NonNull
        public final D CURRENT; // NOSONAR
        @NonNull
        public final D EXPIRED; // NOSONAR
        
        DefaultedValueTriplet(@NonNull Function3<String, T, Type, D> supplier,
                              @NonNull String upcomingKey, @NonNull T upcomingDefaultValue,
                              @NonNull String currentKey, @NonNull T currentDefaultValue,
                              @NonNull String expiredKey, @NonNull T expiredDefaultValue) {
            UPCOMING = upcoming = supplier.invoke(upcomingKey, upcomingDefaultValue, Type.UPCOMING);
            CURRENT = current = supplier.invoke(currentKey, currentDefaultValue, Type.CURRENT);
            EXPIRED = expired = supplier.invoke(expiredKey, expiredDefaultValue, Type.EXPIRED);
        }
    }
    
}
