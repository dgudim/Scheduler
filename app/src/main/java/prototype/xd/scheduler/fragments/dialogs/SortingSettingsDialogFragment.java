package prototype.xd.scheduler.fragments.dialogs;

import static prototype.xd.scheduler.utilities.ImageUtilities.getExpiredUpcomingColor;
import static prototype.xd.scheduler.utilities.ImageUtilities.getHarmonizedFontColorWithBg;
import static prototype.xd.scheduler.utilities.ImageUtilities.getHarmonizedSecondaryFontColorWithBg;
import static prototype.xd.scheduler.utilities.ImageUtilities.getOnBgColor;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.activity.ComponentDialog;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.divider.MaterialDividerItemDecoration;

import java.util.List;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.adapters.SettingsListViewAdapter;
import prototype.xd.scheduler.databinding.DraggableListEntryBinding;
import prototype.xd.scheduler.databinding.SortingSettingsDialogFragmentBinding;
import prototype.xd.scheduler.entities.TodoEntry;
import prototype.xd.scheduler.entities.settings_entries.SettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.SwitchSettingsEntryConfig;
import prototype.xd.scheduler.utilities.Logger;
import prototype.xd.scheduler.utilities.Static;

public class SortingSettingsDialogFragment extends FullScreenSettingsDialogFragment<SortingSettingsDialogFragmentBinding> {
    
    @NonNull
    @Override
    protected SortingSettingsDialogFragmentBinding inflate(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return SortingSettingsDialogFragmentBinding.inflate(inflater, container, false);
    }
    
    @Override
    protected void buildDialogStatic(@NonNull SortingSettingsDialogFragmentBinding binding, @NonNull ComponentDialog dialog) {
        EntryTypeAdapter entryTypeAdapter = new EntryTypeAdapter();
        binding.orderRecyclerView.setLayoutManager(new LinearLayoutManager(wrapper.context));
        binding.orderRecyclerView.setAdapter(entryTypeAdapter);
        entryTypeAdapter.attachDragToRecyclerView(binding.orderRecyclerView);
    
        List<SettingsEntryConfig> settingsEntries = List.of(
                new SwitchSettingsEntryConfig(
                        Static.SORTING_TREAT_GLOBAL_ITEMS_AS_TODAYS, R.string.sorting_treat_global_as_todays,
                        (buttonView, isChecked) -> entryTypeAdapter.setGlobalEventsVisible(!isChecked), true),
                new SwitchSettingsEntryConfig(Static.SORTING_SORT_CALENDAR_SEPARATELY, R.string.sorting_sort_calendar_separately,
                        (buttonView, isChecked) -> entryTypeAdapter.setCalendarEventsVisible(isChecked), true));
    
        binding.settingsRecyclerView.setLayoutManager(new LinearLayoutManager(wrapper.context));
        MaterialDividerItemDecoration divider = new MaterialDividerItemDecoration(wrapper.context, LinearLayout.VERTICAL);
        divider.setLastItemDecorated(false);
        binding.settingsRecyclerView.addItemDecoration(divider);
        binding.settingsRecyclerView.setAdapter(new SettingsListViewAdapter(wrapper, settingsEntries));
    }
    
    @Override
    protected void buildDialogDynamic(@NonNull SortingSettingsDialogFragmentBinding binding, @NonNull ComponentDialog dialog) {
        // None of it is dynamic
    }
    
    private static final class EntryTypeAdapter extends RecyclerView.Adapter<EntryTypeAdapter.CardViewHolder> {
        
        public static final String NAME = EntryTypeAdapter.class.getSimpleName();
        
        @NonNull
        private final ItemTouchHelper itemDragHelper;
        @NonNull
        private final List<TodoEntry.EntryType> sortOrder;
        
        private EntryTypeAdapter() {
            itemDragHelper = new ItemTouchHelper(new DragHelperCallback(this));
            sortOrder = Static.TODO_ITEM_SORTING_ORDER.getUnique();
            addEntryTypeIfNeeded(TodoEntry.EntryType.UPCOMING);
            addEntryTypeIfNeeded(TodoEntry.EntryType.TODAY);
            addEntryTypeIfNeeded(TodoEntry.EntryType.EXPIRED);
        }
        
