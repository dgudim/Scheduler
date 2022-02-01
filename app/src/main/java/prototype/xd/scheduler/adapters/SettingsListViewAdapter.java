package prototype.xd.scheduler.adapters;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;

import prototype.xd.scheduler.entities.settingsEntries.SettingsEntry;

public class SettingsListViewAdapter extends BaseAdapter {
    
    private final ArrayList<SettingsEntry> settingsEntries;
    
    public SettingsListViewAdapter(ArrayList<SettingsEntry> settingsEntries) {
        this.settingsEntries = settingsEntries;
    }
    
    @Override
    public int getItemViewType(int i) {
        return settingsEntries.get(i).getType();
    }
    
    @Override
    public int getViewTypeCount() {
        return SettingsEntry.getTypesCount();
    }
    
    @Override
    public int getCount() {
        return settingsEntries.size();
    }
    
    @Override
    public Object getItem(int i) {
        return settingsEntries.get(i);
    }
    
    @Override
    public long getItemId(int i) {
        return i;
    }
    
    @Override
    public View getView(int i, View view, ViewGroup parent) {
        return settingsEntries.get(i).get(view, parent);
    }
}
