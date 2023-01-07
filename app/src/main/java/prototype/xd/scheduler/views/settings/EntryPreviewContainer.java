package prototype.xd.scheduler.views.settings;

import static prototype.xd.scheduler.utilities.GraphicsUtilities.getExpiredUpcomingColor;
import static prototype.xd.scheduler.utilities.Keys.TODO_ITEM_VIEW_TYPE;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.card.MaterialCardView;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.views.lockscreen.LockScreenTodoItemView;

public abstract class EntryPreviewContainer {
    
    private final ViewGroup container;
    private final Context context;
    private final LayoutInflater inflater;
    private final boolean timeVisible;
    
    private LockScreenTodoItemView.TodoItemViewType todoItemViewType;
    
    private LockScreenTodoItemView<?> currentEntryPreview;
    private LockScreenTodoItemView<?> upcomingEntryPreview;
    private LockScreenTodoItemView<?> expiredEntryPreview;
    
    @Nullable
    private MaterialCardView currentFontColorSelector;
    @Nullable
    private MaterialCardView currentBorderColorSelector;
    @Nullable
    private MaterialCardView currentBgColorSelector;
    
    @Nullable
    private MaterialCardView upcomingFontColorSelector;
    @Nullable
    private MaterialCardView upcomingBorderColorSelector;
    @Nullable
    private MaterialCardView upcomingBgColorSelector;
    
    @Nullable
    private MaterialCardView expiredFontColorSelector;
    @Nullable
    private MaterialCardView expiredBorderColorSelector;
    @Nullable
    private MaterialCardView expiredBgColorSelector;
    
    protected EntryPreviewContainer(@NonNull Context context,
                                    @NonNull ViewGroup container,
                                    boolean timeVisible) {
        inflater = LayoutInflater.from(context);
        this.todoItemViewType = TODO_ITEM_VIEW_TYPE.get();
        this.context = context;
        this.container = container;
        this.timeVisible = timeVisible;
        inflate(false);
    }
    
    protected abstract int currentFontColorGetter();
    
    protected abstract int currentBgColorGetter();
    
    protected abstract int currentBorderColorGetter();
    
    protected abstract int currentBorderThicknessGetter();
    
    private void inflate(boolean update) {
        currentEntryPreview = LockScreenTodoItemView.inflateViewByType(todoItemViewType, container, inflater);
        upcomingEntryPreview = LockScreenTodoItemView.inflateViewByType(todoItemViewType, container, inflater);
        expiredEntryPreview = LockScreenTodoItemView.inflateViewByType(todoItemViewType, container, inflater);
        
        upcomingEntryPreview.setTitleText(context.getString(R.string.settings_preview_upcoming));
        currentEntryPreview.setTitleText(context.getString(R.string.settings_preview_current));
        expiredEntryPreview.setTitleText(context.getString(R.string.settings_preview_expired));
        
        if (!timeVisible) {
            currentEntryPreview.hideIndicatorAndTime();
            upcomingEntryPreview.hideIndicatorAndTime();
            expiredEntryPreview.hideIndicatorAndTime();
        }
        
        container.removeAllViews();
        container.addView(upcomingEntryPreview.getRoot());
        container.addView(currentEntryPreview.getRoot());
        container.addView(expiredEntryPreview.getRoot());
        if (update) {
            refreshAll();
        }
    }
    
    public void refreshAll() {
        updatePreviewFontAndBgColors(currentFontColorGetter(), currentBgColorGetter());
        updatePreviewBorderColors(currentBorderColorGetter());
        
        updateUpcomingPreviewBorderThickness(Keys.UPCOMING_BORDER_THICKNESS.get());
        updateCurrentPreviewBorderThickness(currentBorderThicknessGetter());
        updateExpiredPreviewBorderThickness(Keys.EXPIRED_BORDER_THICKNESS.get());
        
        updatePreviewFontSize(Keys.FONT_SIZE.get());
    }
    
    public void setTodoItemViewType(LockScreenTodoItemView.TodoItemViewType newTodoItemViewType) {
        if (todoItemViewType != newTodoItemViewType) {
            TODO_ITEM_VIEW_TYPE.put(todoItemViewType);
            inflate(true);
        }
        todoItemViewType = newTodoItemViewType;
    }
    
    public void attachCurrentSelectors(@NonNull MaterialCardView currentFontColorSelector,
                                       @NonNull MaterialCardView currentBorderColorSelector,
                                       @NonNull MaterialCardView currentBackgroundColorSelector) {
        this.currentFontColorSelector = currentFontColorSelector;
        this.currentBorderColorSelector = currentBorderColorSelector;
        this.currentBgColorSelector = currentBackgroundColorSelector;
    }
    
    public void attachUpcomingSelectors(@NonNull MaterialCardView upcomingFontColorSelector,
                                        @NonNull MaterialCardView upcomingBorderColorSelector,
                                        @NonNull MaterialCardView upcomingBackgroundColorSelector) {
        this.upcomingFontColorSelector = upcomingFontColorSelector;
        this.upcomingBorderColorSelector = upcomingBorderColorSelector;
        this.upcomingBgColorSelector = upcomingBackgroundColorSelector;
    }
    
