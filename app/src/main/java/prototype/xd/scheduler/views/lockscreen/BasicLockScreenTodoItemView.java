package prototype.xd.scheduler.views.lockscreen;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import prototype.xd.scheduler.databinding.BasicEntryBinding;
import prototype.xd.scheduler.utilities.ImageUtilities;

public class BasicLockScreenTodoItemView extends LockScreenTodoItemView<BasicEntryBinding> {
    
    public BasicLockScreenTodoItemView(@NonNull BasicEntryBinding binding) {
        super(binding);
    }
    
    @NonNull
    @Override
    public View getClickableRoot() {
        return binding.backgroundOutline;
    }
    
    @NonNull
    @Override
    public BasicLockScreenTodoItemView setBackgroundColor(@ColorInt int color) {
        binding.backgroundMain.setBackgroundColor(color);
        return this;
    }
    
    @NonNull
    @Override
    public BasicLockScreenTodoItemView setBorderColor(@ColorInt int color) {
        binding.backgroundOutline.setBackgroundColor(color);
        return this;
    }
    
    @NonNull
    @Override
    public BasicLockScreenTodoItemView setTitleTextColor(@ColorInt int color) {
        binding.titleText.setTextColor(color);
        return this;
    }
    
    @NonNull
    @Override
    public BasicLockScreenTodoItemView setIndicatorColor(@ColorInt int color) {
        binding.indicatorView.setBackgroundColor(color);
        return this;
    }
    
    @NonNull
    @Override
    public BasicLockScreenTodoItemView setTimeTextColor(@ColorInt int color) {
        binding.timeText.setTextColor(color);
        return this;
    }
    
    @Override
    public void setBackgroundDrawable(@NonNull Drawable drawable) {
        binding.backgroundMain.setBackground(drawable);
    }
    
    @Override
    void setBackgroundBitmap(@NonNull Bitmap bgBitmap) {
        setBackgroundDrawable(new BitmapDrawable(binding.getRoot().getResources(), ImageUtilities.makeMutable(bgBitmap)));
    }
    
    @Override
    public void setBorderSizePX(int sizePX) {
        binding.backgroundOutline.setPadding(sizePX, sizePX, sizePX, sizePX);
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
        binding.indicatorView.setVisibility(View.GONE);
    }
}
