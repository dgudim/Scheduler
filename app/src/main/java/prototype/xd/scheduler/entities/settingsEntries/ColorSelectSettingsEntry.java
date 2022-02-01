package prototype.xd.scheduler.entities.settingsEntries;

import static prototype.xd.scheduler.MainActivity.preferences;
import static prototype.xd.scheduler.entities.settingsEntries.SettingsEntryType.COLOR_SELECT;
import static prototype.xd.scheduler.utilities.Utilities.invokeColorDialogue;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import prototype.xd.scheduler.R;

public class ColorSelectSettingsEntry extends SettingsEntry {
    
    private final String colorKey;
    private final int defaultColor;
    private final String text;
    
    public ColorSelectSettingsEntry(String colorKey, int defaultColor, String text) {
        super(R.layout.settings_color_select_entry);
        this.colorKey = colorKey;
        this.defaultColor = defaultColor;
        this.text = text;
        entryType = COLOR_SELECT;
    }
    
    @Override
    protected View InitInnerViews(View convertView, ViewGroup viewGroup) {
        ((TextView) convertView.findViewById(R.id.textView)).setText(text);
        CardView colorSelect = convertView.findViewById(R.id.color);
        colorSelect.setCardBackgroundColor(preferences.getInt(colorKey, defaultColor));
        colorSelect.setOnClickListener(view -> invokeColorDialogue((CardView) view, colorKey, defaultColor));
        return super.InitInnerViews(convertView, viewGroup);
    }
}
