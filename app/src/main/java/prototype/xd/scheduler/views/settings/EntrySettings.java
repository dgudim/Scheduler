package prototype.xd.scheduler.views.settings;

import static java.lang.Math.max;
import static prototype.xd.scheduler.entities.Group.findGroupInList;
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
import static prototype.xd.scheduler.utilities.Utilities.invokeColorDialogue;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.function.Consumer;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.GroupList;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.entities.TodoListEntryList;
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
                    // TODO: 20.11.2022 handle entry updates
                    todoListEntryManager.setBitmapUpdateFlag(false);
                }).setView(bnd.getRoot()).create();
    }
    
    public void show(final TodoListEntry entry, final Context context) {
        initialise(entry, context);
        dialog.show();
    }
    
    private void initialise(TodoListEntry entry, Context context) {
        
        todoListEntry = entry;
        
        updateAllIndicators();
        updatePreviews(todoListEntry.fontColor.get(), todoListEntry.bgColor.get(), todoListEntry.borderColor.get(), todoListEntry.borderThickness.get());
        
        final GroupList groupList = todoListEntryManager.getGroups();
        bnd.groupSpinner.setSimpleItems(Group.groupListToNames(groupList, context));
        bnd.groupSpinner.setSelectedItem(max(groupIndexInList(groupList, entry.getRawGroupName()), 0));
        
        bnd.editGroupButton.setOnClickListener(v -> {
            
            int selection = bnd.groupSpinner.getSelectedItem();
            
            if (selection == 0) {
                return;
            }
            
            Group selectedGroup = groupList.get(selection);
            String selectedGroupName = selectedGroup.getRawName();
            
            displayEditTextDialogue(v.getContext(),
                    R.string.edit, R.string.name,
                    R.string.cancel, R.string.save, R.string.delete_group,
                    selectedGroupName,
                    (view2, name, selectedIndex) -> {
                        int groupIndex = groupIndexInList(groupList, name);
                        
                        String newName = name;
                        int i = 0;
                        while (groupIndex > 0) {
                            i++;
                            newName = name + "(" + i + ")";
                            groupIndex = groupIndexInList(groupList, newName);
                        }
                        
                        selectedGroup.setName(newName);
                        bnd.groupSpinner.setNewItemNames(Group.groupListToNames(groupList, context));
                        return true;
                    },
                    (view2, text, selectedIndex) -> {
                        displayConfirmationDialogue(view2.getContext(),
                                R.string.delete, R.string.are_you_sure,
                                R.string.no, R.string.yes,
                                v1 -> {
                                    groupList.remove(selectedGroup);
                                    forEachWithGroupMatch(selectedGroupName, entry1 -> entry1.changeGroup(null));
                                    rebuild(context);
                                });
                        return true;
                    });
        });
        
        bnd.groupSpinner.setOnItemClickListener((parent, view, position, id) -> {
            if (!groupList.get(position).equals(entry.getGroup())) {
                entry.changeGroup(groupList.get(position));
                rebuild(context);
            }
        });
        
        bnd.addGroupButton.setOnClickListener(v -> displayEditTextDialogue(v.getContext(), R.string.add_current_config_as_group_prompt,
                R.string.add_current_config_as_group_message, R.string.name,
                R.string.cancel, R.string.add,
                (view, text, selection) -> {
                    Group existingGroup = findGroupInList(groupList, text);
                    if (existingGroup != null) {
                        displayConfirmationDialogue(view.getContext(), R.string.group_with_same_name_exists, R.string.overwrite_prompt,
                                R.string.cancel, R.string.overwrite, v1 -> addGroupToGroupList(groupList, text, existingGroup, context));
                    } else {
                        addGroupToGroupList(groupList, text, null, context);
                    }
                    return true;
                }));
        
        bnd.settingsResetButton.setOnClickListener(v ->
                displayConfirmationDialogue(v.getContext(),
                        R.string.reset_settings_prompt,
                        R.string.cancel, R.string.reset,
                        view -> {
                            entry.removeDisplayParams();
                            entry.changeGroup(null);
                            rebuild(context);
                        }));
        
        bnd.fontColorSelector.setOnClickListener(view -> invokeColorDialogue(
                bnd.fontColorState, this, todoListEntryManager,
                entry, FONT_COLOR, entry.fontColor.get()));
        
        bnd.backgroundColorSelector.setOnClickListener(view -> invokeColorDialogue(
                bnd.backgroundColorState, this, todoListEntryManager,
                entry, BG_COLOR, entry.bgColor.get()));
        
        bnd.borderColorSelector.setOnClickListener(view -> invokeColorDialogue(
                bnd.borderColorState, this, todoListEntryManager,
                entry, BORDER_COLOR, entry.borderColor.get()));
        
        Utilities.setSliderChangeListener(
                bnd.borderThicknessDescription,
                bnd.borderThicknessBar, bnd.borderThicknessState,
                this, bnd.previewBorder, R.string.settings_border_thickness,
                BORDER_THICKNESS, entry.borderThickness.get());
        
        Utilities.setSliderChangeListener(
                bnd.priorityDescription,
                bnd.priorityBar, bnd.priorityState,
                this, null, R.string.settings_priority,
                PRIORITY, entry.priority.get());
        
        Utilities.setSliderChangeListener(
                bnd.adaptiveColorBalanceDescription,
                bnd.adaptiveColorBalanceBar, bnd.adaptiveColorBalanceState,
                this, null, R.string.settings_adaptive_color_balance,
                ADAPTIVE_COLOR_BALANCE, entry.adaptiveColorBalance.get());
        
        Utilities.setSliderChangeListener(
                bnd.showDaysUpcomingDescription,
                bnd.showDaysUpcomingBar, bnd.showDaysUpcomingState,
                this, null, R.string.settings_show_days_upcoming,
                UPCOMING_ITEMS_OFFSET, entry.upcomingDayOffset.get());
        
        Utilities.setSliderChangeListener(
                bnd.showDaysExpiredDescription,
                bnd.showDaysExpiredBar, bnd.showDaysExpiredState,
                this, null, R.string.settings_show_days_expired,
                EXPIRED_ITEMS_OFFSET, entry.expiredDayOffset.get());
        
        Utilities.setSwitchChangeListener(
                bnd.showOnLockSwitch,
                bnd.showOnLockState,
                todoListEntryManager, entry,
                SHOW_ON_LOCK, entry.getRawParameter(SHOW_ON_LOCK, Boolean::parseBoolean));
        
    }
    
    private void forEachWithGroupMatch(String groupName, Consumer<TodoListEntry> action) {
        TodoListEntryList todoListEntries = todoListEntryManager.getTodoListEntries();
        for (TodoListEntry entry : todoListEntries) {
            if (entry.getRawGroupName().equals(groupName)) {
                action.accept(entry);
            }
        }
    }
    
    private void addGroupToGroupList(GroupList groupList,
                                     String groupName,
                                     @Nullable Group existingGroup,
                                     Context context) {
        Group newGroup;
        if (existingGroup != null) {
            // we have overwritten the group, overwrite it on other entries
            newGroup = existingGroup;
            existingGroup.setParams(todoListEntry.getDisplayParams());
            forEachWithGroupMatch(groupName, entry -> entry.changeGroup(existingGroup));
        } else {
            newGroup = new Group(groupName, todoListEntry.getDisplayParams());
            groupList.add(newGroup);
        }
        
        todoListEntry.changeGroup(newGroup);
        todoListEntry.removeDisplayParams();
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
