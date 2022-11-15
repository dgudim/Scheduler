package prototype.xd.scheduler.views.settings;

import static java.lang.Math.max;
import static prototype.xd.scheduler.entities.Group.groupIndexInList;
import static prototype.xd.scheduler.utilities.DialogueUtilities.displayConfirmationDialogue;
import static prototype.xd.scheduler.utilities.DialogueUtilities.displayEditTextDialogue;
import static prototype.xd.scheduler.utilities.Keys.ADAPTIVE_COLOR_BALANCE;
import static prototype.xd.scheduler.utilities.Keys.BG_COLOR;
import static prototype.xd.scheduler.utilities.Keys.BORDER_COLOR;
import static prototype.xd.scheduler.utilities.Keys.BORDER_THICKNESS;
import static prototype.xd.scheduler.utilities.Keys.EXPIRED_ITEMS_OFFSET;
import static prototype.xd.scheduler.utilities.Keys.FONT_COLOR;
import static prototype.xd.scheduler.utilities.Keys.PRIORITY;
import static prototype.xd.scheduler.utilities.Keys.SERVICE_UPDATE_SIGNAL;
import static prototype.xd.scheduler.utilities.Keys.SHOW_ON_LOCK;
import static prototype.xd.scheduler.utilities.Keys.UPCOMING_ITEMS_OFFSET;
import static prototype.xd.scheduler.utilities.PreferencesStore.servicePreferences;
import static prototype.xd.scheduler.utilities.Utilities.setSwitchChangeListener;
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

import java.util.List;
import java.util.function.Consumer;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.utilities.TodoListEntryManager;
import prototype.xd.scheduler.utilities.Utilities;

public class EntrySettings extends PopupSettingsView {
    
    public final TodoListEntryManager todoListEntryManager;
    private TodoListEntry todoListEntry;
    
    public EntrySettings(final TodoListEntryManager todoListEntryManager, @NonNull final Context context) {
        super(context);
        
        bnd.hideExpiredItemsByTimeContainer.setVisibility(View.GONE);
        bnd.hideByContentContainer.setVisibility(View.GONE);
        bnd.entrySettingsTitle.setVisibility(View.GONE);
        
        this.todoListEntryManager = todoListEntryManager;
        
        dialog = new AlertDialog.Builder(context, R.style.FullScreenDialog)
                .setOnDismissListener(dialog -> {
                    todoListEntryManager.saveGroupsAndEntriesAsync();
                    todoListEntryManager.updateTodoListAdapter(false, true);
                }).setView(bnd.getRoot()).create();
    }
    
    public void show(final TodoListEntry entry, final Context context) {
        initialise(entry, context);
        dialog.show();
    }
    
