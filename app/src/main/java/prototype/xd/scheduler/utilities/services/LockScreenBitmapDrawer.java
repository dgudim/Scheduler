package prototype.xd.scheduler.utilities.services;

import static android.util.Log.INFO;
import static android.util.Log.WARN;
import static prototype.xd.scheduler.utilities.BitmapUtilities.fingerPrintAndSaveBitmap;
import static prototype.xd.scheduler.utilities.BitmapUtilities.hashBitmap;
import static prototype.xd.scheduler.utilities.BitmapUtilities.makeMutable;
import static prototype.xd.scheduler.utilities.BitmapUtilities.noFingerPrint;
import static prototype.xd.scheduler.utilities.BitmapUtilities.readStream;
import static prototype.xd.scheduler.utilities.DateManager.DEFAULT_BACKGROUND_NAME;
import static prototype.xd.scheduler.utilities.DateManager.WEEK_DAYS;
import static prototype.xd.scheduler.utilities.DateManager.currentDay;
import static prototype.xd.scheduler.utilities.DateManager.getCurrentTimestamp;
import static prototype.xd.scheduler.utilities.Keys.DISPLAY_METRICS_DENSITY;
import static prototype.xd.scheduler.utilities.Keys.DISPLAY_METRICS_HEIGHT;
import static prototype.xd.scheduler.utilities.Keys.DISPLAY_METRICS_WIDTH;
import static prototype.xd.scheduler.utilities.Keys.PREFERENCES;
import static prototype.xd.scheduler.utilities.Keys.SETTINGS_DEFAULT_TODO_ITEM_VIEW_TYPE;
import static prototype.xd.scheduler.utilities.Keys.TODO_ITEM_VIEW_TYPE;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.Utilities.getFile;
import static prototype.xd.scheduler.utilities.Utilities.isVerticalOrientation;
import static prototype.xd.scheduler.utilities.Utilities.loadGroups;
import static prototype.xd.scheduler.utilities.Utilities.loadTodoEntries;
import static prototype.xd.scheduler.utilities.Utilities.sortEntries;

import android.annotation.SuppressLint;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.GroupList;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.views.lockscreen.LockScreenTodoItemView;
import prototype.xd.scheduler.views.lockscreen.LockScreenTodoItemView.TodoItemViewType;

class LockScreenBitmapDrawer {
    
    private static final String NAME = "Lockscreen bitmap drawer";
    
    public final int displayWidth;
    public final int displayHeight;
    
    private volatile boolean busy = false;
    
    private final WallpaperManager wallpaperManager;
    private final SharedPreferences preferences;
    private final LayoutInflater layoutInflater;
    
    private long previous_hash;
    
    public LockScreenBitmapDrawer(Context context) throws IllegalStateException {
        wallpaperManager = WallpaperManager.getInstance(context);
        preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    
        if (preferences.getFloat(DISPLAY_METRICS_DENSITY, -1) == -1) {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(displayMetrics);
            
            preferences.edit()
                    .putInt(DISPLAY_METRICS_WIDTH, displayMetrics.widthPixels)
                    .putInt(DISPLAY_METRICS_HEIGHT, displayMetrics.heightPixels)
                    .putFloat(DISPLAY_METRICS_DENSITY, displayMetrics.density).apply();
            
            log(INFO, NAME, "got display metrics: " + displayMetrics);
        }
        
        previous_hash = 0;
        
        displayWidth = preferences.getInt(DISPLAY_METRICS_WIDTH, 100);
        displayHeight = preferences.getInt(DISPLAY_METRICS_HEIGHT, 100);
        
        layoutInflater = LayoutInflater.from(context);
    }
    
