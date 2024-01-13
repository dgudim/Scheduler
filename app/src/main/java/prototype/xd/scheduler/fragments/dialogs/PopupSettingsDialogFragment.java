package prototype.xd.scheduler.fragments.dialogs;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.ComponentDialog;
import androidx.annotation.CallSuper;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.EntrySettingsBinding;
import prototype.xd.scheduler.entities.settings_entries.EntryPreviewContainer;
import prototype.xd.scheduler.utilities.ImageUtilities;
import prototype.xd.scheduler.utilities.Static;
import prototype.xd.scheduler.utilities.TodoEntryManager;

// TODO: use ViewModel and savedInstanceState to transfer data instead of using the constructor for proper config updates
@MainThread
public abstract class PopupSettingsDialogFragment extends BaseCachedDialogFragment<EntrySettingsBinding, ComponentDialog> {
    
    @SuppressLint("UnknownNullness")
    protected EntryPreviewContainer entryPreviewContainer;
    
    protected int defaultTextColor;
    @Nullable
    private final TodoEntryManager todoEntryManager;
    
    @NonNull
    @Override
    protected EntrySettingsBinding inflate(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return EntrySettingsBinding.inflate(inflater, container, false);
    }
    
    // fragment creation begin
    @Override
    @MainThread
    @CallSuper
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialog);
    }
    
    PopupSettingsDialogFragment(@Nullable final TodoEntryManager todoEntryManager) {
        this.todoEntryManager = todoEntryManager;
    }
    
    @Override
    protected void buildDialogStatic(@NonNull EntrySettingsBinding bnd, @NonNull ComponentDialog dialog) {
        defaultTextColor = bnd.hideExpiredItemsByTimeSwitch.getCurrentTextColor();
        entryPreviewContainer = getEntryPreviewContainer(bnd);
        
        bnd.showDaysUpcomingSlider.setValueTo(Static.SETTINGS_MAX_EXPIRED_UPCOMING_ITEMS_OFFSET);
        bnd.showDaysExpiredSlider.setValueTo(Static.SETTINGS_MAX_EXPIRED_UPCOMING_ITEMS_OFFSET);
        
        new ImageUtilities.SliderTinter(wrapper.context, Static.BG_COLOR.UPCOMING.defaultValue).tintSlider(bnd.showDaysUpcomingSlider);
        new ImageUtilities.SliderTinter(wrapper.context, Static.BG_COLOR.EXPIRED.defaultValue).tintSlider(bnd.showDaysExpiredSlider);
        
        entryPreviewContainer.attachCurrentSelectors(
                bnd.currentFontColorSelector,
                bnd.currentBorderColorSelector,
                bnd.currentBackgroundColorSelector);
        
        bnd.settingsCloseButton.setOnClickListener(v -> dismiss());
    }
    
    protected void rebuild() {
        buildDialogDynamic(requireBinding(), (ComponentDialog) requireDialog());
    }
    
    @NonNull
    @Override
    protected ComponentDialog buildDialog() {
        return getBaseDialog();
    }
    
    @NonNull
    public abstract EntryPreviewContainer getEntryPreviewContainer(@NonNull EntrySettingsBinding bnd);
    
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
    
    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        if (todoEntryManager != null) {
            todoEntryManager.performDeferredTasks();
        }
        super.onDismiss(dialog);
    }
    
    protected void updateAllIndicators(@NonNull EntrySettingsBinding bnd) {
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