    private void initialise(TodoListEntry entry, Context context) {
        
        todoListEntry = entry;
        
        updateAllIndicators();
        updatePreviews(todoListEntry.fontColor_original, todoListEntry.bgColor_original, todoListEntry.borderColor_original, todoListEntry.border_thickness_original);
        
        final List<Group> groupList = todoListEntryManager.getGroups();
        
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
                                    R.string.edit, R.string.name,
                                    R.string.cancel, R.string.save, R.string.delete_group,
                                    groupList.get(position).getName(),
                                    (view2, text, selectedIndex) -> {
                                        int groupIndex = groupIndexInList(groupList, text);
                                        if (groupIndex == 0) {
                                            text += "(1)";
                                        } else if (groupIndex > 0) {
                                            String intermediateText = text;
                                            int i = 0;
                                            while (groupIndex > 0) {
                                                i++;
                                                intermediateText = text + "(" + i + ")";
                                                groupIndex = groupIndexInList(groupList, intermediateText);
                                            }
                                            text = intermediateText;
                                        }
                                        
                                        String origName = groupList.get(position).getName();
                                        groupList.get(position).setName(text);
                                        
                                        String finalText = text;
                                        forEachWithGroupMatch(origName, entry -> entry.setGroupName(finalText));
                                        
                                        notifyDataSetChanged();
                                        return true;
                                    },
                                    (view2, text, selectedIndex) -> {
                                        displayConfirmationDialogue(view2.getContext(),
                                                R.string.delete, R.string.are_you_sure,
                                                R.string.no, R.string.yes,
                                                v -> {
                                                    String origName = groupList.get(bnd.groupSpinner.getSelectedItemPosition()).getName();
                                                    groupList.remove(bnd.groupSpinner.getSelectedItemPosition());
                                                    forEachWithGroupMatch(origName, TodoListEntry::resetGroup);
                                                    rebuild(context);
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
        bnd.groupSpinner.setAdapter(arrayAdapter);
        bnd.groupSpinner.setSelectionSilent(max(groupIndexInList(groupList, entry.getGroupName()), 0));
    
        bnd.groupSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!groupList.get(position).equals(entry.getGroup())) {
                    entry.changeGroup(groupList.get(position));
                    rebuild(context);
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //ignore
            }
        });
        
        bnd.addGroup.setOnClickListener(v -> displayEditTextDialogue(v.getContext(), R.string.add_current_config_as_group_prompt,
                R.string.add_current_config_as_group_message, R.string.name,
                R.string.cancel, R.string.add,
                (view, text, selection) -> {
                    int groupIndex = groupIndexInList(groupList, text);
                    if (groupIndex >= 0) {
                        displayConfirmationDialogue(view.getContext(), R.string.group_with_same_name_exists, R.string.overwrite_prompt,
                                R.string.cancel, R.string.overwrite, v1 -> addGroupToGroupList(groupList, text, groupIndex, context, arrayAdapter));
                    } else {
                        addGroupToGroupList(groupList, text, groupIndex, context, arrayAdapter);
                    }
                    return true;
                }));
        
        bnd.settingsResetButton.setOnClickListener(v ->
                displayConfirmationDialogue(v.getContext(),
                        R.string.reset_settings_prompt,
                        R.string.cancel, R.string.reset,
                        view -> {
                            entry.removeDisplayParams();
                            entry.resetGroup();
                            rebuild(context);
                        }));
        
        bnd.fontColorSelector.setOnClickListener(view -> invokeColorDialogue(
                bnd.fontColorState, this, todoListEntryManager,
                entry, FONT_COLOR, entry.fontColor_original));
        
        bnd.backgroundColorSelector.setOnClickListener(view -> invokeColorDialogue(
                bnd.backgroundColorState, this, todoListEntryManager,
                entry, BG_COLOR, entry.bgColor_original));
        
        bnd.borderColorSelector.setOnClickListener(view -> invokeColorDialogue(
                bnd.borderColorState, this, todoListEntryManager,
                entry, BORDER_COLOR, entry.borderColor_original));
        
        Utilities.setSliderChangeListener(
                bnd.borderThicknessDescription,
                bnd.borderThicknessBar, bnd.borderThicknessState,
                this, bnd.previewBorder, R.string.settings_border_thickness,
                BORDER_THICKNESS, entry.border_thickness_original);
        
        Utilities.setSliderChangeListener(
                bnd.priorityDescription,
                bnd.priorityBar, bnd.priorityState,
                this, null, R.string.settings_priority,
                PRIORITY, entry.priority);
        
        Utilities.setSliderChangeListener(
                bnd.adaptiveColorBalanceDescription,
                bnd.adaptiveColorBalanceBar, bnd.adaptiveColorBalanceState,
                this, null, R.string.settings_adaptive_color_balance,
                ADAPTIVE_COLOR_BALANCE, entry.adaptiveColorBalance);
        
        Utilities.setSliderChangeListener(
                bnd.showDaysUpcomingDescription,
                bnd.showDaysUpcomingBar, bnd.showDaysUpcomingState,
                this, null, R.string.settings_show_days_upcoming,
                UPCOMING_ITEMS_OFFSET, entry.dayOffset_upcoming);
        
        Utilities.setSliderChangeListener(
                bnd.showDaysExpiredDescription,
                bnd.showDaysExpiredBar, bnd.showDaysExpiredState,
                this, null, R.string.settings_show_days_expired,
                EXPIRED_ITEMS_OFFSET, entry.dayOffset_expired);
        
        Utilities.setSwitchChangeListener(
                bnd.showOnLockSwitch,
                bnd.showOnLockState,
                todoListEntryManager, entry,
                SHOW_ON_LOCK, entry.showOnLock);
        
    }
    
    private void forEachWithGroupMatch(String groupName, Consumer<TodoListEntry> action) {
        List<TodoListEntry> todoListEntries = todoListEntryManager.getTodoListEntries();
        for(TodoListEntry entry: todoListEntries) {
            if (entry.getGroupName().equals(groupName)) {
                action.accept(entry);
            }
        }
    }
    
    private void addGroupToGroupList(List<Group> groupList,
                                     String groupName,
                                     int groupIndex,
                                     Context context,
                                     ArrayAdapter<Group> arrayAdapter) {
        Group createdGroup = new Group(groupName, todoListEntry.getDisplayParams());
        if (groupIndex >= 0) {
            groupList.set(groupIndex, createdGroup);
            forEachWithGroupMatch(groupName, entry -> entry.changeGroup(createdGroup));
        } else {
            groupIndex = groupList.size();
            groupList.add(createdGroup);
        }
        
        todoListEntry.removeDisplayParams();
        arrayAdapter.notifyDataSetChanged();
        bnd.groupSpinner.setSelectionSilent(groupIndex);
        todoListEntry.changeGroup(groupList.get(groupIndex));
        rebuild(context);
    }
    
    private void rebuild(Context context) {
        servicePreferences.edit().putBoolean(SERVICE_UPDATE_SIGNAL, true).apply();
        initialise(todoListEntry, context);
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
