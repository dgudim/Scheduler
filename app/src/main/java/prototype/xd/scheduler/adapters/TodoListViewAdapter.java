package prototype.xd.scheduler.adapters;

import static java.lang.Math.max;
import static prototype.xd.scheduler.utilities.GraphicsUtilities.getHarmonizedFontColorWithBg;
import static prototype.xd.scheduler.utilities.GraphicsUtilities.getHarmonizedSecondaryFontColorWithBg;
import static prototype.xd.scheduler.utilities.GraphicsUtilities.mixTwoColors;
import static prototype.xd.scheduler.utilities.DateManager.currentlySelectedDayUTC;
import static prototype.xd.scheduler.utilities.DialogUtilities.displayConfirmationDialogue;
import static prototype.xd.scheduler.utilities.DialogUtilities.displayEntryAdditionEditDialog;
import static prototype.xd.scheduler.utilities.Keys.DAY_FLAG_GLOBAL_STR;
import static prototype.xd.scheduler.utilities.Keys.END_DAY_UTC;
import static prototype.xd.scheduler.utilities.Keys.IS_COMPLETED;
import static prototype.xd.scheduler.utilities.Keys.START_DAY_UTC;
import static prototype.xd.scheduler.utilities.Keys.TEXT_VALUE;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
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
import prototype.xd.scheduler.utilities.TodoEntryManager;
import prototype.xd.scheduler.views.settings.EntrySettings;
import prototype.xd.scheduler.views.settings.SystemCalendarSettings;

public class TodoListViewAdapter extends RecyclerView.Adapter<TodoListViewAdapter.EntryViewHolder<?>> {
    
    static class EntryViewHolder<V extends ViewBinding> extends RecyclerView.ViewHolder {
        
        @NonNull
        private final V viewBinding;
        @NonNull
        private final Context context;
        @NonNull
        private final Lifecycle lifecycle;
        @NonNull
        private final FragmentManager fragmentManager;
        
        EntryViewHolder(@NonNull final V viewBinding,
                        @NonNull final Lifecycle lifecycle,
                        @NonNull FragmentManager fragmentManager) {
            super(viewBinding.getRoot());
            this.viewBinding = viewBinding;
            this.lifecycle = lifecycle;
            this.fragmentManager = fragmentManager;
            context = viewBinding.getRoot().getContext();
        }
        
        private void displayDeletionDialog(@NonNull final TodoEntry entry,
                                           @NonNull final TodoEntryManager todoEntryManager) {
            displayConfirmationDialogue(context, lifecycle,
                    R.string.delete, R.string.are_you_sure,
                    R.string.no, R.string.yes,
                    view2 -> todoEntryManager.removeEntry(entry));
        }
        
        private void displayEditDialog(@NonNull final TodoEntry entry,
                                       @NonNull final TodoEntryManager todoEntryManager) {
            final List<Group> groupList = todoEntryManager.getGroups();
            
            displayEntryAdditionEditDialog(fragmentManager, context, lifecycle,
                    entry, groupList,
                    (view2, text, dialogBinding, selectedIndex) -> {
                        entry.changeGroup(groupList.get(selectedIndex));
                        boolean isGlobal = dialogBinding.globalEntrySwitch.isChecked();
                        if (isGlobal) {
                            entry.changeParameters(
                                    TEXT_VALUE, text,
                                    IS_COMPLETED, Boolean.toString(false),
                                    START_DAY_UTC, DAY_FLAG_GLOBAL_STR,
                                    END_DAY_UTC, DAY_FLAG_GLOBAL_STR);
                        } else {
                            entry.changeParameters(
                                    TEXT_VALUE, text,
                                    START_DAY_UTC, dialogBinding.dayFromButton.getSelectedDayUTCStr(),
                                    END_DAY_UTC, dialogBinding.dayToButton.getSelectedDayUTCStr());
                        }
                        todoEntryManager.performDeferredTasks();
                        return true;
                    });
        }
        
        private void bindToSystemCalendarEntry(@NonNull final TodoEntry entry,
                                               @NonNull final SystemCalendarSettings systemCalendarSettings) {
            ListSelectionCalendarBinding bnd = (ListSelectionCalendarBinding) viewBinding;
            
            bnd.eventColor.setCardBackgroundColor(entry.event.color);
            bnd.timeText.setText(entry.getCalendarEntryTimeSpan(context, currentlySelectedDayUTC));
            bnd.timeText.setTextColor(getHarmonizedSecondaryFontColorWithBg(
                    entry.fontColor.get(currentlySelectedDayUTC),
                    entry.bgColor.get(currentlySelectedDayUTC)));
            bnd.settings.setOnClickListener(v -> systemCalendarSettings.show(entry));
        }
        
