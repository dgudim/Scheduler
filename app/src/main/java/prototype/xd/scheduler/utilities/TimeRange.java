package prototype.xd.scheduler.utilities;

import static prototype.xd.scheduler.utilities.DateManager.msToDays;
import static prototype.xd.scheduler.utilities.DateManager.msUTCtoDaysLocal;

import androidx.annotation.NonNull;

public class TimeRange {
    
    public static final String NAME = TimeRange.class.getSimpleName();
    
    public static final TimeRange NullRange = new TimeRange(-1, -1);
    
    private boolean inDays;
    
    private long start;
    private long end;
    
    public TimeRange(long startMsUTC, long endMsUTC) {
        start = startMsUTC;
        end = endMsUTC;
    }
    
    @NonNull
    public TimeRange toDays(boolean clone, boolean local) {
        if (clone) {
            return new TimeRange(start, end).toDays(false, local);
        }
        if (inDays) {
            Logger.warning(NAME, "Trying to convert range to days but it's already in days");
            return this;
        }
        start = local ? msUTCtoDaysLocal(start) : msToDays(start);
        end = local ? msUTCtoDaysLocal(end) : msToDays(end);
        inDays = true;
        return this;
    }
    
    public long getStart() {
        return start;
    }
    
    public long getEnd() {
        return end;
    }
    
    @NonNull
    @Override
    public String toString() {
        return "Time range in " + (inDays ? "days" : "ms") + " from " + start + " to " + end;
    }
}
