package prototype.xd.scheduler.utilities;

import static prototype.xd.scheduler.utilities.Keys.ENTRIES_FILE;
import static prototype.xd.scheduler.utilities.Keys.ENTRIES_FILE_BACKUP;
import static prototype.xd.scheduler.utilities.Keys.GROUPS_FILE;
import static prototype.xd.scheduler.utilities.Keys.GROUPS_FILE_BACKUP;
import static prototype.xd.scheduler.utilities.Keys.MERGE_ENTRIES;
import static prototype.xd.scheduler.utilities.Keys.ROOT_DIR;
import static prototype.xd.scheduler.utilities.Keys.TODO_ITEM_SORTING_ORDER;
import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.Logger.warning;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getTodoEntriesFromCalendars;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.PluralsRes;
import androidx.annotation.StringRes;
import androidx.collection.ArraySet;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.slider.Slider;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.GroupList;
import prototype.xd.scheduler.entities.SystemCalendar;
import prototype.xd.scheduler.entities.TodoEntry;
import prototype.xd.scheduler.views.Switch;
import prototype.xd.scheduler.views.settings.PopupSettingsView;

@SuppressWarnings({"unchecked"})
public class Utilities {
    
    public static final String NAME = Utilities.class.getSimpleName();
    
    public static File getFile(String filename) {
        return new File(ROOT_DIR.get(), filename);
    }
    
    public static Path getPath(String filename) {
        return getFile(filename).toPath();
    }
    
    private Utilities() {
        throw new IllegalStateException(NAME + " can't be instantiated");
    }
    
    public static String nullWrapper(String str) {
        return str == null ? "" : str.trim();
    }
    
    public static boolean isVerticalOrientation(@NonNull Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }
    
    public static <T extends Exception> void throwOnFalse(boolean result, String message, Class<T> exceptionClass)
            throws T, IllegalArgumentException {
        if (!result) {
            try {
                throw exceptionClass.getConstructor(String.class).newInstance(message);
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException |
                     NoSuchMethodException e) {
                e.printStackTrace();
                throw new IllegalArgumentException("Error throwing exception");
            }
        }
    }
    
    public static List<TodoEntry> loadTodoEntries(long dayStart, long dayEnd,
                                                  GroupList groups,
                                                  @NonNull List<SystemCalendar> calendars,
                                                  boolean attachGroupToEntry) {
        
        List<TodoEntry> readEntries = new ArrayList<>();
        try {
            readEntries = loadObjectWithBackup(ENTRIES_FILE, ENTRIES_FILE_BACKUP);
            
            int id = 0;
            for (TodoEntry entry : readEntries) {
                // post deserialize
                entry.initGroupAndId(groups, id++, attachGroupToEntry);
            }
            
            Logger.info(NAME, "Read todo list: " + readEntries.size());
        } catch (IOException e) {
            Logger.info(NAME, "No todo list: (" + e + ")");
        } catch (Exception e) {
            logException(NAME, e);
        }
        
        readEntries.addAll(getTodoEntriesFromCalendars(dayStart, dayEnd, calendars));
        return readEntries;
    }
    
    public static synchronized void saveEntries(List<TodoEntry> entries) {
        try {
            List<TodoEntry> entriesToSave = new ArrayList<>();
            
            for (int i = 0; i < entries.size(); i++) {
                TodoEntry entry = entries.get(i);
                if (!entry.isFromSystemCalendar()) {
                    entriesToSave.add(entry);
                }
            }
            
            moveFile(ENTRIES_FILE, ENTRIES_FILE_BACKUP);
            saveObject(ENTRIES_FILE, entriesToSave);
            
            Logger.info(NAME, "Saved todo list");
        } catch (IOException e) {
            Logger.error(NAME, "Missing permission, failed saving todo list");
        } catch (Exception e) {
            logException(NAME, e);
        }
    }
    
    public static GroupList loadGroups() {
        GroupList groups = new GroupList();
        groups.add(Group.NULL_GROUP); // add "null" group
        try {
            groups.addAll(loadObjectWithBackup(GROUPS_FILE, GROUPS_FILE_BACKUP));
            return groups;
        } catch (IOException e) {
            Logger.info(NAME, "No groups file (" + e + ")");
        } catch (Exception e) {
            logException(NAME, e);
        }
        return groups;
    }
    
