package prototype.xd.scheduler.adapters;

import static prototype.xd.scheduler.utilities.Utilities.getPluralString;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.CalendarColorEntryBinding;
import prototype.xd.scheduler.entities.SystemCalendar;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;
import prototype.xd.scheduler.views.settings.SystemCalendarSettings;

/**
 * Grid adapter class for displaying per color event settings
 */
public class CalendarColorsViewAdapter extends RecyclerView.Adapter<CalendarColorsViewAdapter.EntryViewHolder> {
    
    @NonNull
    private final SystemCalendarSettings systemCalendarSettings;
    
    @NonNull
    private final SystemCalendar calendar;
    
    @NonNull
    private final ContextWrapper wrapper;
    
    public CalendarColorsViewAdapter(@NonNull final SystemCalendarSettings systemCalendarSettings,
                                     @NonNull final SystemCalendar calendar,
                                     @NonNull final ContextWrapper wrapper) {
        this.systemCalendarSettings = systemCalendarSettings;
        this.calendar = calendar;
        this.wrapper = wrapper;
        // No colors repeat
        setHasStableIds(true);
    }
    
    /**
     * View holder for this adapter
     */
    static class EntryViewHolder extends RecyclerView.ViewHolder {
        
        @NonNull
        private final CalendarColorEntryBinding binding;
        
        EntryViewHolder(@NonNull final CalendarColorEntryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        
        private void bind(int eventColor, int eventCount,
                          @NonNull ContextWrapper wrapper,
                          @NonNull final SystemCalendar calendar,
                          @NonNull final SystemCalendarSettings systemCalendarSettings) {
            
            String eventPrefKey = calendar.makeEventPrefKey(eventColor);
            
            binding.color.setCardBackgroundColor(eventColor);
            binding.titleDefault.setVisibility(eventCount == calendar.data.color ? View.VISIBLE : View.GONE);
            binding.eventCount.setText(getPluralString(binding.getRoot().getContext(), R.plurals.calendar_event_count, eventCount));
            binding.openSettingsButton.setOnClickListener(v ->
                    systemCalendarSettings.show(
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
                wrapper, calendar, systemCalendarSettings);
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
