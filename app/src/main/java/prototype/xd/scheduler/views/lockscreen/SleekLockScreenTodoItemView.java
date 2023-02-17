package prototype.xd.scheduler.views.lockscreen;

import android.content.res.ColorStateList;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import prototype.xd.scheduler.databinding.SleekEntryBinding;

public class SleekLockScreenTodoItemView extends LockScreenTodoItemView<SleekEntryBinding> {
    
    SleekLockScreenTodoItemView(@NonNull SleekEntryBinding binding) {
        super(binding);
    }
    
    @NonNull
    @Override
    public View getClickableRoot() {
        return viewBinding.backgroundOutline;
    }
    
    @NonNull
    @Override
    public SleekLockScreenTodoItemView setBackgroundColor(@ColorInt int color) {
        viewBinding.backgroundMain.setCardBackgroundColor(color);
        return this;
    }
    
    @NonNull
    @Override
    public SleekLockScreenTodoItemView setBorderColor(@ColorInt int color) {
        viewBinding.backgroundOutline.setCardBackgroundColor(color);
        return this;
    }
    
    @NonNull
    @Override
    public SleekLockScreenTodoItemView setTitleTextColor(@ColorInt int color) {
        viewBinding.titleText.setTextColor(color);
        return this;
    }
    
    @NonNull
    @Override
    public SleekLockScreenTodoItemView setIndicatorColor(@ColorInt int color) {
        viewBinding.indicatorView.setBackgroundTintList(ColorStateList.valueOf(color));
        return this;
    }
    
    @NonNull
    @Override
    public SleekLockScreenTodoItemView setTimeTextColor(@ColorInt int color) {
        viewBinding.timeTextStart.setTextColor(color);
        viewBinding.timeText.setTextColor(color);
        return this;
    }
    
    @Override
    public void setBorderSizePX(int sizePX) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) viewBinding.backgroundMain.getLayoutParams();
        params.setMargins(sizePX, sizePX, sizePX, sizePX);
        viewBinding.backgroundMain.setLayoutParams(params);
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
    public void setTimeStartText(@NonNull String text) {
        viewBinding.timeTextStart.setText(text);
    }
    
    @Override
    public void setTimeTextSize(float sizeSP) {
        viewBinding.timeTextStart.setTextSize(TypedValue.COMPLEX_UNIT_DIP, sizeSP);
        viewBinding.timeText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, sizeSP);
    }
    
    @Override
    public void hideIndicatorAndTime() {
        viewBinding.timeTextStart.setVisibility(View.GONE);
        viewBinding.timeText.setVisibility(View.GONE);
        viewBinding.indicatorView.setVisibility(View.GONE);
    }
}
