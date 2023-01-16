package prototype.xd.scheduler.utilities.services;

import static prototype.xd.scheduler.utilities.DateManager.currentDayUTC;
import static prototype.xd.scheduler.utilities.DateManager.getCurrentTimestampUTC;
import static prototype.xd.scheduler.utilities.DateManager.getCurrentWeekdayLocaleAgnosticString;
import static prototype.xd.scheduler.utilities.GraphicsUtilities.fingerPrintAndSaveBitmap;
import static prototype.xd.scheduler.utilities.GraphicsUtilities.hashBitmap;
import static prototype.xd.scheduler.utilities.GraphicsUtilities.makeMutable;
import static prototype.xd.scheduler.utilities.GraphicsUtilities.noFingerPrint;
import static prototype.xd.scheduler.utilities.GraphicsUtilities.readBitmapFromFile;
import static prototype.xd.scheduler.utilities.Keys.DISPLAY_METRICS_DENSITY;
import static prototype.xd.scheduler.utilities.Keys.DISPLAY_METRICS_HEIGHT;
import static prototype.xd.scheduler.utilities.Keys.DISPLAY_METRICS_WIDTH;
import static prototype.xd.scheduler.utilities.Keys.LOCKSCREEN_VIEW_VERTICAL_BIAS;
import static prototype.xd.scheduler.utilities.Keys.SERVICE_FAILED;
import static prototype.xd.scheduler.utilities.Keys.SETTINGS_MAX_EXPIRED_UPCOMING_ITEMS_OFFSET;
import static prototype.xd.scheduler.utilities.Keys.TODO_ITEM_VIEW_TYPE;
import static prototype.xd.scheduler.utilities.Keys.WALLPAPER_OBTAIN_FAILED;
import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getAllCalendars;
import static prototype.xd.scheduler.utilities.Utilities.getFile;
import static prototype.xd.scheduler.utilities.Utilities.isVerticalOrientation;
import static prototype.xd.scheduler.utilities.Utilities.loadGroups;
import static prototype.xd.scheduler.utilities.Utilities.loadTodoEntries;
import static prototype.xd.scheduler.utilities.Utilities.sortEntries;

import android.annotation.SuppressLint;
import android.app.WallpaperManager;
import android.content.Context;
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
import androidx.constraintlayout.widget.ConstraintLayout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import prototype.xd.scheduler.databinding.LockscreenRootContainerBinding;
import prototype.xd.scheduler.entities.GroupList;
import prototype.xd.scheduler.entities.TodoEntry;
import prototype.xd.scheduler.utilities.DateManager;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.Logger;
import prototype.xd.scheduler.views.lockscreen.LockScreenTodoItemView;
import prototype.xd.scheduler.views.lockscreen.LockScreenTodoItemView.TodoItemViewType;

class LockScreenBitmapDrawer {
    
    public static final String NAME = LockScreenBitmapDrawer.class.getSimpleName();
    
    public final int displayWidth;
    public final int displayHeight;
    
    private volatile boolean busy = false;
    
    private final WallpaperManager wallpaperManager;
    private final LayoutInflater layoutInflater;
    
    private long previousHash;
    
    public LockScreenBitmapDrawer(Context context) throws IllegalStateException {
        wallpaperManager = WallpaperManager.getInstance(context);
        
        if (DISPLAY_METRICS_DENSITY.get() == -1) {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(displayMetrics);
            
            Keys.edit()
                    .putInt(DISPLAY_METRICS_WIDTH.key, displayMetrics.widthPixels)
                    .putInt(DISPLAY_METRICS_HEIGHT.key, displayMetrics.heightPixels)
                    .putFloat(DISPLAY_METRICS_DENSITY.key, displayMetrics.density)
                    .apply();
            
            Logger.info(NAME, "got display metrics: " + displayMetrics);
        }
        
        previousHash = 0;
        
        displayWidth = DISPLAY_METRICS_WIDTH.get();
        displayHeight = DISPLAY_METRICS_HEIGHT.get();
        
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
    
    private Bitmap getBitmapToDrawOn() throws IOException {
        Bitmap bitmap = getBitmapFromLockScreen();
        File bg = getBackgroundAccordingToDayAndTime();
        
        if (noFingerPrint(bitmap)) {
            bitmap = fingerPrintAndSaveBitmap(bitmap, bg);
        } else {
            if (bg.exists()) {
                bitmap = readBitmapFromFile(bg);
            } else {
                File defFile = getFile(DateManager.DEFAULT_BACKGROUND_NAME);
                if (defFile.exists()) {
                    bitmap = readBitmapFromFile(defFile);
                } else {
                    throw new FileNotFoundException("No available background to load");
                }
            }
        }
        return makeMutable(bitmap);
    }
    
    public boolean constructBitmap(@NonNull Context context, boolean forceRedraw) {
        
        if (!isVerticalOrientation(context)) {
            Logger.warning(NAME, "Not starting bitmap thread, orientation not vertical");
            return false;
        }
        
        if (!busy) {
            busy = true;
            new Thread(() -> {
                try {
                    
                    long time = getCurrentTimestampUTC();
                    Logger.info(NAME, " ------------ Setting wallpaper ------------ ");
                    
                    Bitmap bitmap = getBitmapToDrawOn();
                    
                    drawItemsOnBitmap(context, bitmap, forceRedraw);
                    Logger.info(NAME, "Processed wallpaper in " + (getCurrentTimestampUTC() - time) + "ms");
                    
                    time = getCurrentTimestampUTC();
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK);
                    Logger.info(NAME, " ------------ Set wallpaper in " + (getCurrentTimestampUTC() - time) / 1000f + "s ------------ ");
                    
                } catch (InterruptedException e) {
                    Logger.info(NAME, e.getMessage());
                    // relay
                    Thread.currentThread().interrupt();
                } catch (FileNotFoundException e) {
                    WALLPAPER_OBTAIN_FAILED.put(Boolean.TRUE);
                    logException(NAME, e);
                } catch (Exception e) {
                    SERVICE_FAILED.put(Boolean.TRUE);
                    logException(NAME, e);
                }
                busy = false;
            }, "Bitmap thread").start();
            return true;
        }
        return false;
    }
    
