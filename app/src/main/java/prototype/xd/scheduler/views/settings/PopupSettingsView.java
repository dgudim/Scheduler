package prototype.xd.scheduler.views.settings;

import android.app.AlertDialog;
import android.widget.TextView;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.EntrySettingsBinding;
import prototype.xd.scheduler.utilities.ContextWrapper;
import prototype.xd.scheduler.utilities.GraphicsUtilities;
import prototype.xd.scheduler.utilities.Static;
import prototype.xd.scheduler.utilities.TodoEntryManager;

@MainThread
public abstract class PopupSettingsView {
    
    @NonNull
    protected final EntrySettingsBinding bnd;
    
    protected final EntryPreviewContainer entryPreviewContainer;
    
    @NonNull
    protected final ContextWrapper wrapper;
    @NonNull
    protected final AlertDialog dialog;
    protected final int defaultTextColor;
    
    PopupSettingsView(@NonNull final ContextWrapper wrapper,
                      @Nullable final TodoEntryManager todoEntryManager) {
        
        this.wrapper = wrapper;
        
        bnd = EntrySettingsBinding.inflate(wrapper.getLayoutInflater());
        defaultTextColor = bnd.hideExpiredItemsByTimeSwitch.getCurrentTextColor();
        
        bnd.showDaysUpcomingSlider.setValueTo(Static.SETTINGS_MAX_EXPIRED_UPCOMING_ITEMS_OFFSET);
        bnd.showDaysExpiredSlider.setValueTo(Static.SETTINGS_MAX_EXPIRED_UPCOMING_ITEMS_OFFSET);
        
        new GraphicsUtilities.SliderTinter(wrapper.context, Static.BG_COLOR.UPCOMING.defaultValue).tintSlider(bnd.showDaysUpcomingSlider);
        new GraphicsUtilities.SliderTinter(wrapper.context, Static.BG_COLOR.EXPIRED.defaultValue).tintSlider(bnd.showDaysExpiredSlider);
        
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
    
    @NonNull
    public abstract EntryPreviewContainer getEntryPreviewContainer();
    
    /**
     * public method that should be called when some parameter changes, for example from a switch listener
     * should probably call {@link #setStateIconColor}
     */
    public abstract <T> void notifyParameterChanged(@NonNull TextView displayTo, @NonNull String parameterKey, @NonNull T value);
    
    /**
     * public method that should be called when some color changes (font, bg, border), for example from a switch listener
     */
    public void notifyColorChanged(@NonNull Static.DefaultedInteger value, int newColor) {
        entryPreviewContainer.notifyColorChanged(value, newColor);
    }
    
    /**
     * internal method for changing state icon color
     *
     * @param icon         TextView to colorize
     * @param parameterKey key of the changed parameter
     */
    protected abstract void setStateIconColor(@NonNull TextView icon, @NonNull String parameterKey);
    
    protected void updateAllIndicators() {
        setStateIconColor(bnd.fontColorState, Static.FONT_COLOR.CURRENT.key);
        setStateIconColor(bnd.backgroundColorState, Static.BG_COLOR.CURRENT.key);
        setStateIconColor(bnd.borderColorState, Static.BORDER_COLOR.CURRENT.key);
        setStateIconColor(bnd.borderThicknessState, Static.BORDER_THICKNESS.CURRENT.key);
        setStateIconColor(bnd.priorityState, Static.PRIORITY.key);
        setStateIconColor(bnd.showOnLockState, Static.CALENDAR_SHOW_ON_LOCK.key);
        setStateIconColor(bnd.adaptiveColorBalanceState, Static.ADAPTIVE_COLOR_BALANCE.key);
        setStateIconColor(bnd.showDaysUpcomingState, Static.UPCOMING_ITEMS_OFFSET.key);
        setStateIconColor(bnd.showDaysExpiredState, Static.EXPIRED_ITEMS_OFFSET.key);
        setStateIconColor(bnd.hideExpiredItemsByTimeState, Static.HIDE_EXPIRED_ENTRIES_BY_TIME.key);
        setStateIconColor(bnd.hideByContentSwitchState, Static.HIDE_ENTRIES_BY_CONTENT.key);
        setStateIconColor(bnd.hideByContentFieldState, Static.HIDE_ENTRIES_BY_CONTENT_CONTENT.key);
    }
}
