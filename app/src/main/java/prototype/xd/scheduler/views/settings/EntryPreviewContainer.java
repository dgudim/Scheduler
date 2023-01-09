package prototype.xd.scheduler.views.settings;

import static prototype.xd.scheduler.utilities.GraphicsUtilities.mixColorWithBg;
import static prototype.xd.scheduler.utilities.Keys.TODO_ITEM_VIEW_TYPE;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.Triplet;
import prototype.xd.scheduler.views.lockscreen.LockScreenTodoItemView;

public abstract class EntryPreviewContainer {
    
    private final ViewGroup container;
    private final Context context;
    private final LayoutInflater inflater;
    private final boolean timeVisible;
    
    private final Triplet.ColorTriplet fontColor = new Triplet.ColorTriplet();
    private final Triplet.ColorTriplet bgColor = new Triplet.ColorTriplet();
    private final Triplet.ColorTriplet borderColor = new Triplet.ColorTriplet();
    
    private int adaptiveColorBalance;
    private final int surfaceColor;
    
    private LockScreenTodoItemView.TodoItemViewType todoItemViewType;
    
    private final Triplet<LockScreenTodoItemView<?>> entryPreview = new Triplet<>();
    
    private final Triplet<MaterialCardView> fontColorSelector = new Triplet<>();
    private final Triplet<MaterialCardView> borderColorSelector = new Triplet<>();
    private final Triplet<MaterialCardView> bgColorSelector = new Triplet<>();
 
    protected EntryPreviewContainer(@NonNull Context context,
                                    @NonNull ViewGroup container,
                                    boolean timeVisible) {
        inflater = LayoutInflater.from(context);
        this.todoItemViewType = TODO_ITEM_VIEW_TYPE.get();
        this.context = context;
        this.container = container;
        this.timeVisible = timeVisible;
        surfaceColor = MaterialColors.getColor(container, R.attr.colorSurface);
        inflate(false);
    }
    
    protected abstract int currentFontColorGetter();
    
    protected abstract int currentBgColorGetter();
    
    protected abstract int currentBorderColorGetter();
    
    protected abstract int currentBorderThicknessGetter();
    
    protected abstract int adaptiveColorBalanceGetter();
    
    private void inflate(boolean update) {
        entryPreview.upcoming = LockScreenTodoItemView.inflateViewByType(todoItemViewType, container, inflater);
        entryPreview.current = LockScreenTodoItemView.inflateViewByType(todoItemViewType, container, inflater);
        entryPreview.expired = LockScreenTodoItemView.inflateViewByType(todoItemViewType, container, inflater);
    
        entryPreview.upcoming.setTitleText(context.getString(R.string.settings_preview_upcoming));
        entryPreview.current.setTitleText(context.getString(R.string.settings_preview_current));
        entryPreview.expired.setTitleText(context.getString(R.string.settings_preview_expired));
        
        if (!timeVisible) {
            entryPreview.upcoming.hideIndicatorAndTime();
            entryPreview.current.hideIndicatorAndTime();
            entryPreview.expired.hideIndicatorAndTime();
        }
        
        container.removeAllViews();
        container.addView(entryPreview.upcoming.getRoot());
        container.addView(entryPreview.current.getRoot());
        container.addView(entryPreview.expired.getRoot());
        if (update) {
            refreshAll();
        }
    }
    
    public void refreshAll() {
        
        fontColor.upcoming = Keys.UPCOMING_FONT_COLOR.get();
        fontColor.current = currentFontColorGetter();
        fontColor.expired = Keys.EXPIRED_FONT_COLOR.get();
        
        bgColor.upcoming = Keys.UPCOMING_BG_COLOR.get();
        bgColor.current = currentBgColorGetter();
        bgColor.expired = Keys.EXPIRED_BG_COLOR.get();
        
        borderColor.upcoming = Keys.UPCOMING_BORDER_COLOR.get();
        borderColor.current = currentBorderColorGetter();
        borderColor.expired = Keys.EXPIRED_BORDER_COLOR.get();
        
        adaptiveColorBalance = currentBorderColorGetter();
        
        updatePreviewFontAndBgColors();
        updatePreviewBorderColors();
        
        setUpcomingPreviewBorderThickness(Keys.UPCOMING_BORDER_THICKNESS.get());
        setCurrentPreviewBorderThickness(currentBorderThicknessGetter());
        setExpiredPreviewBorderThickness(Keys.UPCOMING_BORDER_THICKNESS.get());
        
        setPreviewFontSize(Keys.FONT_SIZE.get());
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
        fontColorSelector.current   = currentFontColorSelector;
        borderColorSelector.current = currentBorderColorSelector;
        bgColorSelector.current     = currentBackgroundColorSelector;
    }
    
    public void attachUpcomingSelectors(@NonNull MaterialCardView upcomingFontColorSelector,
                                        @NonNull MaterialCardView upcomingBorderColorSelector,
                                        @NonNull MaterialCardView upcomingBackgroundColorSelector) {
        fontColorSelector.upcoming   = upcomingFontColorSelector;
        borderColorSelector.upcoming = upcomingBorderColorSelector;
        bgColorSelector.upcoming     = upcomingBackgroundColorSelector;
    }
    
