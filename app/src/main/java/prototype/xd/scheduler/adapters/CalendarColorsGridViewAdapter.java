package prototype.xd.scheduler.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.collection.ArrayMap;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.SystemCalendar;
import prototype.xd.scheduler.views.settings.SystemCalendarSettings;

public class CalendarColorsGridViewAdapter extends BaseAdapter {
    
    private final int calendarColor;
    
    private final ArrayMap<Integer, Integer> eventColorCountMap;
    
    private final SystemCalendarSettings systemCalendarSettings;
    
    private final SystemCalendar calendar;
    
    public CalendarColorsGridViewAdapter(final SystemCalendarSettings systemCalendarSettings, final SystemCalendar calendar) {
        this.systemCalendarSettings = systemCalendarSettings;
        eventColorCountMap = calendar.eventColorCountMap;
        calendarColor = calendar.color;
        this.calendar = calendar;
    }
    
    @Override
    public int getCount() {
        return eventColorCountMap.size();
    }
    
    @Override
    public Object getItem(int i) {
        return eventColorCountMap.keyAt(i);
    }
    
    @Override
    public long getItemId(int i) {
        return i;
    }
    
    @Override
    public View getView(int i, View view, ViewGroup parent) {
        
        if (view == null) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.calendar_color_entry, parent, false);
        }
        
        int color = eventColorCountMap.keyAt(i);
        int count = eventColorCountMap.valueAt(i);
        
        ((CardView) view.findViewById(R.id.color)).setCardBackgroundColor(color);
        view.findViewById(R.id.title_default).setVisibility(calendarColor == color ? View.VISIBLE : View.GONE);
        ((TextView) view.findViewById(R.id.event_count)).setText(view.getContext().getString(R.string.calendar_events, count));
        view.findViewById(R.id.settings).setOnClickListener(v -> systemCalendarSettings.show(calendar.makeKey(color)));
        
        return view;
    }
}
