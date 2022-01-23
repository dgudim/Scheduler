package prototype.xd.scheduler.utilities;

import static prototype.xd.scheduler.MainActivity.preferences;
import static prototype.xd.scheduler.entities.Group.BLANK_NAME;
import static prototype.xd.scheduler.entities.Group.createGroup;
import static prototype.xd.scheduler.entities.Group.readGroupFile;
import static prototype.xd.scheduler.entities.Group.saveGroupsFile;
import static prototype.xd.scheduler.utilities.Keys.*;
import static prototype.xd.scheduler.utilities.Utilities.addSeekBarChangeListener;
import static prototype.xd.scheduler.utilities.Utilities.addSwitchChangeListener;
import static prototype.xd.scheduler.utilities.Utilities.invokeColorDialogue;
import static prototype.xd.scheduler.utilities.Utilities.saveEntries;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

import prototype.xd.scheduler.HomeFragment;
import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoListEntry;

public class EntrySettings {
    
    TodoListEntry entry;
    TextView fontColor_view_state;
    TextView bgColor_view_state;
    TextView padColor_view_state;
    TextView adaptiveColor_switch_state;
    TextView priority_state;
    TextView padSize_state;
    TextView show_on_lock_state;
    TextView adaptiveColor_bar_state;
    TextView showDaysBeforehand_bar_state;
    TextView showDaysAfter_bar_state;
    
    public EntrySettings(final Context context, final HomeFragment fragment, View settingsView, final TodoListEntry entry, ArrayList<TodoListEntry> allEntries) {
        
        initialise(entry, context, fragment, settingsView, allEntries);
        
        final AlertDialog.Builder alert = new AlertDialog.Builder(context);
        
        alert.setOnDismissListener(dialog -> {
            saveEntries(fragment.todoListEntries);
            fragment.listViewAdapter.updateData(preferences.getBoolean(NEED_TO_RECONSTRUCT_BITMAP, false));
            preferences.edit().putBoolean(NEED_TO_RECONSTRUCT_BITMAP, false).apply();
        });
        alert.setView(settingsView);
        alert.show();
    }
    
