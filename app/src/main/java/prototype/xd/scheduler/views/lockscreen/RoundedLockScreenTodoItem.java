package prototype.xd.scheduler.views.lockscreen;

import android.util.TypedValue;
import android.view.View;

import androidx.cardview.widget.CardView;

import prototype.xd.scheduler.databinding.RoundedEntryBinding;

public class RoundedLockScreenTodoItem extends LockScreenTodoItemView<RoundedEntryBinding> {
    
    public RoundedLockScreenTodoItem(RoundedEntryBinding binding) {
        super(binding);
    }
    
    @Override
    public void setBackgroundColor(int color) {
        viewBinding.backgroundMain.setCardBackgroundColor(color);
    }
    
    @Override
    public void setBorderColor(int color) {
        viewBinding.backgroundOutline.setCardBackgroundColor(color);
    }
    
    @Override
    public void setTitleTextColor(int color) {
        viewBinding.titleText.setTextColor(color);
    }
    
    @Override
    public void setIndicatorColor(int color) {
        viewBinding.indicatorView.setCardBackgroundColor(color);
    }
    
    @Override
    public void setBorderSize(int sizePX) {
        CardView.LayoutParams params = (CardView.LayoutParams) viewBinding.backgroundMain.getLayoutParams();
        params.setMargins(sizePX, sizePX, sizePX, sizePX);
        viewBinding.backgroundMain.setLayoutParams(params);
    }
    
    @Override
    public void setTitleText(String text) {
        viewBinding.titleText.setText(text);
    }
    
    @Override
    public void setTitleTextSize(float sizeDP) {
        viewBinding.titleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, sizeDP);
    }
    
    @Override
    public void setTimeText(String text) {
        viewBinding.timeText.setText(text);
    }
    
    @Override
    public void setTimeTextSize(float sizeDP) {
        viewBinding.timeText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, sizeDP);
    }
    
    @Override
    public void hideIndicatorAndTime() {
        viewBinding.timeText.setVisibility(View.GONE);
        viewBinding.indicatorView.setVisibility(View.GONE);
    }
}
