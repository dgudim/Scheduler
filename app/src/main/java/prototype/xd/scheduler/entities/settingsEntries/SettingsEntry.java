package prototype.xd.scheduler.entities.settingsEntries;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

enum SettingsEntryType {CALENDAR_ACCOUNT, CALENDAR, COLOR_SELECT, RESET_BUTTONS, SEEK_BAR, SWITCH, TITLE_BAR, UNDEFINED}

public class SettingsEntry {
    
    protected final int layoutId;
    protected SettingsEntryType entryType;
    
    SettingsEntry(int layoutId) {
        this.layoutId = layoutId;
    }
    
    public int getType(){
        return entryType.ordinal();
    }
    
    public static int getTypesCount(){
        return SettingsEntryType.values().length;
    }
    
    protected View inflate(LayoutInflater inflater, ViewGroup viewGroup) {
        return inflater.inflate(layoutId, viewGroup, false);
    }
    
    public View get(View convertView, ViewGroup viewGroup, LayoutInflater inflater) {
        if (convertView == null) {
            convertView = inflate(inflater, viewGroup);
        }
        return InitInnerViews(convertView);
    }
    
    protected View InitInnerViews(View rootView) {
        return rootView;
    }
}
