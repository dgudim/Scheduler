package prototype.xd.scheduler.views.settings;

import static java.lang.Math.max;
import static prototype.xd.scheduler.MainActivity.preferences_service;
import static prototype.xd.scheduler.entities.Group.createGroup;
import static prototype.xd.scheduler.entities.Group.groupIndexInList;
import static prototype.xd.scheduler.entities.Group.readGroupFile;
import static prototype.xd.scheduler.entities.Group.saveGroupsFile;
import static prototype.xd.scheduler.utilities.DialogueUtilities.displayConfirmationDialogue;
import static prototype.xd.scheduler.utilities.DialogueUtilities.displayEditTextDialogue;
import static prototype.xd.scheduler.utilities.Keys.ADAPTIVE_COLOR_BALANCE;
import static prototype.xd.scheduler.utilities.Keys.ADAPTIVE_COLOR_ENABLED;
import static prototype.xd.scheduler.utilities.Keys.BG_COLOR;
import static prototype.xd.scheduler.utilities.Keys.BORDER_COLOR;
import static prototype.xd.scheduler.utilities.Keys.BORDER_THICKNESS;
import static prototype.xd.scheduler.utilities.Keys.EXPIRED_ITEMS_OFFSET;
import static prototype.xd.scheduler.utilities.Keys.FONT_COLOR;
import static prototype.xd.scheduler.utilities.Keys.PRIORITY;
import static prototype.xd.scheduler.utilities.Keys.SERVICE_UPDATE_SIGNAL;
import static prototype.xd.scheduler.utilities.Keys.SHOW_ON_LOCK;
import static prototype.xd.scheduler.utilities.Keys.UPCOMING_ITEMS_OFFSET;
import static prototype.xd.scheduler.utilities.Utilities.addSeekBarChangeListener;
import static prototype.xd.scheduler.utilities.Utilities.addSwitchChangeListener;
import static prototype.xd.scheduler.utilities.Utilities.invokeColorDialogue;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.utilities.TodoListEntryStorage;

public class EntrySettings extends PopupSettingsView {
    
    private final AlertDialog dialog;
    public final TodoListEntryStorage todoListEntryStorage;
    private TodoListEntry todoListEntry;
    
    public EntrySettings(final TodoListEntryStorage todoListEntryStorage, final View settingsView) {
        super(settingsView);
        this.todoListEntryStorage = todoListEntryStorage;
        
        dialog = new AlertDialog.Builder(settingsView.getContext()).setOnDismissListener(dialog -> {
            todoListEntryStorage.saveEntries();
            todoListEntryStorage.updateTodoListAdapter(false);
        }).setView(settingsView).create();
    }
    
    public void show(final TodoListEntry entry, final Context context) {
        initialise(entry, context);
        dialog.show();
    }
    
    private void initialise(TodoListEntry entry, Context context) {
        
        todoListEntry = entry;
        
        updateIndicatorsAndPreviews(entry);
        
        final ArrayList<Group> groupList = new ArrayList<>();
        groupList.add(new Group(context));
        groupList.addAll(readGroupFile());
        
        final ArrayAdapter<Group> arrayAdapter = new ArrayAdapter<Group>(context, android.R.layout.simple_spinner_item, groupList) {
            @NonNull
            @Override
            public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                final View view = super.getView(position, convertView, parent);
                if (convertView == null) {
                    if (position == 0) {
                        view.setOnLongClickListener(null);
                    } else {
                        view.setOnLongClickListener(view1 -> {
                            
                            displayEditTextDialogue(view1.getContext(),
                                    R.string.edit, R.string.title,
                                    R.string.cancel, R.string.save, R.string.delete_group,
                                    groupList.get(position).getName(),
                                    (view2, text, selectedIndex) -> {
                                        String origName = groupList.get(position).getName();
                                        groupList.get(position).setName(text);
                                        saveGroupsFile(groupList);
                                        for (TodoListEntry entry1 : todoListEntryStorage.getTodoListEntries()) {
                                            if (entry1.getGroupName().equals(origName)) {
                                                entry1.setGroupName(text);
                                            }
                                        }
                                        todoListEntryStorage.saveEntries();
                                        notifyDataSetChanged();
                                        return true;
                                    },
                                    (view2, text, selectedIndex) -> {
                                        displayConfirmationDialogue(view2.getContext(),
                                                R.string.delete, R.string.are_you_sure,
                                                R.string.no, R.string.yes,
                                                v -> {
                                                    String origName = groupList.get(group_spinner.getSelectedItemPosition()).getName();
                                                    groupList.remove(group_spinner.getSelectedItemPosition());
                                                    saveGroupsFile(groupList);
                                                    for (TodoListEntry entry1 : todoListEntryStorage.getTodoListEntries()) {
                                                        if (entry1.getGroupName().equals(origName)) {
                                                            entry1.resetGroup();
                                                        }
                                                    }
                                                    todoListEntryStorage.saveEntries();
                                                    group_spinner.setSelection(0);
                                                    updateIndicatorsAndPreviews(entry);
                                                    notifyDataSetChanged();
                                                });
                                        return true;
                                    });
                            return true;
                        });
                    }
                }
                return view;
            }
        };
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        group_spinner.setAdapter(arrayAdapter);
        group_spinner.setSelection(max(groupIndexInList(groupList, entry.getGroupName()), 0));
        
