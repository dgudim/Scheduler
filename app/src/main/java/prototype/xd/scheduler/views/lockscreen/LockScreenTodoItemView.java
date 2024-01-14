package prototype.xd.scheduler.views.lockscreen;

import static prototype.xd.scheduler.utilities.DateManager.currentDayUTC;
import static prototype.xd.scheduler.utilities.ImageUtilities.dpToPx;
import static prototype.xd.scheduler.utilities.ImageUtilities.getAverageColor;
import static prototype.xd.scheduler.utilities.ImageUtilities.getHarmonizedFontColorWithBg;
import static prototype.xd.scheduler.utilities.ImageUtilities.getHarmonizedSecondaryFontColorWithBg;
import static prototype.xd.scheduler.utilities.ImageUtilities.pxToDp;
import static prototype.xd.scheduler.utilities.Static.DEFAULT_TITLE_FONT_SIZE_MULTIPLIER;
import static prototype.xd.scheduler.utilities.Static.GLOBAL_ITEMS_LABEL_POSITION;
import static prototype.xd.scheduler.utilities.Static.ITEM_FULL_WIDTH_LOCK;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.viewbinding.ViewBinding;

import java.util.regex.Pattern;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.BasicEntryBinding;
import prototype.xd.scheduler.databinding.RoundedEntryBinding;
import prototype.xd.scheduler.databinding.SleekEntryBinding;
import prototype.xd.scheduler.entities.TodoEntry;
import prototype.xd.scheduler.utilities.ImageUtilities;
import prototype.xd.scheduler.utilities.Static;

// base class for lockscreen todolist entries
@SuppressWarnings("UnusedReturnValue")
public abstract class LockScreenTodoItemView<V extends ViewBinding> {
    
    @NonNull
    protected final V binding;
    @NonNull
    private final View root;
    private final Context context;
    
    private static final Pattern timeSplitPattern = Pattern.compile(Static.TIME_RANGE_SEPARATOR);
    
    LockScreenTodoItemView(@NonNull V binding) {
        this.binding = binding;
        root = binding.getRoot();
        context = binding.getRoot().getContext();
    }
    
    @NonNull
    protected abstract View getClickableRoot();
    
