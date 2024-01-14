package prototype.xd.scheduler.adapters;

import static prototype.xd.scheduler.utilities.DateManager.currentTimestampUTC;
import static prototype.xd.scheduler.utilities.DateManager.getTimeSpan;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import prototype.xd.scheduler.databinding.ListSelectionEventBinding;
import prototype.xd.scheduler.entities.SystemCalendar;
import prototype.xd.scheduler.entities.SystemCalendarEvent;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;

public class CalendarEventViewAdapter extends RecyclerView.Adapter<CalendarEventViewAdapter.EntryViewHolder> {
    
    private final List<SystemCalendarEvent> events;
    private final ContextWrapper wrapper;
    
    public CalendarEventViewAdapter(@NonNull ContextWrapper wrapper, @NonNull SystemCalendar calendar) {
        this.wrapper = wrapper;
        events = new ArrayList<>(calendar.systemCalendarEventMap.values());
        events.sort(Collections.reverseOrder(Comparator.comparingLong(SystemCalendarEvent::getStartMsUTC)));
        // Each event is unique
        setHasStableIds(true);
    }
    
    static class EntryViewHolder extends BindingViewHolder<ListSelectionEventBinding> {
        
        EntryViewHolder(@NonNull ListSelectionEventBinding binding) {
            super(binding);
        }
        
        private void bind(@NonNull SystemCalendarEvent event, @NonNull Context context) {
            binding.eventText.setText(event.data.title);
            binding.eventColor.setCardBackgroundColor(event.data.color);
            binding.recurrenceText.setVisibility(event.isRecurring() ? View.VISIBLE : View.GONE);
            binding.timeText.setText(getTimeSpan(event.getFirstInstanceTimeRange(), currentTimestampUTC, event.isAllDay(), context));
        }
        
    }
    
    @NonNull
    @Override
    public EntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new EntryViewHolder(ListSelectionEventBinding.inflate(wrapper.getLayoutInflater(), parent, false));
    }
    
    @Override
    public void onBindViewHolder(@NonNull EntryViewHolder holder, int position) {
        holder.bind(events.get(position), wrapper.context);
    }
    
    @Override
    public int getItemCount() {
        return events.size();
    }
    
    @Override
    public long getItemId(int position) {
        return events.get(position).hashCode();
    }
}