    private void initialise(final TodoListEntry entry, final Context context, final HomeFragment fragment, final View settingsView, final ArrayList<TodoListEntry> allEntries) {
        this.entry = entry;
        
        View fontColor_view = settingsView.findViewById(R.id.textColor);
        View bgColor_view = settingsView.findViewById(R.id.backgroundColor);
        View padColor_view = settingsView.findViewById(R.id.bevelColor);
        View add_group = settingsView.findViewById(R.id.addGroup);
        fontColor_view.setBackgroundColor(entry.fontColor);
        bgColor_view.setBackgroundColor(entry.bgColor);
        padColor_view.setBackgroundColor(entry.bevelColor);
        
        fontColor_view_state = settingsView.findViewById(R.id.font_color_state);
        bgColor_view_state = settingsView.findViewById(R.id.background_color_state);
        padColor_view_state = settingsView.findViewById(R.id.bevel_color_state);
        priority_state = settingsView.findViewById(R.id.priority_state);
        padSize_state = settingsView.findViewById(R.id.bevel_size_state);
        show_on_lock_state = settingsView.findViewById(R.id.show_on_lock_state);
        adaptiveColor_switch_state = settingsView.findViewById(R.id.adaptive_color_state);
        adaptiveColor_bar_state = settingsView.findViewById(R.id.adaptive_color_balance_state);
        showDaysBeforehand_bar_state = settingsView.findViewById(R.id.days_beforehand_state);
        showDaysAfter_bar_state = settingsView.findViewById(R.id.days_after_state);
        
        updateAllIndicators();
        
        final ArrayList<Group> groupList = readGroupFile();
        final ArrayList<String> groupNames = new ArrayList<>();
        for (Group group : groupList) {
            groupNames.add(group.name);
        }
        final Spinner groupSpinner = settingsView.findViewById(R.id.groupSpinner);
        
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, groupNames) {
            @NonNull
            @Override
            public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                final View view = super.getView(position, convertView, parent);
                view.setLongClickable(true);
                if (convertView == null) {
                    view.setOnLongClickListener(view1 -> {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        if (groupNames.get(position).equals(BLANK_NAME)) {
                            builder.setTitle("Нельзя переименовать эту группу");
                            builder.setMessage("Ты сломаешь сброс настроек");
                        } else {
                            builder.setTitle("Изменить");
                            
                            final EditText input = new EditText(context);
                            input.setInputType(InputType.TYPE_CLASS_TEXT);
                            input.setText(groupNames.get(position));
                            input.setHint("Название");
                            
                            builder.setView(input);
                            
                            builder.setPositiveButton("Сохранить", (dialog, which) -> {
                                if (!input.getText().toString().equals(BLANK_NAME)) {
                                    groupNames.set(position, input.getText().toString());
                                    String origName = groupList.get(position).name;
                                    groupList.get(position).name = input.getText().toString();
                                    saveGroupsFile(groupList);
                                    for (TodoListEntry entry1 : allEntries) {
                                        if (entry1.group.name.equals(origName)) {
                                            entry1.group.name = input.getText().toString();
                                        }
                                    }
                                    saveEntries(fragment.todoListEntries);
                                    notifyDataSetChanged();
                                } else {
                                    dialog.dismiss();
                                    final AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
                                    builder1.setTitle("Нельзя так называть группу");
                                    builder1.setMessage("Ты сломаешь сброс настроек");
                                    builder1.show();
                                }
                            });
                            
                            builder.setNeutralButton("Удалить группу", (dialog, which) -> {
                                AlertDialog.Builder builder12 = new AlertDialog.Builder(context);
                                builder12.setTitle("Удалить");
                                builder12.setMessage("Вы уверены?");
                                
                                builder12.setPositiveButton("Да", (dialog1, which1) -> {
                                    String origName = groupNames.get(groupSpinner.getSelectedItemPosition());
                                    groupList.remove(groupSpinner.getSelectedItemPosition());
                                    groupNames.remove(groupSpinner.getSelectedItemPosition());
                                    saveGroupsFile(groupList);
                                    for (TodoListEntry entry1 : allEntries) {
                                        if (entry1.group.name.equals(origName)) {
                                            entry1.resetGroup();
                                        }
                                    }
                                    saveEntries(fragment.todoListEntries);
                                    groupSpinner.setSelection(groupNames.indexOf(BLANK_NAME));
                                    updateAllIndicators();
                                    notifyDataSetChanged();
                                });
                                
                                builder12.setNegativeButton("Нет", (dialog12, which12) -> dialog12.dismiss());
                                
                                builder12.show();
                            });
                            
                            builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());
                            
                            
                        }
                        builder.show();
                        return true;
                    });
                }
                return view;
            }
        };
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        groupSpinner.setAdapter(arrayAdapter);
        groupSpinner.setSelection(groupNames.indexOf(entry.group.name));
        
        groupSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!groupNames.get(position).equals(entry.group.name)) {
                    entry.changeGroup(groupNames.get(position));
                    saveEntries(fragment.todoListEntries);
                    preferences.edit().putBoolean(NEED_TO_RECONSTRUCT_BITMAP, true).apply();
                    initialise(entry, context, fragment, settingsView, allEntries);
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            
            }
        });
        
        add_group.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Добавить текущую конфигурацию как группу?");
            builder.setMessage("\n (Будут добавлены только те параметры, которые были изменены вручную)(Зеленые ромбики)");
            
            final EditText input = new EditText(context);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setHint("Название");
            builder.setView(input);
            
            builder.setPositiveButton("Добавить", (dialog, which) -> {
                if (input.getText().toString().equals(BLANK_NAME)) {
                    AlertDialog.Builder builder13 = new AlertDialog.Builder(context);
                    builder13.setTitle("Нельзя создать группу с таким названием");
                    builder13.setMessage("Ты сломаешь сброс настроек");
                    
                    builder13.show();
                    
                } else if (groupNames.contains(input.getText().toString())) {
                    AlertDialog.Builder builder13 = new AlertDialog.Builder(context);
                    builder13.setTitle("Группа с таким именем уже существует");
                    builder13.setMessage("Перезаписать?");
                    
                    builder13.setPositiveButton("Да", (dialog14, which14) -> {
                        Group createdGroup = createGroup(input.getText().toString(), entry.getDisplayParams());
                        groupList.set(groupNames.indexOf(input.getText().toString()), createdGroup);
                        saveGroupsFile(groupList);
                        for (TodoListEntry entry12 : allEntries) {
                            if (entry12.group.name.equals(input.getText().toString())) {
                                entry12.removeDisplayParams();
                                entry12.changeGroup(createdGroup);
                            }
                        }
                        updateAllIndicators();
                        preferences.edit().putBoolean(NEED_TO_RECONSTRUCT_BITMAP, true).apply();
                    });
                    
                    builder13.setNegativeButton("Нет", (dialog13, which13) -> dialog13.dismiss());
                    
                    builder13.show();
                } else {
                    Group createdGroup = createGroup(input.getText().toString(), entry.getDisplayParams());
                    groupNames.add(createdGroup.name);
                    groupList.add(createdGroup);
                    arrayAdapter.notifyDataSetChanged();
                    groupSpinner.setSelection(groupNames.size() - 1);
                    saveGroupsFile(groupList);
                    entry.removeDisplayParams();
                    entry.changeGroup(createdGroup);
                    saveEntries(fragment.todoListEntries);
                    updateAllIndicators();
                }
            });
            
            builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());
            
            
            builder.show();
        });
        
        settingsView.findViewById(R.id.settingsResetButton).setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Сбросить настройки?");
            
            builder.setPositiveButton("Да", (dialog, which) -> {
                entry.removeDisplayParams();
                entry.resetGroup();
                saveEntries(fragment.todoListEntries);
                preferences.edit().putBoolean(NEED_TO_RECONSTRUCT_BITMAP, true).apply();
                initialise(entry, context, fragment, settingsView, allEntries);
            });
            builder.setNegativeButton("Нет", (dialog, which) -> dialog.dismiss());
            
            builder.show();
        });
        
        fontColor_view.setOnClickListener(view -> invokeColorDialogue(
                context, view, fontColor_view_state, fragment,
                entry, FONT_COLOR, entry.fontColor, true));
        
        bgColor_view.setOnClickListener(view -> invokeColorDialogue(
                context, view, bgColor_view_state, fragment,
                entry, BG_COLOR, entry.bgColor, true));
        
        padColor_view.setOnClickListener(view -> invokeColorDialogue(
                context, view, padColor_view_state, fragment,
                entry, BEVEL_COLOR, entry.bevelColor, true));
        
        addSeekBarChangeListener(
                settingsView.findViewById(R.id.bevelThicknessDescription),
                settingsView.findViewById(R.id.bevelThicknessBar),
                padSize_state, fragment, R.string.settings_bevel_thickness, entry,
                BEVEL_THICKNESS, entry.bevelThickness);
        
        addSeekBarChangeListener(
                settingsView.findViewById(R.id.priorityDescription),
                settingsView.findViewById(R.id.priorityBar),
                priority_state, fragment, R.string.settings_priority, entry,
                PRIORITY, entry.priority);
        
        addSeekBarChangeListener(
                settingsView.findViewById(R.id.adaptive_color_balance_description),
                settingsView.findViewById(R.id.adaptive_color_balance_bar),
                adaptiveColor_bar_state, fragment, R.string.settings_adaptive_color_balance, entry,
                ADAPTIVE_COLOR_BALANCE, entry.adaptiveColorBalance);
        
        addSeekBarChangeListener(
                settingsView.findViewById(R.id.show_days_beforehand_description),
                settingsView.findViewById(R.id.show_days_beforehand_bar),
                showDaysBeforehand_bar_state, fragment, R.string.settings_show_days_beforehand, entry,
                BEFOREHAND_ITEMS_OFFSET, entry.dayOffset_beforehand);
        
        addSeekBarChangeListener(
                settingsView.findViewById(R.id.show_days_after_description),
                settingsView.findViewById(R.id.show_days_after_bar),
                showDaysAfter_bar_state, fragment, R.string.settings_show_days_after, entry,
                AFTER_ITEMS_OFFSET, entry.dayOffset_after);
        
        addSwitchChangeListener(
                settingsView.findViewById(R.id.showOnLockSwitch),
                show_on_lock_state,
                fragment, entry,
                SHOW_ON_LOCK, entry.showOnLock);
        
        addSwitchChangeListener(
                settingsView.findViewById(R.id.adaptive_color_switch),
                adaptiveColor_switch_state,
                fragment, entry,
                ADAPTIVE_COLOR_ENABLED, entry.adaptiveColorEnabled);
    }
    
    void updateAllIndicators() {
        entry.setStateIconColor(fontColor_view_state, FONT_COLOR);
        entry.setStateIconColor(bgColor_view_state, BG_COLOR);
        entry.setStateIconColor(padColor_view_state, BEVEL_COLOR);
        entry.setStateIconColor(padSize_state, BEVEL_THICKNESS);
        entry.setStateIconColor(priority_state, PRIORITY);
        entry.setStateIconColor(show_on_lock_state, SHOW_ON_LOCK);
        entry.setStateIconColor(adaptiveColor_switch_state, ADAPTIVE_COLOR_ENABLED);
        entry.setStateIconColor(adaptiveColor_bar_state, ADAPTIVE_COLOR_BALANCE);
        entry.setStateIconColor(showDaysBeforehand_bar_state, BEFOREHAND_ITEMS_OFFSET);
        entry.setStateIconColor(showDaysAfter_bar_state, AFTER_ITEMS_OFFSET);
    }
    
}
