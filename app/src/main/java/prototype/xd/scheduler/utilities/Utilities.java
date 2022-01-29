package prototype.xd.scheduler.utilities;

import static prototype.xd.scheduler.MainActivity.preferences;
import static prototype.xd.scheduler.utilities.Keys.NEED_TO_RECONSTRUCT_BITMAP;
import static prototype.xd.scheduler.utilities.Logger.ContentType.ERROR;
import static prototype.xd.scheduler.utilities.Logger.ContentType.INFO;
import static prototype.xd.scheduler.utilities.Logger.ContentType.WARNING;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getAllTodoListEntriesFromCalendars;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Comparator;

import prototype.xd.scheduler.HomeFragment;
import prototype.xd.scheduler.R;
import prototype.xd.scheduler.SettingsFragment;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.views.Switch;
import prototype.xd.scheduler.views.settings.SystemCalendarSettings;

@SuppressWarnings({"ResultOfMethodCallIgnored", "unchecked"})
public class Utilities {
    
    public static File rootDir;
    
    public static void initStorage(Context context) {
        rootDir = context.getExternalFilesDir("");
        if (rootDir == null) {
            throw new NullPointerException("Shared storage not available wtf");
        }
        if (!rootDir.exists()) {
            rootDir.mkdirs();
        }
    }
    
    public static ArrayList<TodoListEntry> loadTodoEntries(Context context) {
        
        ArrayList<TodoListEntry> readEntries = new ArrayList<>();
        try {
            ArrayList<String[]> entryParams = loadObject("list");
            ArrayList<String> entryGroupNames = loadObject("list_groupData");
            
            if (!(entryParams.size() == entryGroupNames.size())) {
                log(WARNING, "entryParams length: " + entryParams.size() + " entryGroupNames length: " + entryGroupNames.size());
            }
            
            for (int i = 0; i < entryParams.size(); i++) {
                readEntries.add(new TodoListEntry(entryParams.get(i), entryGroupNames.get(i)));
            }
        } catch (Exception e) {
            log(INFO, "no todo list");
            logException(e);
        }
        
        readEntries.addAll(getAllTodoListEntriesFromCalendars(context));
        return sortEntries(readEntries);
        
    }
    
    public static void saveEntries(ArrayList<TodoListEntry> entries) {
        try {
            ArrayList<String[]> entryParams = new ArrayList<>();
            ArrayList<String> entryGroupNames = new ArrayList<>();
            
            for (int i = 0; i < entries.size(); i++) {
                TodoListEntry entry = entries.get(i);
                if (!entry.fromSystemCalendar) {
                    entryParams.add(entry.params);
                    entryGroupNames.add(entry.group.name);
                }
            }
            
            saveObject("list", entryParams);
            saveObject("list_groupData", entryGroupNames);
            log(INFO, "saving todo list");
        } catch (Exception e) {
            log(ERROR, "missing permission, failed to save todo list");
        }
    }
    
    public static <T> T loadObject(String fileName) throws IOException, ClassNotFoundException {
        File file = new File(rootDir, fileName);
        FileInputStream f = new FileInputStream(file);
        ObjectInputStream s = new ObjectInputStream(f);
        Object object = s.readObject();
        s.close();
        return (T) object;
    }
    
