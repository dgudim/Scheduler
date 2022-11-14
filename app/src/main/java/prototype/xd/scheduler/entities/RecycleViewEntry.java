package prototype.xd.scheduler.entities;

// class providing necessary methods for use with recycle view
public abstract class RecycleViewEntry {
    long id = -1;
    
    public abstract int getType();
    
    public void assignId(long id) {
        this.id = id;
    }
    
    public long getId() {
        if (id == -1) {
            throw new IllegalStateException("Can't get unset id");
        }
        return id;
    }
}