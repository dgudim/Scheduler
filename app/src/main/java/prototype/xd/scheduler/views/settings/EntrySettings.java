package prototype.xd.scheduler.views.settings;

import static java.lang.Math.max;
import static prototype.xd.scheduler.entities.Group.findGroupInList;
import static prototype.xd.scheduler.utilities.DialogUtilities.displayConfirmationDialogue;
import static prototype.xd.scheduler.utilities.DialogUtilities.displayGroupAdditionEditDialog;
import static prototype.xd.scheduler.utilities.Keys.ADAPTIVE_COLOR_BALANCE;
import static prototype.xd.scheduler.utilities.Keys.BG_COLOR;
import static prototype.xd.scheduler.utilities.Keys.BORDER_COLOR;
import static prototype.xd.scheduler.utilities.Keys.BORDER_THICKNESS;
import static prototype.xd.scheduler.utilities.Keys.EXPIRED_ITEMS_OFFSET;
import static prototype.xd.scheduler.utilities.Keys.FONT_COLOR;
import static prototype.xd.scheduler.utilities.Keys.PRIORITY;
import static prototype.xd.scheduler.utilities.Keys.UPCOMING_ITEMS_OFFSET;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;

import java.util.List;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.utilities.DialogUtilities;
import prototype.xd.scheduler.utilities.TodoListEntryManager;
import prototype.xd.scheduler.utilities.Utilities;

public class EntrySettings extends PopupSettingsView {
    
    public final TodoListEntryManager todoListEntryManager;
    private TodoListEntry todoListEntry;
    
    public EntrySettings(@NonNull final TodoListEntryManager todoListEntryManager,
                         @NonNull final Context context,
                         @NonNull final Lifecycle lifecycle) {
        super(context, todoListEntryManager, lifecycle);
        
        bnd.hideExpiredItemsByTimeContainer.setVisibility(View.GONE);
        bnd.hideByContentContainer.setVisibility(View.GONE);
        bnd.entrySettingsTitle.setVisibility(View.GONE);
        bnd.showOnLockContainer.setVisibility(View.GONE);
        
        this.todoListEntryManager = todoListEntryManager;
    }
    
    public void show(final TodoListEntry entry, final Context context) {
        initialise(entry, context);
        dialog.show();
    }
    
