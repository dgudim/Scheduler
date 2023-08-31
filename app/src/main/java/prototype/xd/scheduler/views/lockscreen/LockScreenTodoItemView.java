package prototype.xd.scheduler.views.lockscreen;

import static prototype.xd.scheduler.utilities.ColorUtilities.getAverageColor;
import static prototype.xd.scheduler.utilities.ColorUtilities.getHarmonizedFontColorWithBg;
import static prototype.xd.scheduler.utilities.ColorUtilities.getHarmonizedSecondaryFontColorWithBg;
import static prototype.xd.scheduler.utilities.DateManager.currentDayUTC;
import static prototype.xd.scheduler.utilities.Static.DEFAULT_TITLE_FONT_SIZE_MULTIPLIER;
import static prototype.xd.scheduler.utilities.Static.DISPLAY_METRICS_DENSITY;
import static prototype.xd.scheduler.utilities.Static.GLOBAL_ITEMS_LABEL_POSITION;
import static prototype.xd.scheduler.utilities.Static.ITEM_FULL_WIDTH_LOCK;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.viewbinding.ViewBinding;

import java.util.regex.Pattern;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.BasicEntryBinding;
import prototype.xd.scheduler.databinding.RoundedEntryBinding;
import prototype.xd.scheduler.databinding.SleekEntryBinding;
import prototype.xd.scheduler.entities.TodoEntry;
import prototype.xd.scheduler.utilities.Static;

// base class for lockscreen todolist entries
@SuppressWarnings("UnusedReturnValue")
public abstract class LockScreenTodoItemView<V extends ViewBinding> {
    
    @NonNull
    protected final V viewBinding;
    @NonNull
    private final View root;
    private final Context context;
    
    private static final Pattern timeSplitPattern = Pattern.compile(Static.TIME_RANGE_SEPARATOR);
    
    LockScreenTodoItemView(@NonNull V binding) {
        viewBinding = binding;
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
        setBorderSizePX((int) (sizeDP * DISPLAY_METRICS_DENSITY.get()));
    }
    
    public abstract void setBorderSizePX(int sizePX);
    
    
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
        
        viewBinding.getRoot().setLayoutParams(new LinearLayout.LayoutParams(
                ITEM_FULL_WIDTH_LOCK.get() ?
                        LinearLayout.LayoutParams.MATCH_PARENT : LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        
        return this;
    }
    
    public void applyLayoutDependentParameters(@NonNull TodoEntry entry, @NonNull Bitmap bgBitmap, @NonNull ViewGroup container) {
        
        if (entry.isAdaptiveColorEnabled()) {
            int width = root.getWidth();
            int height = root.getHeight();
            
            int[] pixels = new int[width * height];
            //                                                                                 add container y offset
            bgBitmap.getPixels(pixels, 0, width, (int) root.getX(), (int) (root.getY() + container.getY()), width, height);
            entry.setAverageBackgroundColor(getAverageColor(pixels));
        }
        
        mixAndSetBgAndTextColors(entry.isFromSystemCalendar(),
                entry.fontColor.get(currentDayUTC),
                entry.getAdaptiveColor(entry.bgColor.get(currentDayUTC)));
        setBorderColor(entry.getAdaptiveColor(entry.borderColor.get(currentDayUTC)));
    }
    
    @NonNull
    public LockScreenTodoItemView<V> mixAndSetBgAndTextColors(boolean setTimeTextColor, int fontColor, int backgroundColor) {
        // setup colors
        setBackgroundColor(backgroundColor);
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
        switch (todoItemViewType) {
            case SLEEK:
                return new SleekLockScreenTodoItemView(SleekEntryBinding.inflate(layoutInflater, parent, false));
            case ROUNDED:
                return new RoundedLockScreenTodoItem(RoundedEntryBinding.inflate(layoutInflater, parent, false));
            case BASIC:
            default:
                return new BasicLockScreenTodoItemView(BasicEntryBinding.inflate(layoutInflater, parent, false));
        }
    }
}
