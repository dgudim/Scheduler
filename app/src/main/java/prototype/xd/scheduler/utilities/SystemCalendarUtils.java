package prototype.xd.scheduler.utilities;

import static android.util.Log.INFO;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;
import static prototype.xd.scheduler.utilities.QueryUtilities.query;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.CalendarContract;
import android.text.Spannable;
import android.text.SpannableString;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.entities.calendars.SystemCalendar;
import prototype.xd.scheduler.entities.calendars.SystemCalendarEvent;

public class SystemCalendarUtils {
    
    private SystemCalendarUtils() {
        throw new IllegalStateException("System calendar utility class");
    }
    
    public static final List<String> calendarColumns = new ArrayList<>(Arrays.asList(
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.CALENDAR_TIME_ZONE));
    
    public static final List<String> calendarEventsColumns = new ArrayList<>(Arrays.asList(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DISPLAY_COLOR,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.DURATION,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.RRULE,
            CalendarContract.Events.RDATE,
            CalendarContract.Events.EXRULE,
            CalendarContract.Events.EXDATE,
            CalendarContract.Events.DELETED,
            CalendarContract.Events.EVENT_TIMEZONE));
    
    public static List<SystemCalendar> getAllCalendars(Context context, boolean loadMinimal) {
        ContentResolver contentResolver = context.getContentResolver();
        
        List<SystemCalendar> systemCalendars = new ArrayList<>();
        Cursor cursor = query(contentResolver, CalendarContract.Calendars.CONTENT_URI, calendarColumns.toArray(new String[0]), null);
        int calendars = cursor.getCount();
        cursor.moveToFirst();
        for (int i = 0; i < calendars; i++) {
            systemCalendars.add(new SystemCalendar(cursor, contentResolver, loadMinimal));
            cursor.moveToNext();
        }
        cursor.close();
        return systemCalendars;
    }
    
    public static List<TodoListEntry> getTodoListEntriesFromCalendars(Context context, long dayStart, long dayEnd,
                                                                      @Nullable List<SystemCalendar> calendars) {
        List<TodoListEntry> todoListEntries = new ArrayList<>();
        List<SystemCalendar> cals = (calendars != null) ? calendars : getAllCalendars(context, false);
        for (SystemCalendar calendar : cals) {
            todoListEntries.addAll(calendar.getVisibleTodoListEntries(dayStart, dayEnd));
        }
        log(INFO, "CalendarUtils", "read calendar entries: " + todoListEntries.size());
        return todoListEntries;
    }
    
    public static List<String> generateSubKeysFromKey(String calendarKey) {
        List<String> calendarSubKeys = new ArrayList<>();
        String[] splitKey = calendarKey.split("_");
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < splitKey.length; i++) {
            if (i != 0) {
                buffer.append('_');
            }
            buffer.append(splitKey[i]);
            calendarSubKeys.add(buffer.toString());
        }
        return calendarSubKeys;
    }
    
    public static Spannable calendarKeyToReadable(Context context, String calendarKey) {
        String[] splitKey = calendarKey.split("_");
        switch (splitKey.length) {
            case 3:
                if (splitKey[0].equals(splitKey[1])) {
                    splitKey[1] = context.getString(R.string.calendar_main);
                }
                return Utilities.colorize(context.getString(R.string.editing_system_calendar_color, splitKey[1], splitKey[0]),
                        "■", Integer.parseInt(splitKey[2]));
            case 2:
                if (splitKey[0].equals(splitKey[1])) {
                    splitKey[1] = context.getString(R.string.calendar_main);
                }
                return new SpannableString(context.getString(R.string.editing_system_calendar, splitKey[1], splitKey[0]));
            case 1:
            default:
                return new SpannableString(calendarKey);
        }
    }
    
    public static int getFirstValidKeyIndex(List<String> calendarSubKeys, String parameter) {
        for (int i = calendarSubKeys.size() - 1; i >= 0; i--) {
            try {
                if (preferences.getString(calendarSubKeys.get(i) + "_" + parameter, null) != null) {
                    return i;
                }
            } catch (ClassCastException e) {
                return i;
            }
        }
        return -1;
    }
    
    public static String getFirstValidKey(List<String> calendarSubKeys, String parameter) {
        int index = getFirstValidKeyIndex(calendarSubKeys, parameter);
        return index == -1 ? parameter : calendarSubKeys.get(index) + "_" + parameter;
    }
    
    public static String makeKey(SystemCalendar calendar) {
        return calendar.account_name + "_" + calendar.name;
    }
    
    public static String makeKey(SystemCalendar calendar, int possibleEventColor) {
        return makeKey(calendar) + "_" + possibleEventColor;
    }
    
    public static String makeKey(SystemCalendarEvent event) {
        return makeKey(event.associatedCalendar, event.color);
    }
    
}
