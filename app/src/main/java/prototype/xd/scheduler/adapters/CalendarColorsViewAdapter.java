package prototype.xd.scheduler.adapters;

import static prototype.xd.scheduler.utilities.Utilities.getPluralString;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.CalendarColorEntryBinding;
import prototype.xd.scheduler.entities.SystemCalendar;
import prototype.xd.scheduler.fragments.dialogs.CalendarSettingsDialogFragment;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;

/**
 * Grid adapter class for displaying per color event settings
 */
public class CalendarColorsViewAdapter extends RecyclerView.Adapter<CalendarColorsViewAdapter.EntryViewHolder> {
    
    private final SystemCalendar calendar;
    
    @NonNull
    private final ContextWrapper wrapper;
    
    public CalendarColorsViewAdapter(@NonNull final ContextWrapper wrapper,
                                     @NonNull final SystemCalendar calendar) {
        this.wrapper = wrapper;
        this.calendar = calendar;
        // No colors repeat
        setHasStableIds(true);
    }
    
    /**
     * View holder for this adapter
     */
    static class EntryViewHolder extends BindingViewHolder<CalendarColorEntryBinding> {
        
        EntryViewHolder(@NonNull CalendarColorEntryBinding binding) {
            super(binding);
        }
        
        private void bind(int eventColor, int eventCount,
                          @NonNull ContextWrapper wrapper,
                          @NonNull final SystemCalendar calendar) {
            
            String eventPrefKey = calendar.makeEventPrefKey(eventColor);
            
            binding.color.setCardBackgroundColor(eventColor);
            binding.titleDefault.setVisibility(eventCount == calendar.data.color ? View.VISIBLE : View.GONE);
            binding.eventCount.setText(getPluralString(binding.getRoot().getContext(), R.plurals.calendar_event_count, eventCount));
            binding.openSettingsButton.setOnClickListener(v ->
                    CalendarSettingsDialogFragment.show(
                            eventPrefKey, calendar.makeEventSubKeys(eventPrefKey),
                            eventColor, wrapper));
        }
    }
    
    @NonNull
    @Override
    public EntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CalendarColorsViewAdapter.EntryViewHolder(
                CalendarColorEntryBinding.inflate(wrapper.getLayoutInflater(), parent, false));
    }
    
    @Override
    public void onBindViewHolder(@NonNull EntryViewHolder holder, int position) {
        holder.bind(
                calendar.eventColorCountMap.keyAt(position),
                calendar.eventColorCountMap.valueAt(position),
                wrapper, calendar);
    }
    
    @Override
    public int getItemCount() {
        return calendar.eventColorCountMap.size();
    }
    
    @Override
    public long getItemId(int position) {
        return calendar.eventColorCountMap.keyAt(position);
    }
}
