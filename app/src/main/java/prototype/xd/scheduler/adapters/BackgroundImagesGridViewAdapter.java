package prototype.xd.scheduler.adapters;

import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.Utilities.getFile;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.function.Consumer;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.utilities.Keys;

public class BackgroundImagesGridViewAdapter extends BaseAdapter {
    
    private final Consumer<Integer> bgSelectionClickedCallback;
    private final String[] availableDays_localized;
    
    public BackgroundImagesGridViewAdapter(@NonNull final Context context,
                                           @NonNull final Consumer<Integer> bgSelectionClickedCallback) {
        this.bgSelectionClickedCallback = bgSelectionClickedCallback;
        availableDays_localized = new String[]{
                context.getString(R.string.day_monday),
                context.getString(R.string.day_tuesday),
                context.getString(R.string.day_wednesday),
                context.getString(R.string.day_thursday),
                context.getString(R.string.day_friday),
                context.getString(R.string.day_saturday),
                context.getString(R.string.day_sunday),
                context.getString(R.string.day_default)
        };
    }
    
    @Override
    public int getCount() {
        return Keys.WEEK_DAYS.size();
    }
    
    @Override
    public Object getItem(int i) {
        return Keys.WEEK_DAYS.get(i);
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
        
        ((TextView) convertView.findViewById(R.id.bg_title)).setText(availableDays_localized[i]);
        ImageView imageView = convertView.findViewById(R.id.bg_image);
        
        try {
            FileInputStream inputStream = new FileInputStream(getFile(Keys.WEEK_DAYS.get(i) + ".png_min.png"));
            imageView.setImageBitmap(BitmapFactory.decodeStream(inputStream));
            inputStream.close();
        } catch (FileNotFoundException e) {
            imageView.setImageResource(R.drawable.ic_not);
        } catch (IOException e) {
            logException("GridViewAdapter", e);
        }
        
        convertView.findViewById(R.id.bg_image_container).setOnClickListener(v -> bgSelectionClickedCallback.accept(i));
        
        return convertView;
    }
}
