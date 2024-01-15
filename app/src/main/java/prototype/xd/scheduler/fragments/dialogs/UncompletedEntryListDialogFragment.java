package prototype.xd.scheduler.fragments.dialogs;

import static prototype.xd.scheduler.utilities.DialogUtilities.getRecyclerviewGap;
import static prototype.xd.scheduler.utilities.ImageUtilities.dpToPx;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Collection;
import java.util.Set;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.adapters.TodoListViewAdapter;
import prototype.xd.scheduler.databinding.ListViewBinding;
import prototype.xd.scheduler.entities.TodoEntry;
import prototype.xd.scheduler.utilities.TodoEntryManager;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;

public class UncompletedEntryListDialogFragment extends AlertSettingsDialogFragment<ListViewBinding> { // NOSONAR
    
    public static void show(@NonNull ContextWrapper wrapper) {
        new UncompletedEntryListDialogFragment().show(wrapper.childFragmentManager, "uncompleted_entry_list");
    }
    
    @NonNull
    private final Set<TodoEntry> originalEntries = new ArraySet<>();
    
    @Override
    protected void setVariablesFromData() {
        // No variables
    }
    
    @NonNull
    @Override
    protected ListViewBinding inflate(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return ListViewBinding.inflate(inflater, container, false);
    }
    
    @Override
    protected void buildDialogBody(@NonNull ListViewBinding binding) {
        int pad = dpToPx(9);
        binding.recyclerView.setPadding(0, 0, 0, pad);
        binding.recyclerView.addItemDecoration(getRecyclerviewGap(wrapper, LinearLayout.VERTICAL, R.dimen.list_item_vertical_padding));
        var entryAdapter = new TodoListViewAdapter(wrapper);
        entryAdapter.setListSupplier(todoEntryManager -> {
            Collection<TodoEntry> entryList = todoEntryManager.getRegularTodoEntries();
            Set<TodoEntry> filteredEntryList = new ArraySet<>();
            for (var entry : entryList) {
                if ((!entry.isCompleted() && !entry.isGlobal())
                        || originalEntries.contains(entry)) {
                    filteredEntryList.add(entry);
                }
            }
            if (originalEntries.isEmpty()) {
                originalEntries.addAll(filteredEntryList);
            }
            return filteredEntryList;
        });
        binding.recyclerView.setAdapter(entryAdapter);
        binding.recyclerView.setItemAnimator(null);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(wrapper.context));
        
        // Update adapter after list update
        TodoEntryManager.getInstance(wrapper.context).onListChanged(this, aBoolean -> entryAdapter.notifyEntryListChanged());
    }
    
    @Override
    protected void buildDialogFrame(@NonNull MaterialAlertDialogBuilder builder) {
        builder.setIcon(R.drawable.ic_calendar_month_55);
        builder.setTitle(R.string.uncompleted_events);
        builder.setMessage(R.string.view_uncompleted_events_description_dialog);
    }
}
