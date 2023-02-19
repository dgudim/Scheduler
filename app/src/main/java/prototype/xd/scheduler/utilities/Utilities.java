package prototype.xd.scheduler.utilities;

import static prototype.xd.scheduler.entities.TodoEntryList.TODO_LIST_INITIAL_CAPACITY;
import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.Logger.warning;
import static prototype.xd.scheduler.utilities.Static.ENTRIES_FILE;
import static prototype.xd.scheduler.utilities.Static.ENTRIES_FILE_BACKUP;
import static prototype.xd.scheduler.utilities.Static.GROUPS_FILE;
import static prototype.xd.scheduler.utilities.Static.GROUPS_FILE_BACKUP;
import static prototype.xd.scheduler.utilities.Static.MERGE_ENTRIES;
import static prototype.xd.scheduler.utilities.Static.PACKAGE_PROVIDER_NAME;
import static prototype.xd.scheduler.utilities.Static.ROOT_DIR;
import static prototype.xd.scheduler.utilities.Static.TODO_ITEM_SORTING_ORDER;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.addTodoEntriesFromCalendars;

import android.app.Activity;
import android.content.ClipData;
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
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.core.content.FileProvider;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.slider.Slider;
import com.google.common.collect.Sets;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.GroupList;
import prototype.xd.scheduler.entities.SystemCalendar;
import prototype.xd.scheduler.entities.TodoEntry;
import prototype.xd.scheduler.entities.TodoEntryList;
import prototype.xd.scheduler.views.Switch;
import prototype.xd.scheduler.views.settings.PopupSettingsView;

@SuppressWarnings("unchecked")
public final class Utilities {
    
    public static final String NAME = Utilities.class.getSimpleName();
    
    private Utilities() throws InstantiationException {
        throw new InstantiationException(NAME);
    }
    
    @NonNull
    public static File getFile(@NonNull String filename) {
        return new File(ROOT_DIR.get(), filename);
    }
    
    @NonNull
    public static Uri getUri(@NonNull Context context, @NonNull File file) {
        return FileProvider.getUriForFile(context, PACKAGE_PROVIDER_NAME, file);
    }
    
    @NonNull
    public static Path getPath(@NonNull String filename) {
        return getFile(filename).toPath();
    }
    
    @NonNull
    public static String nullWrapper(@Nullable String str) {
        return str == null ? "" : str.trim();
    }
    
    public static boolean isVerticalOrientation(@NonNull Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }
    
    @NonNull
    public static List<TodoEntry> loadTodoEntries(long dayStart, long dayEnd,
                                                  @NonNull GroupList groups,
                                                  @NonNull Collection<SystemCalendar> calendars) {
        
        List<TodoEntry> readEntries = new ArrayList<>(TODO_LIST_INITIAL_CAPACITY);
        try {
            readEntries.addAll(loadObjectWithBackup(ENTRIES_FILE, ENTRIES_FILE_BACKUP));
            
            int id = 0;
            for (TodoEntry entry : readEntries) {
                // post deserialize
                entry.initGroupAndId(groups, id);
                id++;
            }
            
            Logger.info(NAME, "Read todo list: " + readEntries.size());
        } catch (IOException e) {
            Logger.info(NAME, "No todo list: (" + e + ")");
        } catch (Exception e) {
            logException(NAME, e);
        }
        
        addTodoEntriesFromCalendars(dayStart, dayEnd, calendars, readEntries);
        return readEntries;
    }
    
    public static synchronized void saveEntries(@Nullable TodoEntryList entries) {
        
        if (entries == null) {
            Logger.error(NAME, "Trying to save null entry list");
            return;
        }
        
        try {
            saveObjectWithBackup(ENTRIES_FILE, ENTRIES_FILE_BACKUP, entries.getRegularEntries());
            Logger.info(NAME, "Saved todo list");
        } catch (IOException e) {
            Logger.error(NAME, "Missing permission, failed saving todo list: " + e);
        } catch (Exception e) {
            logException(NAME, e);
        }
    }
    
