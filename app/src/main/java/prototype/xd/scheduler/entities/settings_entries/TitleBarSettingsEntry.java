package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.TITLE_BAR;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import prototype.xd.scheduler.R;

public class TitleBarSettingsEntry extends SettingsEntry{
    
    private final String text;
    
    public TitleBarSettingsEntry(String text) {
        super(R.layout.settings_title_entry);
        this.text = text;
        entryType = TITLE_BAR;
    }
    
    @Override
    protected View initInnerViews(View convertView, ViewGroup viewGroup) {
        ((TextView)convertView.findViewById(R.id.textView)).setText(text);
        return super.initInnerViews(convertView, viewGroup);
    }
}
