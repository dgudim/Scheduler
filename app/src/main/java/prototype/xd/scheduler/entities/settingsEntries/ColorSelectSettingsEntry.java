package prototype.xd.scheduler.entities.settingsEntries;

import static prototype.xd.scheduler.MainActivity.preferences;
import static prototype.xd.scheduler.utilities.BitmapUtilities.createSolidColorCircle;
import static prototype.xd.scheduler.utilities.Utilities.invokeColorDialogue;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import prototype.xd.scheduler.R;

public class ColorSelectSettingsEntry extends SettingsEntry{
    
    private final String colorKey;
    private final int defaultColor;
    private final Context context;
    private final String text;
    
    public ColorSelectSettingsEntry(String colorKey, int defaultColor, String text, Context context) {
        super(R.layout.settings_color_select_entry);
        this.colorKey = colorKey;
        this.defaultColor = defaultColor;
        this.text = text;
        this.context = context;
    }
    
    @Override
    protected View InitInnerViews(View rootView) {
        ((TextView)rootView.findViewById(R.id.textView)).setText(text);
        ImageView imageView = rootView.findViewById(R.id.imageView);
        imageView.setImageBitmap(createSolidColorCircle(preferences.getInt(colorKey, defaultColor)));
        imageView.setOnClickListener(view -> invokeColorDialogue(colorKey, (ImageView) view, defaultColor, context));
        return super.InitInnerViews(rootView);
    }
}
