package prototype.xd.scheduler;

import static prototype.xd.scheduler.utilities.BitmapUtilities.getHarmonizedFontColor;
import static prototype.xd.scheduler.utilities.BitmapUtilities.getOnSurfaceColor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

import prototype.xd.scheduler.databinding.DraggableListEntryBinding;
import prototype.xd.scheduler.databinding.SortingSettingsFragmentBinding;
import prototype.xd.scheduler.entities.TodoEntry;
import prototype.xd.scheduler.utilities.Keys;

public class SortingSettingsFragment extends BaseSettingsFragment<SortingSettingsFragmentBinding> {
    
    @Override
    public SortingSettingsFragmentBinding inflate(@NonNull LayoutInflater inflater, ViewGroup container) {
        return SortingSettingsFragmentBinding.inflate(inflater, container, false);
    }
    
    // view creation end (fragment visible)
    @Override
    public void onViewCreated(@NonNull final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EntryTypeAdapter entryTypeAdapter = new EntryTypeAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        binding.recyclerView.setAdapter(entryTypeAdapter);
        entryTypeAdapter.attachDragToRecyclerView(binding.recyclerView);
    }
    
    private static class EntryTypeAdapter extends RecyclerView.Adapter<EntryTypeAdapter.CardViewHolder> {
        
        private final ItemTouchHelper itemDragHelper;
        private final List<TodoEntry.EntryType> sortOrder;
        
        private EntryTypeAdapter() {
            itemDragHelper = new ItemTouchHelper(new DragHelperCallback(this));
            sortOrder = Keys.TODO_ITEM_SORTING_ORDER.get();
        }
        
        public void attachDragToRecyclerView(RecyclerView recyclerView) {
            itemDragHelper.attachToRecyclerView(recyclerView);
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
            Keys.TODO_ITEM_SORTING_ORDER.put(sortOrder);
            notifyItemMoved(fromPosition, toPosition);
        }
        
        private static class CardViewHolder extends RecyclerView.ViewHolder {
            
            @NonNull
            private final DraggableListEntryBinding binding;
            @NonNull
            private final Context context;
            
            private CardViewHolder(DraggableListEntryBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
                context = binding.getRoot().getContext();
            }
            
            @SuppressLint("ClickableViewAccessibility")
            private void bind(TodoEntry.EntryType entryType, final ItemTouchHelper dragHelper) {
                String text;
                int bgColor = 0;
                int fontColor = 0;
                int borderColor = 0;
                switch (entryType) {
                    case UPCOMING:
                        text = context.getString(R.string.upcoming_entries);
                        bgColor = Keys.UPCOMING_BG_COLOR.get();
                        fontColor = Keys.UPCOMING_FONT_COLOR.get();
                        borderColor = Keys.UPCOMING_BORDER_COLOR.get();
                        break;
                    case EXPIRED:
                        text = context.getString(R.string.expired_entries);
                        bgColor = Keys.EXPIRED_BG_COLOR.get();
                        fontColor = Keys.EXPIRED_FONT_COLOR.get();
                        borderColor = Keys.EXPIRED_BORDER_COLOR.get();
                        break;
                    case TODAY:
                        text = context.getString(R.string.today_entries);
                        break;
                    case GLOBAL:
                        text = context.getString(R.string.global_entries);
                        break;
                    case UNKNOWN:
                    default:
                        text = context.getString(R.string.other_entries);
                        break;
                }
                
                if (bgColor == 0) {
                    bgColor = Keys.BG_COLOR.get();
                    fontColor = Keys.FONT_COLOR.get();
                    borderColor = Keys.BORDER_COLOR.get();
                }
                
                binding.itemText.setText(text);
                binding.itemText.setTextColor(getHarmonizedFontColor(fontColor, bgColor));
                binding.card.setCardBackgroundColor(bgColor);
                binding.card.setStrokeColor(borderColor);
                binding.dragHandle.setImageTintList(ColorStateList.valueOf(getOnSurfaceColor(bgColor)));
                binding.dragHandle.setOnTouchListener(
                        (v, event) -> {
                            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                                dragHelper.startDrag(CardViewHolder.this);
                                return true;
                            }
                            return false;
                        });
            }
        }
    }
    
    private static class DragHelperCallback extends ItemTouchHelper.Callback {
        
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
