package prototype.xd.scheduler.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSpinner;

public class Spinner extends AppCompatSpinner {
    private boolean ignoreCheckedChange;
    
    public Spinner(@NonNull Context context) {
        super(context);
    }
    
    public Spinner(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }
    
    public Spinner(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    
    @Override
    public void setOnItemSelectedListener(@Nullable final OnItemSelectedListener listener) {
        if (listener == null) {
            super.setOnItemSelectedListener(null);
            return;
        }
        super.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (ignoreCheckedChange) {
                    return;
                }
                listener.onItemSelected(parent, view, position, id);
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                listener.onNothingSelected(parent);
            }
        });
    }
    
    public void setSelectionSilent(int position) {
        ignoreCheckedChange = true;
        setSelection(position, false);
        ignoreCheckedChange = false;
    }
}
