package prototype.xd.scheduler.views.settings;

import static prototype.xd.scheduler.MainActivity.preferences;
import static prototype.xd.scheduler.utilities.BitmapUtilities.mixTwoColors;
import static prototype.xd.scheduler.utilities.Keys.DEFAULT_COLOR_MIX_FACTOR;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.views.Spinner;
import prototype.xd.scheduler.views.Switch;

public class PopupSettingsView {
    
    protected final CardView fontColor_select;
    protected final CardView bgColor_select;
    protected final CardView borderColor_select;
    
    protected final View add_group;
    protected final Spinner group_spinner;
    
    protected final TextView fontColor_view_state;
    protected final TextView bgColor_view_state;
    protected final TextView padColor_view_state;
    protected final TextView adaptiveColor_switch_state;
    protected final TextView priority_state;
    protected final TextView border_size_state;
    protected final TextView show_on_lock_state;
    protected final TextView adaptiveColor_bar_state;
    protected final TextView showDaysUpcoming_bar_state;
    protected final TextView showDaysExpired_bar_state;
    
    private final TextView preview_text_upcoming;
    private final LinearLayout preview_border_upcoming;
    
    protected final TextView preview_text;
    public final LinearLayout preview_border;
    
    private final TextView preview_text_expired;
    private final LinearLayout preview_border_expired;
    
    protected final TextView border_thickness_description;
    protected final SeekBar border_thickness_bar;
    protected final TextView priority_description;
    protected final SeekBar priority_bar;
    protected final TextView adaptive_color_balance_description;
    protected final SeekBar adaptive_color_balance_bar;
    protected final TextView show_days_beforehand_description;
    protected final SeekBar show_days_beforehand_bar;
    protected final TextView show_days_after_description;
    protected final SeekBar show_days_after_bar;
    protected final Switch show_on_lock_switch;
    protected final Switch adaptive_color_switch;
    
    protected final View settings_reset_button;
    
    PopupSettingsView(View settingsView) {
        
        fontColor_select = settingsView.findViewById(R.id.fontColor);
        bgColor_select = settingsView.findViewById(R.id.backgroundColor);
        borderColor_select = settingsView.findViewById(R.id.borderColor);
        add_group = settingsView.findViewById(R.id.addGroup);
        group_spinner = settingsView.findViewById(R.id.groupSpinner);
        
        fontColor_view_state = settingsView.findViewById(R.id.font_color_state);
        bgColor_view_state = settingsView.findViewById(R.id.background_color_state);
        padColor_view_state = settingsView.findViewById(R.id.border_color_state);
        priority_state = settingsView.findViewById(R.id.priority_state);
        border_size_state = settingsView.findViewById(R.id.bevel_size_state);
        show_on_lock_state = settingsView.findViewById(R.id.show_on_lock_state);
        adaptiveColor_switch_state = settingsView.findViewById(R.id.adaptive_color_state);
        adaptiveColor_bar_state = settingsView.findViewById(R.id.adaptive_color_balance_state);
        showDaysUpcoming_bar_state = settingsView.findViewById(R.id.days_beforehand_state);
        showDaysExpired_bar_state = settingsView.findViewById(R.id.days_after_state);
        
        preview_text_upcoming = settingsView.findViewById(R.id.preview_text_upcoming);
        preview_border_upcoming = settingsView.findViewById(R.id.preview_border_upcoming);
        
        preview_text = settingsView.findViewById(R.id.preview_text);
        preview_border = settingsView.findViewById(R.id.preview_border);
        
        preview_text_expired = settingsView.findViewById(R.id.preview_text_expired);
        preview_border_expired = settingsView.findViewById(R.id.preview_border_expired);
        
        border_thickness_description = settingsView.findViewById(R.id.bevel_thickness_description);
        border_thickness_bar = settingsView.findViewById(R.id.bevel_thickness_bar);
        priority_description = settingsView.findViewById(R.id.priority_description);
        priority_bar = settingsView.findViewById(R.id.priority_bar);
        adaptive_color_balance_description = settingsView.findViewById(R.id.adaptive_color_balance_description);
        adaptive_color_balance_bar = settingsView.findViewById(R.id.adaptive_color_balance_bar);
        show_days_beforehand_description = settingsView.findViewById(R.id.show_days_beforehand_description);
        show_days_beforehand_bar = settingsView.findViewById(R.id.show_days_beforehand_bar);
        show_days_after_description = settingsView.findViewById(R.id.show_days_after_description);
        show_days_after_bar = settingsView.findViewById(R.id.show_days_after_bar);
        show_on_lock_switch = settingsView.findViewById(R.id.show_on_lock_switch);
        adaptive_color_switch = settingsView.findViewById(R.id.adaptive_color_switch);
        
        settings_reset_button = settingsView.findViewById(R.id.settings_reset_button);
    }
    