    @SuppressLint("MissingPermission")
    private Bitmap getBitmapFromLockScreen() throws IOException {
        ParcelFileDescriptor wallpaperFile = wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_LOCK);
        if (wallpaperFile == null) {
            wallpaperFile = wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_SYSTEM);
        }
        if (wallpaperFile != null) {
            Bitmap bitmap = BitmapFactory.decodeFileDescriptor(wallpaperFile.getFileDescriptor());
            wallpaperFile.close();
            return bitmap;
        } else {
            Bitmap blankBitmap = Bitmap.createBitmap(displayWidth, displayHeight, Bitmap.Config.ARGB_8888);
            blankBitmap.eraseColor(13);
            return blankBitmap;
        }
    }
    
    public boolean constructBitmap(BackgroundSetterService backgroundSetterService) {
        
        if (!isVerticalOrientation(backgroundSetterService)) {
            log(WARN, NAME, "Not starting bitmap thread, orientation not vertical");
            return false;
        }
        
        if (!busy) {
            busy = true;
            new Thread(() -> {
                try {
                    
                    long time = getCurrentTimestamp();
                    log(INFO, NAME, "Setting wallpaper");
                    
                    Bitmap bitmap = getBitmapFromLockScreen();
                    File bg = getBackgroundAccordingToDayAndTime();
                    
                    if (noFingerPrint(bitmap)) {
                        bitmap = fingerPrintAndSaveBitmap(bitmap, bg);
                    } else {
                        if (bg.exists()) {
                            bitmap = readStream(new FileInputStream(bg));
                        } else {
                            File defFile = getFile(DEFAULT_BACKGROUND_NAME);
                            if (defFile.exists()) {
                                bitmap = readStream(new FileInputStream(defFile));
                            } else {
                                throw new FileNotFoundException("No available background to load");
                            }
                        }
                    }
                    
                    bitmap = makeMutable(bitmap);
                    drawItemsOnBitmap(backgroundSetterService, bitmap);
                    log(INFO, NAME, "Processed wallpaper in " + (getCurrentTimestamp() - time) + "ms");
                    time = getCurrentTimestamp();
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK);
                    log(INFO, NAME, "Set wallpaper in " + (getCurrentTimestamp() - time) / 1000f + "s");
                    
                } catch (InterruptedException e) {
                    log(INFO, NAME, e.getMessage());
                } catch (Exception e) {
                    logException(NAME, e);
                }
                busy = false;
            }, "Bitmap thread").start();
            return true;
        }
        return false;
    }
    
    @SuppressLint("InflateParams")
    private void drawItemsOnBitmap(@NonNull Context context, @NonNull Bitmap bitmap) throws InterruptedException {
        
        Canvas canvas = new Canvas(bitmap);
        GroupList groups = loadGroups();
        TodoItemViewType todoItemViewType = TodoItemViewType.valueOf(preferences.getString(TODO_ITEM_VIEW_TYPE, SETTINGS_DEFAULT_TODO_ITEM_VIEW_TYPE));
        // load user defined entries (from files)
        // add entries from all calendars spanning 4 weeks
        // sort and filter entries
        List<TodoListEntry> toAdd =
                filterEntries(
                        sortEntries(
                                loadTodoEntries(context, currentDay - 14, currentDay + 14, groups, null, false), currentDay));
        
        long currentHash = toAdd.hashCode() + preferences.getAll().hashCode() + hashBitmap(bitmap) + currentDay + todoItemViewType.ordinal();
        if (previous_hash == currentHash) {
            throw new InterruptedException("No need to update the bitmap, list is the same, bailing out");
        }
        previous_hash = currentHash;
        
        // inflate the root container
        LinearLayout rootView = (LinearLayout) layoutInflater.inflate(R.layout.lockscreen_root_container, null);
        
        List<LockScreenTodoItemView<?>> itemViews = new ArrayList<>();
        
        // first pass, add all views, setup layout independent parameters
        for (TodoListEntry todoListEntry : toAdd) {
            LockScreenTodoItemView<?> itemView = LockScreenTodoItemView.inflateViewByType(todoItemViewType, rootView, layoutInflater);
            itemView.applyLayoutIndependentParameters(todoListEntry, preferences);
            itemViews.add(itemView);
            rootView.addView(itemView.getRoot());
        }
        
        // get measure specs with screen size
        int measuredWidthSpec = View.MeasureSpec.makeMeasureSpec(displayWidth, View.MeasureSpec.EXACTLY);
        int measuredHeightSpec = View.MeasureSpec.makeMeasureSpec(displayHeight, View.MeasureSpec.EXACTLY);
        
        // measure and layout the view with the screen dimensions
        rootView.measure(measuredWidthSpec, measuredHeightSpec);
        // lay everything out assigning real sizes
        rootView.layout(0, 0, rootView.getMeasuredWidth(), rootView.getMeasuredHeight());
        
        // second pass, apply layout dependent parameters
        for (int i = 0; i < itemViews.size(); i++) {
            itemViews.get(i).applyLayoutDependentParameters(toAdd.get(i), bitmap);
        }
        
        rootView.draw(canvas);
    }
    
    private File getBackgroundAccordingToDayAndTime() {
        
        if (!preferences.getBoolean(Keys.ADAPTIVE_BACKGROUND_ENABLED, Keys.SETTINGS_DEFAULT_ADAPTIVE_BACKGROUND_ENABLED)) {
            return getFile(DEFAULT_BACKGROUND_NAME);
        }
        
        int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        CharSequence dayString;
        if (day == 1) {
            dayString = WEEK_DAYS[6];
        } else {
            dayString = WEEK_DAYS[day - 2];
        }
        
        return getFile(dayString + ".png");
    }
    
    
    private List<TodoListEntry> filterEntries(List<TodoListEntry> entries) {
        List<TodoListEntry> toAdd = new ArrayList<>();
        for (TodoListEntry entry : entries) {
            if (entry.isVisibleOnLockscreenToday()) {
                toAdd.add(entry);
            }
        }
        return toAdd;
    }
}
