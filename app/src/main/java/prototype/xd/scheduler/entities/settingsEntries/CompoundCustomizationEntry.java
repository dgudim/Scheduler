package prototype.xd.scheduler.entities.settingsEntries;

import static prototype.xd.scheduler.entities.settingsEntries.SettingsEntryType.COMPOUND_CUSTOMIZATION;
import static prototype.xd.scheduler.utilities.BitmapUtilities.mixTwoColors;
import static prototype.xd.scheduler.utilities.Keys.DEFAULT_COLOR_MIX_FACTOR;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;
import static prototype.xd.scheduler.utilities.Utilities.addSeekBarChangeListener;
import static prototype.xd.scheduler.utilities.Utilities.invokeColorDialogue;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.utilities.Keys;

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
    public View get(View convertView, ViewGroup viewGroup) {
        if (convertView == null) {
            convertView = LayoutInflater.from(viewGroup.getContext()).inflate(layoutId, viewGroup, false);
            
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
            
            addSeekBarChangeListener(
                    convertView.findViewById(R.id.upcoming_border_thickness_text),
                    convertView.findViewById(R.id.upcoming_border_thickness_seek_bar),
                    this, R.string.settings_upcoming_border_thickness,
                    Keys.UPCOMING_BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_UPCOMING_BORDER_THICKNESS);
    
            addSeekBarChangeListener(
                    convertView.findViewById(R.id.current_border_thickness_text),
                    convertView.findViewById(R.id.current_border_thickness_seek_bar),
                    this, R.string.settings_current_border_thickness,
                    Keys.BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_BORDER_THICKNESS);
    
            addSeekBarChangeListener(
                    convertView.findViewById(R.id.expired_border_thickness_text),
                    convertView.findViewById(R.id.expired_border_thickness_seek_bar),
                    this, R.string.settings_expired_border_thickness,
                    Keys.EXPIRED_BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_EXPIRED_BORDER_THICKNESS);
            
        }
        return InitInnerViews(convertView, viewGroup);
    }
    
    protected void updatePreviews() {
        updatePreviewFonts();
        updatePreviewBgs();
        updatePreviewBorders();
        updateUpcomingPreviewBorderThickness(preferences.getInt(Keys.UPCOMING_BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_UPCOMING_BORDER_THICKNESS));
        updateCurrentPreviewBorderThickness(preferences.getInt(Keys.BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_BORDER_THICKNESS));
        updateExpiredPreviewBorderThickness(preferences.getInt(Keys.EXPIRED_BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_EXPIRED_BORDER_THICKNESS));
    }
    
    public void updateUpcomingPreviewBorderThickness(int border_thickness) {
        preview_border_upcoming.setPadding(border_thickness,
                border_thickness, border_thickness, 0);
    }
    
    public void updateCurrentPreviewBorderThickness(int border_thickness) {
        preview_border.setPadding(border_thickness,
                border_thickness, border_thickness, 0);
    }
    
    public void updateExpiredPreviewBorderThickness(int border_thickness) {
        preview_border_expired.setPadding(border_thickness,
                border_thickness, border_thickness, 0);
    }
    
    public void updatePreviewFonts() {
        
        int fontColor = preferences.getInt(Keys.FONT_COLOR, Keys.SETTINGS_DEFAULT_FONT_COLOR);
        int fontColor_upcoming = preferences.getInt(Keys.UPCOMING_FONT_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_FONT_COLOR);
        int fontColor_expired = preferences.getInt(Keys.EXPIRED_FONT_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_FONT_COLOR);
        
        this.fontColor_upcoming_select.setCardBackgroundColor(fontColor_upcoming);
        fontColor_current_select.setCardBackgroundColor(fontColor);
        this.fontColor_expired_select.setCardBackgroundColor(fontColor_expired);
        
        preview_text_upcoming.setTextColor(mixTwoColors(fontColor, fontColor_upcoming, DEFAULT_COLOR_MIX_FACTOR));
        preview_text.setTextColor(fontColor);
        preview_text_expired.setTextColor(mixTwoColors(fontColor, fontColor_expired, DEFAULT_COLOR_MIX_FACTOR));
    }
    
    public void updatePreviewBgs() {
        
        int bgColor = preferences.getInt(Keys.BG_COLOR, Keys.SETTINGS_DEFAULT_BG_COLOR);
        int bgColor_upcoming = preferences.getInt(Keys.UPCOMING_BG_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BG_COLOR);
        int bgColor_expired = preferences.getInt(Keys.EXPIRED_BG_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BG_COLOR);
        
        this.bgColor_upcoming_select.setCardBackgroundColor(bgColor_upcoming);
        bgColor_current_select.setCardBackgroundColor(bgColor);
        this.bgColor_expired_select.setCardBackgroundColor(bgColor_expired);
        
        preview_text_upcoming.setBackgroundColor(mixTwoColors(bgColor, bgColor_upcoming, DEFAULT_COLOR_MIX_FACTOR));
        preview_text.setBackgroundColor(bgColor);
        preview_text_expired.setBackgroundColor(mixTwoColors(bgColor, bgColor_expired, DEFAULT_COLOR_MIX_FACTOR));
    }
    
    public void updatePreviewBorders() {
        
        int borderColor = preferences.getInt(Keys.BORDER_COLOR, Keys.SETTINGS_DEFAULT_BORDER_COLOR);
        int borderColor_upcoming = preferences.getInt(Keys.UPCOMING_BORDER_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BORDER_COLOR);
        int borderColor_expired = preferences.getInt(Keys.EXPIRED_BORDER_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BORDER_COLOR);
        
        this.borderColor_upcoming_select.setCardBackgroundColor(borderColor_upcoming);
        borderColor_current_select.setCardBackgroundColor(borderColor);
        this.borderColor_expired_select.setCardBackgroundColor(borderColor_expired);
        
        preview_border_upcoming.setBackgroundColor(mixTwoColors(borderColor, borderColor_upcoming, DEFAULT_COLOR_MIX_FACTOR));
        preview_border.setBackgroundColor(borderColor);
        preview_border_expired.setBackgroundColor(mixTwoColors(borderColor, borderColor_expired, DEFAULT_COLOR_MIX_FACTOR));
    }
}
