package prototype.xd.scheduler.fragments.dialogs;

import static java.lang.Math.max;
import static prototype.xd.scheduler.entities.Group.groupIndexInList;
import static prototype.xd.scheduler.utilities.DateManager.currentlySelectedDayUTC;
import static prototype.xd.scheduler.utilities.DialogUtilities.callIfInputNotEmpty;
import static prototype.xd.scheduler.utilities.Utilities.fancyHideUnhideView;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.util.List;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.AddEditEntryDialogFragmentBinding;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoEntry;
import prototype.xd.scheduler.utilities.DialogUtilities;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;
import prototype.xd.scheduler.views.DateSelectButton;
import prototype.xd.scheduler.views.SelectableAutoCompleteTextView;

public class AddEditEntryDialogFragment extends BaseCachedDialogFragment<AddEditEntryDialogFragmentBinding, AlertDialog> { // NOSONAR This is a fragment
    
    private TodoEntry entry;
    private List<Group> groupList;
    private EditEntryConfirmationListener confirmationListener; // NOSONAR, used is show()
    
    public void show(@Nullable TodoEntry entry,
                     @NonNull List<Group> groupList,
                     @NonNull EditEntryConfirmationListener confirmationListener,
                     @NonNull ContextWrapper wrapper) {
        this.entry = entry;
        this.groupList = groupList;
        this.confirmationListener = confirmationListener;
        show(wrapper.childFragmentManager, "add_edit" + entry);
    }
    
    @Override
    protected void buildDialogStatic(@NonNull AddEditEntryDialogFragmentBinding binding, @NonNull AlertDialog dialog) {
        binding.dayFromButton.setRole(DateSelectButton.Role.START_DAY, binding.dayToButton);
        binding.dayToButton.setRole(DateSelectButton.Role.END_DAY, binding.dayFromButton);
        
        binding.globalEntrySwitch.setOnCheckedChangeListener((buttonView, isChecked, fromUser) -> {
            fancyHideUnhideView(binding.dayFromButton, !isChecked, fromUser);
            fancyHideUnhideView(binding.dayToButton, !isChecked, fromUser);
            fancyHideUnhideView(binding.dateFromToArrow, !isChecked, fromUser);
            fancyHideUnhideView(binding.divider2, !isChecked, fromUser);
        });
    }
    
    @Override
    protected void buildDialogDynamic(@NonNull AddEditEntryDialogFragmentBinding binding, @NonNull AlertDialog dialog) {
        dialog.setTitle(entry == null ? R.string.add_event_fab : R.string.edit_event);
        dialog.setIcon(entry == null ? R.drawable.ic_add_task_24 : R.drawable.ic_edit_24);
        
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
                    confirmationListener.onClick(text, binding, selectedIndex[0]);
                    dismiss();
                }));
        
        binding.dayFromButton.setup(wrapper.childFragmentManager, entry == null ? currentlySelectedDayUTC : entry.startDayLocal.get());
        binding.dayToButton.setup(wrapper.childFragmentManager, entry == null ? currentlySelectedDayUTC : entry.endDayLocal.get());
        
        binding.globalEntrySwitch.setChecked(entry != null && entry.isGlobal());
        
        DialogUtilities.setupEditText(binding.entryNameEditText, entry == null ? "" : entry.getRawTextValue());
    }
    
    @NonNull
    @Override
    protected AddEditEntryDialogFragmentBinding inflate(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return AddEditEntryDialogFragmentBinding.inflate(inflater, container, false);
    }
    
    @NonNull
    @Override
    protected AlertDialog buildDialog() {
        return getAlertDialog();
    }
    
    @FunctionalInterface
    public interface EditEntryConfirmationListener {
        void onClick(@NonNull String text, @NonNull AddEditEntryDialogFragmentBinding dialogBinding, int selectedIndex);
    }
}
