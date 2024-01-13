package prototype.xd.scheduler.fragments.dialogs;

import static prototype.xd.scheduler.utilities.DialogUtilities.getRecyclerviewGap;
import static prototype.xd.scheduler.utilities.ImageUtilities.dpToPx;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.adapters.CalendarEventViewAdapter;
import prototype.xd.scheduler.databinding.ListViewBinding;
import prototype.xd.scheduler.entities.SystemCalendar;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;

public class CalendarEventListDialogFragment extends BaseCachedDialogFragment<ListViewBinding, AlertDialog> { // NOSONAR
    private CalendarEventViewAdapter calendarEventViewAdapter;
    private SystemCalendar calendar;
    
    public void show(@NonNull final SystemCalendar calendar, @NonNull final ContextWrapper wrapper) {
        this.calendar = calendar;
        show(wrapper.childFragmentManager, calendar + " events");
    }
    
    @NonNull
    @Override
    protected ListViewBinding inflate(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return ListViewBinding.inflate(inflater, container, false);
    }
    
    @Override
    protected void buildDialogStatic(@NonNull ListViewBinding binding, @NonNull AlertDialog dialog) {
        calendarEventViewAdapter = new CalendarEventViewAdapter(wrapper);
        
        dialog.setIcon(R.drawable.ic_calendar_month_55);
        dialog.setTitle(R.string.title_all_calendar_events);
        dialog.setMessage(wrapper.getString(R.string.title_all_calendar_events_description));
        
        int pad = dpToPx(9);
        binding.recyclerView.setPadding(0, 0, pad, pad);
        binding.recyclerView.addItemDecoration(getRecyclerviewGap(wrapper, LinearLayout.VERTICAL, R.dimen.list_item_vertical_padding));
        binding.recyclerView.setAdapter(calendarEventViewAdapter);
        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(wrapper.context));
    }
    
    @Override
    protected void buildDialogDynamic(@NonNull ListViewBinding binding, @NonNull AlertDialog dialog) {
        calendarEventViewAdapter.setCalendar(calendar);
    }
    
    @NonNull
    @Override
    protected AlertDialog buildDialog() {
        return getAlertDialog();
    }
}
