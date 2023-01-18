package prototype.xd.scheduler.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.materialswitch.MaterialSwitch;

public class Switch extends MaterialSwitch {
    private boolean programmaticChange;
    
    public Switch(@NonNull Context context) {
        super(context);
    }
    
    public Switch(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }
    
    public Switch(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    
    @Override
    public void setOnCheckedChangeListener(@Nullable final OnCheckedChangeListener listener) {
        if (listener == null) {
            super.setOnCheckedChangeListener(null);
            return;
        }
        super.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (programmaticChange) {
                return;
            }
            listener.onCheckedChanged(buttonView, isChecked);
        });
    }
    
    public void setOnCheckedChangeListener(@Nullable final OnSilentCheckedChangeListener listener) {
        if (listener == null) {
            super.setOnCheckedChangeListener(null);
            return;
        }
        super.setOnCheckedChangeListener((buttonView, isChecked) ->
                listener.onCheckedChanged(buttonView, isChecked, !programmaticChange));
    }
    
    public void setCheckedSilent(boolean checked) {
        if (isChecked() != checked) {
            programmaticChange = true;
            setChecked(checked);
            programmaticChange = false;
            jumpDrawablesToCurrentState();
        }
    }
    
    @FunctionalInterface
    public interface OnSilentCheckedChangeListener {
        void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked, boolean fromUser);
    }
}
