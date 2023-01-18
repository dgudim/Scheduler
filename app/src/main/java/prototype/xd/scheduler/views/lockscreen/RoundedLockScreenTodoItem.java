package prototype.xd.scheduler.views.lockscreen;

import android.content.res.ColorStateList;
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
        return viewBinding.backgroundOutline;
    }
    
    @Override
    public void setBackgroundColor(@ColorInt int color) {
        viewBinding.backgroundMain.setCardBackgroundColor(color);
    }
    
    @Override
    public void setBorderColor(@ColorInt int color) {
        viewBinding.backgroundOutline.setCardBackgroundColor(color);
    }
    
    @Override
    public void setTitleTextColor(@ColorInt int color) {
        viewBinding.titleText.setTextColor(color);
    }
    
    @Override
    public void setIndicatorColor(@ColorInt int color) {
        viewBinding.indicatorView.setBackgroundTintList(ColorStateList.valueOf(color));
    }
    
    @Override
    public void setTimeTextColor(@ColorInt int color) {
        viewBinding.timeText.setTextColor(color);
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
    public void setTimeTextSize(float sizeSP) {
        viewBinding.timeText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, sizeSP);
    }
    
    @Override
    public void hideIndicatorAndTime() {
        viewBinding.timeText.setVisibility(View.GONE);
        viewBinding.indicatorView.setVisibility(View.INVISIBLE);
    }
}