    public static void saveObject(String fileName, Object object) throws IOException {
        File file = new File(rootDir, fileName);
        FileOutputStream f = new FileOutputStream(file);
        ObjectOutputStream s = new ObjectOutputStream(f);
        s.writeObject(object);
        s.close();
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
    
    public static ArrayList<TodoListEntry> sortEntries(ArrayList<TodoListEntry> entries) {
        ArrayList<TodoListEntry> newEntries = new ArrayList<>();
        ArrayList<TodoListEntry> oldEntries = new ArrayList<>();
        ArrayList<TodoListEntry> todayEntries = new ArrayList<>();
        ArrayList<TodoListEntry> globalEntries = new ArrayList<>();
        ArrayList<TodoListEntry> otherEntries = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).isTodayEntry) {
                todayEntries.add(entries.get(i));
            } else if (entries.get(i).isOldEntry) {
                oldEntries.add(entries.get(i));
            } else if (entries.get(i).isNewEntry) {
                newEntries.add(entries.get(i));
            } else if (entries.get(i).isGlobalEntry) {
                globalEntries.add(entries.get(i));
            } else {
                otherEntries.add(entries.get(i));
            }
        }
        
        ArrayList<TodoListEntry> merged = new ArrayList<>();
        merged.addAll(todayEntries);
        merged.addAll(globalEntries);
        merged.addAll(newEntries);
        merged.addAll(oldEntries);
        merged.addAll(otherEntries);
        merged.sort(new TodoListEntryGroupComparator());
        merged.sort(new TodoListEntryPriorityComparator());
        
        return merged;
    }
    
    public static void addSeekBarChangeListener(final TextView displayTo, SeekBar seekBar, final Fragment fragment, final int stringResource, final String key, int defaultValue) {
        displayTo.setText(fragment.getString(stringResource, preferences.getInt(key, defaultValue)));
        seekBar.setProgress(preferences.getInt(key, defaultValue));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                displayTo.setText(fragment.getString(stringResource, progress));
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            
            }
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                preferences.edit().putInt(key, seekBar.getProgress()).apply();
            }
        });
    }
    
    public static void addSeekBarChangeListener(final TextView displayTo, SeekBar seekBar, final TextView stateIcon, final HomeFragment fragment, final int stringResource, final TodoListEntry todoListEntry, final String parameter, int initialValue) {
        displayTo.setText(fragment.getString(stringResource, initialValue));
        seekBar.setProgress(initialValue);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                displayTo.setText(fragment.getString(stringResource, progress));
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            
            }
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                todoListEntry.changeParameter(parameter, String.valueOf(seekBar.getProgress()));
                saveEntries(fragment.todoListEntries);
                preferences.edit().putBoolean(NEED_TO_RECONSTRUCT_BITMAP, true).apply();
                todoListEntry.setStateIconColor(stateIcon, parameter);
            }
        });
    }
    
    public static void addSeekBarChangeListener(final TextView displayTo, final SeekBar seekBar, final TextView stateIcon, final SystemCalendarSettings systemCalendarSettings, final SettingsFragment fragment, final int stringResource, final String calendarKey, final String parameter, final int initialValue) {
        displayTo.setText(fragment.getString(stringResource, initialValue));
        seekBar.setProgress(initialValue);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                displayTo.setText(fragment.getString(stringResource, progress));
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            
            }
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                preferences.edit().putInt(calendarKey + "_" + parameter, seekBar.getProgress()).apply();
                systemCalendarSettings.setStateIconColor(stateIcon, parameter);
            }
        });
    }
    
    public static void addSwitchChangeListener(final Switch tSwitch, final String key, boolean defaultValue) {
        tSwitch.setChecked(preferences.getBoolean(key, defaultValue), false);
        tSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> preferences.edit().putBoolean(key, isChecked).apply());
    }
    
    public static void addSwitchChangeListener(final Switch tSwitch, final TextView stateIcon, final HomeFragment fragment, final TodoListEntry entry, final String parameter, boolean initialValue) {
        tSwitch.setChecked(initialValue, false);
        tSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            entry.changeParameter(parameter, String.valueOf(isChecked));
            saveEntries(fragment.todoListEntries);
            preferences.edit().putBoolean(NEED_TO_RECONSTRUCT_BITMAP, true).apply();
            entry.setStateIconColor(stateIcon, parameter);
        });
    }
    
    public static void addSwitchChangeListener(final Switch tSwitch, final TextView stateIcon, final SystemCalendarSettings systemCalendarSettings, final String calendarKey, final String parameter, boolean initialValue) {
        tSwitch.setChecked(initialValue, false);
        tSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(calendarKey + "_" + parameter, isChecked).apply();
            preferences.edit().putBoolean(NEED_TO_RECONSTRUCT_BITMAP, true).apply();
            systemCalendarSettings.setStateIconColor(stateIcon, parameter);
        });
    }
    
    public static void invokeColorDialogue(final Context context, final View target, final String key, final int defaultValue) {
        ColorPickerDialogBuilder
                .with(context)
                .setTitle(context.getString(R.string.choose_color))
                .initialColor(preferences.getInt(key, defaultValue))
                .showAlphaSlider(false)
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(12)
                .setPositiveButton(context.getString(R.string.apply), (dialog, selectedColor, allColors) -> {
                    preferences.edit().putInt(key, selectedColor).apply();
                    target.setBackgroundColor(preferences.getInt(key, defaultValue));
                }).setNegativeButton(R.string.cancel, (dialog, which) -> {
            
        }).build().show();
    }
    
    public static void invokeColorDialogue(final Context context, final View target, final TextView stateIcon, final HomeFragment fragment, final TodoListEntry todoListEntry, final String parameter, final int initialValue, final boolean setReconstructFlag) {
        ColorPickerDialogBuilder
                .with(context)
                .setTitle(context.getString(R.string.choose_color))
                .initialColor(initialValue)
                .showAlphaSlider(false)
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(12)
                .setPositiveButton(context.getString(R.string.apply), (dialog, selectedColor, allColors) -> {
                    todoListEntry.changeParameter(parameter, String.valueOf(selectedColor));
                    saveEntries(fragment.todoListEntries);
                    target.setBackgroundColor(selectedColor);
                    preferences.edit().putBoolean(NEED_TO_RECONSTRUCT_BITMAP, setReconstructFlag).apply();
                    todoListEntry.setStateIconColor(stateIcon, parameter);
                }).setNegativeButton(R.string.cancel, (dialog, which) -> {
            
        }).build().show();
    }
    
    public static void invokeColorDialogue(final Context context, final View target, final TextView stateIcon, SystemCalendarSettings systemCalendarSettings, String calendarKey, final String parameter, final int initialValue, final boolean setReconstructFlag) {
        ColorPickerDialogBuilder
                .with(context)
                .setTitle(context.getString(R.string.choose_color))
                .initialColor(initialValue)
                .showAlphaSlider(false)
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(12)
                .setPositiveButton(context.getString(R.string.apply), (dialog, selectedColor, allColors) -> {
                    preferences.edit().putInt(calendarKey + "_" + parameter, selectedColor).apply();
                    target.setBackgroundColor(selectedColor);
                    preferences.edit().putBoolean(NEED_TO_RECONSTRUCT_BITMAP, setReconstructFlag).apply();
                    systemCalendarSettings.setStateIconColor(stateIcon, parameter);
                }).setNegativeButton(R.string.cancel, (dialog, which) -> {
            
        }).build().show();
    }
    
    public static long RFC2445ToMilliseconds(String str) {
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
        return Integer.compare(o2.priority, o1.priority);
    }
}

class TodoListEntryGroupComparator implements Comparator<TodoListEntry> {
    @Override
    public int compare(TodoListEntry o1, TodoListEntry o2) {
        if (o1.fromSystemCalendar || o2.fromSystemCalendar) {
            return Long.compare(o1.timestamp_start, o2.timestamp_start);
        }
        return Integer.compare(o1.group.name.hashCode(), o2.group.name.hashCode());
    }
}
