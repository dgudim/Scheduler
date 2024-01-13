package prototype.xd.scheduler.fragments.dialogs;

import static java.lang.Math.max;
import static prototype.xd.scheduler.entities.Group.findGroupInList;
import static prototype.xd.scheduler.utilities.DialogUtilities.displayAttentionDialog;
import static prototype.xd.scheduler.utilities.DialogUtilities.displayDeletionDialog;
import static prototype.xd.scheduler.utilities.DialogUtilities.displayMessageDialog;
import static prototype.xd.scheduler.utilities.Static.ADAPTIVE_COLOR_BALANCE;
import static prototype.xd.scheduler.utilities.Static.BG_COLOR;
import static prototype.xd.scheduler.utilities.Static.BORDER_COLOR;
import static prototype.xd.scheduler.utilities.Static.BORDER_THICKNESS;
import static prototype.xd.scheduler.utilities.Static.EXPIRED_ITEMS_OFFSET;
import static prototype.xd.scheduler.utilities.Static.FONT_COLOR;
import static prototype.xd.scheduler.utilities.Static.PRIORITY;
import static prototype.xd.scheduler.utilities.Static.UPCOMING_ITEMS_OFFSET;

import android.view.View;
import android.widget.TextView;

import androidx.activity.ComponentDialog;
import androidx.annotation.ColorInt;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import java.util.List;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.EntrySettingsBinding;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoEntry;
import prototype.xd.scheduler.entities.settings_entries.EntryPreviewContainer;
import prototype.xd.scheduler.utilities.DialogUtilities;
import prototype.xd.scheduler.utilities.TodoEntryManager;
import prototype.xd.scheduler.utilities.Utilities;

public class EntrySettingsDialogFragment extends PopupSettingsDialogFragment {
    
    @NonNull
    public final TodoEntryManager nntodoEntryManager;
    private TodoEntry entry;
    private final AddEditGroupDialogFragment addEditGroupDialog;
    
    public EntrySettingsDialogFragment(@NonNull final TodoEntryManager todoEntryManager) {
        super(todoEntryManager);
        nntodoEntryManager = todoEntryManager;
        addEditGroupDialog = new AddEditGroupDialogFragment();
    }
    
    @NonNull
    @Override
    public EntryPreviewContainer getEntryPreviewContainer(@NonNull EntrySettingsBinding bnd) {
        return new EntryPreviewContainer(wrapper, bnd.previewContainer, false) {
            @ColorInt
            @Override
            protected int currentFontColorGetter() {
                return entry.fontColor.getToday();
            }
            
            @ColorInt
            @Override
            protected int currentBgColorGetter() {
                return entry.bgColor.getToday();
            }
            
            @ColorInt
            @Override
            protected int currentBorderColorGetter() {
                return entry.borderColor.getToday();
            }
            
            @Override
            protected int currentBorderThicknessGetter() {
                return entry.borderThickness.getToday();
            }
            
            @IntRange(from = 0, to = 10)
            @Override
            protected int adaptiveColorBalanceGetter() {
                return entry.adaptiveColorBalance.getToday();
            }
        };
    }
    
    public void show(@NonNull final TodoEntry entry, @NonNull FragmentManager manager) {
        this.entry = entry;
        show(manager, "entry_settings: " + entry);
    }
    
