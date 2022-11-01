package prototype.xd.scheduler.entities.settings_entries;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

enum SettingsEntryType {
    CALENDAR_ACCOUNT, CALENDAR, COMPOUND_CUSTOMIZATION,
    RESET_BUTTON, SEEK_BAR, SEEK_BAR_DISCRETE, SWITCH, TITLE_BAR, ADAPTIVE_BACKGROUND_SETTINGS, APP_THEME_SELECTOR
}

public abstract class SettingsEntry {
    
    protected int layoutId;
    protected SettingsEntryType entryType;
    
    SettingsEntry(int layoutId) {
        this.layoutId = layoutId;
    }
    
    public int getType() {
        return entryType.ordinal();
    }
    
    public static int getTypesCount() {
        return SettingsEntryType.values().length;
    }
    
    public View get(View convertView, ViewGroup viewGroup) {
        if (convertView == null) {
            convertView = LayoutInflater.from(viewGroup.getContext()).inflate(layoutId, viewGroup, false);
            cacheViews(convertView);
        }
        return initInnerViews(convertView, viewGroup);
    }
    
    protected void cacheViews(View convertView) {
        //optional
    }
    
    protected View initInnerViews(View convertView, ViewGroup viewGroup) {
        return convertView;
    }
}