    protected void setStateIconColor(TextView icon, String parameter) {}
    
    void updateAllIndicators() {
        setStateIconColor(fontColor_view_state, Keys.FONT_COLOR);
        setStateIconColor(bgColor_view_state, Keys.BG_COLOR);
        setStateIconColor(padColor_view_state, Keys.BORDER_COLOR);
        setStateIconColor(border_size_state, Keys.BORDER_THICKNESS);
        setStateIconColor(priority_state, Keys.PRIORITY);
        setStateIconColor(show_on_lock_state, Keys.SHOW_ON_LOCK);
        setStateIconColor(adaptiveColor_switch_state, Keys.ADAPTIVE_COLOR_ENABLED);
        setStateIconColor(adaptiveColor_bar_state, Keys.ADAPTIVE_COLOR_BALANCE);
        setStateIconColor(showDaysUpcoming_bar_state, Keys.UPCOMING_ITEMS_OFFSET);
        setStateIconColor(showDaysExpired_bar_state, Keys.EXPIRED_ITEMS_OFFSET);
    }
    
    protected void updatePreviews(int fontColor, int bgColor, int borderColor, int borderThickness) {
        updatePreviewFont(fontColor);
        updatePreviewBg(bgColor);
        updatePreviewBorder(borderColor);
        preview_border.setPadding(borderThickness,
                borderThickness, borderThickness, 0);
        
        int upcoming_border_thickness = preferences.getInt(Keys.UPCOMING_BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_UPCOMING_BORDER_THICKNESS);
        int expired_border_thickness = preferences.getInt(Keys.EXPIRED_BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_EXPIRED_BORDER_THICKNESS);
        
        preview_border_upcoming.setPadding(upcoming_border_thickness,
                upcoming_border_thickness, upcoming_border_thickness, 0);
        preview_border_expired.setPadding(expired_border_thickness,
                expired_border_thickness, expired_border_thickness, 0);
    }
    
    public void updatePreviewFont(int fontColor) {
        fontColor_select.setCardBackgroundColor(fontColor);
        preview_text.setTextColor(fontColor);
        preview_text_upcoming.setTextColor(mixTwoColors(fontColor,
                preferences.getInt(Keys.UPCOMING_FONT_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_FONT_COLOR), DEFAULT_COLOR_MIX_FACTOR));
        preview_text_expired.setTextColor(mixTwoColors(fontColor,
                preferences.getInt(Keys.EXPIRED_FONT_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_FONT_COLOR), DEFAULT_COLOR_MIX_FACTOR));
    }
    
    public void updatePreviewBg(int bgColor) {
        bgColor_select.setCardBackgroundColor(bgColor);
        preview_text.setBackgroundColor(bgColor);
        preview_text_upcoming.setBackgroundColor(mixTwoColors(bgColor,
                preferences.getInt(Keys.UPCOMING_BG_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BG_COLOR), DEFAULT_COLOR_MIX_FACTOR));
        preview_text_expired.setBackgroundColor(mixTwoColors(bgColor,
                preferences.getInt(Keys.EXPIRED_BG_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BG_COLOR), DEFAULT_COLOR_MIX_FACTOR));
    }
    
    public void updatePreviewBorder(int borderColor) {
        borderColor_select.setCardBackgroundColor(borderColor);
        preview_border.setBackgroundColor(borderColor);
        preview_border_upcoming.setBackgroundColor(mixTwoColors(borderColor,
                preferences.getInt(Keys.UPCOMING_BORDER_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BORDER_COLOR), DEFAULT_COLOR_MIX_FACTOR));
        preview_border_expired.setBackgroundColor(mixTwoColors(borderColor,
                preferences.getInt(Keys.EXPIRED_BORDER_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BORDER_COLOR), DEFAULT_COLOR_MIX_FACTOR));
    }
    
}