    @Override
    protected void buildDialogStatic(@NonNull EntrySettingsBinding bnd, @NonNull ComponentDialog dialog) {
        bnd.hideExpiredItemsByTimeContainer.setVisibility(View.GONE);
        bnd.hideByContentContainer.setVisibility(View.GONE);
        bnd.entrySettingsTitle.setVisibility(View.GONE);
        bnd.showOnLockContainer.setVisibility(View.GONE);
        super.buildDialogStatic(bnd, dialog);
    }
    
    
    @Override
    public void buildDialogDynamic(@NonNull EntrySettingsBinding bnd, @NonNull ComponentDialog dialog) {
        updateAllIndicators(bnd);
        entryPreviewContainer.refreshAll(true);
        
        final List<Group> groupList = nntodoEntryManager.getGroups();
        bnd.groupSpinner.setSimpleItems(Group.groupListToNames(groupList, wrapper));
        bnd.groupSpinner.setSelectedItem(max(Group.groupIndexInList(groupList, entry.getRawGroupName()), 0));
        
        bnd.editGroupButton.setOnClickListener(v -> {
            
            int selection = bnd.groupSpinner.getSelectedItem();
            
            if (selection == 0) {
                displayAttentionDialog(wrapper, R.string.null_group_edit_message, R.string.close);
                return;
            }
            
            var selectedGroup = groupList.get(selection);
            
            addEditGroupDialog.show(selectedGroup,
                    name -> {
                        int groupIndex = Group.groupIndexInList(groupList, name);
                        
                        String newName = name;
                        int i = 0;
                        while (groupIndex > 0) {
                            i++;
                            newName = name + "(" + i + ")";
                            groupIndex = Group.groupIndexInList(groupList, newName);
                        }
                        
                        nntodoEntryManager.setNewGroupName(selectedGroup, newName);
                        bnd.groupSpinner.setNewItemNames(Group.groupListToNames(groupList, wrapper));
                    }, additionDialog ->
                            displayDeletionDialog(wrapper, (deletionDialog, whichButton) -> {
                                additionDialog.dismiss();
                                nntodoEntryManager.removeGroup(selection);
                                rebuild();
                            }), wrapper);
            
        });
        
        bnd.groupSpinner.setOnItemClickListener((parent, view, position, id) -> {
            if (nntodoEntryManager.changeEntryGroup(entry, groupList.get(position))) {
                rebuild();
            }
        });
        
        bnd.addGroupButton.setOnClickListener(v -> addEditGroupDialog.show(null,
                text -> {
                    var existingGroup = findGroupInList(groupList, text);
                    if (existingGroup.isNullGroup()) {
                        addGroupToGroupList(text, null);
                    } else {
                        displayMessageDialog(wrapper, builder -> {
                            builder.setTitle(R.string.group_with_same_name_exists);
                            builder.setMessage(R.string.overwrite_prompt);
                            builder.setIcon(R.drawable.ic_settings_45);
                            builder.setNegativeButton(R.string.cancel, null);
                            
                            builder.setPositiveButton(R.string.overwrite, (dialogInterface, whichButton) ->
                                    addGroupToGroupList(text, existingGroup));
                        });
                    }
                }, null, wrapper));
        
        bnd.settingsResetButton.setOnClickListener(v ->
                displayMessageDialog(wrapper, builder -> {
                    builder.setTitle(R.string.reset_settings_prompt);
                    builder.setMessage(R.string.reset_entry_settings_description);
                    builder.setIcon(R.drawable.ic_clear_all_24);
                    builder.setNegativeButton(R.string.cancel, null);
                    
                    builder.setPositiveButton(R.string.reset, (dialogInterface, whichButton) -> {
                        if (nntodoEntryManager.resetEntrySettings(entry)) {
                            rebuild();
                        }
                    });
                }));
        
        bnd.currentFontColorSelector.setOnClickListener(view -> DialogUtilities.displayColorPicker(
                wrapper,
                bnd.fontColorState, this,
                FONT_COLOR.CURRENT,
                parameterKey -> entry.fontColor.getToday()));
        
        bnd.currentBackgroundColorSelector.setOnClickListener(view -> DialogUtilities.displayColorPicker(
                wrapper,
                bnd.backgroundColorState, this,
                BG_COLOR.CURRENT,
                parameterKey -> entry.bgColor.getToday()));
        
        bnd.currentBorderColorSelector.setOnClickListener(view -> DialogUtilities.displayColorPicker(
                wrapper,
                bnd.borderColorState, this,
                BORDER_COLOR.CURRENT,
                parameterKey -> entry.borderColor.getToday()));
        
        Utilities.setSliderChangeListener(
                bnd.borderThicknessDescription,
                bnd.borderThicknessSlider, bnd.borderThicknessState,
                this, R.string.settings_border_thickness,
                BORDER_THICKNESS.CURRENT,
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
        
        if (entry.isGlobal()) {
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
                                     @Nullable Group existingGroup) {
        boolean rebuild;
        if (existingGroup != null) {
            // automatically handles parameter invalidation on other entries and saving of the group
            rebuild = nntodoEntryManager.setNewGroupParams(existingGroup, entry.getDisplayParams());
        } else {
            var newGroup = new Group(groupName, entry.getDisplayParams());
            nntodoEntryManager.addGroup(newGroup);
            entry.changeGroup(newGroup);
            rebuild = true;
        }
    
        entry.removeDisplayParams();
        if (rebuild) {
            rebuild();
        }
    }
    
    @Override
    public <T> void notifyParameterChanged(@NonNull TextView displayTo, @NonNull String
            parameterKey, @NonNull T value) {
        entry.changeParameters(parameterKey, String.valueOf(value));
        setStateIconColor(displayTo, parameterKey);
    }
    
    @Override
    protected void setStateIconColor(@NonNull TextView icon, @NonNull String parameterKey) {
        entry.setStateIconColor(icon, parameterKey);
    }
}
