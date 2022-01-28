package prototype.xd.scheduler.adapters;

import static prototype.xd.scheduler.utilities.DateManager.availableDays;
import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.Utilities.callImageFileChooser;
import static prototype.xd.scheduler.utilities.Utilities.rootDir;

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
    
    private final LayoutInflater inflater;
    private final Activity rootActivity;
    
    public BackgroundImagesGridViewAdapter(SettingsFragment fragment) {
        inflater = LayoutInflater.from(fragment.context);
        rootActivity = fragment.requireActivity();
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
            convertView = inflater.inflate(R.layout.background_image_entry, parent, false);
        }
        
        ((TextView) convertView.findViewById(R.id.bg_title)).setText(availableDays[i]);
        ImageView imageView = convertView.findViewById(R.id.bg_image);
        
        try {
            FileInputStream inputStream = new FileInputStream(new File(rootDir, availableDays[i] + ".png_min.png"));
            imageView.setImageBitmap(BitmapFactory.decodeStream(inputStream));
            inputStream.close();
        } catch (FileNotFoundException e) {
            imageView.setImageResource(R.drawable.ic_not);
        } catch (IOException e) {
            logException(e);
        }
    
        imageView.setOnClickListener(v -> {
            callImageFileChooser(rootActivity, i);
        });
        
        return convertView;
    }
    
    
}
