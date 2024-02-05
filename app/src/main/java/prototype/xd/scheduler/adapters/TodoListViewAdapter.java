package prototype.xd.scheduler.adapters;

import static java.lang.Math.max;
import static prototype.xd.scheduler.utilities.DateManager.currentlySelectedDayUTC;
import static prototype.xd.scheduler.utilities.ImageUtilities.dimColorToBg;
import static prototype.xd.scheduler.utilities.ImageUtilities.getHarmonizedFontColorWithBg;
import static prototype.xd.scheduler.utilities.ImageUtilities.getHarmonizedSecondaryFontColorWithBg;
import static prototype.xd.scheduler.utilities.ImageUtilities.getOnBgColor;
import static prototype.xd.scheduler.utilities.Static.END_DAY_UTC;
import static prototype.xd.scheduler.utilities.Static.GLOBAL_ITEMS_LABEL_POSITION;
import static prototype.xd.scheduler.utilities.Static.IS_COMPLETED;
import static prototype.xd.scheduler.utilities.Static.START_DAY_UTC;
import static prototype.xd.scheduler.utilities.Utilities.addIntFlag;
import static prototype.xd.scheduler.utilities.Utilities.removeIntFlag;

import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.ListSelectionCalendarBinding;
import prototype.xd.scheduler.databinding.ListSelectionTodoBinding;
import prototype.xd.scheduler.entities.TodoEntry;
import prototype.xd.scheduler.fragments.dialogs.AddEditEntryDialogFragment;
import prototype.xd.scheduler.fragments.dialogs.CalendarEventInfoDialogFragment;
import prototype.xd.scheduler.fragments.dialogs.CalendarSettingsDialogFragment;
import prototype.xd.scheduler.fragments.dialogs.EntrySettingsDialogFragment;
import prototype.xd.scheduler.utilities.DialogUtilities;
import prototype.xd.scheduler.utilities.Static;
import prototype.xd.scheduler.utilities.TodoEntryManager;
import prototype.xd.scheduler.utilities.Utilities;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;

/**
 * List adapter class for displaying todos and calendar entries
 */
public class TodoListViewAdapter extends RecyclerView.Adapter<TodoListViewAdapter.EntryViewHolder<?>> {
    
    /**
     * View holder for this adapter
     *
     * @param <V> ListSelectionTodoBinding or ListSelectionCalendarBinding
     */
    static class EntryViewHolder<V extends ViewBinding> extends BindingViewHolder<V> {
        
        @NonNull
        private final ContextWrapper wrapper;
        
        EntryViewHolder(@NonNull final V viewBinding,
                        @NonNull final ContextWrapper wrapper) {
            super(viewBinding);
            this.wrapper = wrapper;
        }
        
        /**
         * Display a dialog confirming entry deletion, should be called on regular non-calendar entries
         *
         * @param entry            entry to be deleted
         * @param todoEntryManager entry manager (containing the entry)
         */
        private void displayDeletionDialog(@NonNull final TodoEntry entry,
                                           @NonNull final TodoEntryManager todoEntryManager) {
            DialogUtilities.displayDeletionDialog(wrapper, (dialog, whichButton) -> {
                todoEntryManager.removeEntry(entry);
                Utilities.displayToast(wrapper.context, R.string.event_deleted_message, Toast.LENGTH_SHORT);
            });
        }
        
