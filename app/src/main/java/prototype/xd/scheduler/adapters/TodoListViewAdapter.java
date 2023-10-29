package prototype.xd.scheduler.adapters;

import static java.lang.Math.max;
import static prototype.xd.scheduler.utilities.ColorUtilities.dimColorToBg;
import static prototype.xd.scheduler.utilities.ColorUtilities.getHarmonizedFontColorWithBg;
import static prototype.xd.scheduler.utilities.ColorUtilities.getHarmonizedSecondaryFontColorWithBg;
import static prototype.xd.scheduler.utilities.DateManager.currentlySelectedDayUTC;
import static prototype.xd.scheduler.utilities.Static.DAY_FLAG_GLOBAL_STR;
import static prototype.xd.scheduler.utilities.Static.END_DAY_UTC;
import static prototype.xd.scheduler.utilities.Static.GLOBAL_ITEMS_LABEL_POSITION;
import static prototype.xd.scheduler.utilities.Static.IS_COMPLETED;
import static prototype.xd.scheduler.utilities.Static.START_DAY_UTC;
import static prototype.xd.scheduler.utilities.Static.TEXT_VALUE;
import static prototype.xd.scheduler.utilities.Utilities.addIntFlag;
import static prototype.xd.scheduler.utilities.Utilities.removeIntFlag;

import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.ListSelectionCalendarBinding;
import prototype.xd.scheduler.databinding.ListSelectionTodoBinding;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoEntry;
import prototype.xd.scheduler.fragments.dialogs.AddEditEntryDialogFragment;
import prototype.xd.scheduler.utilities.DialogUtilities;
import prototype.xd.scheduler.utilities.Static;
import prototype.xd.scheduler.utilities.TodoEntryManager;
import prototype.xd.scheduler.utilities.Utilities;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;
import prototype.xd.scheduler.views.settings.EntrySettings;
import prototype.xd.scheduler.views.settings.SystemCalendarSettings;

/**
 * List adapter class for displaying todos and calendar entries
 */
public class TodoListViewAdapter extends RecyclerView.Adapter<TodoListViewAdapter.EntryViewHolder<?>> {
    
    /**
     * View holder for this adapter
     *
     * @param <V> ListSelectionTodoBinding or ListSelectionCalendarBinding
     */
    static class EntryViewHolder<V extends ViewBinding> extends RecyclerView.ViewHolder {
        
        @NonNull
        private final V viewBinding;
        @NonNull
        private final ContextWrapper wrapper;
        
        private final AddEditEntryDialogFragment addEditEntryDialog;
        
