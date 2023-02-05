package prototype.xd.scheduler.utilities;

import static prototype.xd.scheduler.utilities.QueryUtilities.getLong;
import static prototype.xd.scheduler.utilities.QueryUtilities.query;
import static prototype.xd.scheduler.utilities.Static.KEY_SEPARATOR;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.CalendarContract;
import android.text.Spannable;
import android.text.SpannableString;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.SystemCalendar;
import prototype.xd.scheduler.entities.SystemCalendarEvent;
import prototype.xd.scheduler.entities.TodoEntry;

public final class SystemCalendarUtils {
    
    public static final String NAME = SystemCalendarUtils.class.getSimpleName();
    
    private SystemCalendarUtils() throws InstantiationException {
        throw new InstantiationException(NAME);
    }
    
    /**
     * List of columns to read from CalendarContract.Calendars.CONTENT_URI
     */
    public static final List<String> CALENDAR_COLUMNS = List.of(
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.CALENDAR_TIME_ZONE);
    
    /**
     * List of columns to read from Events.CONTENT_URI
     */
    public static final List<String> CALENDAR_EVENT_COLUMNS = List.of(
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
            CalendarContract.Events.ORIGINAL_INSTANCE_TIME);
    
    /**
     * Retrieve all calendars from the system
     *
     * @param context     context, will be used to get a ContentResolver
     * @return a list of system calendars
     */
    @NonNull
    public static List<SystemCalendar> getAllCalendars(@NonNull Context context) {
        ContentResolver resolver = context.getContentResolver();
    
        final int MIN_EVENTS = 10;
        final int MIN_EXCEPTIONS = 16;
    
        try (Cursor cursor = query(contentResolver, Events.CONTENT_URI, CALENDAR_EVENT_COLUMNS.toArray(new String[0]),
                Events.CALENDAR_ID + " = " + id + " AND " + Events.DELETED + " = 0")) {
        
            events = new ArrayList<>(max(cursor.getCount() - MIN_EXCEPTIONS, MIN_EVENTS));
            exceptionLists = new HashMap<>(MIN_EXCEPTIONS);
        
            while (cursor.moveToNext()) {
                long originalInstanceTime = getLong(cursor, CALENDAR_EVENT_COLUMNS, CalendarContract.Events.ORIGINAL_INSTANCE_TIME);
                if (originalInstanceTime == 0) {
                    events.add(new SystemCalendarEvent(cursor, this));
                } else {
                    // if original id is set this event is an exception to some other event
                    exceptionLists.computeIfAbsent(getLong(cursor, CALENDAR_EVENT_COLUMNS, Events.ORIGINAL_ID),
                                    e -> new ArrayList<>())
                            .add(originalInstanceTime);
                }
            }
        }
        
        List<SystemCalendar> systemCalendars;
        
        try (Cursor cursor = query(resolver, CalendarContract.Calendars.CONTENT_URI, CALENDAR_COLUMNS.toArray(new String[0]), null)) {
            systemCalendars = new ArrayList<>(cursor.getCount());
            
            while (cursor.moveToNext()) {
                systemCalendars.add(new SystemCalendar(cursor, resolver));
            }
        }
        
        Logger.info(NAME, "Loaded " + systemCalendars.size() + " calendars");
        return systemCalendars;
    }
    
    public static void addMissingCalendars(@NonNull Context context, @NonNull List<SystemCalendar> calendars) {
        ContentResolver resolver = context.getContentResolver();
        
        List<Long> previousIds = new ArrayList<>(calendars.size());
        for(SystemCalendar cal: calendars) {
            previousIds.add(cal.id);
        }
        List<Long> newIds = new ArrayList<>(calendars.size());
        
        try (Cursor cursor = query(resolver, CalendarContract.Calendars.CONTENT_URI, CALENDAR_COLUMNS.toArray(new String[0]), null)) {
            while (cursor.moveToNext()) {
                long id = getLong(cursor, CALENDAR_COLUMNS, CalendarContract.Calendars._ID);
                newIds.add(id);
                int index = previousIds.indexOf(id);
                if(index != -1) {
                    // calendar already exists
                    SystemCalendar oldCalendar = calendars.get(index);
                    oldCalendar.loadMissingEvents();
                    Logger.info(NAME, "Refreshed " + oldCalendar);
                    continue;
                }
                SystemCalendar newCalendar = new SystemCalendar(cursor, resolver);
                calendars.add(newCalendar);
                Logger.info(NAME, "Added " + newCalendar);
            }
        }
        
        // remove deleted calendars
        calendars.removeIf(calendar -> {
            if(!newIds.contains(calendar.id)) {
                Logger.info(NAME, "Removed " + calendar);
                return true;
            }
            return false;
        });
    }
    
    /**
     * Get TodoEntries from a list of system calendars
     *
     * @param dayStart  minimum day
     * @param dayEnd    maximum day
     * @param calendars list of calendars to get from
     * @param list      list to add entries to
     */
    static void addTodoEntriesFromCalendars(long dayStart, long dayEnd,
                                            @NonNull List<SystemCalendar> calendars,
                                            @NonNull List<TodoEntry> list) {
        int initialSize = list.size();
        for (SystemCalendar calendar : calendars) {
            // add all events from all calendars
            calendar.addVisibleEventsToList(dayStart, dayEnd, list);
        }
        Logger.info(NAME, "Read calendar entries: " + (list.size() - initialSize));
    }
    
    @NonNull
    public static Spannable calendarKeyToReadable(@NonNull Context context, @NonNull String calendarKey) {
        String[] splitKey = calendarKey.split(KEY_SEPARATOR);
        switch (splitKey.length) {
            case 3:
                if (splitKey[0].equals(splitKey[1])) {
                    splitKey[1] = context.getString(R.string.calendar_main);
                }
                return Utilities.colorizeText(context.getString(R.string.editing_system_calendar_color, splitKey[1], splitKey[0]),
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
    
    // FOR DEBUGGING
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
    
    //public static String addSpaces(String input, int len) {
    //    StringBuilder out = new StringBuilder(input);
    //    for (int i = input.length(); i < len; i++) {
    //        out.append(" ");
    //    }
    //    return out.toString();
    //}
    
    
}
