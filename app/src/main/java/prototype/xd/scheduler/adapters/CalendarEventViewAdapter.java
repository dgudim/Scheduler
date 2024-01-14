package prototype.xd.scheduler.adapters;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.ListSelectionEventBinding;
import prototype.xd.scheduler.entities.SystemCalendar;
import prototype.xd.scheduler.entities.SystemCalendarEvent;
import prototype.xd.scheduler.utilities.DateManager;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;

public class CalendarEventViewAdapter extends RecyclerView.Adapter<CalendarEventViewAdapter.EntryViewHolder> {
    
    private final SystemCalendar calendar;
    private final ContextWrapper wrapper;
    
    public CalendarEventViewAdapter(@NonNull ContextWrapper wrapper, @NonNull SystemCalendar calendar) {
        this.wrapper = wrapper;
        this.calendar = calendar;
        // Each event is unique
        setHasStableIds(true);
    }
    
    static class EntryViewHolder extends BindingViewHolder<ListSelectionEventBinding> {
        
        EntryViewHolder(@NonNull ListSelectionEventBinding binding) {
            super(binding);
        }
        
        private void bind(@NonNull SystemCalendarEvent event, @NonNull ContextWrapper wrapper) {
            binding.eventText.setText(event.data.title);
            binding.eventColor.setCardBackgroundColor(event.data.color);
            binding.recurrenceText.setVisibility(event.isRecurring() ? View.VISIBLE : View.GONE);
            String timeSpan = DateManager.getTimeSpan(event.getFirstInstanceTimeRange());
            if (event.isAllDay()) {
                binding.timeText.setText(wrapper.getString(R.string.calendar_event_all_day) + " (" + timeSpan + ")");
            } else {
                binding.timeText.setText(timeSpan);
            }
        }
        
    }
    
    @NonNull
    @Override
    public EntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new EntryViewHolder(ListSelectionEventBinding.inflate(wrapper.getLayoutInflater(), parent, false));
    }
    
    @Override
    public void onBindViewHolder(@NonNull EntryViewHolder holder, int position) {
        holder.bind(calendar.systemCalendarEventMap.valueAt(position), wrapper);
    }
    
    @Override
    public int getItemCount() {
        return calendar.systemCalendarEventMap.size();
    }
    
    @Override
    public long getItemId(int position) {
        return calendar.systemCalendarEventMap.valueAt(position).hashCode();
    }
}
