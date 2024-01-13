package prototype.xd.scheduler.fragments.dialogs;

import static prototype.xd.scheduler.utilities.DateManager.currentlySelectedDayUTC;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.CalendarEntryInfoBinding;
import prototype.xd.scheduler.entities.TodoEntry;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;

public class CalendarEventInfoDialogFragment extends BaseCachedDialogFragment<CalendarEntryInfoBinding, AlertDialog> { // NOSONAR This is a fragment
    
    private TodoEntry entry;
    
    public void show(@Nullable TodoEntry entry,
                     @NonNull ContextWrapper wrapper) {
        this.entry = entry;
        show(wrapper.childFragmentManager, entry + " info");
    }
    
    @Override
    protected void buildDialogStatic(@NonNull CalendarEntryInfoBinding binding, @NonNull AlertDialog dialog) {
        dialog.setIcon(R.drawable.ic_info_24);
    }
    
    @Override
    protected void buildDialogDynamic(@NonNull CalendarEntryInfoBinding binding, @NonNull AlertDialog dialog) {
        dialog.setTitle(entry.event.data.title);
        String desc = entry.event.data.description;
        dialog.setMessage(desc.isEmpty() ? wrapper.getString(R.string.no_description) : desc);
        binding.date.setText(entry.getCalendarEntryTimeSpan(wrapper.context, currentlySelectedDayUTC));
    }
    
    @NonNull
    @Override
    protected CalendarEntryInfoBinding inflate(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return CalendarEntryInfoBinding.inflate(inflater, container, false);
    }
    
    @NonNull
    @Override
    protected AlertDialog buildDialog() {
        return getAlertDialog();
    }
    
}
