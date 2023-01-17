package prototype.xd.scheduler.adapters;

import static prototype.xd.scheduler.utilities.Utilities.getPluralString;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.collection.ArrayMap;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.SystemCalendar;
import prototype.xd.scheduler.views.settings.SystemCalendarSettings;

/**
 * Grid adapter class for displaying per color event settings
 */
public class CalendarColorsGridViewAdapter extends BaseAdapter {
    
    // base calendar color (the default one)
    private final int calendarColor;
    
    // event color to event count map
    @NonNull
    private final ArrayMap<Integer, Integer> eventColorCountMap;
    
    @NonNull
    private final SystemCalendarSettings systemCalendarSettings;
    
    @NonNull
    private final SystemCalendar calendar;
    
    public CalendarColorsGridViewAdapter(@NonNull final SystemCalendarSettings systemCalendarSettings,
                                         @NonNull final SystemCalendar calendar) {
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
    
    @NonNull
    @Override
    public View getView(int i, @Nullable View view, @NonNull ViewGroup parent) {
        
        if (view == null) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.calendar_color_entry, parent, false);
        }
        
        int color = eventColorCountMap.keyAt(i);
        int count = eventColorCountMap.valueAt(i);
        
        ((CardView) view.findViewById(R.id.color)).setCardBackgroundColor(color);
        view.findViewById(R.id.title_default).setVisibility(calendarColor == color ? View.VISIBLE : View.GONE);
        ((TextView) view.findViewById(R.id.event_count)).setText(getPluralString(view.getContext(), R.plurals.calendar_event_count, count));
        view.findViewById(R.id.open_settings_button).setOnClickListener(v -> systemCalendarSettings.show(calendar.makePrefKey(color), color));
        
        return view;
    }
}
