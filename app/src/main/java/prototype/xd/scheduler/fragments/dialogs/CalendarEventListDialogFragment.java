package prototype.xd.scheduler.fragments.dialogs;

import static prototype.xd.scheduler.utilities.DialogUtilities.getRecyclerviewGap;
import static prototype.xd.scheduler.utilities.ImageUtilities.dpToPx;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.apache.commons.lang3.mutable.MutableObject;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.adapters.CalendarEventViewAdapter;
import prototype.xd.scheduler.databinding.ListViewBinding;
import prototype.xd.scheduler.entities.SystemCalendar;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;

public class CalendarEventListDialogFragment extends AlertSettingsDialogFragment<ListViewBinding> { // NOSONAR
    
    public static class CalendarEventListDialogData extends ViewModel {
        
        public final MutableObject<SystemCalendar> calendar = new MutableObject<>();
        
        @NonNull
        public static CalendarEventListDialogData getInstance(@NonNull ContextWrapper wrapper) {
            return new ViewModelProvider(wrapper.activity).get(CalendarEventListDialogData.class);
        }
    }
    
    private SystemCalendar calendar;
    
    public static void show(@NonNull final SystemCalendar calendar, @NonNull final ContextWrapper wrapper) {
        CalendarEventListDialogData.getInstance(wrapper).calendar.setValue(calendar);
        new CalendarEventListDialogFragment().show(wrapper.childFragmentManager, "calendar_event_list");
    }
    
    @Override
    protected void setVariablesFromData() {
        calendar = CalendarEventListDialogData.getInstance(wrapper).calendar.getValue();
    }
    
    @NonNull
    @Override
    protected ListViewBinding inflate(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return ListViewBinding.inflate(inflater, container, false);
    }
    
    @Override
    protected void buildDialogFrame(@NonNull MaterialAlertDialogBuilder builder) {
        builder.setIcon(R.drawable.ic_calendar_month_55);
        builder.setTitle(R.string.title_all_calendar_events);
        builder.setMessage(R.string.title_all_calendar_events_description);
    }
    
    @Override
    protected void buildDialogBody(@NonNull ListViewBinding binding) {
        int pad = dpToPx(9);
        binding.recyclerView.setPadding(0, 0, pad, pad);
        binding.recyclerView.addItemDecoration(getRecyclerviewGap(wrapper, LinearLayout.VERTICAL, R.dimen.list_item_vertical_padding));
        binding.recyclerView.setAdapter(new CalendarEventViewAdapter(wrapper, calendar));
        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(wrapper.context));
    }
}
