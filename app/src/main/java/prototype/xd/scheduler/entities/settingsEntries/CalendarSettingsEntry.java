package prototype.xd.scheduler.entities.settingsEntries;

import static prototype.xd.scheduler.MainActivity.preferences;
import static prototype.xd.scheduler.entities.settingsEntries.SettingsEntryType.CALENDAR;

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
import prototype.xd.scheduler.calendarUtilities.SystemCalendar;
import prototype.xd.scheduler.entities.Views.CheckBox;
import prototype.xd.scheduler.utilities.CalendarColorsGridViewAdapter;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.SystemCalendarSettings;

public class CalendarSettingsEntry extends SettingsEntry {
    
    private final Context context;
    private final ViewGroup root;
    private final SettingsFragment fragment;
    private final String calendarName;
    private final String calendarKey;
    private final int calendarColor;
    
    private final CalendarColorsGridViewAdapter gridViewAdapter;
    
    public CalendarSettingsEntry(final Context context, final ViewGroup root, final SettingsFragment fragment, SystemCalendar calendar) {
        super(R.layout.calendar_entry);
        this.context = context;
        this.root = root;
        this.fragment = fragment;
        this.calendarName = calendar.name;
        calendarKey = calendar.account_name + "_" + calendarName;
        this.calendarColor = calendar.color;
        entryType = CALENDAR;
        
        gridViewAdapter = new CalendarColorsGridViewAdapter(context, calendar);
    }
    
    @Override
    protected View InitInnerViews(View rootView) {
        ((TextView) rootView.findViewById(R.id.calendar_name)).setText(calendarName);
        CheckBox checkBox = rootView.findViewById(R.id.check_box);
        checkBox.setButtonTintList(ColorStateList.valueOf(calendarColor));
        checkBox.setChecked(preferences.getBoolean(calendarKey + "_" + Keys.VISIBLE, Keys.CALENDAR_SETTINGS_DEFAULT_VISIBLE), false);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(calendarKey + "_" + Keys.VISIBLE, isChecked).apply();
        });
        rootView.findViewById(R.id.edit_button).setOnClickListener(v -> new SystemCalendarSettings(context, fragment, LayoutInflater.from(context).inflate(R.layout.entry_settings, root, false), calendarKey));
        rootView.findViewById(R.id.color_selector).setOnClickListener(v -> {
            final AlertDialog.Builder alert = new AlertDialog.Builder(context);
            
            View view = LayoutInflater.from(context).inflate(R.layout.grid_color_select, root, false);
            GridView gridView = view.findViewById(R.id.grid_view);
            gridView.setNumColumns(5);
            gridView.setHorizontalSpacing(0);
            gridView.setVerticalSpacing(0);
            gridView.setAdapter(gridViewAdapter);
            
            alert.setView(view);
            alert.show();
        });
        return super.InitInnerViews(rootView);
    }
}
