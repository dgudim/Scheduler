package prototype.xd.scheduler.adapters;

import static prototype.xd.scheduler.utilities.DateManager.availableDays;
import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.Utilities.callImageFileChooser;
import static prototype.xd.scheduler.utilities.Utilities.getRootDir;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.SettingsFragment;

public class BackgroundImagesGridViewAdapter extends BaseAdapter {
    
    private final Activity rootActivity;
    private final String[] availableDays_localized;
    
    public BackgroundImagesGridViewAdapter(SettingsFragment fragment) {
        rootActivity = fragment.requireActivity();
        availableDays_localized = new String[]{
                fragment.getString(R.string.day_monday),
                fragment.getString(R.string.day_tuesday),
                fragment.getString(R.string.day_wednesday),
                fragment.getString(R.string.day_thursday),
                fragment.getString(R.string.day_friday),
                fragment.getString(R.string.day_saturday),
                fragment.getString(R.string.day_sunday),
                fragment.getString(R.string.day_default)
        };
    }
    
    @Override
    public int getCount() {
        return availableDays.length;
    }
    
    @Override
    public Object getItem(int i) {
        return availableDays[i];
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
            FileInputStream inputStream = new FileInputStream(new File(getRootDir(parent.getContext()), availableDays[i] + ".png_min.png"));
            imageView.setImageBitmap(BitmapFactory.decodeStream(inputStream));
            inputStream.close();
        } catch (FileNotFoundException e) {
            imageView.setImageResource(R.drawable.ic_not);
        } catch (IOException e) {
            logException(e);
        }
        
        imageView.setOnClickListener(v -> callImageFileChooser(rootActivity, i));
        
        return convertView;
    }
    
    
}