    @NonNull
    public LockScreenTodoItemView<V> setOnClickListener(@Nullable View.OnClickListener onClickListener) {
        View view = getClickableRoot();
        view.setFocusable(true);
        TypedValue themedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.selectableItemBackground, themedValue, true);
        view.setForeground(AppCompatResources.getDrawable(context, themedValue.resourceId));
        view.setOnClickListener(onClickListener);
        return this;
    }
    
    @NonNull
    public LockScreenTodoItemView<V> addToContainer(@NonNull ViewGroup container) {
        container.addView(root);
        return this;
    }
    
    @NonNull
    public abstract LockScreenTodoItemView<V> setBackgroundColor(@ColorInt int color);
    
    @NonNull
    public abstract LockScreenTodoItemView<V> setBorderColor(@ColorInt int color);
    
    @NonNull
    public abstract LockScreenTodoItemView<V> setTitleTextColor(@ColorInt int color);
    
    @NonNull
    public abstract LockScreenTodoItemView<V> setIndicatorColor(@ColorInt int color);
    
    @NonNull
    public abstract LockScreenTodoItemView<V> setTimeTextColor(@ColorInt int color);
    
    // should not be overridden
    public void setBorderSizeDP(int sizeDP) {
        // convert to dp to pixels
        setBorderSizePX(dpToPx(sizeDP));
    }
    
    protected abstract void setBorderSizePX(int sizePX);
    
    
    public abstract void setTitleTextSize(float sizeSP);
    
    public abstract void setTimeTextSize(float sizeSP);
    
    public void setCombinedTextSize(float sizeSP) {
        setTitleTextSize(sizeSP * DEFAULT_TITLE_FONT_SIZE_MULTIPLIER);
        setTimeTextSize(sizeSP);
    }
    
    public void setTimeStartText(@NonNull String text) {
        // empty by default, not all views support this
    }
    
    public abstract void setTitleText(@NonNull String text);
    
    public abstract void setTimeSpanText(@NonNull String text);
    
    public abstract void setBackgroundDrawable(@NonNull Drawable bitmapDrawable);
    
    void setBackgroundBitmap(@NonNull Bitmap bgBitmap) {
        Resources resources = binding.getRoot().getResources();
        RoundedBitmapDrawable roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(resources, ImageUtilities.makeMutable(bgBitmap));
        roundedBitmapDrawable.setCornerRadius(resources.getDimensionPixelSize(R.dimen.card_corner_radius) - dpToPx(1));
        // For some reason -1dp is needed
        setBackgroundDrawable(roundedBitmapDrawable);
    }
    
    
    public abstract void hideIndicatorAndTime();
    
    
    @NonNull
    public LockScreenTodoItemView<V> applyLayoutIndependentParameters(@NonNull TodoEntry entry) {
        
        int fontSizeSP = Static.FONT_SIZE.get();
        
        setBorderSizeDP(entry.borderThickness.get(currentDayUTC));
        
        setTitleText(entry.getTextOnDay(currentDayUTC, context, GLOBAL_ITEMS_LABEL_POSITION.get()));
        setTitleTextSize(fontSizeSP * DEFAULT_TITLE_FONT_SIZE_MULTIPLIER);
        
        if (entry.isFromSystemCalendar()) {
            String timeSpan = entry.getCalendarEntryTimeSpan(context, currentDayUTC);
            setTimeSpanText(timeSpan);
            
            setTimeStartText(timeSplitPattern.split(timeSpan)[0]);
            setTimeTextSize(fontSizeSP);
            setIndicatorColor(entry.getCalendarEventColor());
        } else {
            hideIndicatorAndTime();
        }
        
        binding.getRoot().setLayoutParams(new LinearLayout.LayoutParams(
                ITEM_FULL_WIDTH_LOCK.get() ?
                        ViewGroup.LayoutParams.MATCH_PARENT : ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        
        return this;
    }
    
    public void applyLayoutDependentParameters(@NonNull TodoEntry entry,
                                               @NonNull Bitmap bgBitmap,
                                               @NonNull ImageUtilities.BitmapEffectsPipe effectsPipe,
                                               @NonNull ViewGroup container) {
        
        int averageBgColor = entry.bgColor.get(currentDayUTC);
        
        if (effectsPipe.isActive() || entry.isAdaptiveColorEnabled()) {
            int borderThicknessDp = entry.borderThickness.get(currentDayUTC);
            double verticalPaddingDp = pxToDp(container.getResources().getDimension(R.dimen.lockscreen_item_vertical_padding));
            int topBottom = dpToPx(verticalPaddingDp + borderThicknessDp);
            int leftRight = dpToPx(borderThicknessDp);
            
            float outerX = root.getX();
            float outerY = root.getY() + container.getY();
            int innerX = (int) (outerX + leftRight);
            int innerY = (int) (outerY + topBottom);
            
            int outerWidth = root.getWidth();
            int outerHeight = root.getHeight();
            int innerWidth = outerWidth - leftRight * 2;
            int innerHeight = outerHeight - topBottom * 2;
            
            int[] pixels = new int[outerWidth * outerHeight];
            bgBitmap.getPixels(pixels, 0, outerWidth, innerX, innerY, innerWidth, innerHeight);
            entry.setAverageBackgroundColor(getAverageColor(pixels));
            
            averageBgColor = entry.getAdaptiveColor(averageBgColor);
            
            if (effectsPipe.isActive()) {
                setBackgroundBitmap(effectsPipe.processBitmap(
                        Bitmap.createBitmap(bgBitmap, innerX, innerY, innerWidth, innerHeight),
                        averageBgColor,
                        container.getContext()));
            }
        }
        
        mixAndSetBgAndTextColors(
                entry.isFromSystemCalendar(),
                !effectsPipe.isActive(),
                entry.fontColor.get(currentDayUTC),
                averageBgColor);
        setBorderColor(entry.getAdaptiveColor(entry.borderColor.get(currentDayUTC)));
    }
    
    @NonNull
    public LockScreenTodoItemView<V> mixAndSetBgAndTextColors(int fontColor, int backgroundColor) {
        return mixAndSetBgAndTextColors(true, true, fontColor, backgroundColor);
    }
    
    @NonNull
    public LockScreenTodoItemView<V> mixAndSetBgAndTextColors(boolean setTimeTextColor, boolean setBgColor,
                                                              int fontColor, int backgroundColor) {
        // setup colors
        if (setBgColor) {
            setBackgroundColor(backgroundColor);
        }
        setTitleTextColor(getHarmonizedFontColorWithBg(fontColor, backgroundColor));
        if (setTimeTextColor) {
            setTimeTextColor(getHarmonizedSecondaryFontColorWithBg(fontColor, backgroundColor));
        }
        return this;
    }
    
    public enum TodoItemViewType {
        BASIC, ROUNDED, SLEEK
    }
    
    @NonNull
    public static LockScreenTodoItemView<?> inflateViewByType(@NonNull TodoItemViewType todoItemViewType, @Nullable ViewGroup parent, @NonNull LayoutInflater layoutInflater) {
        return switch (todoItemViewType) {
            case SLEEK ->
                    new SleekLockScreenTodoItemView(SleekEntryBinding.inflate(layoutInflater, parent, false));
            case ROUNDED ->
                    new RoundedLockScreenTodoItem(RoundedEntryBinding.inflate(layoutInflater, parent, false));
            default ->
                    new BasicLockScreenTodoItemView(BasicEntryBinding.inflate(layoutInflater, parent, false));
        };
    }
}
