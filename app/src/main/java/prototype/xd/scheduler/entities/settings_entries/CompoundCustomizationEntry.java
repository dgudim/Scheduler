package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.COMPOUND_CUSTOMIZATION;
import static prototype.xd.scheduler.utilities.BitmapUtilities.mixTwoColors;
import static prototype.xd.scheduler.utilities.Keys.DEFAULT_COLOR_MIX_FACTOR;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;
import static prototype.xd.scheduler.utilities.Utilities.invokeColorDialogue;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.Utilities;

public class CompoundCustomizationEntry extends SettingsEntry {
    
    private CardView bgColor_upcoming_select;
    private CardView bgColor_current_select;
    private CardView bgColor_expired_select;
    
    private CardView fontColor_upcoming_select;
    private CardView fontColor_current_select;
    private CardView fontColor_expired_select;
    
    private CardView borderColor_upcoming_select;
    private CardView borderColor_current_select;
    private CardView borderColor_expired_select;
    
    private TextView preview_text_upcoming;
    private LinearLayout preview_border_upcoming;
    
    private TextView preview_text;
    private LinearLayout preview_border;
    
    private TextView preview_text_expired;
    private LinearLayout preview_border_expired;
    
    public CompoundCustomizationEntry() {
        super(R.layout.settings_compound_customization_entry);
        entryType = COMPOUND_CUSTOMIZATION;
    }
    
    @Override
    protected void cacheViews(View convertView) {
        bgColor_current_select = convertView.findViewById(R.id.backgroundColor);
        bgColor_upcoming_select = convertView.findViewById(R.id.backgroundColor_upcoming);
        bgColor_expired_select = convertView.findViewById(R.id.backgroundColor_expired);
    
        fontColor_current_select = convertView.findViewById(R.id.fontColor);
        fontColor_upcoming_select = convertView.findViewById(R.id.fontColor_upcoming);
        fontColor_expired_select = convertView.findViewById(R.id.fontColor_expired);
    
        borderColor_current_select = convertView.findViewById(R.id.borderColor);
        borderColor_upcoming_select = convertView.findViewById(R.id.borderColor_upcoming);
        borderColor_expired_select = convertView.findViewById(R.id.borderColor_expired);
    
        preview_text_upcoming = convertView.findViewById(R.id.preview_text_upcoming);
        preview_border_upcoming = convertView.findViewById(R.id.preview_border_upcoming);
    
        preview_text = convertView.findViewById(R.id.preview_text);
        preview_border = convertView.findViewById(R.id.preview_border);
    
        preview_text_expired = convertView.findViewById(R.id.preview_text_expired);
        preview_border_expired = convertView.findViewById(R.id.preview_border_expired);
    
        updatePreviews();
    
        bgColor_current_select.setOnClickListener(v -> invokeColorDialogue(bgColor_current_select, this,
                Keys.BG_COLOR, Keys.SETTINGS_DEFAULT_BG_COLOR));
        bgColor_upcoming_select.setOnClickListener(v -> invokeColorDialogue(bgColor_upcoming_select, this,
                Keys.UPCOMING_BG_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BG_COLOR));
        bgColor_expired_select.setOnClickListener(v -> invokeColorDialogue(bgColor_expired_select, this,
                Keys.EXPIRED_BG_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BG_COLOR));
    
        fontColor_current_select.setOnClickListener(v -> invokeColorDialogue(fontColor_current_select, this,
                Keys.FONT_COLOR, Keys.SETTINGS_DEFAULT_FONT_COLOR));
        fontColor_upcoming_select.setOnClickListener(v -> invokeColorDialogue(fontColor_upcoming_select, this,
                Keys.UPCOMING_FONT_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_FONT_COLOR));
        fontColor_expired_select.setOnClickListener(v -> invokeColorDialogue(fontColor_expired_select, this,
                Keys.EXPIRED_FONT_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_FONT_COLOR));
    
        borderColor_current_select.setOnClickListener(v -> invokeColorDialogue(borderColor_current_select, this,
                Keys.BORDER_COLOR, Keys.SETTINGS_DEFAULT_BORDER_COLOR));
        borderColor_upcoming_select.setOnClickListener(v -> invokeColorDialogue(borderColor_upcoming_select, this,
                Keys.UPCOMING_BORDER_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BORDER_COLOR));
        borderColor_expired_select.setOnClickListener(v -> invokeColorDialogue(borderColor_expired_select, this,
                Keys.EXPIRED_BORDER_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BORDER_COLOR));
    
        Utilities.addSliderChangeListener(
                convertView.findViewById(R.id.upcoming_border_thickness_text),
                convertView.findViewById(R.id.upcoming_border_thickness_seek_bar),
                this, R.string.settings_upcoming_border_thickness,
                Keys.UPCOMING_BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_UPCOMING_BORDER_THICKNESS);
    
        Utilities.addSliderChangeListener(
                convertView.findViewById(R.id.current_border_thickness_text),
                convertView.findViewById(R.id.current_border_thickness_seek_bar),
                this, R.string.settings_current_border_thickness,
                Keys.BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_BORDER_THICKNESS);
    
        Utilities.addSliderChangeListener(
                convertView.findViewById(R.id.expired_border_thickness_text),
                convertView.findViewById(R.id.expired_border_thickness_seek_bar),
                this, R.string.settings_expired_border_thickness,
                Keys.EXPIRED_BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_EXPIRED_BORDER_THICKNESS);
    }
    
