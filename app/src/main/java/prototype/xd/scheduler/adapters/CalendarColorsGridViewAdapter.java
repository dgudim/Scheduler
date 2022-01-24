package prototype.xd.scheduler.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.SettingsFragment;
import prototype.xd.scheduler.calendarUtilities.SystemCalendar;
import prototype.xd.scheduler.entities.Views.settings.SystemCalendarSettings;

public class CalendarColorsGridViewAdapter extends BaseAdapter {
    
    private final int calendarColor;
   
    private final ArrayList<Integer> colors;
    private final ArrayList<Integer> color_eventCounts;
    
    private final Context context;
    private final LayoutInflater inflater;
    private final SettingsFragment fragment;
    private final ViewGroup root;
    
    private final String key;
    
    public CalendarColorsGridViewAdapter(final Context context, final SettingsFragment fragment, final ViewGroup root, final SystemCalendar systemCalendar) {
        this.fragment = fragment;
        this.root = root;
        colors = systemCalendar.availableEventColors;
        calendarColor = systemCalendar.color;
        color_eventCounts = systemCalendar.eventCountsForColors;
        this.context = context;
        key = systemCalendar.account_name + "_" + systemCalendar.name + "_" + systemCalendar.color;
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
        view.findViewById(R.id.settings).setOnClickListener(v -> new SystemCalendarSettings(context, fragment, inflater.inflate(R.layout.entry_settings, root, false), key));
        
        return view;
    }
}
