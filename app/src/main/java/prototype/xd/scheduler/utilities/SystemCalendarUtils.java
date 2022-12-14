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

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.SystemCalendar;
import prototype.xd.scheduler.entities.SystemCalendarEvent;
import prototype.xd.scheduler.entities.TodoListEntry;

public class SystemCalendarUtils {
    
    private static final String NAME = "CalendarUtils";
    
    private SystemCalendarUtils() {
        throw new IllegalStateException("System calendar utility class");
    }
    
    public static final List<String> calendarColumns = Collections.unmodifiableList(Arrays.asList(
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.CALENDAR_TIME_ZONE));
    
    public static final List<String> calendarEventsColumns =  Collections.unmodifiableList(Arrays.asList(
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
            CalendarContract.Events.EVENT_TIMEZONE,
            // for exception events
            CalendarContract.Events._ID,
            CalendarContract.Events.ORIGINAL_ID,
            CalendarContract.Events.ORIGINAL_INSTANCE_TIME));
    
    public static List<SystemCalendar> getAllCalendars(Context context, boolean loadMinimal) {
        ContentResolver contentResolver = context.getContentResolver();
        
        List<SystemCalendar> systemCalendars = new ArrayList<>();
        Cursor cursor = query(contentResolver, CalendarContract.Calendars.CONTENT_URI, calendarColumns.toArray(new String[0]), null);
        int calendarCount = cursor.getCount();
        cursor.moveToFirst();
        for (int i = 0; i < calendarCount; i++) {
            systemCalendars.add(new SystemCalendar(cursor, contentResolver, loadMinimal));
            cursor.moveToNext();
        }
        cursor.close();
        log(INFO, NAME, "Loaded " + systemCalendars.size() + " calendars");
        return systemCalendars;
    }
    
    public static List<TodoListEntry> getTodoListEntriesFromCalendars(long dayStart, long dayEnd,
                                                                      @NonNull List<SystemCalendar> calendars) {
        List<TodoListEntry> todoListEntries = new ArrayList<>();
        for (SystemCalendar calendar : calendars) {
            // add all events from all calendars
            for (SystemCalendarEvent event : calendar.getVisibleTodoListEvents(dayStart, dayEnd)) {
                todoListEntries.add(new TodoListEntry(event));
            }
        }
        log(INFO, NAME, "Read calendar entries: " + todoListEntries.size());
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
    
    public static boolean getFirstValidBooleanValue(List<String> calendarSubKeys, String parameterKey, boolean defaultValue) {
        return preferences.getBoolean(getFirstValidKey(calendarSubKeys, parameterKey), defaultValue);
    }
    
    public static int getFirstValidIntValue(List<String> calendarSubKeys, String parameter, int defaultValue) {
        return preferences.getInt(getFirstValidKey(calendarSubKeys, parameter), defaultValue);
    }
    
    public static String getFirstValidKey(List<String> calendarSubKeys, String parameter) {
        int index = getFirstValidKeyIndex(calendarSubKeys, parameter);
        return index == -1 ? parameter : calendarSubKeys.get(index) + "_" + parameter;
    }
    
    //// FOR DEBUGGING
    //public static void printTable(Cursor cursor) {
    //    cursor.moveToFirst();
    //
    //    ArrayList<ArrayList<String>> table = new ArrayList<>();
    //    ArrayList<Integer> column_sizes = new ArrayList<>();
    //    String[] column_names = cursor.getColumnNames();
    //    table.add(new ArrayList<>(Arrays.asList(column_names)));
    //
    //    for (String column_name : column_names) {
    //        column_sizes.add(column_name.length());
    //    }
    //
    //    for (int row = 0; row < cursor.getCount(); row++) {
    //        ArrayList<String> record = new ArrayList<>();
    //        for (int column = 0; column < column_names.length; column++) {
    //            String column_val = cursor.getString(column) + "";
    //            record.add(column_val);
    //            column_sizes.set(column, max(column_sizes.get(column), column_val.length()));
    //        }
    //        table.add(record);
    //        cursor.moveToNext();
    //    }
    //
    //    System.out.println("TABLE DIMENSIONS: " + table.size() + " x " + column_names.length);
    //    System.out.println("TABLE DIMENSIONS_RAW: " + cursor.getCount() + " x " + cursor.getColumnNames().length);
    //
    //    for (int row = 0; row < table.size(); row++) {
    //        for (int column = 0; column < column_names.length; column++) {
    //            System.out.print(addSpaces(table.get(row).get(column), column_sizes.get(column) + 1));
    //        }
    //        System.out.println();
    //    }
    //
    //    cursor.moveToFirst();
    //}
    //
    //public static String addSpaces(String input, int len) {
    //    StringBuilder out = new StringBuilder(input);
    //    for (int i = input.length(); i < len; i++) {
    //        out.append(" ");
    //    }
    //    return out.toString();
    //}
    
    
}
