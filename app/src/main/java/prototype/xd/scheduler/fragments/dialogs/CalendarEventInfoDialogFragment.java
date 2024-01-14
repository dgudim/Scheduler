package prototype.xd.scheduler.fragments.dialogs;

import static prototype.xd.scheduler.utilities.DateManager.currentlySelectedDayUTC;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.apache.commons.lang3.mutable.MutableObject;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.CalendarEntryInfoBinding;
import prototype.xd.scheduler.entities.TodoEntry;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;

public class CalendarEventInfoDialogFragment extends AlertSettingsDialogFragment<CalendarEntryInfoBinding> { // NOSONAR This is a fragment
    
    public static class CalendarEventInfoDialogData extends ViewModel {
        
        public final MutableObject<TodoEntry> entry = new MutableObject<>();
        
        @NonNull
        public static CalendarEventInfoDialogData getInstance(@NonNull ContextWrapper wrapper) {
            return new ViewModelProvider(wrapper.activity).get(CalendarEventInfoDialogData.class);
        }
    }
    
    private TodoEntry entry;
    
    public static void show(@Nullable TodoEntry entry, @NonNull ContextWrapper wrapper) {
        CalendarEventInfoDialogData.getInstance(wrapper).entry.setValue(entry);
        new CalendarEventInfoDialogFragment().show(wrapper.childFragmentManager, "calendar_event_info");
    }
    
    @Override
    protected void setVariablesFromData() {
        entry = CalendarEventInfoDialogData.getInstance(wrapper).entry.getValue();
    }
    
    @Override
    protected void buildDialogFrame(@NonNull MaterialAlertDialogBuilder builder) {
        builder.setIcon(R.drawable.ic_info_24);
        builder.setTitle(entry.event.data.title);
        String desc = entry.event.data.description;
        builder.setMessage(desc.isEmpty() ? wrapper.getString(R.string.no_description) : desc);
    }
    
    @Override
    protected void buildDialogBody(@NonNull CalendarEntryInfoBinding binding) {
        binding.date.setText(entry.getCalendarEntryTimeSpan(wrapper.context, currentlySelectedDayUTC));
    }
    
    @NonNull
    @Override
    protected CalendarEntryInfoBinding inflate(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return CalendarEntryInfoBinding.inflate(inflater, container, false);
    }
}