    @NonNull
    public static GroupList loadGroups() {
        GroupList groups = new GroupList();
        // add "null" group
        groups.add(Group.NULL_GROUP);
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
    
    public static synchronized void saveGroups(@Nullable GroupList groups) {
        if (groups == null) {
            Logger.error(NAME, "Trying to save null group list");
            return;
        }
        try {
            
            GroupList groupsToSave = new GroupList();
            
            for (Group group : groups) {
                if (!group.isNullGroup()) {
                    groupsToSave.add(group);
                }
            }
            
            saveObjectWithBackup(GROUPS_FILE, GROUPS_FILE_BACKUP, groupsToSave);
            
            Logger.info(NAME, "Saved group list");
        } catch (IOException e) {
            logException(NAME, e);
        }
    }
    
    private static <T> T loadObject(@NonNull String fileName) throws IOException, ClassNotFoundException {
        try (ObjectInputStream stream = new ObjectInputStream(Files.newInputStream(getPath(fileName)))) {
            return (T) stream.readObject();
        }
    }
    
    private static <T> T loadObjectWithBackup(@NonNull String fileName, @NonNull String backupName) throws IOException, ClassNotFoundException {
        try {
            return loadObject(fileName);
        } catch (IOException | ClassNotFoundException e) {
            T obj = loadObject(backupName);
            // restore backup if it was read successfully
            Files.copy(getPath(backupName), getPath(fileName),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            warning(NAME, "Restored " + backupName + " to " + fileName + " (" + e + ")");
            return obj;
        }
    }
    
    private static void saveObjectWithBackup(@NonNull String fileName, @NonNull String backupName, @NonNull Object object) throws IOException {
        File originalFile = getFile(fileName);
        Path originalPath = originalFile.toPath();
        if (originalFile.exists()) {
            Files.move(originalPath, getPath(backupName),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        }
        try (ObjectOutputStream stream = new ObjectOutputStream(Files.newOutputStream(originalPath))) {
            stream.writeObject(object);
        }
    }
    
    @NonNull
    public static <T extends Fragment> T findFragmentInNavHost(@NonNull FragmentActivity activity, @NonNull Class<T> targetFragmentClass) {
        List<Fragment> fragments = Objects.requireNonNull(activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment))
                .getChildFragmentManager()
                .getFragments();
        for (Fragment fragment : fragments) {
            if (targetFragmentClass.isInstance(fragment)) {
                return (T) fragment;
            }
        }
        throw new IllegalArgumentException("Fragment " + targetFragmentClass + " not found");
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
    
    public static void callImageFileChooser(@NonNull ActivityResultLauncher<Intent> callback) {
        Intent chooseImage = new Intent(Intent.ACTION_GET_CONTENT);
        chooseImage.addCategory(Intent.CATEGORY_OPENABLE);
        chooseImage.setType("image/*");
        callback.launch(Intent.createChooser(chooseImage, null));
    }
    
    public static void shareFiles(@NonNull Context context, @NonNull String mimetype, @NonNull List<File> files) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        ArrayList<Uri> uris = new ArrayList<>(files.size());
        ClipData clipData = null;
        for (File file : files) {
            Uri uri = getUri(context, file);
            ClipData.Item clipItem = new ClipData.Item(uri);
            uris.add(uri);
            if (clipData == null) {
                clipData = new ClipData(null, new String[]{mimetype}, clipItem);
                continue;
            }
            clipData.addItem(clipItem);
        }
        // for compatibility with older apps
        shareIntent.putExtra(Intent.EXTRA_STREAM, uris);
        // for newer apps
        shareIntent.setClipData(clipData);
        // allow to read the content
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareIntent.setType(mimetype);
        context.startActivity(Intent.createChooser(shareIntent, null));
    }
    
    /**
     * Opens a url in default browser
     *
     * @param context any context
     * @param url     url to open
     */
    public static void openUrl(@NonNull Context context, @NonNull String url) {
        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }
    
    public static <T extends Activity> void switchActivity(@NonNull Activity currentActivity,
                                                           @NonNull Class<T> newActivity) {
        currentActivity.startActivity(new Intent(currentActivity, newActivity));
        currentActivity.finish();
    }
    
    @NonNull
    public static List<TodoEntry> sortEntries(@NonNull List<TodoEntry> entries, long targetDay) {
        
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
    
    @NonNull
    private static List<TodoEntry> mergeInstances(@NonNull List<TodoEntry> entries) {
        Set<Integer> seen = new ArraySet<>(entries.size());
        entries.removeIf(e -> e.isFromSystemCalendar() && !seen.add(e.getInstanceHash()));
        return entries;
    }
    
    @FunctionalInterface
    public interface SliderOnChangeKeyedListener {
        void onValueChanged(@NonNull Slider slider, int sliderValue, boolean fromUser, @NonNull Static.DefaultedInteger value);
    }
    
    public static void setSliderChangeListener(@NonNull final TextView displayTo,
                                               @NonNull final Slider slider,
                                               @Nullable final SliderOnChangeKeyedListener onChangeListener,
                                               @StringRes @PluralsRes final int stringResource,
                                               @NonNull Static.DefaultedInteger value,
                                               boolean zeroIsOff) {
        Context context = displayTo.getContext();
        IntFunction<String> textFormatter = progress -> {
            if (progress == 0 && zeroIsOff) {
                return context.getString(stringResource, context.getString(R.string.off));
            } else {
                return getQuantityString(context, stringResource, progress);
            }
        };
        setSliderChangeListener(displayTo, slider, onChangeListener, value, textFormatter);
    }
    
    //listener for general settings
    public static void setSliderChangeListener(@NonNull final TextView displayTo,
                                               @NonNull final Slider slider,
                                               @Nullable final SliderOnChangeKeyedListener onChangeListener,
                                               @NonNull Static.DefaultedInteger value,
                                               @NonNull IntFunction<String> textFormatter) {
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
    public static void setSliderChangeListener(@NonNull final TextView displayTo,
                                               @NonNull final Slider slider,
                                               @NonNull final TextView stateIcon,
                                               @NonNull final PopupSettingsView settingsView,
                                               @StringRes @PluralsRes final int stringResource,
                                               @NonNull final Static.DefaultedInteger value,
                                               @NonNull final ToIntFunction<Static.DefaultedInteger> initialValueFactory,
                                               @Nullable final Slider.OnChangeListener customProgressListener,
                                               @Nullable final Slider.OnSliderTouchListener customTouchListener) {
        int initialValue = initialValueFactory.applyAsInt(value);
        displayTo.setText(getQuantityString(displayTo.getContext(), stringResource, initialValue));
        slider.clearOnChangeListeners();
        slider.setValue(initialValue);
        slider.addOnChangeListener((slider1, progress, fromUser) -> {
            if (customProgressListener != null) {
                customProgressListener.onValueChange(slider1, progress, fromUser);
            }
            if (fromUser) {
                displayTo.setText(getQuantityString(displayTo.getContext(), stringResource, (int) progress));
            }
        });
        slider.clearOnSliderTouchListeners();
        slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                if (customTouchListener != null) {
                    customTouchListener.onStartTrackingTouch(slider);
                }
            }
            
            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                if (customTouchListener != null) {
                    customTouchListener.onStopTrackingTouch(slider);
                }
                settingsView.notifyParameterChanged(stateIcon, value.key, (int) slider.getValue());
            }
        });
    }
    
