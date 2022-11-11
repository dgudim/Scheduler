package prototype.xd.scheduler.utilities.services;

import static android.util.Log.INFO;
import static android.util.Log.WARN;
import static prototype.xd.scheduler.utilities.BitmapUtilities.fingerPrintAndSaveBitmap;
import static prototype.xd.scheduler.utilities.BitmapUtilities.getAverageColor;
import static prototype.xd.scheduler.utilities.BitmapUtilities.hashBitmap;
import static prototype.xd.scheduler.utilities.BitmapUtilities.makeMutable;
import static prototype.xd.scheduler.utilities.BitmapUtilities.noFingerPrint;
import static prototype.xd.scheduler.utilities.BitmapUtilities.readStream;
import static prototype.xd.scheduler.utilities.DateManager.DEFAULT_BACKGROUND_NAME;
import static prototype.xd.scheduler.utilities.DateManager.WEEK_DAYS;
import static prototype.xd.scheduler.utilities.DateManager.currentDay;
import static prototype.xd.scheduler.utilities.DateManager.getCurrentTimestamp;
import static prototype.xd.scheduler.utilities.Keys.DISPLAY_METRICS_HEIGHT;
import static prototype.xd.scheduler.utilities.Keys.DISPLAY_METRICS_SCALED_DENSITY;
import static prototype.xd.scheduler.utilities.Keys.DISPLAY_METRICS_WIDTH;
import static prototype.xd.scheduler.utilities.Keys.ITEM_FULL_WIDTH_LOCK;
import static prototype.xd.scheduler.utilities.Keys.PREFERENCES;
import static prototype.xd.scheduler.utilities.Keys.SETTINGS_DEFAULT_ITEM_FULL_WIDTH_LOCK;
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
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.color.MaterialColors;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.utilities.Keys;

class LockScreenBitmapDrawer {
    
    private static final String NAME = "Lockscreen bitmap drawer";
    
    public final int displayWidth;
    public final int displayHeight;
    private float scaledDensity;
    
    private volatile boolean busy = false;
    
    private final WallpaperManager wallpaperManager;
    private final SharedPreferences preferences;
    private final LayoutInflater layoutInflater;
    
    private long previous_hash;
    
    private float densityScaledFontSize = 0;
    
    public LockScreenBitmapDrawer(Context context) throws IllegalStateException {
        wallpaperManager = WallpaperManager.getInstance(context);
        preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        
        if ((scaledDensity = preferences.getFloat(DISPLAY_METRICS_SCALED_DENSITY, -1)) == -1) {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(displayMetrics);
            
            preferences.edit()
                    .putInt(DISPLAY_METRICS_WIDTH, displayMetrics.widthPixels)
                    .putInt(DISPLAY_METRICS_HEIGHT, displayMetrics.heightPixels)
                    .putFloat(DISPLAY_METRICS_SCALED_DENSITY, displayMetrics.density).apply();
            scaledDensity = displayMetrics.density;
            
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
        
        densityScaledFontSize = preferences.getInt(Keys.FONT_SIZE, Keys.SETTINGS_DEFAULT_FONT_SIZE) / 2.7F * scaledDensity;
        
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
        List<Group> groups = loadGroups(context);
        
        // load user defined entries (from files)
        // add entries from all calendars spanning 4 weeks
        // sort and filter entries
        List<TodoListEntry> toAdd =
                filterEntries(
                        sortEntries(
                                loadTodoEntries(context, currentDay - 14, currentDay + 14, groups, null), currentDay));
        
        long currentHash = toAdd.hashCode() + preferences.getAll().hashCode() + hashBitmap(bitmap) + currentDay;
        if (previous_hash == currentHash) {
            throw new InterruptedException("No need to update the bitmap, list is the same, bailing out");
        }
        previous_hash = currentHash;
        
        // inflate the root container
        LinearLayout rootView = (LinearLayout) layoutInflater.inflate(R.layout.lockscreen_root_container, null);
        
        List<View> children = new ArrayList<>();
        
        // first pass, inflate all views, hide indicators and time text on not system calendar entries
        for (TodoListEntry todoListEntry : toAdd) {
            View basicView = layoutInflater.inflate(R.layout.entry_basic, null);
            
            TextView timeText = basicView.findViewById(R.id.time_text);
            View indicator = basicView.findViewById(R.id.indicator_view);
            
            if (todoListEntry.fromSystemCalendar) {
                timeText.setText(todoListEntry.getTimeSpan(context));
                timeText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, densityScaledFontSize);
                indicator.setBackgroundColor(todoListEntry.event.color);
            } else {
                timeText.setVisibility(View.GONE);
                indicator.setVisibility(View.GONE);
            }
            
            children.add(basicView);
        }
        
        // second pass, apply common values
        for (int i = 0; i < children.size(); i++) {
            TodoListEntry todoListEntry = toAdd.get(i);
            View child = children.get(i);
            
            if (todoListEntry.isAdaptiveColorEnabled()) {
                int width = child.getWidth();
                int height = child.getHeight();
                
                int[] pixels = new int[width * height];
                bitmap.getPixels(pixels, 0, width, (int) child.getX(), (int) child.getY(), width, height);
                todoListEntry.averageBackgroundColor = getAverageColor(pixels);
            }
            
            int bgColor = todoListEntry.getAdaptiveColor(todoListEntry.borderColor);
            
            // set border and bg colors
            child.findViewById(R.id.background_outline).setBackgroundColor(bgColor);
            child.findViewById(R.id.background_main).setBackgroundColor(
                    todoListEntry.getAdaptiveColor(todoListEntry.bgColor));
            
            // set text value and harmonize text color to make sure it's visible
            TextView title = child.findViewById(R.id.title_text);
            title.setText(todoListEntry.getTextOnDay(currentDay, context));
            title.setTextColor(MaterialColors.harmonize(todoListEntry.fontColor, bgColor));
            title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, densityScaledFontSize * 1.1F);
            
            child.setLayoutParams(new LinearLayout.LayoutParams(
                    preferences.getBoolean(ITEM_FULL_WIDTH_LOCK, SETTINGS_DEFAULT_ITEM_FULL_WIDTH_LOCK) ?
                            LinearLayout.LayoutParams.MATCH_PARENT : LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            
            rootView.addView(child);
        }
        
        // lay everything out assigning real sizes
        int measuredWidthSpec = View.MeasureSpec.makeMeasureSpec(displayWidth, View.MeasureSpec.EXACTLY);
        int measuredHeightSpec = View.MeasureSpec.makeMeasureSpec(displayHeight, View.MeasureSpec.EXACTLY);
        
        // measure and layout the view with the screen dimensions
        rootView.measure(measuredWidthSpec, measuredHeightSpec);
        rootView.layout(0, 0, rootView.getMeasuredWidth(), rootView.getMeasuredHeight());
        
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
            if (entry.isVisibleOnLockscreen()) {
                toAdd.add(entry);
            }
        }
        return toAdd;
    }
}
