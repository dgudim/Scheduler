package prototype.xd.scheduler.fragments.dialogs;

import static java.lang.Math.max;
import static prototype.xd.scheduler.entities.Group.groupIndexInList;
import static prototype.xd.scheduler.utilities.DateManager.currentlySelectedDayUTC;
import static prototype.xd.scheduler.utilities.DialogUtilities.callIfInputNotEmpty;
import static prototype.xd.scheduler.utilities.Static.DAY_FLAG_GLOBAL_STR;
import static prototype.xd.scheduler.utilities.Static.END_DAY_UTC;
import static prototype.xd.scheduler.utilities.Static.IS_COMPLETED;
import static prototype.xd.scheduler.utilities.Static.START_DAY_UTC;
import static prototype.xd.scheduler.utilities.Static.TEXT_VALUE;
import static prototype.xd.scheduler.utilities.Utilities.fancyHideUnhideView;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.apache.commons.lang3.mutable.MutableObject;

import java.util.List;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.AddEditEntryDialogFragmentBinding;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoEntry;
import prototype.xd.scheduler.utilities.DialogUtilities;
import prototype.xd.scheduler.utilities.SArrayMap;
import prototype.xd.scheduler.utilities.TodoEntryManager;
import prototype.xd.scheduler.utilities.Utilities;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;
import prototype.xd.scheduler.views.DateSelectButton;
import prototype.xd.scheduler.views.SelectableAutoCompleteTextView;

public class AddEditEntryDialogFragment extends AlertSettingsDialogFragment<AddEditEntryDialogFragmentBinding> { // NOSONAR
    
    public static class AddEditEntryDialogData extends ViewModel {
        
        public final MutableObject<TodoEntry> entry = new MutableObject<>();
        
        @NonNull
        public static AddEditEntryDialogData getInstance(@NonNull ContextWrapper wrapper) {
            return new ViewModelProvider(wrapper.activity).get(AddEditEntryDialogData.class);
        }
    }
    
    private TodoEntry entry;
    private TodoEntryManager todoEntryManager;
    private List<Group> groupList;
    
    public static void show(@Nullable TodoEntry entry, @NonNull ContextWrapper wrapper) {
        AddEditEntryDialogData.getInstance(wrapper).entry.setValue(entry);
        new AddEditEntryDialogFragment().show(wrapper.childFragmentManager, "add_edit_todo_entry");
    }
    
    @Override
    protected void setVariablesFromData() {
        entry = AddEditEntryDialogData.getInstance(wrapper).entry.getValue();
        todoEntryManager = TodoEntryManager.getInstance(wrapper.context);
        groupList = todoEntryManager.getGroups();
    }
    
    @NonNull
    @Override
    protected AddEditEntryDialogFragmentBinding inflate(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return AddEditEntryDialogFragmentBinding.inflate(inflater, container, false);
    }
    
    @Override
    protected void buildDialogFrame(@NonNull MaterialAlertDialogBuilder builder) {
        builder.setTitle(entry == null ? R.string.add_event_fab : R.string.edit_event);
        builder.setIcon(entry == null ? R.drawable.ic_add_task_24 : R.drawable.ic_edit_24);
    }
    
    @Override
    protected void buildDialogBody(@NonNull AddEditEntryDialogFragmentBinding binding) {
        
        binding.dayFromButton.setRole(DateSelectButton.Role.START_DAY, binding.dayToButton);
        binding.dayToButton.setRole(DateSelectButton.Role.END_DAY, binding.dayFromButton);
        
        binding.globalEntrySwitch.setOnCheckedChangeListener((buttonView, isChecked, fromUser) -> {
            fancyHideUnhideView(binding.dayFromButton, !isChecked, fromUser);
            fancyHideUnhideView(binding.dayToButton, !isChecked, fromUser);
            fancyHideUnhideView(binding.dateFromToArrow, !isChecked, fromUser);
            fancyHideUnhideView(binding.divider2, !isChecked, fromUser);
        });
        
        String[] items = Group.groupListToNames(groupList, wrapper);
        SelectableAutoCompleteTextView groupSpinner = binding.groupSpinner;
        groupSpinner.setSimpleItems(items);
        
        int initialGroupIndex = entry == null ? 0 : max(groupIndexInList(groupList, entry.getRawGroupName()), 0);
        
        final int[] selectedIndex = {initialGroupIndex};
        
        groupSpinner.setSelectedItem(initialGroupIndex);
        groupSpinner.setOnItemClickListener((parent, view, position, id) -> selectedIndex[0] = position);
        
        DialogUtilities.setupButtons(this, binding.twoButtons,
                R.string.cancel, entry == null ? R.string.add : R.string.save,
                v -> callIfInputNotEmpty(binding.entryNameEditText, text -> {
                    onConfirmed(text, binding, selectedIndex[0]);
                    dismiss();
                }));
        
        binding.dayFromButton.setup(wrapper.childFragmentManager, entry == null ? currentlySelectedDayUTC : entry.startDayLocal.get());
        binding.dayToButton.setup(wrapper.childFragmentManager, entry == null ? currentlySelectedDayUTC : entry.endDayLocal.get());
        
        binding.globalEntrySwitch.setChecked(entry != null && entry.isGlobal());
        
        DialogUtilities.setupEditText(binding.entryNameEditText, entry == null ? "" : entry.getRawTextValue());
    }
    
    public void onConfirmed(@NonNull String text, @NonNull AddEditEntryDialogFragmentBinding dialogBinding, int selectedIndex) {
        if (entry == null) {
            SArrayMap<String, String> values = new SArrayMap<>();
            values.put(TEXT_VALUE, text);
            boolean isGlobal = dialogBinding.globalEntrySwitch.isChecked();
            values.put(START_DAY_UTC, isGlobal ? DAY_FLAG_GLOBAL_STR :
                    dialogBinding.dayFromButton.getSelectedDayUTCStr());
            values.put(END_DAY_UTC, isGlobal ? DAY_FLAG_GLOBAL_STR :
                    dialogBinding.dayToButton.getSelectedDayUTCStr());
            values.put(IS_COMPLETED, Boolean.toString(false));
            
            todoEntryManager.addEntry(new TodoEntry(values, groupList.get(selectedIndex), System.currentTimeMillis()));
            
            Utilities.displayToast(wrapper.context, R.string.event_created_message, Toast.LENGTH_SHORT);
        } else {
            entry.changeGroup(groupList.get(selectedIndex));
            boolean isGlobal = dialogBinding.globalEntrySwitch.isChecked();
            if (isGlobal) {
                // change entry type to global
                entry.changeParameters(
                        TEXT_VALUE, text,
                        IS_COMPLETED, Boolean.toString(false),
                        START_DAY_UTC, DAY_FLAG_GLOBAL_STR,
                        END_DAY_UTC, DAY_FLAG_GLOBAL_STR);
            } else {
                // change entry type to regular
                entry.changeParameters(
                        TEXT_VALUE, text,
                        START_DAY_UTC, dialogBinding.dayFromButton.getSelectedDayUTCStr(),
                        END_DAY_UTC, dialogBinding.dayToButton.getSelectedDayUTCStr());
            }
            // save stuff, notify days changed, etc.
            todoEntryManager.performDeferredTasks();
            Utilities.displayToast(wrapper.context, R.string.event_saved_message, Toast.LENGTH_SHORT);
        }
    }
}
