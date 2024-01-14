package prototype.xd.scheduler.fragments.dialogs;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.apache.commons.lang3.mutable.MutableObject;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.adapters.CalendarColorsViewAdapter;
import prototype.xd.scheduler.databinding.ListViewBinding;
import prototype.xd.scheduler.entities.SystemCalendar;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;

public class CalendarColorSettingsDialogFragment extends AlertSettingsDialogFragment<ListViewBinding> { // NOSONAR this is a fragment
    
    public static class CalendarColorSettingsDialogData extends ViewModel {
        
        public final MutableObject<SystemCalendar> calendar = new MutableObject<>();
        
        @NonNull
        public static CalendarColorSettingsDialogData getInstance(@NonNull ContextWrapper wrapper) {
            return new ViewModelProvider(wrapper.activity).get(CalendarColorSettingsDialogData.class);
        }
    }
    
    private SystemCalendar calendar;
    
    public static void show(@NonNull final SystemCalendar calendar, @NonNull final ContextWrapper wrapper) {
        CalendarColorSettingsDialogData.getInstance(wrapper).calendar.setValue(calendar);
        new CalendarColorSettingsDialogFragment().show(wrapper.childFragmentManager, "calendar_per_color_settings");
    }
    
    @Override
    protected void setVariablesFromData() {
        calendar = CalendarColorSettingsDialogData.getInstance(wrapper).calendar.getValue();
    }
    
    @NonNull
    @Override
    protected ListViewBinding inflate(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return ListViewBinding.inflate(inflater, container, false);
    }
    
    @Override
    protected void buildDialogFrame(@NonNull MaterialAlertDialogBuilder builder) {
        builder.setIcon(R.drawable.ic_palette_45);
        builder.setTitle(R.string.title_edit_events_with_color);
        builder.setMessage(R.string.title_edit_events_with_color_description);
    }
    
    @Override
    protected void buildDialogBody(@NonNull ListViewBinding binding) {
        int pad = wrapper.getDimensionPixelSize(R.dimen.grid_item_padding);
        int padHalf = wrapper.getDimensionPixelSize(R.dimen.grid_item_padding_half);
        binding.recyclerView.setPadding(padHalf, 0, padHalf, pad);
        binding.recyclerView.setAdapter(new CalendarColorsViewAdapter(wrapper, calendar));
        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.setLayoutManager(new GridLayoutManager(wrapper.context, 2));
    }
}