        group_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!groupList.get(position).equals(entry.getGroup())) {
                    entry.changeGroup(groupList.get(position));
                    todoListEntryStorage.saveEntries();
                    preferences_service.edit().putBoolean(SERVICE_UPDATE_SIGNAL, true).apply();
                    initialise(entry, context);
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            
            }
        });
        
        add_group.setOnClickListener(v -> displayEditTextDialogue(v.getContext(), R.string.add_current_config_as_group_prompt,
                R.string.add_current_config_as_group_message, R.string.title,
                R.string.cancel, R.string.add,
                (view, text, selection) -> {
                    int groupIndex = groupIndexInList(groupList, text);
                    if (groupIndex >= 0) {
                        
                        displayConfirmationDialogue(view.getContext(), R.string.group_with_same_name_exists, R.string.overwrite_prompt,
                                R.string.cancel, R.string.overwrite, v1 -> {
                                    Group createdGroup = createGroup(text, entry.getDisplayParams());
                                    groupList.set(groupIndex, createdGroup);
                                    saveGroupsFile(groupList);
                                    
                                    entry.removeDisplayParams();
                                    group_spinner.setSelection(groupIndex);
                                    
                                    for (TodoListEntry entry2 : todoListEntryStorage.getTodoListEntries()) {
                                        if (entry2.getGroupName().equals(text)) {
                                            entry2.changeGroup(createdGroup);
                                        }
                                    }
                                });
                        
                    } else {
                        Group createdGroup = createGroup(text, entry.getDisplayParams());
                        groupList.add(createdGroup);
                        saveGroupsFile(groupList);
                        arrayAdapter.notifyDataSetChanged();
                        entry.removeDisplayParams();
                        group_spinner.setSelection(groupList.size() - 1);
                    }
                    return true;
                }));
        
        settings_reset_button.setOnClickListener(v ->
                displayConfirmationDialogue(v.getContext(),
                        R.string.reset_settings_prompt,
                        R.string.cancel, R.string.reset,
                        (view) -> {
                            entry.removeDisplayParams();
                            entry.resetGroup();
                            todoListEntryStorage.saveEntries();
                            preferences_service.edit().putBoolean(SERVICE_UPDATE_SIGNAL, true).apply();
                            initialise(entry, context);
                        }));
        
        fontColor_select.setOnClickListener(view -> invokeColorDialogue(
                fontColor_view_state, this, todoListEntryStorage,
                entry, FONT_COLOR, entry.fontColor_original));
        
        bgColor_select.setOnClickListener(view -> invokeColorDialogue(
                bgColor_view_state, this, todoListEntryStorage,
                entry, BG_COLOR, entry.bgColor_original));
        
        borderColor_select.setOnClickListener(view -> invokeColorDialogue(
                padColor_view_state, this, todoListEntryStorage,
                entry, BORDER_COLOR, entry.borderColor_original));
        
        addSeekBarChangeListener(
                border_thickness_description,
                border_thickness_bar, border_size_state,
                this, true, R.string.settings_border_thickness,
                BORDER_THICKNESS, entry.border_thickness_original);
        
        addSeekBarChangeListener(
                priority_description,
                priority_bar, priority_state,
                this, false, R.string.settings_priority,
                PRIORITY, entry.priority);
        
        addSeekBarChangeListener(
                adaptive_color_balance_description,
                adaptive_color_balance_bar, adaptiveColor_bar_state,
                this, false, R.string.settings_adaptive_color_balance,
                ADAPTIVE_COLOR_BALANCE, entry.adaptiveColorBalance);
        
        addSeekBarChangeListener(
                show_days_beforehand_description,
                show_days_beforehand_bar, showDaysUpcoming_bar_state,
                this, false, R.string.settings_show_days_upcoming,
                UPCOMING_ITEMS_OFFSET, entry.dayOffset_upcoming);
        
        addSeekBarChangeListener(
                show_days_after_description,
                show_days_after_bar, showDaysExpired_bar_state,
                this, false, R.string.settings_show_days_expired,
                EXPIRED_ITEMS_OFFSET, entry.dayOffset_expired);
        
        addSwitchChangeListener(
                show_on_lock_switch,
                show_on_lock_state,
                todoListEntryStorage, entry,
                SHOW_ON_LOCK, entry.showOnLock);
        
        addSwitchChangeListener(
                adaptive_color_switch,
                adaptiveColor_switch_state,
                todoListEntryStorage, entry,
                ADAPTIVE_COLOR_ENABLED, entry.adaptiveColorEnabled);
    }
    
    private void updateIndicatorsAndPreviews(TodoListEntry entry) {
        updateAllIndicators();
        updatePreviews(
                entry.fontColor_original,
                entry.bgColor_original,
                entry.borderColor_original,
                entry.border_thickness_original);
    }
    
    public void changeEntryParameter(TextView icon, String parameter, String value) {
        todoListEntry.changeParameter(parameter, value);
        setStateIconColor(icon, parameter);
    }
    
    @Override
    protected void setStateIconColor(TextView icon, String parameter) {
        todoListEntry.setStateIconColor(icon, parameter);
    }
}
