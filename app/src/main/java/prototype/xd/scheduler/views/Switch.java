package prototype.xd.scheduler.views;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.materialswitch.MaterialSwitch;

public class Switch extends MaterialSwitch {
    private boolean ignoreCheckedChange = false;
    
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
            if (ignoreCheckedChange) {
                return;
            }
            listener.onCheckedChanged(buttonView, isChecked);
        });
    }
    
    public void setCheckedSilent(boolean checked) {
        if (isChecked() != checked) {
            ignoreCheckedChange = true;
            setChecked(checked);
            ignoreCheckedChange = false;
            jumpDrawablesToCurrentState();
        }
    }
}
