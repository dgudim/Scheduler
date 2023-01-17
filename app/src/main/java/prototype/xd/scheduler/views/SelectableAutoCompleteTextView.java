package prototype.xd.scheduler.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;

public class SelectableAutoCompleteTextView extends MaterialAutoCompleteTextView {
    
    int selectedItemPosition = ListView.INVALID_POSITION;
    
    public SelectableAutoCompleteTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }
    
    /**
     * A small workaround wrapper to set selected item programmatically (MaterialAutoCompleteTextView's selection does not work properly)
     * does not fire "onItemClicked"
     * @param position item position
     */
    public void setSelectedItem(int position) {
        setListSelection(position);
        selectedItemPosition = position;
        setText(getAdapter().getItem(position).toString(), false);
    }
    
    public void setNewItemNames(@NonNull String[] items) {
        setSimpleItems(items);
        setSelectedItem(selectedItemPosition);
    }
    
    public int getSelectedItem() {
        return selectedItemPosition;
    }
    
    @Override
    public void setOnItemClickListener(@Nullable AdapterView.OnItemClickListener l) {
        super.setOnItemClickListener((parent, view, position, id) -> {
            selectedItemPosition = position;
            if(l != null) {
                l.onItemClick(parent, view, position, id);
            }
        });
    }
}
