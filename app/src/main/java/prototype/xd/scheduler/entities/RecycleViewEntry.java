package prototype.xd.scheduler.entities;

/**
 * Class providing necessary methods for use with recycle view (stable ids)
 */
public abstract class RecycleViewEntry {
    private long recyclerViewId = -1;
    
    public abstract int getRecyclerViewType();
    
    public void assignRecyclerViewId(long recyclerViewId) {
        this.recyclerViewId = recyclerViewId;
    }
    
    public long getRecyclerViewId() throws IllegalArgumentException {
        if (recyclerViewId == -1) {
            throw new IllegalStateException("Can't get unset id");
        }
        return recyclerViewId;
    }
}
