package prototype.xd.scheduler.views.lockscreen;

import static prototype.xd.scheduler.utilities.BitmapUtilities.getAverageColor;
import static prototype.xd.scheduler.utilities.BitmapUtilities.getFontColor;
import static prototype.xd.scheduler.utilities.BitmapUtilities.getTimeTextColor;
import static prototype.xd.scheduler.utilities.DateManager.currentDayUTC;
import static prototype.xd.scheduler.utilities.Keys.DEFAULT_TITLE_FONT_SIZE_MULTIPLIER;
import static prototype.xd.scheduler.utilities.Keys.DISPLAY_METRICS_DENSITY;
import static prototype.xd.scheduler.utilities.Keys.ITEM_FULL_WIDTH_LOCK;
import static prototype.xd.scheduler.utilities.Keys.SHOW_GLOBAL_ITEMS_LABEL_LOCK;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import prototype.xd.scheduler.databinding.BasicEntryBinding;
import prototype.xd.scheduler.databinding.RoundedEntryBinding;
import prototype.xd.scheduler.databinding.SleekEntryBinding;
import prototype.xd.scheduler.entities.TodoEntry;
import prototype.xd.scheduler.utilities.Keys;

// base class for lockscreen todolist entries
public abstract class LockScreenTodoItemView<V extends ViewBinding> {
    
    final V viewBinding;
    final View root;
    final Context context;
    
    LockScreenTodoItemView(V binding) {
        viewBinding = binding;
        root = binding.getRoot();
        context = binding.getRoot().getContext();
    }
    
    public View getRoot() {
        return root;
    }
    
    public abstract void setBackgroundColor(int color);
    
    public abstract void setBorderColor(int color);
    
    public abstract void setTitleTextColor(int color);
    
    public abstract void setIndicatorColor(int color);
    
    public abstract void setTimeTextColor(int color);
    
    // should not be overridden
    public void setBorderSizeDP(int sizeDP) {
        setBorderSizePX((int) (sizeDP * DISPLAY_METRICS_DENSITY.get()));
    }
    
    public abstract void setBorderSizePX(int sizePX);
    
    
    public abstract void setTitleTextSize(float sizeSP);
    
    public abstract void setTimeTextSize(float sizeSP);
    
    public void setCombinedTextSize(float sizeSP) {
        setTitleTextSize(sizeSP * DEFAULT_TITLE_FONT_SIZE_MULTIPLIER);
        setTimeTextSize(sizeSP);
    }
    
    public void setTimeStartText(String text) {
        // empty by default, not all views support this
    }
    
    public abstract void setTitleText(String text);
    
    public abstract void setTimeSpanText(String text);
    
    
    public abstract void hideIndicatorAndTime();
    
    
    public void applyLayoutIndependentParameters(TodoEntry entry) {
        
        int fontSizeSP = Keys.FONT_SIZE.get();
        
        // convert pixels to dp (equivalent of TypedValue.applyDimension(COMPLEX_UNIT_DIP, value, metrics))
        setBorderSizeDP(entry.borderThickness.get(currentDayUTC));
        
        setTitleText(entry.getTextOnDay(currentDayUTC, context, SHOW_GLOBAL_ITEMS_LABEL_LOCK.get()));
        setTitleTextSize(fontSizeSP * DEFAULT_TITLE_FONT_SIZE_MULTIPLIER);
        
        if (entry.isFromSystemCalendar()) {
            String timeSpan = entry.getCalendarEntryTimeSpan(context, currentDayUTC);
            setTimeSpanText(timeSpan);
            setTimeStartText(timeSpan.split(" - ")[0]);
            setTimeTextSize(fontSizeSP);
            setIndicatorColor(entry.event.color);
        } else {
            hideIndicatorAndTime();
        }
        
        viewBinding.getRoot().setLayoutParams(new LinearLayout.LayoutParams(
                ITEM_FULL_WIDTH_LOCK.get() ?
                        LinearLayout.LayoutParams.MATCH_PARENT : LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
    }
    
    public void applyLayoutDependentParameters(TodoEntry entry, Bitmap bgBitmap, ViewGroup container) {
        
        if (entry.isAdaptiveColorEnabled()) {
            int width = root.getWidth();
            int height = root.getHeight();
            
            int[] pixels = new int[width * height];                       // add container y offset
            bgBitmap.getPixels(pixels, 0, width, (int) root.getX(), (int) (root.getY() + container.getY()), width, height);
            entry.setAverageBackgroundColor(getAverageColor(pixels));
        }
        
        mixAndSetBgAndTextColors(entry.isFromSystemCalendar(),
                entry.fontColor.get(currentDayUTC),
                entry.getAdaptiveColor(entry.bgColor.get(currentDayUTC)));
        setBorderColor(entry.getAdaptiveColor(entry.borderColor.get(currentDayUTC)));
    }
    
    public void mixAndSetBgAndTextColors(boolean setTimeTextColor, int fontColor, int backgroundColor) {
        // setup colors
        setBackgroundColor(backgroundColor);
        setTitleTextColor(getFontColor(fontColor, backgroundColor));
        if (setTimeTextColor) {
            setTimeTextColor(getTimeTextColor(fontColor, backgroundColor));
        }
    }
    
    public enum TodoItemViewType {
        BASIC, ROUNDED, SLEEK
    }
    
    public static LockScreenTodoItemView<?> inflateViewByType(TodoItemViewType todoItemViewType, @Nullable ViewGroup parent, LayoutInflater layoutInflater) {
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