package prototype.xd.scheduler.utilities;

import static android.provider.CalendarContract.Calendars;
import static android.provider.CalendarContract.Events;
import static prototype.xd.scheduler.utilities.QueryUtilities.getLong;
import static prototype.xd.scheduler.utilities.QueryUtilities.query;
import static prototype.xd.scheduler.utilities.Static.KEY_SEPARATOR;
import static prototype.xd.scheduler.utilities.Utilities.processDifference;
import static prototype.xd.scheduler.utilities.Utilities.remapMap;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.text.Spannable;
import android.text.SpannableString;

import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.SystemCalendar;
import prototype.xd.scheduler.entities.SystemCalendarData;
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
            Calendars.CALENDAR_DISPLAY_NAME,
            Calendars.ACCOUNT_NAME,
            Calendars.ACCOUNT_TYPE,
            Calendars._ID,
            Calendars.CALENDAR_ACCESS_LEVEL,
            Calendars.CALENDAR_COLOR,
            Calendars.CALENDAR_TIME_ZONE);
    
    /**
     * List of columns to read from Events.CONTENT_URI
     */
    public static final List<String> CALENDAR_EVENT_COLUMNS = List.of(
            Events._ID,
            Events.CALENDAR_ID,
            Events.TITLE,
            Events.DISPLAY_COLOR,
            Events.DTSTART,
            Events.DTEND,
            Events.DURATION,
            Events.ALL_DAY,
            Events.RRULE,
            Events.RDATE,
            Events.EXRULE,
            Events.EXDATE,
            Events.EVENT_TIMEZONE,
            // for exception events
            Events.ORIGINAL_ID,
            Events.ORIGINAL_INSTANCE_TIME,
            Events.STATUS);
    
    /**
     * Retrieve all calendars from the system
     *
     * @param context context, will be used to get a ContentResolver
     * @return a list of system calendars
     */
    @NonNull
    public static Map<Long, SystemCalendar> loadCalendars(@NonNull Context context, @NonNull Map<Long, SystemCalendar> calendars) {
        long start = System.currentTimeMillis(); // NOSONAR
        
        ContentResolver resolver = context.getContentResolver();
        
        Map<Long, SystemCalendarData> calendarDataMap;
        
        try (Cursor cursor = query(resolver, Calendars.CONTENT_URI, CALENDAR_COLUMNS.toArray(new String[0]), null)) {
            calendarDataMap = new ArrayMap<>(cursor.getCount());
            
            while (cursor.moveToNext()) {
                SystemCalendarData data = new SystemCalendarData(cursor);
                calendarDataMap.put(data.id, data);
            }
        }
        
        try (Cursor cursor = query(resolver, Events.CONTENT_URI, CALENDAR_EVENT_COLUMNS.toArray(new String[0]),
                Events.DELETED + " = 0")) {
            
            while (cursor.moveToNext()) {
                
                long eventCalendarId = getLong(cursor, CALENDAR_EVENT_COLUMNS, Events.CALENDAR_ID);
                SystemCalendarData calendarData = calendarDataMap.get(eventCalendarId);
                if (calendarData == null) {
                    Logger.warning(NAME, "unknown calendar id: " + eventCalendarId);
                    continue;
                }
                
                long originalInstanceTime = getLong(cursor, CALENDAR_EVENT_COLUMNS, Events.ORIGINAL_INSTANCE_TIME);
                if (originalInstanceTime == 0) {
                    calendarData.addEventData(cursor);
                } else {
                    // if original id is set this event is an exception to some other event
                    calendarData.addExceptionToEvent(cursor, originalInstanceTime);
                }
            }
        }
        
        final int[] deleted = {0};
        final int[] updated = {0};
        final int[] skipped = {0};
        final int[] added = {0};
        
        processDifference(remapMap(calendars, calendar -> calendar.data), calendarDataMap,
                (newElement, elementState) -> {
                    SystemCalendar calendar;
                    switch (elementState) {
                        case NEW:
                            calendar = new SystemCalendar(newElement.second);
                            calendars.put(newElement.first, calendar);
                            Logger.info(NAME, "Added " + calendar);
                            added[0]++;
                            break;
                        case DELETED:
                            calendar = Objects.requireNonNull(calendars.get(newElement.first)).unlinkAllTodoEntries();
                            Logger.info(NAME, "Removed " + calendar);
                            deleted[0]++;
                            break;
                        case MODIFIED:
                            calendar = Objects.requireNonNull(calendars.get(newElement.first));
                            calendar.setNewData(newElement.second);
                            Logger.info(NAME, "Updated " + calendar);
                            updated[0]++;
                            break;
                        case NOT_MODIFIED:
                            Logger.info(NAME, "Skipped " + calendars.get(newElement.first));
                            skipped[0]++;
                    }
                });
        
        Logger.infoWithTime(NAME,
                "Added: " + added[0] + " | updated: " + updated[0] + " | skipped: " + skipped[0] + " | deleted: " + deleted[0] + " {time}", start);
        return calendars;
    }
    
    /**
     * Get TodoEntries from a list of system calendars
     *
     * @param dayStart  minimum day
     * @param dayEnd    maximum day
     * @param calendars list of calendars to get entries from
     * @param list      list to add entries to
     */
    static void addTodoEntriesFromCalendars(long dayStart, long dayEnd,
                                            @NonNull Collection<SystemCalendar> calendars,
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
//    public static void printTable(Cursor cursor) {
//        cursor.moveToFirst();
//
//        ArrayList<ArrayList<String>> table = new ArrayList<>();
//        ArrayList<Integer> column_sizes = new ArrayList<>();
//        String[] column_names = cursor.getColumnNames();
//        table.add(new ArrayList<>(Arrays.asList(column_names)));
//
//        for (String column_name : column_names) {
//            column_sizes.add(column_name.length());
//        }
//
//        for (int row = 0; row < cursor.getCount(); row++) {
//            ArrayList<String> record = new ArrayList<>();
//            for (int column = 0; column < column_names.length; column++) {
//                String column_val = cursor.getString(column) + "";
//                record.add(column_val);
//                column_sizes.set(column, max(column_sizes.get(column), column_val.length()));
//            }
//            table.add(record);
//            cursor.moveToNext();
//        }
//
//        System.out.println("TABLE DIMENSIONS: " + table.size() + " x " + column_names.length);
//        System.out.println("TABLE DIMENSIONS_RAW: " + cursor.getCount() + " x " + cursor.getColumnNames().length);
//
//        for (int row = 0; row < table.size(); row++) {
//            for (int column = 0; column < column_names.length; column++) {
//                System.out.print(addSpaces(table.get(row).get(column), column_sizes.get(column) + 1));
//            }
//            System.out.println();
//        }
//
//        cursor.moveToFirst();
//    }
//
//    public static String addSpaces(String input, int len) {
//        StringBuilder out = new StringBuilder(input);
//        for (int i = input.length(); i < len; i++) {
//            out.append(" ");
//        }
//        return out.toString();
//    }
    
    
}