        /**
         * Set view parameters for a calendar entry
         *
         * @param entry target entry
         */
        private void bindToSystemCalendarEntry(@NonNull final TodoEntry entry) {
            ListSelectionCalendarBinding bnd = (ListSelectionCalendarBinding) binding;
            
            bnd.eventColor.setCardBackgroundColor(entry.getCalendarEventColor());
            bnd.timeText.setText(entry.getCalendarEntryTimeSpan(wrapper.context, currentlySelectedDayUTC));
            bnd.timeText.setTextColor(getHarmonizedSecondaryFontColorWithBg(
                    entry.fontColor.get(currentlySelectedDayUTC),
                    entry.bgColor.get(currentlySelectedDayUTC)));
            
            // open calendar settings on settings icon click and on entry long click
            bnd.openSettingsButton.setOnClickListener(v -> CalendarSettingsDialogFragment.show(entry.event, wrapper));
            bnd.backgroundLayer.setOnLongClickListener(v -> {
                CalendarSettingsDialogFragment.show(entry.event, wrapper);
                return true;
            });
            
            // Open event info on entry click
            bnd.backgroundLayer.setOnClickListener(v -> CalendarEventInfoDialogFragment.show(entry, wrapper));
        }
        
        /**
         * Set view parameters for a regular entry
         *
         * @param entry            target entry
         * @param todoEntryManager entry manager (containing the entry)
         */
        private void bindToRegularEntry(@NonNull final TodoEntry entry,
                                        @NonNull final TodoEntryManager todoEntryManager) {
            ListSelectionTodoBinding bnd = (ListSelectionTodoBinding) binding;
            
            bnd.deleteEntryButton.setOnClickListener(view1 -> displayDeletionDialog(entry, todoEntryManager));
            
            bnd.isDone.setCheckedSilent(entry.isCompleted());
            
            bnd.isDone.setOnClickListener(v -> {
                if (entry.isGlobal()) {
                    String selectedDay = String.valueOf(currentlySelectedDayUTC);
                    // global entries become current entries
                    entry.changeParameters(
                            START_DAY_UTC, selectedDay,
                            END_DAY_UTC, selectedDay);
                } else {
                    // not global entries can be checked normally
                    entry.changeParameters(IS_COMPLETED, String.valueOf(bnd.isDone.isChecked()));
                }
                // save stuff, notify days changed, etc.
                todoEntryManager.performDeferredTasks();
            });
            
            // open entry edit dialog on click
            bnd.backgroundLayer.setOnClickListener(view1 -> AddEditEntryDialogFragment.show(entry, wrapper));
            
            // open entry settings on settings icon click and on entry long click
            bnd.backgroundLayer.setOnLongClickListener(v -> {
                EntrySettingsDialogFragment.show(entry, wrapper);
                return true;
            });
            bnd.openSettingsButton.setOnClickListener(v -> EntrySettingsDialogFragment.show(entry, wrapper));
        }
        
        /**
         * Set view parameters common for both types of entries
         *
         * @param entry target entry
         */
        private void bindToCommonPart(@NonNull final TodoEntry entry) {
            // fallback to get view by id because this part is common and we can't cast to any binding
            View root = binding.getRoot();
            TextView todoText = root.findViewById(R.id.eventText);
            MaterialCardView backgroundLayer = root.findViewById(R.id.backgroundLayer);
            
            int bgColor = entry.bgColor.get(currentlySelectedDayUTC);
            int fontColor = getHarmonizedFontColorWithBg(entry.fontColor.get(currentlySelectedDayUTC), bgColor);
            int strokeColor = entry.borderColor.get(currentlySelectedDayUTC);
            ColorStateList iconColor = ColorStateList.valueOf(getOnBgColor(bgColor));
            
            backgroundLayer.setCardBackgroundColor(bgColor);
            backgroundLayer.setStrokeColor(strokeColor);
            
            ((ImageView) root.findViewById(R.id.open_settings_button)).setImageTintList(iconColor);
            if (!entry.isFromSystemCalendar()) {
                ((ImageView) root.findViewById(R.id.delete_entry_button)).setImageTintList(iconColor);
            }
            
            if (entry.isCompletedOrHiddenByContent()) {
                todoText.setTextColor(dimColorToBg(fontColor, bgColor));
                todoText.setPaintFlags(addIntFlag(todoText.getPaintFlags(), Paint.STRIKE_THRU_TEXT_FLAG));
            } else {
                todoText.setTextColor(fontColor);
                todoText.setPaintFlags(removeIntFlag(todoText.getPaintFlags(), Paint.STRIKE_THRU_TEXT_FLAG));
            }
            
            Static.GlobalLabelPos globalLabelPos = GLOBAL_ITEMS_LABEL_POSITION.get();
            if (globalLabelPos == Static.GlobalLabelPos.HIDDEN) {
                // make sure it's visible in the list
                globalLabelPos = Static.GlobalLabelPos.BACK;
            }
            
            todoText.setText(entry.getTextOnDay(currentlySelectedDayUTC, wrapper.context, globalLabelPos));
        }
        
