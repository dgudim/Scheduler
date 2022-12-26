package prototype.xd.scheduler.views.settings;

import static prototype.xd.scheduler.utilities.BitmapUtilities.mixTwoColors;
import static prototype.xd.scheduler.utilities.Keys.DEFAULT_TIME_OFFSET_COLOR_MIX_FACTOR;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.EntrySettingsBinding;
import prototype.xd.scheduler.utilities.DialogDismissLifecycleObserver;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.TodoListEntryManager;

public abstract class PopupSettingsView {
    
    protected final EntrySettingsBinding bnd;
    protected final Context context;
    protected AlertDialog dialog;
    protected final Lifecycle lifecycle;
    protected final int defaultTextColor;
    
    PopupSettingsView(@NonNull final Context context,
                      @Nullable final TodoListEntryManager todoListEntryManager,
                      @NonNull final Lifecycle lifecycle) {
        
        bnd = EntrySettingsBinding.inflate(LayoutInflater.from(context));
        defaultTextColor = bnd.hideExpiredItemsByTimeSwitch.getCurrentTextColor();
        
        bnd.showDaysUpcomingBar.setValueTo(Keys.SETTINGS_MAX_EXPIRED_UPCOMING_ITEMS_OFFSET);
        bnd.showDaysExpiredBar.setValueTo(Keys.SETTINGS_MAX_EXPIRED_UPCOMING_ITEMS_OFFSET);
        
        this.context = context;
        this.lifecycle = lifecycle;
        
        dialog = new AlertDialog.Builder(context, R.style.FullScreenDialog)
                .setOnDismissListener(dialog -> {
                    if (todoListEntryManager != null) {
                        todoListEntryManager.performDeferredTasks();
                    }
                }).setView(bnd.getRoot()).create();
        lifecycle.addObserver(new DialogDismissLifecycleObserver(dialog));
    }
    
    /**
     * public method that should be called when some parameter changes, for example from a switch listener
     * should probably call {@link #setStateIconColor}
     */
    public abstract <T> void notifyParameterChanged(TextView displayTo, String parameterKey, T value);
    
    /**
     * public method that should be called when some color changes (font, bg, border), for example from a switch listener
     */
    public void notifyColorChanged(Keys.DefaultedInteger value, int newColor) {
        if (value.equals(Keys.FONT_COLOR)) {
            updatePreviewFont(newColor);
            return;
        }
        if (value.equals(Keys.BORDER_COLOR)) {
            updatePreviewBorder(newColor);
            return;
        }
        if (value.equals(Keys.BG_COLOR)) {
            updatePreviewBg(newColor);
        }
    }
    
    /**
     * internal method for changing state icon color
     *
     * @param icon         TextView to colorize
     * @param parameterKey key of the changed parameter
     */
    protected abstract void setStateIconColor(TextView icon, String parameterKey);
    
    protected void updateAllIndicators() {
        setStateIconColor(bnd.fontColorState, Keys.FONT_COLOR.key);
        setStateIconColor(bnd.backgroundColorState, Keys.BG_COLOR.key);
        setStateIconColor(bnd.borderColorState, Keys.BORDER_COLOR.key);
        setStateIconColor(bnd.borderThicknessState, Keys.BORDER_THICKNESS.key);
        setStateIconColor(bnd.priorityState, Keys.PRIORITY.key);
        setStateIconColor(bnd.showOnLockState, Keys.CALENDAR_SHOW_ON_LOCK.key);
        setStateIconColor(bnd.adaptiveColorBalanceState, Keys.ADAPTIVE_COLOR_BALANCE.key);
        setStateIconColor(bnd.showDaysUpcomingState, Keys.UPCOMING_ITEMS_OFFSET.key);
        setStateIconColor(bnd.showDaysExpiredState, Keys.EXPIRED_ITEMS_OFFSET.key);
        setStateIconColor(bnd.hideExpiredItemsByTimeState, Keys.HIDE_EXPIRED_ENTRIES_BY_TIME.key);
        setStateIconColor(bnd.hideByContentSwitchState, Keys.HIDE_ENTRIES_BY_CONTENT.key);
        setStateIconColor(bnd.hideByContentFieldState, Keys.HIDE_ENTRIES_BY_CONTENT_CONTENT.key);
    }
    
    protected void updatePreviews(int fontColor, int bgColor, int borderColor, int borderThickness) {
        updatePreviewFont(fontColor);
        updatePreviewBg(bgColor);
        updatePreviewBorder(borderColor);
        bnd.previewBorder.setPadding(borderThickness,
                borderThickness, borderThickness, 0);
        
        int upcomingBorderThickness = Keys.UPCOMING_BORDER_THICKNESS.get();
        int expiredBorderThickness = Keys.EXPIRED_BORDER_THICKNESS.get();
        
        bnd.previewBorderUpcoming.setPadding(upcomingBorderThickness,
                upcomingBorderThickness, upcomingBorderThickness, 0);
        bnd.previewBorderExpired.setPadding(expiredBorderThickness,
                expiredBorderThickness, expiredBorderThickness, 0);
    }
    
    public void updatePreviewFont(int fontColor) {
        bnd.fontColorSelector.setCardBackgroundColor(fontColor);
        bnd.previewText.setTextColor(fontColor);
        bnd.previewTextUpcoming.setTextColor(mixTwoColors(fontColor, Keys.UPCOMING_FONT_COLOR.get(), DEFAULT_TIME_OFFSET_COLOR_MIX_FACTOR));
        bnd.previewTextExpired.setTextColor(mixTwoColors(fontColor, Keys.EXPIRED_FONT_COLOR.get(), DEFAULT_TIME_OFFSET_COLOR_MIX_FACTOR));
    }
    
    public void updatePreviewBg(int bgColor) {
        bnd.backgroundColorSelector.setCardBackgroundColor(bgColor);
        bnd.previewText.setBackgroundColor(bgColor);
        bnd.previewTextUpcoming.setBackgroundColor(mixTwoColors(bgColor, Keys.UPCOMING_BG_COLOR.get(), DEFAULT_TIME_OFFSET_COLOR_MIX_FACTOR));
        bnd.previewTextExpired.setBackgroundColor(mixTwoColors(bgColor, Keys.EXPIRED_BG_COLOR.get(), DEFAULT_TIME_OFFSET_COLOR_MIX_FACTOR));
    }
    
    public void updatePreviewBorder(int borderColor) {
        bnd.borderColorSelector.setCardBackgroundColor(borderColor);
        bnd.previewBorder.setBackgroundColor(borderColor);
        bnd.previewBorderUpcoming.setBackgroundColor(mixTwoColors(borderColor, Keys.UPCOMING_BORDER_COLOR.get(), DEFAULT_TIME_OFFSET_COLOR_MIX_FACTOR));
        bnd.previewBorderExpired.setBackgroundColor(mixTwoColors(borderColor, Keys.EXPIRED_BORDER_COLOR.get(), DEFAULT_TIME_OFFSET_COLOR_MIX_FACTOR));
    }
    
}
