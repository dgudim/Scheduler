package prototype.xd.scheduler.views.settings;

import android.app.AlertDialog;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.EntrySettingsBinding;
import prototype.xd.scheduler.utilities.ContextWrapper;
import prototype.xd.scheduler.utilities.GraphicsUtilities;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.TodoEntryManager;

public abstract class PopupSettingsView {
    
    protected final EntrySettingsBinding bnd;
    
    protected final EntryPreviewContainer entryPreviewContainer;
    
    protected final ContextWrapper wrapper;
    protected final AlertDialog dialog;
    protected final int defaultTextColor;
    
    PopupSettingsView(@NonNull final ContextWrapper wrapper,
                      @Nullable final TodoEntryManager todoEntryManager) {
        
        this.wrapper = wrapper;
        
        bnd = EntrySettingsBinding.inflate(wrapper.getLayoutInflater());
        defaultTextColor = bnd.hideExpiredItemsByTimeSwitch.getCurrentTextColor();
        
        bnd.showDaysUpcomingSlider.setValueTo(Keys.SETTINGS_MAX_EXPIRED_UPCOMING_ITEMS_OFFSET);
        bnd.showDaysExpiredSlider.setValueTo(Keys.SETTINGS_MAX_EXPIRED_UPCOMING_ITEMS_OFFSET);
        
        new GraphicsUtilities.SliderTinter(wrapper.context, Keys.BG_COLOR.UPCOMING.defaultValue).tintSlider(bnd.showDaysUpcomingSlider);
        new GraphicsUtilities.SliderTinter(wrapper.context, Keys.BG_COLOR.EXPIRED.defaultValue).tintSlider(bnd.showDaysExpiredSlider);
        
        entryPreviewContainer = getEntryPreviewContainer();
        entryPreviewContainer.attachCurrentSelectors(
                bnd.currentFontColorSelector,
                bnd.currentBorderColorSelector,
                bnd.currentBackgroundColorSelector);
        
        dialog = wrapper.attachDialogToLifecycle(
                new AlertDialog.Builder(wrapper.context, R.style.FullScreenDialog).setView(bnd.getRoot()).create(),
                dialogInterface -> {
                    if (todoEntryManager != null) {
                        todoEntryManager.performDeferredTasks();
                    }
                });
    }
    
    public abstract EntryPreviewContainer getEntryPreviewContainer();
    
    /**
     * public method that should be called when some parameter changes, for example from a switch listener
     * should probably call {@link #setStateIconColor}
     */
    public abstract <T> void notifyParameterChanged(TextView displayTo, String parameterKey, T value);
    
    /**
     * public method that should be called when some color changes (font, bg, border), for example from a switch listener
     */
    public void notifyColorChanged(Keys.DefaultedInteger value, int newColor) {
        entryPreviewContainer.notifyColorChanged(value, newColor);
    }
    
    /**
     * internal method for changing state icon color
     *
     * @param icon         TextView to colorize
     * @param parameterKey key of the changed parameter
     */
    protected abstract void setStateIconColor(TextView icon, String parameterKey);
    
    protected void updateAllIndicators() {
        setStateIconColor(bnd.fontColorState, Keys.FONT_COLOR.CURRENT.key);
        setStateIconColor(bnd.backgroundColorState, Keys.BG_COLOR.CURRENT.key);
        setStateIconColor(bnd.borderColorState, Keys.BORDER_COLOR.CURRENT.key);
        setStateIconColor(bnd.borderThicknessState, Keys.BORDER_THICKNESS.CURRENT.key);
        setStateIconColor(bnd.priorityState, Keys.PRIORITY.key);
        setStateIconColor(bnd.showOnLockState, Keys.CALENDAR_SHOW_ON_LOCK.key);
        setStateIconColor(bnd.adaptiveColorBalanceState, Keys.ADAPTIVE_COLOR_BALANCE.key);
        setStateIconColor(bnd.showDaysUpcomingState, Keys.UPCOMING_ITEMS_OFFSET.key);
        setStateIconColor(bnd.showDaysExpiredState, Keys.EXPIRED_ITEMS_OFFSET.key);
        setStateIconColor(bnd.hideExpiredItemsByTimeState, Keys.HIDE_EXPIRED_ENTRIES_BY_TIME.key);
        setStateIconColor(bnd.hideByContentSwitchState, Keys.HIDE_ENTRIES_BY_CONTENT.key);
        setStateIconColor(bnd.hideByContentFieldState, Keys.HIDE_ENTRIES_BY_CONTENT_CONTENT.key);
    }
}
