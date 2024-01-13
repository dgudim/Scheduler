package prototype.xd.scheduler.views.lockscreen;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import prototype.xd.scheduler.databinding.RoundedEntryBinding;

public class RoundedLockScreenTodoItem extends LockScreenTodoItemView<RoundedEntryBinding> {
    
    public RoundedLockScreenTodoItem(@NonNull RoundedEntryBinding binding) {
        super(binding);
    }
    
    @NonNull
    @Override
    public View getClickableRoot() {
        return binding.backgroundOutline;
    }
    
    @NonNull
    @Override
    public RoundedLockScreenTodoItem setBackgroundColor(@ColorInt int color) {
        binding.backgroundMain.setCardBackgroundColor(color);
        return this;
    }
    
    @NonNull
    @Override
    public RoundedLockScreenTodoItem setBorderColor(@ColorInt int color) {
        binding.backgroundOutline.setCardBackgroundColor(color);
        return this;
    }
    
    @NonNull
    @Override
    public RoundedLockScreenTodoItem setTitleTextColor(@ColorInt int color) {
        binding.titleText.setTextColor(color);
        return this;
    }
    
    @NonNull
    @Override
    public RoundedLockScreenTodoItem setIndicatorColor(@ColorInt int color) {
        binding.indicatorView.setBackgroundTintList(ColorStateList.valueOf(color));
        return this;
    }
    
    @NonNull
    @Override
    public RoundedLockScreenTodoItem setTimeTextColor(@ColorInt int color) {
        binding.timeText.setTextColor(color);
        return this;
    }
    
    @Override
    public void setBackgroundDrawable(@NonNull Drawable drawable) {
        binding.content.setBackground(drawable);
    }
    
    @Override
    public void setBorderSizePX(int sizePX) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) binding.backgroundMain.getLayoutParams();
        params.setMargins(sizePX, sizePX, sizePX, sizePX);
        binding.backgroundMain.setLayoutParams(params);
    }
    
    @Override
    public void setTitleText(@NonNull String text) {
        binding.titleText.setText(text);
    }
    
    @Override
    public void setTitleTextSize(float sizeSP) {
        binding.titleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, sizeSP);
    }
    
    @Override
    public void setTimeSpanText(@NonNull String text) {
        binding.timeText.setText(text);
    }
    
    @Override
    public void setTimeTextSize(float sizeSP) {
        binding.timeText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, sizeSP);
    }
    
    @Override
    public void hideIndicatorAndTime() {
        binding.timeText.setVisibility(View.GONE);
        binding.indicatorView.setVisibility(View.INVISIBLE);
    }
}
