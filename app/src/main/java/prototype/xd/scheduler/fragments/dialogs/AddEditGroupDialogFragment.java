package prototype.xd.scheduler.fragments.dialogs;

import static prototype.xd.scheduler.utilities.DialogUtilities.callIfInputNotEmpty;
import static prototype.xd.scheduler.utilities.DialogUtilities.setupButtons;
import static prototype.xd.scheduler.utilities.DialogUtilities.setupEditText;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.util.function.Consumer;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.AddEditGroupDialogFragmentBinding;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;

public class AddEditGroupDialogFragment extends BaseCachedDialogFragment<AddEditGroupDialogFragmentBinding, AlertDialog> { // NOSONAR This is a fragment
    
    private Group group;
    private Consumer<String> confirmationListener; // NOSONAR, used is show()
    private Consumer<AddEditGroupDialogFragment> deletionListener;
    
    public void show(@Nullable Group group,
                     @NonNull Consumer<String> confirmationListener,
                     @Nullable Consumer<AddEditGroupDialogFragment> deletionListener,
                     @NonNull ContextWrapper wrapper) {
        this.confirmationListener = confirmationListener;
        this.deletionListener = deletionListener;
        this.group = group;
        show(wrapper.fragmentManager, "add_edit" + group);
    }
    
    @Override
    protected void buildDialogStatic(@NonNull AddEditGroupDialogFragmentBinding binding, @NonNull AlertDialog dialog) {
        // All of it is dynamic
    }
    
    @Override
    protected void buildDialogDynamic(@NonNull AddEditGroupDialogFragmentBinding binding, @NonNull AlertDialog dialog) {
        dialog.setTitle(group == null ? R.string.add_current_config_as_group_prompt : R.string.edit_group);
        dialog.setIcon(group == null ? R.drawable.ic_library_add_24 : R.drawable.ic_edit_24);
        dialog.setMessage(group == null ? wrapper.getString(R.string.add_current_config_as_group_message) : "");
        
        setupEditText(binding.entryNameEditText, group == null ? "" : group.getRawName());
        
        if (deletionListener != null) {
            binding.deleteGroupButton.setOnClickListener(v -> deletionListener.accept(this));
        } else {
            binding.deleteGroupButton.setVisibility(View.GONE);
        }
    
        setupButtons(this, binding.twoButtons,
                R.string.cancel, group == null ? R.string.add : R.string.save,
                v -> callIfInputNotEmpty(binding.entryNameEditText, text -> {
                    confirmationListener.accept(text);
                    dismiss();
                }));
    }
    
    @NonNull
    @Override
    protected AddEditGroupDialogFragmentBinding inflate(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return AddEditGroupDialogFragmentBinding.inflate(inflater, container, false);
    }
    
    @NonNull
    @Override
    protected AlertDialog buildDialog() {
        return getAlertDialog();
    }
}
