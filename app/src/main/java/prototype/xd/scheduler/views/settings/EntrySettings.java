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
import prototype.xd.scheduler.entities.TodoEntry;
import prototype.xd.scheduler.utilities.DialogUtilities;
import prototype.xd.scheduler.utilities.TodoEntryManager;
import prototype.xd.scheduler.utilities.Utilities;

public class EntrySettings extends PopupSettingsView {
    
    public final TodoEntryManager todoEntryManager;
    private TodoEntry todoEntry;
    
    public EntrySettings(@NonNull final TodoEntryManager todoEntryManager,
                         @NonNull final Context context,
                         @NonNull final Lifecycle lifecycle) {
        super(context, todoEntryManager, lifecycle);
        
        bnd.hideExpiredItemsByTimeContainer.setVisibility(View.GONE);
        bnd.hideByContentContainer.setVisibility(View.GONE);
        bnd.entrySettingsTitle.setVisibility(View.GONE);
        bnd.showOnLockContainer.setVisibility(View.GONE);
        
        this.todoEntryManager = todoEntryManager;
    }
    
    @Override
    public EntryPreviewContainer getEntryPreviewContainer() {
        return new EntryPreviewContainer(context, bnd.previewContainer, false) {
            @Override
            protected int currentFontColorGetter() {
                return todoEntry.fontColor.getToday();
            }
            
            @Override
            protected int currentBgColorGetter() {
                return todoEntry.bgColor.getToday();
            }
            
            @Override
            protected int currentBorderColorGetter() {
                return todoEntry.borderColor.getToday();
            }
            
            @Override
            protected int currentBorderThicknessGetter() {
                return todoEntry.borderThickness.getToday();
            }
    
            @Override
            protected int adaptiveColorBalanceGetter() {
                return todoEntry.adaptiveColorBalance.getToday();
            }
        };
    }
    
    public void show(final TodoEntry entry, final Context context) {
        initialise(entry, context);
        dialog.show();
    }
    
    private void initialise(TodoEntry entry, Context context) {
        
        todoEntry = entry;
        
        updateAllIndicators();
        entryPreviewContainer.refreshAll();
        
        final List<Group> groupList = todoEntryManager.getGroups();
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
                                todoEntryManager.removeGroup(selection);
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
        
        bnd.currentFontColorSelector.setOnClickListener(view -> DialogUtilities.invokeColorDialog(
                context, lifecycle,
                bnd.fontColorState, this,
                FONT_COLOR,
                parameterKey -> entry.fontColor.getToday()));
        
        bnd.currentBackgroundColorSelector.setOnClickListener(view -> DialogUtilities.invokeColorDialog(
                context, lifecycle,
                bnd.backgroundColorState, this,
                BG_COLOR,
                parameterKey -> entry.bgColor.getToday()));
        
        bnd.currentBorderColorSelector.setOnClickListener(view -> DialogUtilities.invokeColorDialog(
                context, lifecycle,
                bnd.borderColorState, this,
                BORDER_COLOR,
                parameterKey -> entry.borderColor.getToday()));
        
        Utilities.setSliderChangeListener(
                bnd.borderThicknessDescription,
                bnd.borderThicknessSlider, bnd.borderThicknessState,
                this, R.string.settings_border_thickness,
                BORDER_THICKNESS,
                parameterKey -> entry.borderThickness.getToday(),
                (slider, value, fromUser) -> entryPreviewContainer.setCurrentPreviewBorderThickness((int) value));
        
        Utilities.setSliderChangeListener(
                bnd.priorityDescription,
                bnd.prioritySlider, bnd.priorityState,
                this, R.string.settings_priority,
                PRIORITY,
                parameterKey -> entry.priority.getToday(), null);
        
        Utilities.setSliderChangeListener(
                bnd.adaptiveColorBalanceDescription,
                bnd.adaptiveColorBalanceSlider, bnd.adaptiveColorBalanceState,
                this, R.string.settings_adaptive_color_balance,
                ADAPTIVE_COLOR_BALANCE,
                parameterKey -> entry.adaptiveColorBalance.getToday(),
                (slider, value, fromUser) -> entryPreviewContainer.setPreviewAdaptiveColorBalance((int) value));
        
        if (todoEntry.isGlobal()) {
            // global entries can't have upcoming / expired days
            bnd.showDaysUpcomingExpiredContainer.setVisibility(View.GONE);
        } else {
            bnd.showDaysUpcomingExpiredContainer.setVisibility(View.VISIBLE);
            
            Utilities.setSliderChangeListener(
                    bnd.showDaysUpcomingDescription,
                    bnd.showDaysUpcomingSlider, bnd.showDaysUpcomingState,
                    this, R.plurals.settings_in_n_days,
                    UPCOMING_ITEMS_OFFSET,
                    parameterKey -> entry.upcomingDayOffset.getToday(), null);
            
            Utilities.setSliderChangeListener(
                    bnd.showDaysExpiredDescription,
                    bnd.showDaysExpiredSlider, bnd.showDaysExpiredState,
                    this, R.plurals.settings_after_n_days,
                    EXPIRED_ITEMS_OFFSET,
                    parameterKey -> entry.expiredDayOffset.getToday(), null);
        }
    }
    
    private void addGroupToGroupList(String groupName,
                                     @Nullable Group existingGroup,
                                     Context context) {
        if (existingGroup != null) {
            // setParams automatically handles parameter invalidation on other entries
            existingGroup.setParams(todoEntry.getDisplayParams());
            // save groups manually
            todoEntryManager.saveGroupsAsync();
        } else {
            Group newGroup = new Group(groupName, todoEntry.getDisplayParams());
            todoEntryManager.addGroup(newGroup);
            todoEntry.changeGroup(newGroup);
        }
        
        todoEntry.removeDisplayParams();
        rebuild(context);
    }
    
    private void rebuild(Context context) {
        initialise(todoEntry, context);
    }
    
    @Override
    public <T> void notifyParameterChanged(TextView displayTo, String parameterKey, T value) {
        todoEntry.changeParameters(parameterKey, String.valueOf(value));
        setStateIconColor(displayTo, parameterKey);
    }
    
    @Override
    protected void setStateIconColor(TextView icon, String parameterKey) {
        todoEntry.setStateIconColor(icon, parameterKey);
    }
}
