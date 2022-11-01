package prototype.xd.scheduler.adapters;

import static prototype.xd.scheduler.utilities.SystemCalendarUtils.makeKey;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import java.util.List;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.SettingsFragment;
import prototype.xd.scheduler.entities.calendars.SystemCalendar;

public class CalendarColorsGridViewAdapter extends BaseAdapter {
    
    private final int calendarColor;
    
    private final List<Integer> colors;
    private final List<Integer> color_eventCounts;
    
    private final SettingsFragment fragment;
    
    private final SystemCalendar calendar;
    
    public CalendarColorsGridViewAdapter(final SettingsFragment fragment, final SystemCalendar calendar) {
        this.fragment = fragment;
        colors = calendar.availableEventColors;
        calendarColor = calendar.color;
        color_eventCounts = calendar.eventCountsForColors;
        this.calendar = calendar;
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
        
        if (view == null) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.calendar_color_entry, parent, false);
        }
        ((CardView) view.findViewById(R.id.color)).setCardBackgroundColor(colors.get(i));
        view.findViewById(R.id.title_default).setVisibility(calendarColor == colors.get(i) ? View.VISIBLE : View.GONE);
        ((TextView) view.findViewById(R.id.event_count)).setText(view.getContext().getString(R.string.calendar_events, color_eventCounts.get(i)));
        view.findViewById(R.id.settings).setOnClickListener(v -> fragment.calendarSettingsDialogue.show(makeKey(calendar, colors.get(i))));
        
        return view;
    }
}
