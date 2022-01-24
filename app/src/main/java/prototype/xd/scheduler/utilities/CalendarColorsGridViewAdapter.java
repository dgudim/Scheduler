package prototype.xd.scheduler.utilities;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.calendarUtilities.SystemCalendar;

public class CalendarColorsGridViewAdapter extends BaseAdapter {
    
    private final ArrayList<Integer> colors;
    private final LayoutInflater inflater;
    
    public CalendarColorsGridViewAdapter(Context context, SystemCalendar systemCalendar) {
        colors = systemCalendar.availableEventColors;
        inflater = LayoutInflater.from(context);
    }
    
    @Override
    public int getCount() {
        return colors.size();
    }
    
    @Override
    public Object getItem(int i) {
        return colors.get(i);
    }
    
    @Override
    public long getItemId(int i) {
        return i;
    }
    
    @Override
    public View getView(int i, View view, ViewGroup parent) {
        
        if(view == null){
            view = inflater.inflate(R.layout.calendar_color_entry, parent, false);
        }
        view.findViewById(R.id.color).setBackgroundColor(colors.get(i));
        
        return view;
    }
}