        private void bindToRegularEntry(@NonNull final TodoEntry entry,
                                        @NonNull final TodoEntryManager todoEntryManager,
                                        @NonNull final EntrySettings entrySettings) {
            ListSelectionTodoBinding bnd = (ListSelectionTodoBinding) viewBinding;
            
            bnd.deletionButton.setOnClickListener(view1 -> displayDeletionDialog(entry, todoEntryManager));
            
            bnd.isDone.setCheckedSilent(entry.isCompleted());
            
            bnd.isDone.setOnClickListener(view12 -> {
                if (!entry.isGlobal()) {
                    entry.changeParameters(IS_COMPLETED, String.valueOf(bnd.isDone.isChecked()));
                } else {
                    String selectedDay = String.valueOf(currentlySelectedDayUTC);
                    entry.changeParameters(
                            START_DAY_UTC, selectedDay,
                            END_DAY_UTC, selectedDay);
                }
                todoEntryManager.performDeferredTasks();
            });
            
            bnd.getRoot().setOnLongClickListener(view1 -> {
                displayEditDialog(entry, todoEntryManager);
                return true;
            });
            bnd.settings.setOnClickListener(v -> entrySettings.show(entry, v.getContext()));
        }
        
        private void bindToCommonPart(@NonNull final TodoEntry entry) {
            // fallback to get view by id because this part is common and we can't cast to any binding
            View root = viewBinding.getRoot();
            TextView todoText = root.findViewById(R.id.todoText);
            MaterialCardView backgroundLayer = root.findViewById(R.id.backgroundLayer);
            
            int bgColor = entry.bgColor.get(currentlySelectedDayUTC);
            int fontColor = getHarmonizedFontColorWithBg(entry.fontColor.get(currentlySelectedDayUTC), bgColor);
            
            backgroundLayer.setCardBackgroundColor(bgColor);
            backgroundLayer.setStrokeColor(entry.borderColor.get(currentlySelectedDayUTC));
            
            if (entry.isCompleted() || entry.hideByContent()) {
                todoText.setTextColor(mixTwoColors(fontColor, bgColor, 0.5));
            } else {
                todoText.setTextColor(fontColor);
            }
            
            todoText.setText(entry.getTextOnDay(currentlySelectedDayUTC, context, true));
        }
        
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
    private List<TodoEntry> currentTodoListEntries;
    
    @NonNull
    private final EntrySettings entrySettings;
    @NonNull
    private final SystemCalendarSettings systemCalendarSettings;
    @NonNull
    private final Lifecycle lifecycle;
    @NonNull
    private final FragmentManager fragmentManager;
    
    public TodoListViewAdapter(@NonNull final TodoEntryManager todoEntryManager,
                               @NonNull final Context context,
                               @NonNull final Lifecycle lifecycle,
                               @NonNull FragmentManager fragmentManager) {
        
        this.todoEntryManager = todoEntryManager;
        this.lifecycle = lifecycle;
        this.fragmentManager = fragmentManager;
        currentTodoListEntries = new ArrayList<>();
        entrySettings = new EntrySettings(todoEntryManager, context, lifecycle);
        systemCalendarSettings = new SystemCalendarSettings(todoEntryManager, context, lifecycle);
        setHasStableIds(true);
    }
    
    @Override
    public long getItemId(int i) {
        return currentTodoListEntries.get(i).getId();
    }
    
    @Override
    public int getItemCount() {
        return currentTodoListEntries.size();
    }
    
    public void notifyEntryListChanged() {
        int itemsCount = currentTodoListEntries.size();
        currentTodoListEntries = todoEntryManager.getVisibleTodoListEntries(currentlySelectedDayUTC);
        notifyItemRangeChanged(0, max(itemsCount, currentTodoListEntries.size()));
    }
    
    @NonNull
    @Override
    public EntryViewHolder<?> onCreateViewHolder(@NonNull final ViewGroup parent, int viewType) {
        if (viewType == 1) {
            // calendar entry
            return new EntryViewHolder<>(ListSelectionCalendarBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false), lifecycle, fragmentManager);
        } else {
            // regular entry
            return new EntryViewHolder<>(ListSelectionTodoBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false), lifecycle, fragmentManager);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull EntryViewHolder<?> holder, int position) {
        holder.bindTo(currentTodoListEntries.get(position), todoEntryManager, entrySettings, systemCalendarSettings);
    }
    
    @Override
    public int getItemViewType(int i) {
        return currentTodoListEntries.get(i).getType();
    }
}