        public void attachDragToRecyclerView(RecyclerView recyclerView) {
            itemDragHelper.attachToRecyclerView(recyclerView);
        }
        
        public void setGlobalEventsVisible(boolean globalEventsVisible) {
            addOrRemoveEntryType(TodoEntry.EntryType.GLOBAL, globalEventsVisible);
            Static.TODO_ITEM_SORTING_ORDER.put(sortOrder);
        }
        
        public void setCalendarEventsVisible(boolean calendarEventsVisible) {
            addOrRemoveEntryType(TodoEntry.EntryType.TODAY_CALENDAR, calendarEventsVisible);
            addOrRemoveEntryType(TodoEntry.EntryType.UPCOMING_CALENDAR, calendarEventsVisible);
            addOrRemoveEntryType(TodoEntry.EntryType.EXPIRED_CALENDAR, calendarEventsVisible);
            Static.TODO_ITEM_SORTING_ORDER.put(sortOrder);
        }
        
        private void addOrRemoveEntryType(@NonNull TodoEntry.EntryType type, boolean add) {
            if (add) {
                addEntryTypeIfNeeded(type);
            } else {
                int index = sortOrder.indexOf(type);
                if (index == -1) {
                    Logger.warning(NAME, "Can't remove " + type);
                    return;
                }
                sortOrder.remove(index);
                notifyItemRemoved(index);
            }
        }
        
        private void addEntryTypeIfNeeded(@NonNull TodoEntry.EntryType type) {
            if (sortOrder.contains(type)) {
                Logger.warning(NAME, "Not adding duplicate type: " + type);
                return;
            }
            sortOrder.add(type);
            notifyItemInserted(sortOrder.size());
        }
        
        @NonNull
        @Override
        public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new CardViewHolder(DraggableListEntryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }
        
        @Override
        public void onBindViewHolder(@NonNull CardViewHolder viewHolder, int position) {
            viewHolder.bind(sortOrder.get(position), itemDragHelper);
        }
        
        @Override
        public int getItemCount() {
            return sortOrder.size();
        }
        
        void swapCards(int fromPosition, int toPosition) {
            TodoEntry.EntryType fromNumber = sortOrder.get(fromPosition);
            sortOrder.set(fromPosition, sortOrder.get(toPosition));
            sortOrder.set(toPosition, fromNumber);
            Static.TODO_ITEM_SORTING_ORDER.put(sortOrder);
            notifyItemMoved(fromPosition, toPosition);
        }
        
        private static final class CardViewHolder extends RecyclerView.ViewHolder {
            
            @NonNull
            private final DraggableListEntryBinding binding;
            
