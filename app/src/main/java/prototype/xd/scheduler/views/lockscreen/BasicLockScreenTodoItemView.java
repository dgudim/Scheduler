package prototype.xd.scheduler.views.lockscreen;

import android.content.SharedPreferences;
import android.util.TypedValue;
import android.view.View;

import prototype.xd.scheduler.databinding.BasicEntryBinding;
import prototype.xd.scheduler.entities.TodoListEntry;

public class BasicLockScreenTodoItemView extends LockScreenTodoItemView<BasicEntryBinding> {
    
    
    public BasicLockScreenTodoItemView(BasicEntryBinding binding, TodoListEntry entry, SharedPreferences preferences, float fontSize) {
        super(binding, entry, preferences, fontSize);
    }
    
    @Override
    public void setBackgroundColor(int color) {
        viewBinding.backgroundMain.setBackgroundColor(color);
    }
    
    @Override
    public void setBorderColor(int color) {
    
    }
    
    @Override
    public void setTitleTextColor(int color) {
    
    }
    
    @Override
    public void setIndicatorColor(int color) {
        viewBinding.indicatorView.setBackgroundColor(color);
    }
    
    @Override
    public void setTitleText(String text) {
        viewBinding.titleText.setText(text);
    }
    
    @Override
    public void setTitleTextSize(float size) {
        viewBinding.titleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
    }
    
    @Override
    public void setTimeText(String text) {
        viewBinding.timeText.setText(text);
    }
    
    @Override
    public void setTimeTextSize(float size) {
        viewBinding.timeText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
    }
    
    @Override
    public void hideIndicatorAndTime() {
        viewBinding.timeText.setVisibility(View.GONE);
        viewBinding.indicatorView.setVisibility(View.GONE);
    }
}
