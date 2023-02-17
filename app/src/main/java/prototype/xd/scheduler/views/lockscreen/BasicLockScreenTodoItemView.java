package prototype.xd.scheduler.views.lockscreen;

import android.util.TypedValue;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import prototype.xd.scheduler.databinding.BasicEntryBinding;

public class BasicLockScreenTodoItemView extends LockScreenTodoItemView<BasicEntryBinding> {
    
    public BasicLockScreenTodoItemView(@NonNull BasicEntryBinding binding) {
        super(binding);
    }
    
    @NonNull
    @Override
    public View getClickableRoot() {
        return viewBinding.backgroundOutline;
    }
    
    @NonNull
    @Override
    public BasicLockScreenTodoItemView setBackgroundColor(@ColorInt int color) {
        viewBinding.backgroundMain.setBackgroundColor(color);
        return this;
    }
    
    @NonNull
    @Override
    public BasicLockScreenTodoItemView setBorderColor(@ColorInt int color) {
        viewBinding.backgroundOutline.setBackgroundColor(color);
        return this;
    }
    
    @NonNull
    @Override
    public BasicLockScreenTodoItemView setTitleTextColor(@ColorInt int color) {
        viewBinding.titleText.setTextColor(color);
        return this;
    }
    
    @NonNull
    @Override
    public BasicLockScreenTodoItemView setIndicatorColor(@ColorInt int color) {
        viewBinding.indicatorView.setBackgroundColor(color);
        return this;
    }
    
    @NonNull
    @Override
    public BasicLockScreenTodoItemView setTimeTextColor(@ColorInt int color) {
        viewBinding.timeText.setTextColor(color);
        return this;
    }
    
    @Override
    public void setBorderSizePX(int sizePX) {
        viewBinding.backgroundOutline.setPadding(sizePX, sizePX, sizePX, sizePX);
    }
    
    @Override
    public void setTitleText(@NonNull String text) {
        viewBinding.titleText.setText(text);
    }
    
    @Override
    public void setTitleTextSize(float sizeSP) {
        viewBinding.titleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, sizeSP);
    }
    
    @Override
    public void setTimeSpanText(@NonNull String text) {
        viewBinding.timeText.setText(text);
    }
    
    @Override
    public void setTimeTextSize(float sizeSP) {
        viewBinding.timeText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, sizeSP);
    }
    
    @Override
    public void hideIndicatorAndTime() {
        viewBinding.timeText.setVisibility(View.GONE);
        viewBinding.indicatorView.setVisibility(View.GONE);
    }
}