    public static synchronized void saveGroups(GroupList groups) {
        try {
            
            GroupList groupsToSave = new GroupList();
            
            for (Group group : groups) {
                if (!group.isNullGroup()) {
                    groupsToSave.add(group);
                }
            }
            
            moveFile(GROUPS_FILE, GROUPS_FILE_BACKUP);
            saveObject(GROUPS_FILE, groupsToSave);
            
            Logger.info(NAME, "Saved group list");
        } catch (IOException e) {
            logException(NAME, e);
        }
    }
    
    public static void moveFile(String fileName, String newFileName) throws IOException {
        Files.move(getPath(fileName), getPath(newFileName), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
    
    public static <T> T loadObjectWithBackup(String fileName, String backupName) throws IOException, ClassNotFoundException {
        try {
            return loadObject(fileName);
        } catch (Exception e) {
            T obj = loadObject(backupName);
            // restore backup if it was read successfully
            Files.copy(getPath(backupName), getPath(fileName),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            warning(NAME, "Restored " + backupName + " to " + fileName);
            return obj;
        }
    }
    
    public static <T> T loadObject(String fileName) throws IOException, ClassNotFoundException {
        try (ObjectInputStream s = new ObjectInputStream(Files.newInputStream(getPath(fileName)))) {
            return (T) s.readObject();
        }
    }
    
    public static void saveObject(String fileName, Object object) throws IOException {
        try (ObjectOutputStream s = new ObjectOutputStream(Files.newOutputStream(getPath(fileName)))) {
            s.writeObject(object);
        }
    }
    
    public static <T extends Fragment> T findFragmentInNavHost(@NonNull FragmentActivity activity, Class<T> targetFragmentClass) {
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
    
    public static void navigateToFragment(@NonNull FragmentActivity activity, @IdRes int actionId) {
        try {
            ((NavHostFragment) Objects.requireNonNull(
                    activity.getSupportFragmentManager()
                            .findFragmentById(R.id.nav_host_fragment)))
                    .getNavController()
                    .navigate(actionId);
        } catch (IllegalArgumentException e) {
            Logger.error(NAME, "Error navigating to " + actionId + " Double click? [" + e + "]");
        }
    }
    
    public static void callImageFileChooser(ActivityResultLauncher<Intent> callback) {
        Intent chooseImage = new Intent(Intent.ACTION_GET_CONTENT);
        chooseImage.addCategory(Intent.CATEGORY_OPENABLE);
        chooseImage.setType("image/*");
        callback.launch(Intent.createChooser(chooseImage, "Choose an image"));
    }
    
    public static List<TodoEntry> sortEntries(List<TodoEntry> entries, long targetDay) {
        
        List<TodoEntry.EntryType> sortingOrder = TODO_ITEM_SORTING_ORDER.get();
        
        for (TodoEntry entry : entries) {
            entry.cacheSortingIndex(targetDay, sortingOrder);
            if (entry.isFromSystemCalendar()) {
                // obtain nearest start ms near a particular day for use in sorting later
                entry.cacheNearestStartMsUTC(targetDay);
            }
        }
        
        entries.sort(new TodoEntryGroupComparator());
        entries.sort(new TodoEntryTypeComparator());
        entries.sort(new TodoEntryPriorityComparator());
        
        if (MERGE_ENTRIES.get()) {
            return mergeInstances(entries);
        }
        
        return entries;
    }
    
    private static List<TodoEntry> mergeInstances(List<TodoEntry> entries) {
        Set<Integer> seen = new ArraySet<>(entries.size());
        entries.removeIf(e -> e.isFromSystemCalendar() && !seen.add(e.getInstanceHash()));
        return entries;
    }
    
    @FunctionalInterface
    public interface SliderOnChangeKeyedListener {
        void onValueChanged(@NonNull Slider slider, int sliderValue, boolean fromUser, Keys.DefaultedInteger value);
    }
    
    public static void setSliderChangeListener(final TextView displayTo,
                                               final Slider slider,
                                               @Nullable final SliderOnChangeKeyedListener onChangeListener,
                                               @StringRes @PluralsRes final int stringResource,
                                               Keys.DefaultedInteger value,
                                               boolean zeroIsOff) {
        Context context = displayTo.getContext();
        Function<Integer, String> textFormatter = progress -> {
            if (progress == 0 && zeroIsOff) {
                return context.getString(stringResource, context.getString(R.string.off));
            } else {
                return getQuantityString(context, stringResource, progress);
            }
        };
        setSliderChangeListener(displayTo, slider, onChangeListener, value, textFormatter);
    }
    
    //listener for general settings
    public static void setSliderChangeListener(final TextView displayTo,
                                               final Slider slider,
                                               @Nullable final SliderOnChangeKeyedListener onChangeListener,
                                               Keys.DefaultedInteger value,
                                               @NonNull Function<Integer, String> textFormatter) {
        int loadedVal = value.get();
        displayTo.setText(textFormatter.apply(loadedVal));
        slider.clearOnChangeListeners();
        slider.setValue(loadedVal);
        slider.addOnChangeListener((listenerSlider, progress, fromUser) -> {
            if (fromUser) {
                displayTo.setText(textFormatter.apply((int) progress));
                if (onChangeListener != null) {
                    onChangeListener.onValueChanged(listenerSlider, (int) progress, true, value);
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
                value.put((int) slider.getValue());
            }
        });
    }
    
    //listener for entry and calendar settings
    public static void setSliderChangeListener(final TextView displayTo,
                                               final Slider slider,
                                               final TextView stateIcon,
                                               @NonNull final PopupSettingsView settingsView,
                                               @StringRes @PluralsRes final int stringResource,
                                               final Keys.DefaultedInteger value,
                                               @NonNull final Function<Keys.DefaultedInteger, Integer> initialValueFactory,
                                               @Nullable final Slider.OnChangeListener customProgressListener,
                                               @Nullable final Slider.OnSliderTouchListener customTouchListener) {
        int initialValue = initialValueFactory.apply(value);
        displayTo.setText(getQuantityString(displayTo.getContext(), stringResource, initialValue));
        slider.clearOnChangeListeners();
        slider.setValue(initialValue);
        slider.addOnChangeListener((slider1, progress, fromUser) -> {
            if (customProgressListener != null)
                customProgressListener.onValueChange(slider1, progress, fromUser);
            if (fromUser) {
                displayTo.setText(getQuantityString(displayTo.getContext(), stringResource, (int) progress));
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
                settingsView.notifyParameterChanged(stateIcon, value.key, (int) slider.getValue());
            }
        });
    }
    
    // without custom listeners
    public static void setSliderChangeListener(final TextView displayTo,
                                               final Slider slider,
                                               final TextView stateIcon,
                                               final PopupSettingsView systemCalendarSettings,
                                               @StringRes @PluralsRes final int stringResource,
                                               final Keys.DefaultedInteger value,
                                               final Function<Keys.DefaultedInteger, Integer> initialValueFactory,
                                               @Nullable final Slider.OnChangeListener customProgressListener) {
        setSliderChangeListener(
                displayTo, slider, stateIcon, systemCalendarSettings,
                stringResource, value, initialValueFactory, customProgressListener, null);
    }
    
    //switch listener for regular settings
    public static void setSwitchChangeListener(final Switch tSwitch,
                                               final Keys.DefaultedBoolean defaultedBoolean,
                                               @Nullable CompoundButton.OnCheckedChangeListener onCheckedChangeListener) {
        tSwitch.setCheckedSilent(defaultedBoolean.get());
        tSwitch.setOnCheckedChangeListener((switchView, isChecked) -> {
            defaultedBoolean.put(isChecked);
            if (onCheckedChangeListener != null) {
                onCheckedChangeListener.onCheckedChanged(switchView, isChecked);
            }
        });
    }
    
    //switch listener for calendar settings and entry settings
    public static void setSwitchChangeListener(final Switch tSwitch,
                                               final TextView stateIcon,
                                               final PopupSettingsView settingsView,
                                               final Keys.DefaultedBoolean value,
                                               final Function<Keys.DefaultedBoolean, Boolean> initialValueFactory) {
        tSwitch.setCheckedSilent(initialValueFactory.apply(value));
        tSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                settingsView.notifyParameterChanged(stateIcon, value.key, isChecked));
    }
    
    /**
     * Same as {@link #getPluralString} but also accepts normal strings
     *
     * @param context  any context
     * @param resId    int resource id of the string
     * @param quantity string quantity
     * @return string with the given quantity
     */
    public static String getQuantityString(@NonNull Context context, @StringRes @PluralsRes int resId, int quantity) {
        Resources res = context.getResources();
        if (res.getResourceTypeName(resId).equals("plurals")) {
            return res.getQuantityString(resId, quantity, quantity);
        }
        return res.getString(resId, quantity);
    }
    
    /**
     * Get plural string
     *
     * @param context  any context
     * @param resId    int resource id of the string
     * @param quantity string quantity
     * @return string with the given quantity
     */
    public static String getPluralString(@NonNull Context context, @PluralsRes int resId, int quantity) {
        return context.getResources().getQuantityString(resId, quantity, quantity);
    }
    
    // opens a url in default browser (context)
    
    /**
     * Opens a url in default browser
     *
     * @param context any context
     * @param url     url to open
     */
    public static void openUrl(@NonNull Context context, String url) {
        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }
    
    public static boolean datesEqual(@Nullable LocalDate date1, @Nullable LocalDate date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        return date1.isEqual(date2);
    }
    
    /**
     * Compares values of 2 maps
     *
     * @param map1 first map
     * @param map2 second map
     * @param <K>  key type
     * @param <V>  value type
     * @return keys with different values between two maps
     */
    public static <K, V> Set<K> getChangedKeys(@NonNull final Map<K, V> map1,
                                               @NonNull final Map<K, V> map2) {
        if (map1.isEmpty() && map2.isEmpty() || map1.equals(map2)) {
            return Collections.emptySet();
        }
        if (map1.isEmpty()) {
            return map2.keySet();
        }
        if (map2.isEmpty()) {
            return map1.keySet();
        }
        Set<K> keys = new ArraySet<>(map1.size() + map2.size());
        keys.addAll(map1.keySet());
        keys.addAll(map2.keySet());
        // remove equal values
        keys.removeIf(key -> Objects.equals(map1.get(key), map2.get(key)));
        return keys;
    }
    
    /**
     * Computes symmetric difference between 2 sets
     *
     * @param set1 first set
     * @param set2 second set
     * @param <K>  key type
     * @return symmetric difference between these two sets
     */
    public static <K> Set<K> symmetricDifference(@NonNull final Set<K> set1,
                                                 @NonNull final Set<K> set2) {
        if (set1.equals(set2)) {
            return Collections.emptySet();
        }
        if (set1.isEmpty()) {
            return set2;
        }
        if (set2.isEmpty()) {
            return set1;
        }
        
        Set<K> combined = new ArraySet<>(set1.size() + set2.size());
        combined.addAll(set1);
        combined.addAll(set2);
        
        // intersection contains keys both in set1 and set2
        Set<K> intersection = new ArraySet<>(set1.size());
        intersection.addAll(set1);
        intersection.retainAll(set2); // leave keys that are the same
        
        combined.removeAll(intersection); // remove equal keys
        
        return combined;
    }
    
    /**
     * Display long toast with a message
     *
     * @param context   any context
     * @param textResId resource id of the text to show
     */
    public static void displayToast(@NonNull Context context, @StringRes int textResId) {
        Toast.makeText(context, context.getString(textResId), Toast.LENGTH_LONG).show();
    }
    
    public static void fancyHideUnhideView(@NonNull View view, boolean visible, boolean animate) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (animate) {
            int initialHeight = view.getMeasuredHeight();
            view.animate()
                    .scaleY(visible ? 1 : 0)
                    .translationY(visible ? 0 : -initialHeight / 2f).start();
        }
    }
    
    public static boolean rangesOverlap(long x1, long x2, long y1, long y2) {
        return x2 >= y1 && x1 <= y2;
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
    @NonNull
    public static Spannable colorizeText(@NonNull final String text, final String word, final int argb) {
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

class TodoEntryTypeComparator implements Comparator<TodoEntry> {
    @Override
    public int compare(TodoEntry o1, TodoEntry o2) {
        return Integer.compare(o1.getSortingIndex(), o2.getSortingIndex());
    }
}

class TodoEntryPriorityComparator implements Comparator<TodoEntry> {
    @Override
    public int compare(TodoEntry o1, TodoEntry o2) {
        return Integer.compare(o2.priority.getToday(), o1.priority.getToday());
    }
}

class TodoEntryGroupComparator implements Comparator<TodoEntry> {
    @Override
    public int compare(TodoEntry o1, TodoEntry o2) {
        if (o1.isFromSystemCalendar() || o2.isFromSystemCalendar()) {
            return Long.compare(o1.getCachedNearestStartMsUTC(), o2.getCachedNearestStartMsUTC());
        }
        return Integer.compare(o1.getRawGroupName().hashCode(), o2.getRawGroupName().hashCode());
    }
}