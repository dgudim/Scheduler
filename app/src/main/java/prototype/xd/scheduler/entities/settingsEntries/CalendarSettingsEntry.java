package prototype.xd.scheduler.entities.settingsEntries;

import static prototype.xd.scheduler.MainActivity.preferences;
import static prototype.xd.scheduler.entities.settingsEntries.SettingsEntryType.CALENDAR;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.makeKey;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.TextView;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.SettingsFragment;
import prototype.xd.scheduler.adapters.CalendarColorsGridViewAdapter;
import prototype.xd.scheduler.entities.calendars.SystemCalendar;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.views.CheckBox;

public class CalendarSettingsEntry extends SettingsEntry {
    
    private final Context context;
    private final LayoutInflater inflater;
    private final ViewGroup root;
    private final SettingsFragment fragment;
    private final String calendarName;
    private final String calendarKey;
    private final int calendarEventsCount;
    private final int calendarColor;
    
    private CalendarColorsGridViewAdapter gridViewAdapter;
    
    public CalendarSettingsEntry(final SettingsFragment fragment, final SystemCalendar calendar) {
        super(R.layout.calendar_entry);
        this.context = fragment.context;
        inflater = LayoutInflater.from(context);
        this.root = fragment.rootViewGroup;
        this.fragment = fragment;
        this.calendarName = calendar.name;
        calendarKey = makeKey(calendar);
        this.calendarColor = calendar.color;
        calendarEventsCount = calendar.systemCalendarEvents.size();
        entryType = CALENDAR;
        
        if (calendar.availableEventColors.size() > 0 && calendar.systemCalendarEvents.size() > 0) {
            gridViewAdapter = new CalendarColorsGridViewAdapter(context, fragment, calendar);
        }
    }
    
    @Override
    protected View InitInnerViews(View rootView) {
        ((TextView) rootView.findViewById(R.id.calendar_name)).setText(calendarName);
        ((TextView) rootView.findViewById(R.id.event_count)).setText(context.getString(R.string.calendar_events_full, calendarEventsCount));
        CheckBox checkBox = rootView.findViewById(R.id.check_box);
        checkBox.setButtonTintList(ColorStateList.valueOf(calendarColor));
        checkBox.setChecked(preferences.getBoolean(calendarKey + "_" + Keys.VISIBLE, Keys.CALENDAR_SETTINGS_DEFAULT_VISIBLE), false);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                preferences.edit().putBoolean(calendarKey + "_" + Keys.VISIBLE, isChecked).apply());
        
        rootView.findViewById(R.id.edit_button).setOnClickListener(view -> fragment.calendarSettingsDialogue.show(calendarKey));
        
        View colorSelector = rootView.findViewById(R.id.color_select_button);
        if (gridViewAdapter != null) {
            colorSelector.setVisibility(View.VISIBLE);
            colorSelector.setOnClickListener(v -> {
                final AlertDialog.Builder alert = new AlertDialog.Builder(context);
                
                View view = inflater.inflate(R.layout.grid_selection_view, root, false);
                GridView gridView = view.findViewById(R.id.grid_view);
                gridView.setNumColumns(5);
                gridView.setHorizontalSpacing(5);
                gridView.setVerticalSpacing(5);
                gridView.setAdapter(gridViewAdapter);
                
                alert.setView(view);
                alert.show();
            });
        } else {
            colorSelector.setVisibility(View.GONE);
        }
        
        return super.InitInnerViews(rootView);
    }
}
