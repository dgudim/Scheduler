package prototype.xd.scheduler.views.settings;

import static prototype.xd.scheduler.MainActivity.preferences;
import static prototype.xd.scheduler.entities.Group.BLANK_GROUP_NAME;
import static prototype.xd.scheduler.entities.Group.createGroup;
import static prototype.xd.scheduler.entities.Group.readGroupFile;
import static prototype.xd.scheduler.entities.Group.saveGroupsFile;
import static prototype.xd.scheduler.utilities.Keys.ADAPTIVE_COLOR_BALANCE;
import static prototype.xd.scheduler.utilities.Keys.ADAPTIVE_COLOR_ENABLED;
import static prototype.xd.scheduler.utilities.Keys.AFTER_ITEMS_OFFSET;
import static prototype.xd.scheduler.utilities.Keys.BEFOREHAND_ITEMS_OFFSET;
import static prototype.xd.scheduler.utilities.Keys.BEVEL_COLOR;
import static prototype.xd.scheduler.utilities.Keys.BEVEL_THICKNESS;
import static prototype.xd.scheduler.utilities.Keys.BG_COLOR;
import static prototype.xd.scheduler.utilities.Keys.FONT_COLOR;
import static prototype.xd.scheduler.utilities.Keys.NEED_TO_RECONSTRUCT_BITMAP;
import static prototype.xd.scheduler.utilities.Keys.PRIORITY;
import static prototype.xd.scheduler.utilities.Keys.SHOW_ON_LOCK;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

import prototype.xd.scheduler.HomeFragment;
import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoListEntry;

public class EntrySettings extends PopupSettingsView{
    
    private final TodoListEntry entry;
    
    public EntrySettings(final Context context, final HomeFragment fragment, View settingsView, final TodoListEntry entry, ArrayList<TodoListEntry> allEntries) {
        super(settingsView);
        this.entry = entry;
        
        initialise(context, fragment, allEntries);
        
        final AlertDialog.Builder alert = new AlertDialog.Builder(context);
        
        alert.setOnDismissListener(dialog -> {
            saveEntries(fragment.todoListEntries);
            fragment.listViewAdapter.updateData();
        });
        alert.setView(settingsView);
        alert.show();
    }
    
    private void initialise(final Context context, final HomeFragment fragment, final ArrayList<TodoListEntry> allEntries) {
        
        fontColor_view.setBackgroundColor(entry.fontColor);
        bgColor_view.setBackgroundColor(entry.bgColor);
        padColor_view.setBackgroundColor(entry.bevelColor);
        
        updateAllIndicators();
        
        final ArrayList<Group> groupList = readGroupFile();
        final ArrayList<String> groupNames = new ArrayList<>();
        for (Group group : groupList) {
            groupNames.add(group.name);
        }
        
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, groupNames) {
            @NonNull
            @Override
            public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                final View view = super.getView(position, convertView, parent);
                view.setLongClickable(true);
                if (convertView == null) {
                    view.setOnLongClickListener(view1 -> {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        if (groupNames.get(position).equals(BLANK_GROUP_NAME)) {
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
                                if (!input.getText().toString().equals(BLANK_GROUP_NAME)) {
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
                                    String origName = groupNames.get(group_spinner.getSelectedItemPosition());
                                    groupList.remove(group_spinner.getSelectedItemPosition());
                                    groupNames.remove(group_spinner.getSelectedItemPosition());
                                    saveGroupsFile(groupList);
                                    for (TodoListEntry entry1 : allEntries) {
                                        if (entry1.group.name.equals(origName)) {
                                            entry1.resetGroup();
                                        }
                                    }
                                    saveEntries(fragment.todoListEntries);
                                    group_spinner.setSelection(groupNames.indexOf(BLANK_GROUP_NAME));
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
        group_spinner.setAdapter(arrayAdapter);
        group_spinner.setSelection(groupNames.indexOf(entry.group.name));
        
        group_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!groupNames.get(position).equals(entry.group.name)) {
                    entry.changeGroup(groupNames.get(position));
                    saveEntries(fragment.todoListEntries);
                    preferences.edit().putBoolean(NEED_TO_RECONSTRUCT_BITMAP, true).apply();
                    initialise(context, fragment, allEntries);
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
                if (input.getText().toString().equals(BLANK_GROUP_NAME)) {
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
                    group_spinner.setSelection(groupNames.size() - 1);
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
        
        settings_reset_button.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Сбросить настройки?");
            
            builder.setPositiveButton("Да", (dialog, which) -> {
                entry.removeDisplayParams();
                entry.resetGroup();
                saveEntries(fragment.todoListEntries);
                preferences.edit().putBoolean(NEED_TO_RECONSTRUCT_BITMAP, true).apply();
                initialise(context, fragment, allEntries);
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
                bevel_thickness_description,
                bevel_thickness_bar,
                padSize_state, fragment, R.string.settings_bevel_thickness, entry,
                BEVEL_THICKNESS, entry.bevelThickness);
        
        addSeekBarChangeListener(
                priority_description,
                priority_bar,
                priority_state, fragment, R.string.settings_priority, entry,
                PRIORITY, entry.priority);
        
        addSeekBarChangeListener(
                adaptive_color_balance_description,
                adaptive_color_balance_bar,
                adaptiveColor_bar_state, fragment, R.string.settings_adaptive_color_balance, entry,
                ADAPTIVE_COLOR_BALANCE, entry.adaptiveColorBalance);
        
        addSeekBarChangeListener(
                show_days_beforehand_description,
                show_days_beforehand_bar,
                showDaysBeforehand_bar_state, fragment, R.string.settings_show_days_beforehand, entry,
                BEFOREHAND_ITEMS_OFFSET, entry.dayOffset_beforehand);
        
        addSeekBarChangeListener(
                show_days_after_description,
                show_days_after_bar,
                showDaysAfter_bar_state, fragment, R.string.settings_show_days_after, entry,
                AFTER_ITEMS_OFFSET, entry.dayOffset_after);
        
        addSwitchChangeListener(
                show_on_lock_switch,
                show_on_lock_state,
                fragment, entry,
                SHOW_ON_LOCK, entry.showOnLock);
        
        addSwitchChangeListener(
                adaptive_color_switch,
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