            private CardViewHolder(@NonNull DraggableListEntryBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
            
            @SuppressLint("ClickableViewAccessibility")
            private void bind(@NonNull TodoEntry.EntryType entryType, @NonNull final ItemTouchHelper dragHelper) {
                @StringRes int titleTextRes;
                @StringRes int descriptionTextRes;
                int bgColor = Static.BG_COLOR.CURRENT.get();
                int fontColor = Static.FONT_COLOR.CURRENT.get();
                int borderColor = Static.BORDER_COLOR.CURRENT.get();
                switch (entryType) {
                    case UPCOMING:
                    case UPCOMING_CALENDAR:
                        titleTextRes = R.string.upcoming_events;
                        descriptionTextRes = R.string.upcoming_events_description;
                        bgColor = getExpiredUpcomingColor(bgColor, Static.BG_COLOR.UPCOMING.get());
                        fontColor = getExpiredUpcomingColor(fontColor, Static.FONT_COLOR.UPCOMING.get());
                        borderColor = getExpiredUpcomingColor(borderColor, Static.BORDER_COLOR.UPCOMING.get());
                        break;
                    case EXPIRED:
                    case EXPIRED_CALENDAR:
                        titleTextRes = R.string.expired_events;
                        descriptionTextRes = R.string.expired_events_description;
                        bgColor = getExpiredUpcomingColor(bgColor, Static.BG_COLOR.EXPIRED.get());
                        fontColor = getExpiredUpcomingColor(fontColor, Static.FONT_COLOR.EXPIRED.get());
                        borderColor = getExpiredUpcomingColor(borderColor, Static.BORDER_COLOR.EXPIRED.get());
                        break;
                    case TODAY:
                        titleTextRes = R.string.todays_events;
                        descriptionTextRes = R.string.todays_events_description;
                        break;
                    case GLOBAL:
                        titleTextRes = R.string.global_events;
                        descriptionTextRes = R.string.global_events_description;
                        break;
                    case UNKNOWN:
                    default:
                        titleTextRes = -1;
                        descriptionTextRes = -1;
                }
                
                // extra stuff for calendar entries
                ColorStateList onBgColor = ColorStateList.valueOf(getOnBgColor(bgColor));
                boolean isCalendar = false;
                switch (entryType) {
                    case UPCOMING_CALENDAR:
                        titleTextRes = R.string.upcoming_calendar_events;
                        descriptionTextRes = R.string.upcoming_calendar_events_description;
                        isCalendar = true;
                        break;
                    case EXPIRED_CALENDAR:
                        titleTextRes = R.string.expired_calendar_events;
                        descriptionTextRes = R.string.expired_calendar_events_description;
                        isCalendar = true;
                        break;
                    case TODAY_CALENDAR:
                        titleTextRes = R.string.todays_calendar_events;
                        descriptionTextRes = R.string.todays_calendar_events_description;
                        isCalendar = true;
                        break;
                    default:
                }
                
                if (isCalendar) {
                    binding.calendarIcon.setVisibility(View.VISIBLE);
                    binding.calendarIcon.setImageTintList(onBgColor);
                } else {
                    binding.calendarIcon.setVisibility(View.GONE);
                }
                
                binding.itemText.setText(titleTextRes);
                binding.itemText.setTextColor(getHarmonizedFontColorWithBg(fontColor, bgColor));
                
                binding.itemDescriptionText.setText(descriptionTextRes);
                binding.itemDescriptionText.setTextColor(getHarmonizedSecondaryFontColorWithBg(fontColor, bgColor));
                
                binding.card.setCardBackgroundColor(bgColor);
                binding.card.setStrokeColor(borderColor);
                
                binding.dragHandle.setImageTintList(onBgColor);
                binding.dragHandle.setOnTouchListener(
                        (v, event) -> {
                            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                                dragHelper.startDrag(this);
                                return true;
                            }
                            return false;
                        });
            }
        }
    }
    
    private static final class DragHelperCallback extends ItemTouchHelper.Callback {
        
        private final EntryTypeAdapter entryTypeAdapter;
        
        @Nullable
        private MaterialCardView dragCardView;
        
        private DragHelperCallback(EntryTypeAdapter entryTypeAdapter) {
            this.entryTypeAdapter = entryTypeAdapter;
        }
        
        @Override
        public int getMovementFlags(
                @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            // draggable up and down, not swipe-able
            return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
        }
        
        @Override
        public boolean onMove(
                @NonNull RecyclerView recyclerView,
                @NonNull RecyclerView.ViewHolder from,
                @NonNull RecyclerView.ViewHolder to) {
            entryTypeAdapter.swapCards(
                    from.getBindingAdapterPosition(),
                    to.getBindingAdapterPosition());
            return true;
        }
        
        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            // swipe disabled
        }
        
        @Override
        public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
            super.onSelectedChanged(viewHolder, actionState);
            
            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                dragCardView = (MaterialCardView) viewHolder.itemView;
                dragCardView.setDragged(true);
            } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE && dragCardView != null) {
                dragCardView.setDragged(false);
                dragCardView = null;
            }
        }
    }
}