    public void attachExpiredSelectors(@NonNull MaterialCardView expiredFontColorSelector,
                                       @NonNull MaterialCardView expiredBorderColorSelector,
                                       @NonNull MaterialCardView expiredBackgroundColorSelector) {
        this.expiredFontColorSelector = expiredFontColorSelector;
        this.expiredBorderColorSelector = expiredBorderColorSelector;
        this.expiredBgColorSelector = expiredBackgroundColorSelector;
    }
    
    public void updateUpcomingPreviewBorderThickness(int upcomingBorderThickness) {
        upcomingEntryPreview.setBorderSizeDP(upcomingBorderThickness);
    }
    
    public void updateCurrentPreviewBorderThickness(int currentBorderThickness) {
        currentEntryPreview.setBorderSizeDP(currentBorderThickness);
    }
    
    public void updateExpiredPreviewBorderThickness(int expiredBorderThickness) {
        expiredEntryPreview.setBorderSizeDP(expiredBorderThickness);
    }
    
    
    public void updatePreviewFontSize(int fontSizeSP) {
        currentEntryPreview.setCombinedTextSize(fontSizeSP);
        upcomingEntryPreview.setCombinedTextSize(fontSizeSP);
        expiredEntryPreview.setCombinedTextSize(fontSizeSP);
    }
    
    private void updateSelector(int color, @Nullable MaterialCardView selector) {
        if (selector != null) {
            selector.setCardBackgroundColor(color);
        }
    }
    
    private void updatePreviewFontAndBgColors(int currentFontColor, int currentBgColor) {
        int upcomingFontColor = Keys.UPCOMING_FONT_COLOR.get();
        int expiredFontColor = Keys.EXPIRED_FONT_COLOR.get();
        
        updateSelector(upcomingFontColor, upcomingFontColorSelector);
        updateSelector(currentFontColor, currentFontColorSelector);
        updateSelector(expiredFontColor, expiredFontColorSelector);
        
        int upcomingBgColor = Keys.UPCOMING_BG_COLOR.get();
        int expiredBgColor = Keys.EXPIRED_BG_COLOR.get();
        
        updateSelector(upcomingBgColor, upcomingBgColorSelector);
        updateSelector(currentBgColor, currentBgColorSelector);
        updateSelector(expiredBgColor, expiredBgColorSelector);
        
        upcomingEntryPreview.mixAndSetBgAndTextColors(true,
                getExpiredUpcomingColor(currentFontColor, upcomingFontColor),
                getExpiredUpcomingColor(currentBgColor, upcomingBgColor));
        currentEntryPreview.mixAndSetBgAndTextColors(true, currentFontColor, currentBgColor);
        expiredEntryPreview.mixAndSetBgAndTextColors(true,
                getExpiredUpcomingColor(currentFontColor, expiredFontColor),
                getExpiredUpcomingColor(currentBgColor, expiredBgColor));
    }
    
    public void updatePreviewBgColors(int currentBgColor) {
        updatePreviewFontAndBgColors(currentFontColorGetter(), currentBgColor);
    }
    
    public void updatePreviewFontColors(int currentFontColor) {
        updatePreviewFontAndBgColors(currentFontColor, currentBgColorGetter());
    }
    
    public void updatePreviewBorderColors(int currentBorderColor) {
        int upcomingBorderColor = Keys.UPCOMING_BORDER_COLOR.get();
        int expiredBorderColor = Keys.EXPIRED_BORDER_COLOR.get();
        
        updateSelector(upcomingBorderColor, upcomingBorderColorSelector);
        updateSelector(currentBorderColor, currentBorderColorSelector);
        updateSelector(expiredBorderColor, expiredBorderColorSelector);
        
        upcomingEntryPreview.setBorderColor(getExpiredUpcomingColor(currentBorderColor, upcomingBorderColor));
        currentEntryPreview.setBorderColor(currentBorderColor);
        expiredEntryPreview.setBorderColor(getExpiredUpcomingColor(currentBorderColor, expiredBorderColor));
    }
    
    public void notifyColorChanged(Keys.DefaultedInteger value, int newColor) {
        if (value.equals(Keys.UPCOMING_BG_COLOR) ||
                value.equals(Keys.BG_COLOR) ||
                value.equals(Keys.EXPIRED_BG_COLOR)) {
            updatePreviewBgColors(newColor);
        }
        if (value.equals(Keys.UPCOMING_FONT_COLOR) ||
                value.equals(Keys.FONT_COLOR) ||
                value.equals(Keys.EXPIRED_FONT_COLOR)) {
            updatePreviewFontColors(newColor);
        }
        if (value.equals(Keys.UPCOMING_BORDER_COLOR) ||
                value.equals(Keys.BORDER_COLOR) ||
                value.equals(Keys.EXPIRED_BORDER_COLOR)) {
            updatePreviewBorderColors(newColor);
        }
    }
}