        /**
         * Outer binding method
         *
         * @param currentEntry     target entry
         * @param todoEntryManager entry manager (containing the entry)
         */
        void bindTo(@NonNull final TodoEntry currentEntry, @NonNull final TodoEntryManager todoEntryManager) {
            
            if (currentEntry.isFromSystemCalendar()) {
                bindToSystemCalendarEntry(currentEntry);
            } else {
                bindToRegularEntry(currentEntry, todoEntryManager);
            }
            
            bindToCommonPart(currentEntry);
        }
    }
    
    @NonNull
    private final TodoEntryManager todoEntryManager;
    @NonNull
    private final List<TodoEntry> currentTodoEntries;
    
    @NonNull
    private final ContextWrapper wrapper;
    
    private Function<TodoEntryManager, Collection<TodoEntry>> todoEntryListSupplier;
    
    // default capacity is fine
    @SuppressWarnings("CollectionWithoutInitialCapacity")
    public TodoListViewAdapter(@NonNull final ContextWrapper wrapper) {
        
        todoEntryManager = TodoEntryManager.getInstance(wrapper.context);
        this.wrapper = wrapper;
        currentTodoEntries = new ArrayList<>();
        todoEntryListSupplier = (manager) -> manager.getVisibleTodoEntriesInList(currentlySelectedDayUTC);
        // each entry has a unique id
        setHasStableIds(true);
    }
    
    public void setListSupplier(@NonNull Function<TodoEntryManager, Collection<TodoEntry>> todoEntryListSupplier) {
        this.todoEntryListSupplier = todoEntryListSupplier;
        notifyEntryListChanged();
    }
    
    @Override
    public long getItemId(int i) {
        return currentTodoEntries.get(i).getRecyclerViewId();
    }
    
    @Override
    public int getItemCount() {
        return currentTodoEntries.size();
    }
    
    public int getUncompletedItemCount() {
        int eventCount = 0;
        for (TodoEntry todoEntry : currentTodoEntries) {
            if (!todoEntry.isCompleted() && !todoEntry.isGlobal() && !todoEntry.isHiddenByContent.getBool()) {
                eventCount++;
            }
        }
        return eventCount;
    }
    
    /**
     * Notifies the adapter that the list contents have changed
     */
    public void notifyEntryListChanged() {
        int prevItemsCount = getItemCount();
        currentTodoEntries.clear();
        currentTodoEntries.addAll(todoEntryListSupplier.apply(todoEntryManager));
        notifyItemRangeChanged(0, max(prevItemsCount, currentTodoEntries.size()));
    }
    
    @NonNull
    @Override
    public EntryViewHolder<?> onCreateViewHolder(@NonNull final ViewGroup parent, int viewType) {
        if (viewType == 1) {
            // calendar entry
            return new EntryViewHolder<>(
                    ListSelectionCalendarBinding.inflate(wrapper.getLayoutInflater(), parent, false),
                    wrapper);
        } else {
            // regular entry
            return new EntryViewHolder<>(
                    ListSelectionTodoBinding.inflate(wrapper.getLayoutInflater(), parent, false),
                    wrapper);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull EntryViewHolder<?> holder, int position) {
        holder.bindTo(currentTodoEntries.get(position), todoEntryManager);
    }
    
    @Override
    public int getItemViewType(int i) {
        return currentTodoEntries.get(i).getRecyclerViewType();
    }
}
