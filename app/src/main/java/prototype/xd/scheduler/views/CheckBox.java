package prototype.xd.scheduler.views;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.google.android.material.checkbox.MaterialCheckBox;

public class CheckBox extends MaterialCheckBox {
    private boolean ignoreCheckedChange;
    
    public CheckBox(Context context) {
        super(context);
    }
    
    public CheckBox(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }
    
    public CheckBox(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
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