    // without custom listeners
    public static void setSliderChangeListener(@NonNull final TextView displayTo,
                                               @NonNull final Slider slider,
                                               @NonNull final TextView stateIcon,
                                               @NonNull final PopupSettingsView systemCalendarSettings,
                                               @StringRes @PluralsRes final int stringResource,
                                               @NonNull final Static.DefaultedInteger value,
                                               @NonNull final ToIntFunction<Static.DefaultedInteger> initialValueFactory,
                                               @Nullable final Slider.OnChangeListener customProgressListener) {
        setSliderChangeListener(
                displayTo, slider, stateIcon, systemCalendarSettings,
                stringResource, value, initialValueFactory, customProgressListener, null);
    }
    
    //switch listener for regular settings
    public static void setSwitchChangeListener(@NonNull final Switch tSwitch,
                                               @NonNull final Static.DefaultedBoolean defaultedBoolean,
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
    public static void setSwitchChangeListener(@NonNull final Switch tSwitch,
                                               @NonNull final TextView stateIcon,
                                               @NonNull final PopupSettingsView settingsView,
                                               @NonNull final Static.DefaultedBoolean value,
                                               @NonNull final Predicate<Static.DefaultedBoolean> initialValueFactory) {
        tSwitch.setCheckedSilent(initialValueFactory.test(value));
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
     * @see #getPluralString
     */
    @NonNull
    public static String getQuantityString(@NonNull Context context, @StringRes @PluralsRes int resId, int quantity) throws Resources.NotFoundException {
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
     * @see #getQuantityString
     */
    @NonNull
    public static String getPluralString(@NonNull Context context, @PluralsRes int resId, int quantity) throws Resources.NotFoundException {
        return context.getResources().getQuantityString(resId, quantity, quantity);
    }
    
    public static boolean areDatesEqual(@Nullable LocalDate date1, @Nullable LocalDate date2) {
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
    @NonNull
    public static <K, V> Set<K> getChangedKeys(@NonNull final Map<K, V> map1,
                                               @NonNull final Map<K, V> map2) {
        if ((map1.isEmpty() && map2.isEmpty()) || map1.equals(map2)) {
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
                    .translationY(visible ? 0 : -initialHeight / 2F).start();
        }
    }
    
    /**
     * Check whether two ranges overlap
     *
     * @param x1 start of first range
     * @param x2 end of first range
     * @param y1 start of second range
     * @param y2 end of second range
     * @return whether two integer ranges overlap
     */
    public static boolean doRangesOverlap(long x1, long x2, long y1, long y2) {
        return x2 >= y1 && x1 <= y2;
    }
    
    /**
     * Colorize a specific substring in a string for TextView. Use it like this:
     * <pre> {@code
     * textView.setText(
     *     Strings.colorized("This word is black.","black", Color.BLACK),
     *     TextView.BufferType.SPANNABLE
     * );
     * }</pre>
     *
     * @param text Text that contains a substring to colorize
     * @param word Substring to colorize
     * @param argb Color
     * @return Spannable
     */
    @NonNull
    public static Spannable colorizeText(@NonNull final String text, @NonNull final String word, final int argb) {
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
    
    public enum ElementState {
        NEW, MODIFIED, NOT_MODIFIED, DELETED
    }
    
    /**
     * Computes differences between 2 maps
     *
     * @param oldMap   old map
     * @param newMap   new map
     * @param consumer function to call on each difference
     * @return where there were any changes
     */
    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    public static <K, V> boolean processDifference(@NonNull Map<K, V> oldMap, @NonNull Map<K, V> newMap,
                                                   @NonNull BiConsumer<Pair<K, V>, ElementState> consumer) {
        Set<K> oldSet = oldMap.keySet();
        Set<K> newSet = newMap.keySet();
        boolean changed = false;
        for (K key : Sets.union(oldSet, newSet)) { // NOSONAR
            V oldValue = oldMap.get(key);
            if (oldValue != null) {
                V newValue = newMap.get(key);
                if (newValue != null) {
                    // present in both sets
                    if (oldValue.equals(newValue)) {
                        consumer.accept(Pair.create(key, null), ElementState.NOT_MODIFIED);
                    } else {
                        consumer.accept(Pair.create(key, newMap.get(key)), ElementState.MODIFIED);
                        changed = true;
                    }
                    continue;
                }
                // not present in new set, present in old set
                consumer.accept(Pair.create(key, null), ElementState.DELETED);
                changed = true;
                continue;
            }
            // not present in old set, present in new set
            consumer.accept(Pair.create(key, newMap.get(key)), ElementState.NEW);
            changed = true;
        }
        return changed;
    }
    
    /**
     * Changes values of a map based on some transformer function
     *
     * @param fromMap     map to map
     * @param transformer function to transform the values
     * @param <K>         type of key in the map
     * @param <V1>        original value type
     * @param <V2>        transformed value type
     * @return a map with the new mapped values
     */
    @NonNull
    public static <K, V1, V2> ArrayMap<K, V2> remapMap(@NonNull Map<K, V1> fromMap, @NonNull Function<? super V1, V2> transformer) {
        ArrayMap<K, V2> newMap = new ArrayMap<>(fromMap.size());
        for (Map.Entry<K, V1> entry : fromMap.entrySet()) {
            newMap.put(entry.getKey(), transformer.apply(entry.getValue()));
        }
        return newMap;
    }
    
    public static int addIntFlag(int original, int flag) {
        return original | flag;
    }
    
    public static int removeIntFlag(int original, int flag) {
        return original & (~flag);
    }
    
    public static long rfc2445ToMilliseconds(@NonNull String str) {
        if (str.isEmpty()) {
            throw new IllegalArgumentException("Empty RFC string");
        }
        
        int sign = 1;
        int index = 0;
        
        char c = str.charAt(index);
        
        if (c == '-') {
            sign = -1;
            index++;
        } else if (c == '+') {
            index++;
        }
        
        c = str.charAt(index);
        
        if (c != 'P') {
            throw new IllegalArgumentException("Duration.parse(str='" + str + "') expected 'P' at index=" + index);
        }
        
        index++;
        c = str.charAt(index);
        if (c == 'T') {
            index++;
        }
        
        int weeks = 0;
        int days = 0;
        int hours = 0;
        int minutes = 0;
        int seconds = 0;
        
        int n = 0;
        int len = str.length();
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
    public int compare(@NonNull TodoEntry o1, @NonNull TodoEntry o2) {
        return Integer.compare(o1.getSortingIndex(), o2.getSortingIndex());
    }
}

class TodoEntryPriorityComparator implements Comparator<TodoEntry> {
    @Override
    public int compare(@NonNull TodoEntry o1, @NonNull TodoEntry o2) {
        return Integer.compare(o2.priority.getToday(), o1.priority.getToday());
    }
}

class TodoEntryGroupComparator implements Comparator<TodoEntry> {
    @Override
    public int compare(@NonNull TodoEntry o1, @NonNull TodoEntry o2) {
        if (o1.isFromSystemCalendar() || o2.isFromSystemCalendar()) {
            return Long.compare(o1.getCachedNearestStartMsUTC(), o2.getCachedNearestStartMsUTC());
        }
        return Integer.compare(o1.getRawGroupName().hashCode(), o2.getRawGroupName().hashCode());
    }
}
