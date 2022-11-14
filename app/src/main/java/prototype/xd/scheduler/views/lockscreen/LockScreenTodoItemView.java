package prototype.xd.scheduler.views.lockscreen;

import static prototype.xd.scheduler.utilities.BitmapUtilities.getAverageColor;
import static prototype.xd.scheduler.utilities.DateManager.currentDay;
import static prototype.xd.scheduler.utilities.Keys.ITEM_FULL_WIDTH_LOCK;
import static prototype.xd.scheduler.utilities.Keys.SETTINGS_DEFAULT_ITEM_FULL_WIDTH_LOCK;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import com.google.android.material.color.MaterialColors;

import prototype.xd.scheduler.databinding.BasicEntryBinding;
import prototype.xd.scheduler.databinding.RoundedEntryBinding;
import prototype.xd.scheduler.entities.TodoListEntry;

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
    
    
    public abstract void setBorderSize(int sizePX);
    
    
    public abstract void setTitleText(String text);
    
    public abstract void setTitleTextSize(float sizeDP);
    
    public abstract void setTimeText(String text);
    
    public abstract void setTimeTextSize(float sizeDP);
    
    
    public abstract void hideIndicatorAndTime();
    
    public void applyLayoutIndependentParameters(TodoListEntry entry, SharedPreferences preferences, float fontSizeDP) {
        
        setBorderSize(entry.borderThickness);
        
        setTitleText(entry.getTextOnDay(currentDay, context));
        setTitleTextSize(fontSizeDP * 1.1F);
        
        if (entry.fromSystemCalendar) {
            setTimeText(entry.getTimeSpan(context));
            setTimeTextSize(fontSizeDP);
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
            entry.averageBackgroundColor = getAverageColor(pixels);
        }
        
        int bgColor = entry.getAdaptiveColor(entry.bgColor);
        
        // setup colors
        setBorderColor(entry.getAdaptiveColor(entry.borderColor));
        setBackgroundColor(bgColor);
        setTitleTextColor(MaterialColors.harmonize(entry.fontColor, bgColor));
    }
    
    public enum TodoItemViewType {
        BASIC, ROUNDED
    }
    
    public static LockScreenTodoItemView<?> inflateViewByType(TodoItemViewType todoItemViewType, @Nullable ViewGroup parent, LayoutInflater layoutInflater) {
        switch (todoItemViewType) {
            case ROUNDED:
                return new RoundedLockScreenTodoItem(RoundedEntryBinding.inflate(layoutInflater, parent, false));
            case BASIC:
            default:
                return new BasicLockScreenTodoItemView(BasicEntryBinding.inflate(layoutInflater, parent, false));
        }
    }
}