    private void initialise(TodoListEntry entry, Context context) {
        
        todoListEntry = entry;
        
        updateAllIndicators();
        updatePreviews(
                todoListEntry.fontColor.getToday(),
                todoListEntry.bgColor.getToday(),
                todoListEntry.borderColor.getToday(),
                todoListEntry.borderThickness.getToday());
        
        final List<Group> groupList = todoListEntryManager.getGroups();
        bnd.groupSpinner.setSimpleItems(Group.groupListToNames(groupList, context));
        bnd.groupSpinner.setSelectedItem(max(Group.groupIndexInList(groupList, entry.getRawGroupName()), 0));
        
        bnd.editGroupButton.setOnClickListener(v -> {
            
            int selection = bnd.groupSpinner.getSelectedItem();
            
            if (selection == 0) {
                return;
            }
            
            Group selectedGroup = groupList.get(selection);
            String selectedGroupName = selectedGroup.getRawName();
            
            displayGroupAdditionEditDialog(v.getContext(), lifecycle,
                    R.string.edit, R.string.name,
                    R.string.cancel, R.string.save,
                    selectedGroupName,
                    (view2, name, dialogBinding, selectedIndex) -> {
                        int groupIndex = Group.groupIndexInList(groupList, name);
    
                        String newName = name;
                        int i = 0;
                        while (groupIndex > 0) {
                            i++;
                            newName = name + "(" + i + ")";
                            groupIndex = Group.groupIndexInList(groupList, newName);
                        }
    
                        selectedGroup.setName(newName);
                        bnd.groupSpinner.setNewItemNames(Group.groupListToNames(groupList, context));
                        return true;
                    }, (v2, dialog) -> displayConfirmationDialogue(v2.getContext(), lifecycle,
                            R.string.delete, R.string.are_you_sure,
                            R.string.no, R.string.yes,
                            v1 -> {
                                dialog.dismiss();
                                todoListEntryManager.removeGroup(selection);
                                rebuild(context);
                            }));
            
        });
        
        bnd.groupSpinner.setOnItemClickListener((parent, view, position, id) -> {
            if (entry.changeGroup(groupList.get(position))) {
                rebuild(context);
            }
        });
        
        bnd.addGroupButton.setOnClickListener(v -> displayGroupAdditionEditDialog(v.getContext(), lifecycle,
                R.string.add_current_config_as_group_prompt,
                R.string.add_current_config_as_group_message,
                R.string.cancel, R.string.add, "",
                (view, text, dialogueBinding, selection) -> {
                    Group existingGroup = findGroupInList(groupList, text);
                    if (existingGroup != null) {
                        displayConfirmationDialogue(view.getContext(), lifecycle,
                                R.string.group_with_same_name_exists, R.string.overwrite_prompt,
                                R.string.cancel, R.string.overwrite, v1 -> addGroupToGroupList(text, existingGroup, context));
                    } else {
                        addGroupToGroupList(text, null, context);
                    }
                    return true;
                }, null));
        
        bnd.settingsResetButton.setOnClickListener(v ->
                displayConfirmationDialogue(v.getContext(), lifecycle,
                        R.string.reset_settings_prompt, R.string.reset_calendar_settings_description,
                        R.string.cancel, R.string.reset,
                        view -> {
                            entry.removeDisplayParams();
                            entry.changeGroup(null);
                            rebuild(context);
                        }));
        
        bnd.fontColorSelector.setOnClickListener(view -> DialogUtilities.invokeColorDialog(
                context, lifecycle,
                bnd.fontColorState, this,
                FONT_COLOR,
                parameterKey -> entry.fontColor.getToday()));
        
        bnd.backgroundColorSelector.setOnClickListener(view -> DialogUtilities.invokeColorDialog(
                context, lifecycle,
                bnd.backgroundColorState, this,
                BG_COLOR,
                parameterKey -> entry.bgColor.getToday()));
        
        bnd.borderColorSelector.setOnClickListener(view -> DialogUtilities.invokeColorDialog(
                context, lifecycle,
                bnd.borderColorState, this,
                BORDER_COLOR,
                parameterKey -> entry.borderColor.getToday()));
        
        Utilities.setSliderChangeListener(
                bnd.borderThicknessDescription,
                bnd.borderThicknessBar, bnd.borderThicknessState,
                this, bnd.previewBorder, R.string.settings_border_thickness,
                BORDER_THICKNESS,
                parameterKey -> entry.borderThickness.getToday());
        
        Utilities.setSliderChangeListener(
                bnd.priorityDescription,
                bnd.priorityBar, bnd.priorityState,
                this, null, R.string.settings_priority,
                PRIORITY,
                parameterKey -> entry.priority.getToday());
        
        Utilities.setSliderChangeListener(
                bnd.adaptiveColorBalanceDescription,
                bnd.adaptiveColorBalanceBar, bnd.adaptiveColorBalanceState,
                this, null, R.string.settings_adaptive_color_balance,
                ADAPTIVE_COLOR_BALANCE,
                parameterKey -> entry.adaptiveColorBalance.getToday());
        
        if (todoListEntry.isGlobal()) {
            // global entries can't have upcoming / expired days
            bnd.showDaysUpcomingContainer.setVisibility(View.GONE);
            bnd.showDaysExpiredContainer.setVisibility(View.GONE);
        } else {
            bnd.showDaysUpcomingContainer.setVisibility(View.VISIBLE);
            bnd.showDaysExpiredContainer.setVisibility(View.VISIBLE);
            
            Utilities.setSliderChangeListener(
                    bnd.showDaysUpcomingDescription,
                    bnd.showDaysUpcomingBar, bnd.showDaysUpcomingState,
                    this, null, R.plurals.settings_show_days_upcoming,
                    UPCOMING_ITEMS_OFFSET,
                    parameterKey -> entry.upcomingDayOffset.getToday());
            
            Utilities.setSliderChangeListener(
                    bnd.showDaysExpiredDescription,
                    bnd.showDaysExpiredBar, bnd.showDaysExpiredState,
                    this, null, R.plurals.settings_show_days_expired,
                    EXPIRED_ITEMS_OFFSET,
                    parameterKey -> entry.expiredDayOffset.getToday());
        }
    }
    
    private void addGroupToGroupList(String groupName,
                                     @Nullable Group existingGroup,
                                     Context context) {
        if (existingGroup != null) {
            // setParams automatically handles parameter invalidation on other entries
            existingGroup.setParams(todoListEntry.getDisplayParams());
            // save groups manually
            todoListEntryManager.saveGroupsAsync();
        } else {
            Group newGroup = new Group(groupName, todoListEntry.getDisplayParams());
            todoListEntryManager.addGroup(newGroup);
            todoListEntry.changeGroup(newGroup);
        }
        
        todoListEntry.removeDisplayParams();
        rebuild(context);
    }
    
    private void rebuild(Context context) {
        initialise(todoListEntry, context);
    }
    
    @Override
    public <T> void notifyParameterChanged(TextView displayTo, String parameterKey, T value) {
        todoListEntry.changeParameters(parameterKey, String.valueOf(value));
        setStateIconColor(displayTo, parameterKey);
    }
    
    @Override
    protected void setStateIconColor(TextView icon, String parameterKey) {
        todoListEntry.setStateIconColor(icon, parameterKey);
    }
}
