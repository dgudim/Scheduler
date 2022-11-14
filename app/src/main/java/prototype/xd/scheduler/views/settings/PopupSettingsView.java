package prototype.xd.scheduler.views.settings;

import static prototype.xd.scheduler.utilities.BitmapUtilities.mixTwoColors;
import static prototype.xd.scheduler.utilities.Keys.DEFAULT_COLOR_MIX_FACTOR;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.TextView;

import prototype.xd.scheduler.databinding.EntrySettingsBinding;
import prototype.xd.scheduler.utilities.Keys;

public abstract class PopupSettingsView {
    
    protected final EntrySettingsBinding bnd;
    protected AlertDialog dialog;
    protected final int defaultTextColor;
    
    PopupSettingsView(Context context) {
        bnd = EntrySettingsBinding.inflate(LayoutInflater.from(context));
        defaultTextColor = bnd.hideExpiredItemsByTimeSwitch.getCurrentTextColor();
    }
    
    protected abstract void setStateIconColor(TextView icon, String parameter);
    
    void updateAllIndicators() {
        setStateIconColor(bnd.fontColorState, Keys.FONT_COLOR);
        setStateIconColor(bnd.backgroundColorState, Keys.BG_COLOR);
        setStateIconColor(bnd.borderColorState, Keys.BORDER_COLOR);
        setStateIconColor(bnd.borderThicknessState, Keys.BORDER_THICKNESS);
        setStateIconColor(bnd.priorityState, Keys.PRIORITY);
        setStateIconColor(bnd.showOnLockState, Keys.SHOW_ON_LOCK);
        setStateIconColor(bnd.adaptiveColorBalanceState, Keys.ADAPTIVE_COLOR_BALANCE);
        setStateIconColor(bnd.showDaysUpcomingState, Keys.UPCOMING_ITEMS_OFFSET);
        setStateIconColor(bnd.showDaysExpiredState, Keys.EXPIRED_ITEMS_OFFSET);
        setStateIconColor(bnd.hideExpiredItemsByTimeState, Keys.HIDE_EXPIRED_ENTRIES_BY_TIME);
        setStateIconColor(bnd.hideByContentSwitchState, Keys.HIDE_ENTRIES_BY_CONTENT);
        setStateIconColor(bnd.hideByContentFieldState, Keys.HIDE_ENTRIES_BY_CONTENT_CONTENT);
    }
    
    protected void updatePreviews(int fontColor, int bgColor, int borderColor, int borderThickness) {
        updatePreviewFont(fontColor);
        updatePreviewBg(bgColor);
        updatePreviewBorder(borderColor);
        bnd.previewBorder.setPadding(borderThickness,
                borderThickness, borderThickness, 0);
        
        int upcomingBorderThickness = preferences.getInt(Keys.UPCOMING_BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_UPCOMING_BORDER_THICKNESS);
        int expiredBorderThickness = preferences.getInt(Keys.EXPIRED_BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_EXPIRED_BORDER_THICKNESS);
        
        bnd.previewBorderUpcoming.setPadding(upcomingBorderThickness,
                upcomingBorderThickness, upcomingBorderThickness, 0);
        bnd.previewBorderExpired.setPadding(expiredBorderThickness,
                expiredBorderThickness, expiredBorderThickness, 0);
    }
    
    public void updatePreviewFont(int fontColor) {
        bnd.fontColorSelector.setCardBackgroundColor(fontColor);
        bnd.previewText.setTextColor(fontColor);
        bnd.previewTextUpcoming.setTextColor(mixTwoColors(fontColor,
                preferences.getInt(Keys.UPCOMING_FONT_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_FONT_COLOR), DEFAULT_COLOR_MIX_FACTOR));
        bnd.previewTextExpired.setTextColor(mixTwoColors(fontColor,
                preferences.getInt(Keys.EXPIRED_FONT_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_FONT_COLOR), DEFAULT_COLOR_MIX_FACTOR));
    }
    
    public void updatePreviewBg(int bgColor) {
        bnd.backgroundColorSelector.setCardBackgroundColor(bgColor);
        bnd.previewText.setBackgroundColor(bgColor);
        bnd.previewTextUpcoming.setBackgroundColor(mixTwoColors(bgColor,
                preferences.getInt(Keys.UPCOMING_BG_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BG_COLOR), DEFAULT_COLOR_MIX_FACTOR));
        bnd.previewTextExpired.setBackgroundColor(mixTwoColors(bgColor,
                preferences.getInt(Keys.EXPIRED_BG_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BG_COLOR), DEFAULT_COLOR_MIX_FACTOR));
    }
    
    public void updatePreviewBorder(int borderColor) {
        bnd.borderColorSelector.setCardBackgroundColor(borderColor);
        bnd.previewBorder.setBackgroundColor(borderColor);
        bnd.previewBorderUpcoming.setBackgroundColor(mixTwoColors(borderColor,
                preferences.getInt(Keys.UPCOMING_BORDER_COLOR, Keys.SETTINGS_DEFAULT_UPCOMING_BORDER_COLOR), DEFAULT_COLOR_MIX_FACTOR));
        bnd.previewBorderExpired.setBackgroundColor(mixTwoColors(borderColor,
                preferences.getInt(Keys.EXPIRED_BORDER_COLOR, Keys.SETTINGS_DEFAULT_EXPIRED_BORDER_COLOR), DEFAULT_COLOR_MIX_FACTOR));
    }
    
}