    private long getEntryListHash(List<TodoEntry> entries) {
        long hash = 0;
        for (TodoEntry entry : entries) {
            hash += entry.getLockscreenHash();
        }
        return hash;
    }
    
    @SuppressLint("InflateParams")
    private void drawItemsOnBitmap(@NonNull Context context, @NonNull Bitmap bitmap, boolean forceRedraw) throws InterruptedException {
        
        Canvas canvas = new Canvas(bitmap);
        GroupList groups = loadGroups();
        TodoItemViewType todoItemViewType = TODO_ITEM_VIEW_TYPE.get();
        // load user defined entries (from files)
        // add entries from all calendars
        // filter and sort entries
        List<TodoEntry> toAdd = loadTodoEntries(
                currentDayUTC - SETTINGS_MAX_EXPIRED_UPCOMING_ITEMS_OFFSET,
                currentDayUTC + SETTINGS_MAX_EXPIRED_UPCOMING_ITEMS_OFFSET,
                groups, getAllCalendars(context, false),
                false);
        
        toAdd.removeIf(todoEntry -> !todoEntry.isVisibleOnLockscreenToday());
        toAdd = sortEntries(toAdd, currentDayUTC);
        
        long currentHash = getEntryListHash(toAdd) + Keys.getAll().hashCode() + hashBitmap(bitmap) + currentDayUTC + todoItemViewType.ordinal();
        Logger.debug(NAME, "Previous lockscreen hash: " + previousHash + " | current lockscreen hash: " + currentHash);
        if (previousHash == currentHash) {
            if (forceRedraw) {
                Logger.debug(NAME, "Updating bitmap because 'forceRedraw' is true");
            } else {
                throw new InterruptedException("No need to update the bitmap, list is the same, bailing out");
            }
        }
        previousHash = currentHash;
        
        // inflate the root container
        LockscreenRootContainerBinding binding = LockscreenRootContainerBinding.inflate(layoutInflater);
        LinearLayout containerView = binding.linearContainer;
        ConstraintLayout rootView = binding.getRoot();
        
        // set vertical bias
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) containerView.getLayoutParams();
        params.verticalBias = LOCKSCREEN_VIEW_VERTICAL_BIAS.get() / 100f; // convert percentage to 0 to 1
        containerView.setLayoutParams(params);
        
        List<LockScreenTodoItemView<?>> itemViews = new ArrayList<>();
        
        // first pass, add all views, setup layout independent parameters
        for (TodoEntry todoEntry : toAdd) {
            itemViews.add(LockScreenTodoItemView.inflateViewByType(todoItemViewType, containerView, layoutInflater)
                    .applyLayoutIndependentParameters(todoEntry)
                    .addToContainer(containerView));
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
            itemViews.get(i).applyLayoutDependentParameters(toAdd.get(i), bitmap, containerView);
        }
        
        rootView.draw(canvas);
    }
    
    private File getBackgroundAccordingToDayAndTime() {
        
        if (!Keys.ADAPTIVE_BACKGROUND_ENABLED.get()) {
            return getFile(DateManager.DEFAULT_BACKGROUND_NAME);
        }
        
        return getFile(getCurrentWeekdayLocaleAgnosticString() + ".png");
    }
}
