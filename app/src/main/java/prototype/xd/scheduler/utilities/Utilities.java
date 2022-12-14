package prototype.xd.scheduler.utilities;

import static android.util.Log.ERROR;
import static android.util.Log.INFO;
import static prototype.xd.scheduler.utilities.Keys.ENTRIES_FILE;
import static prototype.xd.scheduler.utilities.Keys.GROUPS_FILE;
import static prototype.xd.scheduler.utilities.Keys.ROOT_DIR;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getAllCalendars;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getTodoListEntriesFromCalendars;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.slider.Slider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.GroupList;
import prototype.xd.scheduler.entities.SystemCalendar;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.views.Switch;
import prototype.xd.scheduler.views.settings.PopupSettingsView;

@SuppressWarnings({"unchecked"})
public class Utilities {
    
    private static final String NAME = "Utilities";
    
    public static String getRootDir() {
        return preferences.getString(ROOT_DIR, "");
    }
    
    public static File getFile(String filename) {
        return new File(getRootDir(), filename);
    }
    
    private Utilities() {
        throw new IllegalStateException("Utility class");
    }
    
    public static String nullWrapper(String str) {
        return str == null ? "" : str.trim();
    }
    
    public static boolean isVerticalOrientation(Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }
    
    public static <T extends Exception> void throwOnFalse(boolean result, String message, Class<T> exceptionClass)
            throws T, IllegalArgumentException {
        if (!result) {
            try {
                throw exceptionClass.getConstructor(String.class).newInstance(message);
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
                throw new IllegalArgumentException("Error throwing exception");
            }
        }
    }
    
    public static List<TodoListEntry> loadTodoEntries(Context context,
                                                      long dayStart, long dayEnd,
                                                      GroupList groups,
                                                      @Nullable List<SystemCalendar> calendars,
                                                      boolean attachGroupToEntry) {
        
        List<TodoListEntry> readEntries = new ArrayList<>();
        try {
            readEntries = loadObject(ENTRIES_FILE);
            
            int id = 0;
            for (TodoListEntry entry : readEntries) {
                // post deserialize
                entry.initGroupAndId(groups, id++, attachGroupToEntry);
            }
            
            log(INFO, NAME, "Read todo list: " + readEntries.size());
        } catch (IOException e) {
            log(INFO, NAME, "No todo list");
        } catch (Exception e) {
            logException(NAME, e);
        }
        
        readEntries.addAll(getTodoListEntriesFromCalendars(
                dayStart, dayEnd,
                calendars == null ? getAllCalendars(context, false) : calendars));
        return readEntries;
    }
    
    public static void saveEntries(List<TodoListEntry> entries) {
        try {
            List<TodoListEntry> entriesToSave = new ArrayList<>();
            
            for (int i = 0; i < entries.size(); i++) {
                TodoListEntry entry = entries.get(i);
                if (!entry.isFromSystemCalendar()) {
                    entriesToSave.add(entry);
                }
            }
            
            saveObject(ENTRIES_FILE, entriesToSave);
            
            log(INFO, NAME, "Saved todo list");
        } catch (IOException e) {
            log(ERROR, NAME, "Missing permission, failed to save todo list");
        } catch (Exception e) {
            logException(NAME, e);
        }
    }
    
    public static GroupList loadGroups() {
        GroupList groups = new GroupList();
        groups.add(Group.NULL_GROUP); // add "null" group
        try {
            groups.addAll(loadObject(GROUPS_FILE));
            return groups;
        } catch (IOException e) {
            log(INFO, NAME, "No groups file, creating one");
            saveGroups(groups);
        } catch (Exception e) {
            logException(NAME, e);
        }
        return groups;
    }
    
    public static void saveGroups(GroupList groups) {
        try {
            
            GroupList groupsToSave = new GroupList();
            
            for (Group group : groups) {
                if (!group.isNullGroup()) {
                    groupsToSave.add(group);
                }
            }
            
            saveObject(GROUPS_FILE, groupsToSave);
            log(INFO, NAME, "Saved group list");
        } catch (IOException e) {
            logException(NAME, e);
        }
    }
    
    public static <T> T loadObject(String fileName) throws IOException, ClassNotFoundException {
        Object object;
        try (ObjectInputStream s = new ObjectInputStream(new FileInputStream(getFile(fileName)))) {
            object = s.readObject();
        }
        return (T) object;
    }
    
    public static void saveObject(String fileName, Object object) throws IOException {
        try (ObjectOutputStream s = new ObjectOutputStream(new FileOutputStream(getFile(fileName)))) {
            s.writeObject(object);
        }
    }
    
