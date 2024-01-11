package prototype.xd.scheduler.fragments.dialogs;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.adapters.CalendarColorsViewAdapter;
import prototype.xd.scheduler.databinding.ListViewBinding;
import prototype.xd.scheduler.entities.SystemCalendar;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;
import prototype.xd.scheduler.views.settings.SystemCalendarSettings;

public class CalendarColorSettingsDialogFragment extends BaseCachedDialogFragment<ListViewBinding, AlertDialog> { // NOSONAR this is a fragment
    
    private CalendarColorsViewAdapter listViewAdapter;
    
    public void show(@NonNull final SystemCalendarSettings systemCalendarSettings,
                     @NonNull final SystemCalendar calendar,
                     @NonNull final ContextWrapper wrapper) {
        listViewAdapter = new CalendarColorsViewAdapter(systemCalendarSettings, calendar, wrapper);
        show(wrapper.fragmentManager, "edit_color" + calendar);
    }
    
    @NonNull
    @Override
    protected ListViewBinding inflate(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return ListViewBinding.inflate(inflater, container, false);
    }
    
    @Override
    protected void buildDialogStatic(@NonNull ListViewBinding binding, @NonNull AlertDialog dialog) {
        dialog.setIcon(R.drawable.ic_palette_45);
        dialog.setTitle(R.string.title_edit_events_with_color);
        dialog.setMessage(wrapper.getString(R.string.title_edit_events_with_color_description));
        
        int pad = wrapper.getResources().getDimensionPixelSize(R.dimen.grid_item_padding);
        int padHalf = wrapper.getResources().getDimensionPixelSize(R.dimen.grid_item_padding_half);
        binding.recyclerView.setPadding(padHalf, 0, padHalf, pad);
        binding.recyclerView.setAdapter(listViewAdapter);
        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.setLayoutManager(new GridLayoutManager(wrapper.context, 2));
    }
    
    @Override
    protected void buildDialogDynamic(@NonNull ListViewBinding binding, @NonNull AlertDialog dialog) {
        // None of it is dynamic
    }
    
    @NonNull
    @Override
    protected AlertDialog buildDialog() {
        return getAlertDialog();
    }
}