    protected void updatePreviews() {
        updatePreviewFonts();
        updatePreviewBgs();
        updatePreviewBorders();
        updateUpcomingPreviewBorderThickness(preferences.getInt(Keys.UPCOMING_BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_UPCOMING_BORDER_THICKNESS));
        updateCurrentPreviewBorderThickness(preferences.getInt(Keys.BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_BORDER_THICKNESS));
        updateExpiredPreviewBorderThickness(preferences.getInt(Keys.EXPIRED_BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_EXPIRED_BORDER_THICKNESS));
    }
    
    public void updateUpcomingPreviewBorderThickness(int borderThickness) {
        preview_border_upcoming.setPadding(borderThickness,
                borderThickness, borderThickness, 0);
    }
    
    public void updateCurrentPreviewBorderThickness(int borderThickness) {
        preview_border.setPadding(borderThickness,
                borderThickness, borderThickness, 0);
    }
    
    public void updateExpiredPreviewBorderThickness(int borderThickness) {
        preview_border_expired.setPadding(borderThickness,
                borderThickness, borderThickness, 0);
    }
    
    public void updatePreviewFonts() {
        
        int fontColor = preferences.getInt(Keys.FONT_COLOR, Keys.SETTINGS_DEFAULT_FONT_COLOR);
        int fontColorUpcoming = preferences.getInt(Keys.UPCOMING_FONT_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_FONT_COLOR);
        int fontColorExpired = preferences.getInt(Keys.EXPIRED_FONT_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_FONT_COLOR);
        
        this.fontColor_upcoming_select.setCardBackgroundColor(fontColorUpcoming);
        fontColor_current_select.setCardBackgroundColor(fontColor);
        this.fontColor_expired_select.setCardBackgroundColor(fontColorExpired);
        
        preview_text_upcoming.setTextColor(mixTwoColors(fontColor, fontColorUpcoming, DEFAULT_COLOR_MIX_FACTOR));
        preview_text.setTextColor(fontColor);
        preview_text_expired.setTextColor(mixTwoColors(fontColor, fontColorExpired, DEFAULT_COLOR_MIX_FACTOR));
    }
    
    public void updatePreviewBgs() {
        
        int bgColor = preferences.getInt(Keys.BG_COLOR, Keys.SETTINGS_DEFAULT_BG_COLOR);
        int bgColorUpcoming = preferences.getInt(Keys.UPCOMING_BG_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BG_COLOR);
        int bgColorExpired = preferences.getInt(Keys.EXPIRED_BG_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BG_COLOR);
        
        this.bgColor_upcoming_select.setCardBackgroundColor(bgColorUpcoming);
        bgColor_current_select.setCardBackgroundColor(bgColor);
        this.bgColor_expired_select.setCardBackgroundColor(bgColorExpired);
        
        preview_text_upcoming.setBackgroundColor(mixTwoColors(bgColor, bgColorUpcoming, DEFAULT_COLOR_MIX_FACTOR));
        preview_text.setBackgroundColor(bgColor);
        preview_text_expired.setBackgroundColor(mixTwoColors(bgColor, bgColorExpired, DEFAULT_COLOR_MIX_FACTOR));
    }
    
    public void updatePreviewBorders() {
        
        int borderColor = preferences.getInt(Keys.BORDER_COLOR, Keys.SETTINGS_DEFAULT_BORDER_COLOR);
        int borderColorUpcoming = preferences.getInt(Keys.UPCOMING_BORDER_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BORDER_COLOR);
        int borderColorExpired = preferences.getInt(Keys.EXPIRED_BORDER_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BORDER_COLOR);
        
        this.borderColor_upcoming_select.setCardBackgroundColor(borderColorUpcoming);
        borderColor_current_select.setCardBackgroundColor(borderColor);
        this.borderColor_expired_select.setCardBackgroundColor(borderColorExpired);
        
        preview_border_upcoming.setBackgroundColor(mixTwoColors(borderColor, borderColorUpcoming, DEFAULT_COLOR_MIX_FACTOR));
        preview_border.setBackgroundColor(borderColor);
        preview_border_expired.setBackgroundColor(mixTwoColors(borderColor, borderColorExpired, DEFAULT_COLOR_MIX_FACTOR));
    }
}