    public static <T extends Fragment> T findFragmentInNavHost(FragmentActivity activity, Class<T> targetFragmentClass) {
        List<Fragment> fragments = Objects.requireNonNull(activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment))
                .getChildFragmentManager()
                .getFragments();
        for (Fragment fragment : fragments) {
            if (targetFragmentClass.isInstance(fragment)) {
                return (T) fragment;
            }
        }
        throw new NullPointerException("Fragment " + targetFragmentClass + " not found");
    }
    
    public static void callImageFileChooser(Activity activity, int requestCode) {
        Intent chooseFile;
        Intent intent;
        chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
        chooseFile.setType("image/*");
        intent = Intent.createChooser(chooseFile, "Choose an image");
        activity.startActivityForResult(intent, requestCode);
    }
    
    public static List<TodoListEntry> sortEntries(List<TodoListEntry> entries, long day) {
        List<TodoListEntry> upcomingEntries = new ArrayList<>();
        List<TodoListEntry> expiredEntries = new ArrayList<>();
        List<TodoListEntry> todayEntries = new ArrayList<>();
        List<TodoListEntry> globalEntries = new ArrayList<>();
        List<TodoListEntry> otherEntries = new ArrayList<>();
        
        for (TodoListEntry entry : entries) {
            if (entry.isGlobal()) {
                globalEntries.add(entry);
                continue;
            }
            switch (entry.getEntryType(day)) {
                case TODAY:
                    todayEntries.add(entry);
                    break;
                case EXPIRED:
                    expiredEntries.add(entry);
                    break;
                case UPCOMING:
                    upcomingEntries.add(entry);
                    break;
                case UNKNOWN:
                default:
                    otherEntries.add(entry);
            }
        }
        
        List<TodoListEntry> merged = new ArrayList<>();
        merged.addAll(todayEntries);
        merged.addAll(globalEntries);
        merged.addAll(upcomingEntries);
        merged.addAll(expiredEntries);
        merged.addAll(otherEntries);
        merged.sort(new TodoListEntryGroupComparator(day));
        merged.sort(new TodoListEntryPriorityComparator());
        
        return merged;
    }
    
    @FunctionalInterface
    public interface SliderOnChangeKeyedListener {
        void onValueChanged(@NonNull Slider slider, float value, boolean fromUser, String key);
    }
    
    //listener for general settings
    public static void setSliderChangeListener(final TextView displayTo,
                                               final Slider slider,
                                               @Nullable final SliderOnChangeKeyedListener onChangeListener,
                                               @StringRes final int stringResource,
                                               final String key,
                                               final int defaultValue,
                                               boolean zeroIsOff) {
        int loadedVal = preferences.getInt(key, defaultValue);
        Context context = displayTo.getContext();
        if (loadedVal == 0 && zeroIsOff) {
            displayTo.setText(context.getString(stringResource, context.getString(R.string.off)));
        } else {
            displayTo.setText(displayTo.getContext().getString(stringResource, loadedVal));
        }
        slider.clearOnChangeListeners();
        slider.setValue(preferences.getInt(key, defaultValue));
        slider.addOnChangeListener((listenerSlider, progress, fromUser) -> {
            if (fromUser) {
                if (progress == 0 && zeroIsOff) {
                    displayTo.setText(context.getString(stringResource, context.getString(R.string.off)));
                } else {
                    displayTo.setText(displayTo.getContext().getString(stringResource, (int) progress));
                }
                if (onChangeListener != null) {
                    onChangeListener.onValueChanged(listenerSlider, progress, true, key);
                }
            }
        });
        slider.clearOnSliderTouchListeners();
        slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                // not needed
            }
            
            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                preferences.edit().putInt(key, (int) slider.getValue()).apply();
            }
        });
    }
    
    //listener for entry and calendar settings
    public static void setSliderChangeListener(final TextView displayTo,
                                               final Slider slider,
                                               final TextView stateIcon,
                                               final PopupSettingsView settingsView,
                                               @Nullable final View borderView,
                                               @StringRes final int stringResource,
                                               final String parameterKey,
                                               final Function<String, Integer> initialValueFactory,
                                               final Slider.OnChangeListener customProgressListener,
                                               final Slider.OnSliderTouchListener customTouchListener) {
        int initialValue = initialValueFactory.apply(parameterKey);
        displayTo.setText(displayTo.getContext().getString(stringResource, initialValue));
        slider.clearOnChangeListeners();
        slider.setValue(initialValue);
        slider.addOnChangeListener((slider1, progress, fromUser) -> {
            if (customProgressListener != null)
                customProgressListener.onValueChange(slider1, progress, fromUser);
            if (fromUser) {
                displayTo.setText(displayTo.getContext().getString(stringResource, (int) progress));
                if (borderView != null) {
                    borderView.setPadding((int) progress, (int) progress, (int) progress, 0);
                }
            }
        });
        slider.clearOnSliderTouchListeners();
        slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                if (customTouchListener != null) customTouchListener.onStartTrackingTouch(slider);
            }
            
            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                if (customTouchListener != null) customTouchListener.onStopTrackingTouch(slider);
                settingsView.notifyParameterChanged(stateIcon, parameterKey, (int) slider.getValue());
            }
        });
    }
    
    // without custom listeners
    public static void setSliderChangeListener(final TextView displayTo, final Slider slider,
                                               final TextView stateIcon, final PopupSettingsView systemCalendarSettings,
                                               @Nullable final View borderView, final int stringResource,
                                               final String parameterKey, final Function<String, Integer> initialValueFactory) {
        setSliderChangeListener(
                displayTo, slider, stateIcon, systemCalendarSettings, borderView,
                stringResource, parameterKey, initialValueFactory, null, null);
    }
    
    //switch listener for regular settings
    public static void setSwitchChangeListener(final Switch tSwitch, final String key, boolean defaultValue) {
        tSwitch.setCheckedSilent(preferences.getBoolean(key, defaultValue));
        tSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> preferences.edit().putBoolean(key, isChecked).apply());
    }
    
    //switch listener for calendar settings and entry settings
    public static void setSwitchChangeListener(final Switch tSwitch,
                                               final TextView stateIcon,
                                               final PopupSettingsView settingsView,
                                               final String parameterKey,
                                               final Function<String, Boolean> initialValueFactory) {
        tSwitch.setCheckedSilent(initialValueFactory.apply(parameterKey));
        tSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                settingsView.notifyParameterChanged(stateIcon, parameterKey, isChecked));
    }
    
    public static void openUrl(Context context, String url) {
        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }
    
    public static void openUrl(Fragment fragment, String url) {
        openUrl(fragment.requireContext(), url);
    }
    
    public static boolean datesEqual(LocalDate date1, LocalDate date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        return date1.isEqual(date2);
    }
    
    /**
     * Colorize a specific substring in a string for TextView. Use it like this: <pre>
     * textView.setText(
     *     Strings.colorized("This word is black.","black", Color.BLACK),
     *     TextView.BufferType.SPANNABLE
     * );
     * </pre>
     *
     * @param text Text that contains a substring to colorize
     * @param word Substring to colorize
     * @param argb Color
     * @return Spannable
     */
    public static Spannable colorizeText(final String text, final String word, final int argb) {
        final Spannable spannable = new SpannableString(text);
        int substringStart = 0;
        int start;
        while ((start = text.indexOf(word, substringStart)) >= 0) {
            spannable.setSpan(
                    new ForegroundColorSpan(argb), start, start + word.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            substringStart = start + word.length();
        }
        return spannable;
    }
    
    public static long rfc2445ToMilliseconds(String str) {
        if (str == null || str.isEmpty())
            throw new IllegalArgumentException("Null or empty RFC string");
        
        int sign = 1;
        int weeks = 0;
        int days = 0;
        int hours = 0;
        int minutes = 0;
        int seconds = 0;
        
        int len = str.length();
        int index = 0;
        char c;
        
        c = str.charAt(0);
        
        if (c == '-') {
            sign = -1;
            index++;
        } else if (c == '+')
            index++;
        
        c = str.charAt(index);
        
        if (c != 'P')
            throw new IllegalArgumentException("Duration.parse(str='" + str + "') expected 'P' at index=" + index);
        
        index++;
        c = str.charAt(index);
        if (c == 'T')
            index++;
        
        int n = 0;
        for (; index < len; index++) {
            c = str.charAt(index);
            
            if (c >= '0' && c <= '9') {
                n *= 10;
                n += c - '0';
            } else if (c == 'W') {
                weeks = n;
                n = 0;
            } else if (c == 'H') {
                hours = n;
                n = 0;
            } else if (c == 'M') {
                minutes = n;
                n = 0;
            } else if (c == 'S') {
                seconds = n;
                n = 0;
            } else if (c == 'D') {
                days = n;
                n = 0;
            } else if (c != 'T') {
                throw new IllegalArgumentException("Duration.parse(str='" + str + "') unexpected char '" + c + "' at index=" + index);
            }
        }
        
        return 1000 * sign * ((7L * 24 * 60 * 60 * weeks)
                + (24L * 60 * 60 * days)
                + (60L * 60 * hours)
                + (60L * minutes)
                + seconds);
    }
}

class TodoListEntryPriorityComparator implements Comparator<TodoListEntry> {
    @Override
    public int compare(TodoListEntry o1, TodoListEntry o2) {
        return Integer.compare(o2.priority.getToday(), o1.priority.getToday());
    }
}

class TodoListEntryGroupComparator implements Comparator<TodoListEntry> {
    
    final long day;
    
    public TodoListEntryGroupComparator(long day) {
        this.day = day;
    }
    
    @Override
    public int compare(TodoListEntry o1, TodoListEntry o2) {
        if (o1.isFromSystemCalendar() || o2.isFromSystemCalendar()) {
            return Long.compare(o1.getNearestEventTimestamp(day), o2.getNearestEventTimestamp(day));
        }
        return Integer.compare(o1.getRawGroupName().hashCode(), o2.getRawGroupName().hashCode());
    }
}