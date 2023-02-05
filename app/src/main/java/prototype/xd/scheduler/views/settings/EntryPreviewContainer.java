package prototype.xd.scheduler.views.settings;

import static prototype.xd.scheduler.utilities.Static.TODO_ITEM_VIEW_TYPE;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;
import prototype.xd.scheduler.utilities.Static;
import prototype.xd.scheduler.utilities.misc.Triplet;
import prototype.xd.scheduler.views.lockscreen.LockScreenTodoItemView;

public abstract class EntryPreviewContainer {
    
    @NonNull
    private final ViewGroup container;
    @NonNull
    private final Context context;
    @NonNull
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
    
    protected EntryPreviewContainer(@NonNull ContextWrapper wrapper,
                                    @NonNull ViewGroup container,
                                    boolean timeVisible) {
        inflater = wrapper.getLayoutInflater();
        todoItemViewType = TODO_ITEM_VIEW_TYPE.get();
        context = wrapper.context;
        this.container = container;
        this.timeVisible = timeVisible;
        surfaceColor = MaterialColors.getColor(container, R.attr.colorSurface);
        inflate();
    }
    
    @ColorInt
    protected abstract int currentFontColorGetter();
    
    @ColorInt
    protected abstract int currentBgColorGetter();
    
    @ColorInt
    protected abstract int currentBorderColorGetter();
    
    protected abstract int currentBorderThicknessGetter();
    
    @IntRange(from = 0, to = 10)
    protected abstract int adaptiveColorBalanceGetter();
    
    private void inflate() {
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
        entryPreview.upcoming.addToContainer(container);
        entryPreview.current.addToContainer(container);
        entryPreview.expired.addToContainer(container);
    }
    
    public void refreshAll(boolean reInflate) {
        
        fontColor.upcoming = Static.FONT_COLOR.UPCOMING.get();
        fontColor.current = currentFontColorGetter();
        fontColor.expired = Static.FONT_COLOR.EXPIRED.get();
        
        bgColor.upcoming = Static.BG_COLOR.UPCOMING.get();
        bgColor.current = currentBgColorGetter();
        bgColor.expired = Static.BG_COLOR.EXPIRED.get();
        
        borderColor.upcoming = Static.BORDER_COLOR.UPCOMING.get();
        borderColor.current = currentBorderColorGetter();
        borderColor.expired = Static.BORDER_COLOR.EXPIRED.get();
        
        adaptiveColorBalance = adaptiveColorBalanceGetter();
        
        updatePreviewFontAndBgColors();
        updatePreviewBorderColors();
        
        setUpcomingPreviewBorderThickness(Static.BORDER_THICKNESS.UPCOMING.get());
        setCurrentPreviewBorderThickness(currentBorderThicknessGetter());
        setExpiredPreviewBorderThickness(Static.BORDER_THICKNESS.UPCOMING.get());
        
        setPreviewFontSize(Static.FONT_SIZE.get());
        
        if (reInflate) {
            setTodoItemViewType(TODO_ITEM_VIEW_TYPE.get());
        }
    }
    
    public void setTodoItemViewType(@NonNull LockScreenTodoItemView.TodoItemViewType newTodoItemViewType) {
        if (todoItemViewType != newTodoItemViewType) {
            TODO_ITEM_VIEW_TYPE.put(newTodoItemViewType);
            todoItemViewType = newTodoItemViewType;
            inflate();
            refreshAll(false);
        }
    }
    
    public void attachCurrentSelectors(@NonNull MaterialCardView currentFontColorSelector,
                                       @NonNull MaterialCardView currentBorderColorSelector,
                                       @NonNull MaterialCardView currentBackgroundColorSelector) {
        fontColorSelector.current = currentFontColorSelector;
        borderColorSelector.current = currentBorderColorSelector;
        bgColorSelector.current = currentBackgroundColorSelector;
    }
    
    public void attachUpcomingSelectors(@NonNull MaterialCardView upcomingFontColorSelector,
                                        @NonNull MaterialCardView upcomingBorderColorSelector,
                                        @NonNull MaterialCardView upcomingBackgroundColorSelector) {
        fontColorSelector.upcoming = upcomingFontColorSelector;
        borderColorSelector.upcoming = upcomingBorderColorSelector;
        bgColorSelector.upcoming = upcomingBackgroundColorSelector;
    }
    
    public void attachExpiredSelectors(@NonNull MaterialCardView expiredFontColorSelector,
                                       @NonNull MaterialCardView expiredBorderColorSelector,
                                       @NonNull MaterialCardView expiredBackgroundColorSelector) {
        fontColorSelector.expired = expiredFontColorSelector;
        borderColorSelector.expired = expiredBorderColorSelector;
        bgColorSelector.expired = expiredBackgroundColorSelector;
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
    
    private static void updateSelector(@ColorInt int color, @Nullable MaterialCardView selector) {
        if (selector != null) {
            selector.setCardBackgroundColor(color);
        }
    }
    
    private void updatePreviewFontAndBgColors() {
        
        fontColor.applyTo(fontColorSelector, EntryPreviewContainer::updateSelector);
        bgColor.applyTo(bgColorSelector, EntryPreviewContainer::updateSelector);
        
        entryPreview.upcoming.mixAndSetBgAndTextColors(true,
                fontColor.getUpcoming(),
                bgColor.getUpcomingMixed(surfaceColor, adaptiveColorBalance));
        
        entryPreview.current.mixAndSetBgAndTextColors(true,
                fontColor.current,
                bgColor.getCurrentMixed(surfaceColor, adaptiveColorBalance));
        
        entryPreview.expired.mixAndSetBgAndTextColors(true,
                fontColor.getExpired(),
                bgColor.getExpiredMixed(surfaceColor, adaptiveColorBalance));
    }
    
    public void setPreviewAdaptiveColorBalance(int adaptiveColorBalance) {
        this.adaptiveColorBalance = adaptiveColorBalance;
        updatePreviewFontAndBgColors();
        updatePreviewBorderColors();
    }
    
    private void updatePreviewBorderColors() {
        
        borderColor.applyTo(borderColorSelector, EntryPreviewContainer::updateSelector);
        
        entryPreview.upcoming.setBorderColor(borderColor.getUpcomingMixed(surfaceColor, adaptiveColorBalance));
        entryPreview.current.setBorderColor(borderColor.getCurrentMixed(surfaceColor, adaptiveColorBalance));
        entryPreview.expired.setBorderColor(borderColor.getExpiredMixed(surfaceColor, adaptiveColorBalance));
    }
    
    public void notifyColorChanged(@NonNull Static.DefaultedInteger value, int newColor) {
        if (Static.BG_COLOR.has(value)) {
            bgColor.setByType(value.getType(), newColor);
            updatePreviewFontAndBgColors();
            return;
        }
        if (Static.FONT_COLOR.has(value)) {
            fontColor.setByType(value.getType(), newColor);
            updatePreviewFontAndBgColors();
            return;
        }
        if (Static.BORDER_COLOR.has(value)) {
            borderColor.setByType(value.getType(), newColor);
            updatePreviewBorderColors();
        }
    }
}
