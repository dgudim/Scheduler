package prototype.xd.scheduler.views.lockscreen;

import static prototype.xd.scheduler.utilities.BitmapUtilities.getAverageColor;
import static prototype.xd.scheduler.utilities.BitmapUtilities.mixTwoColors;
import static prototype.xd.scheduler.utilities.DateManager.currentDay;
import static prototype.xd.scheduler.utilities.Keys.DEFAULT_TITLE_FONT_SIZE_MULTIPLIER;
import static prototype.xd.scheduler.utilities.Keys.DISPLAY_METRICS_DENSITY;
import static prototype.xd.scheduler.utilities.Keys.ITEM_FULL_WIDTH_LOCK;
import static prototype.xd.scheduler.utilities.Keys.SETTINGS_DEFAULT_ITEM_FULL_WIDTH_LOCK;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import com.google.android.material.color.MaterialColors;

import prototype.xd.scheduler.databinding.BasicEntryBinding;
import prototype.xd.scheduler.databinding.RoundedEntryBinding;
import prototype.xd.scheduler.databinding.SleekEntryBinding;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.utilities.Keys;

// base class for lockscreen todolist entries
public abstract class LockScreenTodoItemView<V extends ViewBinding> {
    
    V viewBinding;
    View root;
    Context context;
    
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
    public void setBorderSizeDP(int sizeDP, SharedPreferences preferences) {
        setBorderSizePX((int) (sizeDP * preferences.getFloat(DISPLAY_METRICS_DENSITY, 1)));
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
    
    
    public void applyLayoutIndependentParameters(TodoListEntry entry, SharedPreferences preferences) {
        
        int fontSizeSP = preferences.getInt(Keys.FONT_SIZE, Keys.SETTINGS_DEFAULT_FONT_SIZE);
        
        // convert pixels to dp (equivalent of TypedValue.applyDimension(COMPLEX_UNIT_DIP, value, metrics))
        setBorderSizeDP(entry.borderThickness.get(), preferences);
        
        setTitleText(entry.getTextOnDay(currentDay, context));
        setTitleTextSize(fontSizeSP * DEFAULT_TITLE_FONT_SIZE_MULTIPLIER);
        
        if (entry.isFromSystemCalendar()) {
            String timeSpan = entry.getTimeSpan(context);
            setTimeSpanText(timeSpan);
            setTimeStartText(timeSpan.split(" - ")[0]);
            setTimeTextSize(fontSizeSP);
            setIndicatorColor(entry.event.color);
        } else {
            hideIndicatorAndTime();
        }
        
        viewBinding.getRoot().setLayoutParams(new LinearLayout.LayoutParams(
                preferences.getBoolean(ITEM_FULL_WIDTH_LOCK, SETTINGS_DEFAULT_ITEM_FULL_WIDTH_LOCK) ?
                        LinearLayout.LayoutParams.MATCH_PARENT : LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
    }
    
    public void applyLayoutDependentParameters(TodoListEntry entry, Bitmap bgBitmap) {
        
        if (entry.isAdaptiveColorEnabled()) {
            int width = root.getWidth();
            int height = root.getHeight();
            
            int[] pixels = new int[width * height];
            bgBitmap.getPixels(pixels, 0, width, (int) root.getX(), (int) root.getY(), width, height);
            entry.setAverageBackgroundColor(getAverageColor(pixels));
        }
        
        mixAndSetBgAndTextColors(entry.fontColor.get(), entry.getAdaptiveColor(entry.bgColor.get()));
        setBorderColor(entry.getAdaptiveColor(entry.borderColor.get()));
    }
    
    public void mixAndSetBgAndTextColors(int fontColor, int backgroundColor) {
        // setup colors
        setBackgroundColor(backgroundColor);
        setTitleTextColor(MaterialColors.harmonize(fontColor, backgroundColor));
        // mix and harmonize (85% gray, 15% font color + harmonized with background);
        setTimeTextColor(MaterialColors.harmonize(mixTwoColors(fontColor, Color.DKGRAY, .85), backgroundColor));
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