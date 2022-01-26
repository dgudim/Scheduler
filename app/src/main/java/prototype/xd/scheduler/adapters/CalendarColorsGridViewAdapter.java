package prototype.xd.scheduler.adapters;

import static prototype.xd.scheduler.utilities.SystemCalendarUtils.makeKey;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.SettingsFragment;
import prototype.xd.scheduler.entities.calendars.SystemCalendar;
import prototype.xd.scheduler.views.settings.SystemCalendarSettings;

public class CalendarColorsGridViewAdapter extends BaseAdapter {
    
    private final int calendarColor;
    
    private final ArrayList<Integer> colors;
    private final ArrayList<Integer> color_eventCounts;
    
    private final Context context;
    private final LayoutInflater inflater;
    private final SettingsFragment fragment;
    private final ViewGroup root;
    
    private final SystemCalendar calendar;
    
    public CalendarColorsGridViewAdapter(final Context context, final SettingsFragment fragment, final ViewGroup root, final SystemCalendar calendar) {
        this.fragment = fragment;
        this.root = root;
        colors = calendar.availableEventColors;
        calendarColor = calendar.color;
        color_eventCounts = calendar.eventCountsForColors;
        this.context = context;
        this.calendar = calendar;
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
        
        if (view == null) {
            view = inflater.inflate(R.layout.calendar_color_entry, parent, false);
        }
        view.findViewById(R.id.color).setBackgroundColor(colors.get(i));
        view.findViewById(R.id.title_default).setVisibility(calendarColor == colors.get(i) ? View.VISIBLE : View.GONE);
        ((TextView) view.findViewById(R.id.event_count)).setText(context.getString(R.string.calendar_events, color_eventCounts.get(i)));
        view.findViewById(R.id.settings).setOnClickListener(v ->
                new SystemCalendarSettings(fragment,
                        inflater.inflate(R.layout.entry_settings, root, false),
                        makeKey(calendar, colors.get(i))));
        
        return view;
    }
}
