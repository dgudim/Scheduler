package prototype.xd.scheduler.adapters;

import static prototype.xd.scheduler.utilities.ImageUtilities.readBitmapFromFile;
import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.Utilities.getFile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.function.IntConsumer;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.BackgroundImageEntryBinding;
import prototype.xd.scheduler.utilities.DateManager;

/**
 * Grid adapter class for displaying "per day wallpapers"
 */
public class PerDayBgGridViewAdapter extends RecyclerView.Adapter<PerDayBgGridViewAdapter.EntryViewHolder> {
    
    public static final String NAME = PerDayBgGridViewAdapter.class.getSimpleName();
    
    // called when a user clicks on an image to select a new one
    @NonNull
    private final IntConsumer bgSelectionClickedCallback;
    // fallback day
    private final String defaultDay;
    
    public PerDayBgGridViewAdapter(@NonNull final Context context,
                                   @NonNull final IntConsumer bgSelectionClickedCallback) {
        defaultDay = context.getString(R.string.day_default);
        this.bgSelectionClickedCallback = bgSelectionClickedCallback;
        // each day has a unique id
        setHasStableIds(true);
    }
    
    /**
     * View holder for this adapter
     */
    static class EntryViewHolder extends BindingViewHolder<BackgroundImageEntryBinding> {
        
        EntryViewHolder(@NonNull BackgroundImageEntryBinding binding) {
            super(binding);
        }
        
        private void bind(final int dayId, @NonNull String defaultDay, @NonNull IntConsumer bgSelectionClickedCallback) {
            
            binding.bgTitle.setText(DateManager.getLocalWeekdayByIndex(dayId, defaultDay));
            
            try {
                // load bitmap from file
                binding.bgImage.setImageBitmap(readBitmapFromFile(getFile(DateManager.BG_NAMES_ROOT.get(dayId) + "_min.png")));
            } catch (FileNotFoundException e) { // NOSONAR, this is fine, bg just doesn't exist
                // set default empty image
                binding.bgImage.setImageResource(R.drawable.ic_not_90);
            } catch (IOException e) {
                logException(NAME, e);
            }
            
            binding.bgImageContainer.setOnClickListener(v -> bgSelectionClickedCallback.accept(dayId));
        }
    }
    
    @NonNull
    @Override
    public EntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new PerDayBgGridViewAdapter.EntryViewHolder(
                BackgroundImageEntryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }
    
    @Override
    public void onBindViewHolder(@NonNull EntryViewHolder holder, int position) {
        holder.bind(position, defaultDay, bgSelectionClickedCallback);
    }
    
    @Override
    public long getItemId(int i) {
        return DateManager.BG_NAMES_ROOT.get(i).hashCode();
    }
    
    @Override
    public int getItemCount() {
        return DateManager.BG_NAMES_ROOT.size();
    }
}
