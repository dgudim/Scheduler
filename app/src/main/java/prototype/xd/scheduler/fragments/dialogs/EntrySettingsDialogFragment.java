package prototype.xd.scheduler.fragments.dialogs;

import static java.lang.Math.max;
import static prototype.xd.scheduler.utilities.DialogUtilities.displayAttentionDialog;
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

import androidx.annotation.ColorInt;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import org.apache.commons.lang3.mutable.MutableObject;

import java.util.List;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.EntrySettingsBinding;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoEntry;
import prototype.xd.scheduler.entities.settings_entries.EntryPreviewContainer;
import prototype.xd.scheduler.utilities.DialogUtilities;
import prototype.xd.scheduler.utilities.Utilities;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;

public class EntrySettingsDialogFragment extends PopupSettingsDialogFragment { // NOSONAR
    
    @NonNull
    protected static MutableLiveData<GroupConfirmationData> confirmedGroupName = new MutableLiveData<>();
    @NonNull
    protected static MutableLiveData<Group> deletedGroup = new MutableLiveData<>();
    
    public record GroupConfirmationData(@NonNull Group selectedGroup, @NonNull String name,
                                        @NonNull Group existingGroup) {
    }
    
    public static class EntrySettingsDialogData extends ViewModel {
        
        public final MutableObject<TodoEntry> entry = new MutableObject<>();
        
        @NonNull
        public static EntrySettingsDialogData getInstance(@NonNull ContextWrapper wrapper) {
            return new ViewModelProvider(wrapper.activity).get(EntrySettingsDialogData.class);
        }
    }
    
    private TodoEntry entry;
    
    public static void show(@NonNull final TodoEntry entry, @NonNull ContextWrapper wrapper) {
        EntrySettingsDialogData.getInstance(wrapper).entry.setValue(entry);
        new EntrySettingsDialogFragment().show(wrapper.childFragmentManager, "entry_settings");
    }
    
    @Override
    protected void setVariablesFromData() {
        entry = EntrySettingsDialogData.getInstance(wrapper).entry.getValue();
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
    
    @Override
    public void buildDialogBodyStatic(@NonNull EntrySettingsBinding bnd) {
        bnd.hideExpiredItemsByTimeContainer.setVisibility(View.GONE);
        bnd.hideByContentContainer.setVisibility(View.GONE);
        bnd.entrySettingsTitle.setVisibility(View.GONE);
        bnd.showOnLockContainer.setVisibility(View.GONE);
        super.buildDialogBodyStatic(bnd);
    }
    
    @Override
    public void buildDialogBodyDynamic(@NonNull EntrySettingsBinding bnd) {
        updateAllIndicators(bnd);
        entryPreviewContainer.refreshAll(true);
        
        final List<Group> groupList = todoEntryManager.getGroups();
        bnd.groupSpinner.setSimpleItems(Group.groupListToNames(groupList, wrapper));
        bnd.groupSpinner.setSelectedItem(max(Group.groupIndexInList(groupList, entry.getRawGroupName()), 0));
        
        bnd.editGroupButton.setOnClickListener(v -> {
            
            int selection = bnd.groupSpinner.getSelectedItem();
            
            if (selection == 0) {
                displayAttentionDialog(wrapper, R.string.null_group_edit_message, R.string.close);
                return;
            }
            
            AddEditGroupDialogFragment.show(groupList.get(selection), wrapper);
            
        });
        deletedGroup.setValue(null);
        deletedGroup.removeObservers(this);
        deletedGroup.observe(this, group -> {
            if (group != null) {
                todoEntryManager.removeGroup(group);
                rebuild();
            }
        });
        
        bnd.groupSpinner.setOnItemClickListener((parent, view, position, id) -> {
            if (todoEntryManager.changeEntryGroup(entry, groupList.get(position))) {
                rebuild();
            }
        });
        
        bnd.addGroupButton.setOnClickListener(v -> AddEditGroupDialogFragment.show(wrapper));
        confirmedGroupName.setValue(null);
        confirmedGroupName.removeObservers(this);
        confirmedGroupName.observe(this, confirmationData -> {
            if (confirmationData == null) {
                return;
            }
            Group selectedGroup = confirmationData.selectedGroup;
            String name = confirmationData.name;
            if (selectedGroup.isNull()) {
                // We are adding a new group
                // pair.getRight() - existing group with this name
                addGroupToGroupList(name, confirmationData.existingGroup);
            } else {
                // We are editing a group
                
                int groupIndex;
                int i = 0;
                String newName = name;
                do {
                    i++;
                    groupIndex = Group.groupIndexInList(groupList, newName);
                    if (groupIndex > 0) {
                        newName = name + "(" + i + ")";
                    }
                } while (groupIndex > 0);
                
                todoEntryManager.setNewGroupName(selectedGroup, newName);
                bnd.groupSpinner.setNewItemNames(Group.groupListToNames(groupList, wrapper));
            }
        });
        
        bnd.settingsResetButton.setOnClickListener(v ->
                displayMessageDialog(wrapper, builder -> {
                    builder.setTitle(R.string.reset_settings_prompt);
                    builder.setMessage(R.string.reset_entry_settings_description);
                    builder.setIcon(R.drawable.ic_clear_all_24);
                    builder.setNegativeButton(R.string.cancel, null);
                    
                    builder.setPositiveButton(R.string.reset, (dialogInterface, whichButton) -> {
                        if (todoEntryManager.resetEntrySettings(entry)) {
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
    
    private void addGroupToGroupList(@NonNull String groupName, @NonNull Group existingGroup) {
        boolean rebuild;
        if (existingGroup.isNull()) {
            var newGroup = new Group(groupName, entry.getDisplayParams());
            todoEntryManager.addGroup(newGroup);
            entry.changeGroup(newGroup);
            rebuild = true;
        } else {
            // automatically handles parameter invalidation on other entries and saving of the group
            rebuild = todoEntryManager.setNewGroupParams(existingGroup, entry.getDisplayParams());
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
