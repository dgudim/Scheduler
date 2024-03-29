package prototype.xd.scheduler.fragments.dialogs;

import static prototype.xd.scheduler.fragments.dialogs.EntrySettingsDialogFragment.confirmedGroupName;
import static prototype.xd.scheduler.fragments.dialogs.EntrySettingsDialogFragment.deletedGroup;
import static prototype.xd.scheduler.utilities.DialogUtilities.callIfInputNotEmpty;
import static prototype.xd.scheduler.utilities.DialogUtilities.displayDeletionDialog;
import static prototype.xd.scheduler.utilities.DialogUtilities.displayMessageDialog;
import static prototype.xd.scheduler.utilities.DialogUtilities.setupButtons;
import static prototype.xd.scheduler.utilities.DialogUtilities.setupEditText;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.apache.commons.lang3.mutable.MutableObject;

import java.util.List;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.AddEditGroupDialogFragmentBinding;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.utilities.TodoEntryManager;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;

public class AddEditGroupDialogFragment extends AlertSettingsDialogFragment<AddEditGroupDialogFragmentBinding> { // NOSONAR This is a fragment
    
    public static class AddEditGroupDialogData extends ViewModel {
        
        public final MutableObject<Group> group = new MutableObject<>();
        
        @NonNull
        public static AddEditGroupDialogData getInstance(@NonNull ContextWrapper wrapper) {
            return new ViewModelProvider(wrapper.activity).get(AddEditGroupDialogData.class);
        }
    }
    
    @NonNull
    private Group group = Group.NULL_GROUP;
    
    public static void show(@NonNull Group group, @NonNull ContextWrapper wrapper) {
        AddEditGroupDialogData.getInstance(wrapper).group.setValue(group);
        new AddEditGroupDialogFragment().show(wrapper.childFragmentManager, "add_edit_group");
    }
    
    public static void show(@NonNull ContextWrapper wrapper) {
        show(Group.NULL_GROUP, wrapper);
    }
    
    @Override
    protected void setVariablesFromData() {
        group = AddEditGroupDialogData.getInstance(wrapper).group.getValue();
    }
    
    @Override
    protected void buildDialogFrame(@NonNull MaterialAlertDialogBuilder builder) {
        builder.setTitle(group.isNull() ? R.string.add_current_config_as_group_prompt : R.string.edit_group);
        builder.setIcon(group.isNull() ? R.drawable.ic_library_add_24 : R.drawable.ic_edit_24);
        builder.setMessage(group.isNull() ? R.string.add_current_config_as_group_message : R.string.edit_group_message);
    }
    
    @Override
    protected void buildDialogBody(@NonNull AddEditGroupDialogFragmentBinding binding) {
        List<Group> groupList = TodoEntryManager.getInstance(wrapper.context).getGroups();
        
        setupEditText(binding.entryNameEditText, group.getRawName());
        
        if (group.isNull()) {
            binding.deleteGroupButton.setVisibility(View.GONE);
        } else {
            binding.deleteGroupButton.setOnClickListener(v ->
                    displayDeletionDialog(wrapper, (deletionDialog, whichButton) -> {
                        deletedGroup.setValue(group);
                        dismiss();
                    }));
        }
        
        setupButtons(this, binding.twoButtons,
                R.string.cancel, group.isNull() ? R.string.add : R.string.save,
                v -> callIfInputNotEmpty(binding.entryNameEditText, name -> {
                    Group existingGroup = Group.findGroupInList(groupList, name);
                    if (existingGroup.isNull()) {
                        onConfirm(name, existingGroup);
                    } else {
                        displayMessageDialog(wrapper, builder -> {
                            builder.setTitle(group.isNull() ? R.string.overwrite_prompt : R.string.rename_prompt);
                            builder.setMessage(R.string.group_with_same_name_exists);
                            builder.setIcon(R.drawable.ic_settings_45);
                            builder.setNegativeButton(R.string.cancel, null);
                            builder.setPositiveButton(group.isNull() ? R.string.overwrite : R.string.rename,
                                    (dialogInterface, whichButton) -> onConfirm(name, existingGroup));
                        });
                    }
                }));
    }
    
    @NonNull
    @Override
    protected AddEditGroupDialogFragmentBinding inflate(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return AddEditGroupDialogFragmentBinding.inflate(inflater, container, false);
    }
    
    private void onConfirm(@NonNull String name, @NonNull Group existingGroup) {
        confirmedGroupName.setValue(new EntrySettingsDialogFragment.GroupConfirmationData(group, name, existingGroup));
        dismiss();
    }
}
