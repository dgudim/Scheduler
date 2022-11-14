package prototype.xd.scheduler.views.lockscreen;

import static prototype.xd.scheduler.utilities.DateManager.currentDay;
import static prototype.xd.scheduler.utilities.Keys.ITEM_FULL_WIDTH_LOCK;
import static prototype.xd.scheduler.utilities.Keys.SETTINGS_DEFAULT_ITEM_FULL_WIDTH_LOCK;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.LinearLayout;

import androidx.viewbinding.ViewBinding;

import com.google.android.material.color.MaterialColors;

import prototype.xd.scheduler.entities.TodoListEntry;

// base class for lockscreen todolist entries
public abstract class LockScreenTodoItemView<V extends ViewBinding> {
    
    V viewBinding;
    Context context;
    
    public LockScreenTodoItemView(V binding, TodoListEntry entry, SharedPreferences preferences, float fontSize) {
        viewBinding = binding;
        context = binding.getRoot().getContext();
        apply(entry, preferences, fontSize);
    }
    
    public abstract void setBackgroundColor(int color);
    
    public abstract void setBorderColor(int color);
    
    public abstract void setTitleTextColor(int color);
    
    public abstract void setIndicatorColor(int color);
    
    public abstract void setTitleText(String text);
    
    public abstract void setTitleTextSize(float size);
    
    public abstract void setTimeText(String text);
    
    public abstract void setTimeTextSize(float size);
    
    public abstract void hideIndicatorAndTime();
    
    public void apply(TodoListEntry entry, SharedPreferences preferences, float fontSize) {
        int borderColor = entry.getAdaptiveColor(entry.borderColor);
        int bgColor = entry.getAdaptiveColor(entry.bgColor);
        
        // set border and bg colors
        setBorderColor(borderColor);
        setBackgroundColor(bgColor);
        
        // set text and harmonize color to make sure it's visible
        setTitleText(entry.getTextOnDay(currentDay, context));
        setTitleTextColor(MaterialColors.harmonize(entry.fontColor, bgColor));
        setTitleTextSize(fontSize * 1.1F);
        
        if (entry.fromSystemCalendar) {
            hideIndicatorAndTime();
        } else {
            setTimeText(entry.getTimeSpan(context));
            setTimeTextSize(fontSize);
            setIndicatorColor(entry.event.color);
        }
        
        viewBinding.getRoot().setLayoutParams(new LinearLayout.LayoutParams(
                preferences.getBoolean(ITEM_FULL_WIDTH_LOCK, SETTINGS_DEFAULT_ITEM_FULL_WIDTH_LOCK) ?
                        LinearLayout.LayoutParams.MATCH_PARENT : LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
    }
}