    public void attachExpiredSelectors(@NonNull MaterialCardView expiredFontColorSelector,
                                       @NonNull MaterialCardView expiredBorderColorSelector,
                                       @NonNull MaterialCardView expiredBackgroundColorSelector) {
        fontColorSelector.expired   = expiredFontColorSelector;
        borderColorSelector.expired = expiredBorderColorSelector;
        bgColorSelector.expired     = expiredBackgroundColorSelector;
    }
    
    public void setUpcomingPreviewBorderThickness(int upcomingBorderThickness) {
        entryPreview.upcoming.setBorderSizeDP(upcomingBorderThickness);
    }
    
    public void setCurrentPreviewBorderThickness(int currentBorderThickness) {
        entryPreview.current.setBorderSizeDP(currentBorderThickness);
    }
    
    public void setExpiredPreviewBorderThickness(int expiredBorderThickness) {
        entryPreview.expired.setBorderSizeDP(expiredBorderThickness);
    }
    
    public void setPreviewFontSize(int fontSizeSP) {
        entryPreview.upcoming.setCombinedTextSize(fontSizeSP);
        entryPreview.current.setCombinedTextSize(fontSizeSP);
        entryPreview.expired.setCombinedTextSize(fontSizeSP);
    }
    
    private void updateSelector(int color, @Nullable MaterialCardView selector) {
        if (selector != null) {
            selector.setCardBackgroundColor(color);
        }
    }
    
    private void updatePreviewFontAndBgColors() {
    
        fontColor.applyTo(fontColorSelector, this::updateSelector);
        
        int currentBgColor = mixColorWithBg(bgColor.current, surfaceColor, adaptiveColorBalance);
    
        bgColor.applyTo(bgColorSelector, this::updateSelector);
        
        entryPreview.upcoming.mixAndSetBgAndTextColors(true, fontColor.getUpcomingMixed(), bgColor.getUpcomingMixed(currentBgColor));
        entryPreview.current.mixAndSetBgAndTextColors(true, fontColor.current, currentBgColor);
        entryPreview.expired.mixAndSetBgAndTextColors(true, fontColor.getExpiredMixed(), bgColor.getExpiredMixed(currentBgColor));
    }
    
    public void setPreviewCurrentBgColor(int currentBgColor) {
        bgColor.current = currentBgColor;
        updatePreviewFontAndBgColors();
    }
    
    public void setPreviewCurrentFontColor(int currentFontColor) {
        fontColor.current = currentFontColor;
        updatePreviewFontAndBgColors();
    }
    
    public void setPreviewAdaptiveColorBalance(int adaptiveColorBalance) {
        this.adaptiveColorBalance = adaptiveColorBalance;
        updatePreviewFontAndBgColors();
        updatePreviewBorderColors();
    }
    
    private void updatePreviewBorderColors() {
        
        int currentBorderColor = mixColorWithBg(borderColor.current, surfaceColor, adaptiveColorBalance);
    
        borderColor.applyTo(borderColorSelector, this::updateSelector);
        
        entryPreview.upcoming.setBorderColor(borderColor.getUpcomingMixed(currentBorderColor));
        entryPreview.current.setBorderColor(currentBorderColor);
        entryPreview.expired.setBorderColor(borderColor.getExpiredMixed(currentBorderColor));
    }
    
    public void notifyColorChanged(Keys.DefaultedInteger value, int newColor, boolean checkExpiredUpcoming) {
        if (value.equals(Keys.BG_COLOR)) {
            setPreviewCurrentBgColor(newColor);
            return;
        }
        if (value.equals(Keys.FONT_COLOR)) {
            setPreviewCurrentFontColor(newColor);
            return;
        }
        if (value.equals(Keys.BORDER_COLOR)) {
            borderColor.current = newColor;
            updatePreviewBorderColors();
            return;
        }
        if(checkExpiredUpcoming) {
            if (value.equals(Keys.UPCOMING_BG_COLOR)) {
                bgColor.upcoming = newColor;
                updatePreviewFontAndBgColors();
                return;
            }
            if (value.equals(Keys.UPCOMING_FONT_COLOR)) {
                fontColor.upcoming = newColor;
                updatePreviewFontAndBgColors();
                return;
            }
            if (value.equals(Keys.UPCOMING_BORDER_COLOR)) {
                borderColor.upcoming = newColor;
                updatePreviewBorderColors();
                return;
            }
            if (value.equals(Keys.EXPIRED_BG_COLOR)) {
                bgColor.expired = newColor;
                updatePreviewFontAndBgColors();
                return;
            }
            if (value.equals(Keys.EXPIRED_FONT_COLOR)) {
                fontColor.expired = newColor;
                updatePreviewFontAndBgColors();
                return;
            }
            if (value.equals(Keys.EXPIRED_BORDER_COLOR)) {
                borderColor.expired = newColor;
                updatePreviewBorderColors();
            }
        }
    }
}