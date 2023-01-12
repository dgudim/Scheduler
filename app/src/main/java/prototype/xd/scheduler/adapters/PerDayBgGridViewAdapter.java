package prototype.xd.scheduler.adapters;

import static prototype.xd.scheduler.utilities.GraphicsUtilities.readBitmapFromFile;
import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.Utilities.getFile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.function.Consumer;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.utilities.DateManager;

/**
 * Grid adapter class for displaying "per day wallpapers"
 */
public class PerDayBgGridViewAdapter extends BaseAdapter {
    
    public static final String NAME = PerDayBgGridViewAdapter.class.getSimpleName();
    
    // called when a user clicks on an image to select a new one
    private final Consumer<Integer> bgSelectionClickedCallback;
    // fallback day
    private final String defaultDay;
    public PerDayBgGridViewAdapter(@NonNull final Context context,
                                   @NonNull final Consumer<Integer> bgSelectionClickedCallback) {
        defaultDay = context.getString(R.string.day_default);
        this.bgSelectionClickedCallback = bgSelectionClickedCallback;
    }
    
    @Override
    public int getCount() {
        return DateManager.WEEK_DAYS_ROOT.size();
    }
    
    @Override
    public Object getItem(int i) {
        return DateManager.WEEK_DAYS_ROOT.get(i);
    }
    
    @Override
    public long getItemId(int i) {
        return i;
    }
    
    @Override
    public View getView(int i, View convertView, ViewGroup parent) {
        
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.background_image_entry, parent, false);
        }
        
        ((TextView) convertView.findViewById(R.id.bg_title)).setText(DateManager.getLocalWeekdayByIndex(i, defaultDay));
        ImageView imageView = convertView.findViewById(R.id.bg_image);
        
        try {
            // load bitmap from file
            imageView.setImageBitmap(readBitmapFromFile(getFile(DateManager.WEEK_DAYS_ROOT.get(i) + ".png_min.png")));
        } catch (FileNotFoundException e) {
            // set default empty image
            imageView.setImageResource(R.drawable.ic_not_90);
        } catch (IOException e) {
            logException(NAME, e);
        }
        
        convertView.findViewById(R.id.bg_image_container).setOnClickListener(v -> bgSelectionClickedCallback.accept(i));
        
        return convertView;
    }
}
