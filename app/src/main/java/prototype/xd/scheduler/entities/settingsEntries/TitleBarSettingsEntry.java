package prototype.xd.scheduler.entities.settingsEntries;

import static prototype.xd.scheduler.entities.settingsEntries.SettingsEntryType.TITLE_BAR;

import android.view.View;
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
    protected View InitInnerViews(View rootView) {
        ((TextView)rootView.findViewById(R.id.textView)).setText(text);
        return super.InitInnerViews(rootView);
    }
}
