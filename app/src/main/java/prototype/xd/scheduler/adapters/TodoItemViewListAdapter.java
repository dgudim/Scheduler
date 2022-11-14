package prototype.xd.scheduler.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import prototype.xd.scheduler.views.lockscreen.LockScreenTodoItemView;

public class TodoItemViewListAdapter extends BaseAdapter {
    
    @Override
    public int getCount() {
        return LockScreenTodoItemView.TodoItemViewType.values().length;
    }
    
    @Override
    public Object getItem(int position) {
        return LockScreenTodoItemView.TodoItemViewType.values()[position];
    }
    
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return LockScreenTodoItemView.inflateViewByType((LockScreenTodoItemView.TodoItemViewType)getItem(position),
                parent, LayoutInflater.from(parent.getContext())).getRoot();
    }
}
