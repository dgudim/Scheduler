package prototype.xd.scheduler.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.materialswitch.MaterialSwitch;

public class Switch extends MaterialSwitch {
    private boolean programmaticChange = false;
    
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
    public void setOnCheckedChangeListener(final OnCheckedChangeListener listener) {
        super.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (programmaticChange) {
                return;
            }
            listener.onCheckedChanged(buttonView, isChecked);
        });
    }
    
    public void setOnCheckedChangeListener(final OnSilentCheckedChangeListener listener) {
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
        void onCheckedChanged(CompoundButton buttonView, boolean isChecked, boolean fromUser);
    }
}
