package prototype.xd.scheduler.utilities.services;

import static prototype.xd.scheduler.utilities.DateManager.currentDayUTC;
import static prototype.xd.scheduler.utilities.DateManager.getCurrentTimestampUTC;
import static prototype.xd.scheduler.utilities.DateManager.getCurrentWeekdayBgName;
import static prototype.xd.scheduler.utilities.DateManager.systemTimeZone;
import static prototype.xd.scheduler.utilities.GraphicsUtilities.fingerPrintAndSaveBitmap;
import static prototype.xd.scheduler.utilities.GraphicsUtilities.hashBitmap;
import static prototype.xd.scheduler.utilities.GraphicsUtilities.makeMutable;
import static prototype.xd.scheduler.utilities.GraphicsUtilities.hasNoFingerPrint;
import static prototype.xd.scheduler.utilities.GraphicsUtilities.readBitmapFromFile;
import static prototype.xd.scheduler.utilities.Static.DISPLAY_METRICS_DENSITY;
import static prototype.xd.scheduler.utilities.Static.DISPLAY_METRICS_HEIGHT;
import static prototype.xd.scheduler.utilities.Static.DISPLAY_METRICS_WIDTH;
import static prototype.xd.scheduler.utilities.Static.LOCKSCREEN_VIEW_VERTICAL_BIAS;
import static prototype.xd.scheduler.utilities.Static.SERVICE_FAILED;
import static prototype.xd.scheduler.utilities.Static.SETTINGS_MAX_EXPIRED_UPCOMING_ITEMS_OFFSET;
import static prototype.xd.scheduler.utilities.Static.TODO_ITEM_VIEW_TYPE;
import static prototype.xd.scheduler.utilities.Static.WALLPAPER_OBTAIN_FAILED;
import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getAllCalendars;
import static prototype.xd.scheduler.utilities.Utilities.getFile;
import static prototype.xd.scheduler.utilities.Utilities.isVerticalOrientation;
import static prototype.xd.scheduler.utilities.Utilities.loadGroups;
import static prototype.xd.scheduler.utilities.Utilities.loadTodoEntries;
import static prototype.xd.scheduler.utilities.Utilities.nullWrapper;
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
import prototype.xd.scheduler.utilities.Static;
import prototype.xd.scheduler.utilities.Logger;
import prototype.xd.scheduler.views.lockscreen.LockScreenTodoItemView;
import prototype.xd.scheduler.views.lockscreen.LockScreenTodoItemView.TodoItemViewType;

class LockScreenBitmapDrawer {
    
    public static final String NAME = LockScreenBitmapDrawer.class.getSimpleName();
    
    private final int displayWidth;
    private final int displayHeight;
    
    private volatile boolean busy;
    
    private final WallpaperManager wallpaperManager;
    private final LayoutInflater layoutInflater;
    
    private long previousHash;
    
    LockScreenBitmapDrawer(@NonNull Context context) throws IllegalStateException {
        wallpaperManager = WallpaperManager.getInstance(context);
        
        if (DISPLAY_METRICS_DENSITY.get() == -1) {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay().getRealMetrics(displayMetrics); // NOSONAR, replacement api is garbage
            
            Static.edit()
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
        try (ParcelFileDescriptor wallpaperFile = wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_LOCK)) {
            if (wallpaperFile != null) {
                return BitmapFactory.decodeFileDescriptor(wallpaperFile.getFileDescriptor());
            } else {
                Bitmap blankBitmap = Bitmap.createBitmap(displayWidth, displayHeight, Bitmap.Config.ARGB_8888);
                blankBitmap.eraseColor(13);
                return blankBitmap;
            }
        }
    }
    
    private Bitmap getBitmapToDrawOn() throws IOException {
        Bitmap bitmap = getBitmapFromLockScreen();
        File bg = getBackgroundAccordingToDayAndTime();
        
        if (hasNoFingerPrint(bitmap)) {
            bitmap = fingerPrintAndSaveBitmap(bitmap, bg);
        } else {
            if (!bg.exists()) {
                Logger.warning(NAME, bg + " is not available, falling back to default");
                File defFile = getFile(DateManager.DEFAULT_BACKGROUND_NAME);
                if (!defFile.exists()) {
                    throw new FileNotFoundException("No available background to load");
                }
                bg = defFile;
            }
            bitmap = readBitmapFromFile(bg);
        }
        return makeMutable(bitmap);
    }
    
    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    boolean constructBitmap(@NonNull Context context) {
        
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
                    
                    drawItemsOnBitmap(context, bitmap);
                    Logger.info(NAME, "Processed wallpaper in " + (getCurrentTimestampUTC() - time) + "ms");
                    
                    time = getCurrentTimestampUTC();
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK);
                    Logger.info(NAME, " ------------ Set wallpaper in " + (getCurrentTimestampUTC() - time) / 1000F + "s ------------ ");
                    
                    WALLPAPER_OBTAIN_FAILED.put(Boolean.FALSE);
                    
                } catch (InterruptedException e) {
                    Logger.info(NAME, nullWrapper(e.getMessage()));
                    // relay
                    Thread.currentThread().interrupt();
                } catch (FileNotFoundException e) {
                    // TODO: 02.02.2023 increment instead
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
    
    private static long getEntryListHash(@NonNull List<TodoEntry> entries) {
        long hash = 0;
        for (TodoEntry entry : entries) {
            hash += entry.getLockscreenHash();
        }
        return hash;
    }
    
    @SuppressLint("InflateParams")
    private void drawItemsOnBitmap(@NonNull Context context, @NonNull Bitmap bitmap) throws InterruptedException {
        
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
        
        long currentHash =
                getEntryListHash(toAdd) +
                Static.getAll().hashCode() +
                hashBitmap(bitmap) +
                currentDayUTC +
                todoItemViewType.ordinal() +
                systemTimeZone.getValue().hashCode();
        
        Logger.debug(NAME, "Previous lockscreen hash: " + previousHash + " | current lockscreen hash: " + currentHash);
        if (previousHash == currentHash) {
            throw new InterruptedException("No need to update the bitmap, list is the same, bailing out");
        }
        previousHash = currentHash;
        
        // inflate the root container
        LockscreenRootContainerBinding binding = LockscreenRootContainerBinding.inflate(layoutInflater);
        LinearLayout containerView = binding.linearContainer;
        ConstraintLayout rootView = binding.getRoot();
        
        // set vertical bias
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) containerView.getLayoutParams();
        // convert percentage to 0 to 1
        params.verticalBias = LOCKSCREEN_VIEW_VERTICAL_BIAS.get() / 100F;
        containerView.setLayoutParams(params);
        
        List<LockScreenTodoItemView<?>> itemViews = new ArrayList<>(toAdd.size());
        
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
        
        Canvas canvas = new Canvas(bitmap);
        rootView.draw(canvas);
    }
    
    @NonNull
    private static File getBackgroundAccordingToDayAndTime() {
        
        if (!Static.ADAPTIVE_BACKGROUND_ENABLED.get()) {
            return getFile(DateManager.DEFAULT_BACKGROUND_NAME);
        }
        
        return getFile(getCurrentWeekdayBgName());
    }
}
