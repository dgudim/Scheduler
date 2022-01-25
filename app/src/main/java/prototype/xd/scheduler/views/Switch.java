package prototype.xd.scheduler.views;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class Switch extends SwitchMaterial {
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
    
    public void setChecked(boolean checked, boolean notify) {
        ignoreCheckedChange = !notify;
        setChecked(checked);
        ignoreCheckedChange = false;
    }
}
