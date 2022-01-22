package prototype.xd.scheduler.entities.settingsEntries;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SettingsEntry {
    
    protected final int layoutId;
    protected View view;
    
    SettingsEntry(int layoutId) {
        this.layoutId = layoutId;
    }
    
    protected View inflate(LayoutInflater inflater, ViewGroup viewGroup) {
        return inflater.inflate(layoutId, viewGroup, false);
    }
    
    public View get(ViewGroup viewGroup, LayoutInflater inflater) {
        if (view == null) {
            view = InitInnerViews(inflate(inflater, viewGroup));
        }
        return view;
    }
    
    protected View InitInnerViews(View rootView) {
        return rootView;
    }
}