        EntryViewHolder(@NonNull final V viewBinding,
                        @NonNull final ContextWrapper wrapper,
                        @NonNull final AddEditEntryDialogFragment addEditEntryDialog) {
            super(viewBinding.getRoot());
            this.viewBinding = viewBinding;
            this.wrapper = wrapper;
            this.addEditEntryDialog = addEditEntryDialog;
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
         * Display a dialog to edit an entry (on long click), should be called on regular non-calendar entries
         *
         * @param entry            entry to be edited
         * @param todoEntryManager entry manager (containing the entry)
         */
        private void displayEditDialog(@NonNull final TodoEntry entry,
                                       @NonNull final TodoEntryManager todoEntryManager) {
            final List<Group> groupList = todoEntryManager.getGroups();
            
            addEditEntryDialog.show(entry, groupList,
                    (text, dialogBinding, selectedIndex) -> {
                        entry.changeGroup(groupList.get(selectedIndex));
                        boolean isGlobal = dialogBinding.globalEntrySwitch.isChecked();
                        if (isGlobal) {
                            // change entry type to global
                            entry.changeParameters(
                                    TEXT_VALUE, text,
                                    IS_COMPLETED, Boolean.toString(false),
                                    START_DAY_UTC, DAY_FLAG_GLOBAL_STR,
                                    END_DAY_UTC, DAY_FLAG_GLOBAL_STR);
                        } else {
                            // change entry type to regular
                            entry.changeParameters(
                                    TEXT_VALUE, text,
                                    START_DAY_UTC, dialogBinding.dayFromButton.getSelectedDayUTCStr(),
                                    END_DAY_UTC, dialogBinding.dayToButton.getSelectedDayUTCStr());
                        }
                        // save stuff, notify days changed, etc.
                        todoEntryManager.performDeferredTasks();
                    }, wrapper);
        }
        
        /**
         * Set view parameters for a calendar entry
         *
         * @param entry                  target entry
         * @param systemCalendarSettings settings to open when user clicks the settings button
         */
        private void bindToSystemCalendarEntry(@NonNull final TodoEntry entry,
                                               @NonNull final SystemCalendarSettings systemCalendarSettings) {
            ListSelectionCalendarBinding bnd = (ListSelectionCalendarBinding) viewBinding;
            
            bnd.eventColor.setCardBackgroundColor(entry.getCalendarEventColor());
            bnd.timeText.setText(entry.getCalendarEntryTimeSpan(wrapper.context, currentlySelectedDayUTC));
            bnd.timeText.setTextColor(getHarmonizedSecondaryFontColorWithBg(
                    entry.fontColor.get(currentlySelectedDayUTC),
                    entry.bgColor.get(currentlySelectedDayUTC)));
    
            // open calendar settings on settings icon click and on entry long click
            bnd.openSettingsButton.setOnClickListener(v -> systemCalendarSettings.show(entry.event, wrapper));
            bnd.backgroundLayer.setOnLongClickListener(v -> {
                systemCalendarSettings.show(entry.event, wrapper);
                return true;
            });
        }
        
        /**
         * Set view parameters for a regular entry
         *
         * @param entry            target entry
         * @param todoEntryManager entry manager (containing the entry)
         * @param entrySettings    settings to open when user clicks the settings button
         */
        private void bindToRegularEntry(@NonNull final TodoEntry entry,
                                        @NonNull final TodoEntryManager todoEntryManager,
                                        @NonNull final EntrySettings entrySettings) {
            ListSelectionTodoBinding bnd = (ListSelectionTodoBinding) viewBinding;
            
            bnd.deleteEntryButton.setOnClickListener(view1 -> displayDeletionDialog(entry, todoEntryManager));
            
            bnd.isDone.setCheckedSilent(entry.isCompleted());
            
            bnd.isDone.setOnClickListener(view12 -> {
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
            bnd.backgroundLayer.setOnClickListener(view1 -> displayEditDialog(entry, todoEntryManager));
            
            // open entry settings on settings icon click and on entry long click
            bnd.backgroundLayer.setOnLongClickListener(v -> {
                entrySettings.show(entry, wrapper.fragmentManager);
                return true;
            });
            bnd.openSettingsButton.setOnClickListener(v -> entrySettings.show(entry, wrapper.fragmentManager));
        }
        
        /**
         * Set view parameters common for both types of entries
         *
         * @param entry target entry
         */
        private void bindToCommonPart(@NonNull final TodoEntry entry) {
            // fallback to get view by id because this part is common and we can't cast to any binding
            View root = viewBinding.getRoot();
            TextView todoText = root.findViewById(R.id.todoText);
            MaterialCardView backgroundLayer = root.findViewById(R.id.backgroundLayer);
            
            int bgColor = entry.bgColor.get(currentlySelectedDayUTC);
            int fontColor = getHarmonizedFontColorWithBg(entry.fontColor.get(currentlySelectedDayUTC), bgColor);
            
            backgroundLayer.setCardBackgroundColor(bgColor);
            backgroundLayer.setStrokeColor(entry.borderColor.get(currentlySelectedDayUTC));
            
            if (entry.isCompleted() || entry.isHiddenByContent()) {
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
         * @param currentEntry           target entry
         * @param todoEntryManager       entry manager (containing the entry)
         * @param entrySettings          settings to open when user clicks the settings button on a regular entry
         * @param systemCalendarSettings settings to open when user clicks the settings button on a calendar entry
         */
        void bindTo(@NonNull final TodoEntry currentEntry,
                    @NonNull final TodoEntryManager todoEntryManager,
                    @NonNull final EntrySettings entrySettings,
                    @NonNull final SystemCalendarSettings systemCalendarSettings) {
            
            if (currentEntry.isFromSystemCalendar()) {
                bindToSystemCalendarEntry(currentEntry, systemCalendarSettings);
            } else {
                bindToRegularEntry(currentEntry, todoEntryManager, entrySettings);
            }
            
            bindToCommonPart(currentEntry);
        }
    }
    
    @NonNull
    private final TodoEntryManager todoEntryManager;
    @NonNull
    private final List<TodoEntry> currentTodoEntries;
    
    @NonNull
    private final EntrySettings entrySettings;
    @NonNull
    private final SystemCalendarSettings systemCalendarSettings;
    @NonNull
    private final ContextWrapper wrapper;
    
    public final AddEditEntryDialogFragment addEditEntryDialog;
    
    // default capacity is fine
    @SuppressWarnings("CollectionWithoutInitialCapacity")
    public TodoListViewAdapter(@NonNull final ContextWrapper wrapper,
                               @NonNull final TodoEntryManager todoEntryManager) {
        
        this.todoEntryManager = todoEntryManager;
        this.wrapper = wrapper;
        currentTodoEntries = new ArrayList<>();
        entrySettings = new EntrySettings(todoEntryManager);
        systemCalendarSettings = new SystemCalendarSettings(todoEntryManager);
        addEditEntryDialog = new AddEditEntryDialogFragment();
        // each entry has a unique id
        setHasStableIds(true);
    }
    
    @Override
    public long getItemId(int i) {
        return currentTodoEntries.get(i).getRecyclerViewId();
    }
    
    @Override
    public int getItemCount() {
        return currentTodoEntries.size();
    }
    
    /**
     * Notifies the adapter that the list contents have changed
     */
    public void notifyEntryListChanged() {
        int prevItemsCount = currentTodoEntries.size();
        currentTodoEntries.clear();
        currentTodoEntries.addAll(todoEntryManager.getVisibleTodoEntriesInList(currentlySelectedDayUTC));
        notifyItemRangeChanged(0, max(prevItemsCount, currentTodoEntries.size()));
    }
    
    @NonNull
    @Override
    public EntryViewHolder<?> onCreateViewHolder(@NonNull final ViewGroup parent, int viewType) {
        if (viewType == 1) {
            // calendar entry
            return new EntryViewHolder<>(
                    ListSelectionCalendarBinding.inflate(wrapper.getLayoutInflater(), parent, false),
                    wrapper, addEditEntryDialog);
        } else {
            // regular entry
            return new EntryViewHolder<>(
                    ListSelectionTodoBinding.inflate(wrapper.getLayoutInflater(), parent, false),
                    wrapper, addEditEntryDialog);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull EntryViewHolder<?> holder, int position) {
        holder.bindTo(currentTodoEntries.get(position), todoEntryManager, entrySettings, systemCalendarSettings);
    }
    
    @Override
    public int getItemViewType(int i) {
        return currentTodoEntries.get(i).getRecyclerViewType();
    }
}
