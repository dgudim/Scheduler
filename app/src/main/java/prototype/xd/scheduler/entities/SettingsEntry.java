package prototype.xd.scheduler.entities;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SettingsEntry {
    
    int layoutId;
    
    SettingsEntry(int layoutId){
        this.layoutId = layoutId;
    }
    
    public View inflate(LayoutInflater inflater, ViewGroup viewGroup){
        return inflater.inflate(layoutId, viewGroup, false);
    }
}
