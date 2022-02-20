package prototype.xd.scheduler.utilities;

import static android.util.Log.INFO;
import static android.util.Log.WARN;
import static prototype.xd.scheduler.entities.Group.readGroupFile;
import static prototype.xd.scheduler.utilities.BitmapUtilities.fingerPrintAndSaveBitmap;
import static prototype.xd.scheduler.utilities.BitmapUtilities.getAverageColor;
import static prototype.xd.scheduler.utilities.BitmapUtilities.hashBitmap;
import static prototype.xd.scheduler.utilities.BitmapUtilities.noFingerPrint;
import static prototype.xd.scheduler.utilities.BitmapUtilities.readStream;
import static prototype.xd.scheduler.utilities.DateManager.currentDay;
import static prototype.xd.scheduler.utilities.DateManager.defaultBackgroundName;
import static prototype.xd.scheduler.utilities.DateManager.getBackgroundAccordingToDayAndTime;
import static prototype.xd.scheduler.utilities.DateManager.getCurrentTimestamp;
import static prototype.xd.scheduler.utilities.Keys.BLANK_TEXT;
import static prototype.xd.scheduler.utilities.Keys.DISPLAY_METRICS_HEIGHT;
import static prototype.xd.scheduler.utilities.Keys.DISPLAY_METRICS_SCALED_DENSITY;
import static prototype.xd.scheduler.utilities.Keys.DISPLAY_METRICS_WIDTH;
import static prototype.xd.scheduler.utilities.Keys.ITEM_FULL_WIDTH_LOCK;
import static prototype.xd.scheduler.utilities.Keys.PREFERENCES;
import static prototype.xd.scheduler.utilities.Keys.SETTINGS_DEFAULT_ITEM_FULL_WIDTH_LOCK;
import static prototype.xd.scheduler.utilities.Keys.TEXT_VALUE;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.Utilities.getFile;
import static prototype.xd.scheduler.utilities.Utilities.isVerticalOrientation;
import static prototype.xd.scheduler.utilities.Utilities.loadTodoEntries;
import static prototype.xd.scheduler.utilities.Utilities.sortEntries;

import android.annotation.SuppressLint;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.utilities.services.BackgroundSetterService;

public class LockScreenBitmapDrawer {
    
    public String currentBitmapLongestText = "";
    
    public final int displayWidth;
    public final int displayHeight;
    private float scaledDensity;
    private final Point displayCenter;
    
    private volatile boolean busy = false;
    
    private final WallpaperManager wallpaperManager;
    private final SharedPreferences preferences;
    
    private long previous_hash;
    
    public float fontSize_h = 0;
    private float fontSize_kM = 0;
    
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
            
            log(INFO, "BitmapDrawer", "got display metrics: " + displayMetrics);
        }
        
        previous_hash = 0;
        
        displayWidth = preferences.getInt(DISPLAY_METRICS_WIDTH, 100);
        displayHeight = preferences.getInt(DISPLAY_METRICS_HEIGHT, 100);
        
        displayCenter = new Point(displayWidth / 2, displayHeight / 2);
    }
    
    @SuppressLint("MissingPermission")
    private Bitmap getBitmapFromLockScreen() throws IOException {
        ParcelFileDescriptor wallpaperFile = wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_LOCK);
        if (wallpaperFile == null) {
            wallpaperFile = wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_SYSTEM);
        }
        if (wallpaperFile != null) {
            Bitmap bitmap = BitmapFactory.decodeFileDescriptor(wallpaperFile.getFileDescriptor()).copy(Bitmap.Config.ARGB_8888, true);
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
            log(WARN, "BitmapDrawer", "not starting bitmap thread, orientation not vertical");
            return false;
        }
        
        fontSize_h = preferences.getInt(Keys.FONT_SIZE, Keys.SETTINGS_DEFAULT_FONT_SIZE) * scaledDensity;
        fontSize_kM = fontSize_h * 1.1f;
        
        if (!busy) {
            busy = true;
            new Thread(() -> {
                try {
                    
                    long time = getCurrentTimestamp();
                    log(INFO, "BitmapDrawer", "setting wallpaper");
                    
                    Bitmap bitmap = getBitmapFromLockScreen();
                    File bg = getBackgroundAccordingToDayAndTime();
                    
                    if (noFingerPrint(bitmap)) {
                        bitmap = fingerPrintAndSaveBitmap(bitmap, bg);
                    } else {
                        if (bg.exists()) {
                            bitmap.recycle();
                            bitmap = readStream(new FileInputStream(bg));
                        } else {
                            File defFile = getFile(defaultBackgroundName);
                            if (defFile.exists()) {
                                bitmap.recycle();
                                bitmap = readStream(new FileInputStream(defFile));
                            } else {
                                throw new FileNotFoundException("No available background to load");
                            }
                        }
                    }
                    
                    drawStringsOnBitmap(backgroundSetterService, bitmap);
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK);
                    log(INFO, "BitmapDrawer", "set wallpaper in " + (getCurrentTimestamp() - time) / 1000f + "s");
                    
                    bitmap.recycle();
                } catch (Exception e) {
                    logException("BitmapDrawer", e);
                }
                busy = false;
            }, "Bitmap thread").start();
            return true;
        }
        return false;
    }
    
    private void drawStringsOnBitmap(BackgroundSetterService backgroundSetterService, Bitmap src) throws InterruptedException {
        
        Canvas canvas = new Canvas(src);
        ArrayList<Group> groups = readGroupFile(backgroundSetterService);
        
        ArrayList<TodoListEntry> toAdd = filterItems(sortEntries(
                loadTodoEntries(backgroundSetterService, currentDay - 14, currentDay + 14, groups), currentDay), backgroundSetterService);
        long currentHash = toAdd.hashCode() + preferences.getAll().hashCode() + hashBitmap(src) + currentDay;
        if (previous_hash == currentHash) {
            throw new InterruptedException("No need to update the bitmap, list is the same, bailing out");
        }
        previous_hash = currentHash;
        
        if (!toAdd.isEmpty()) {
            
            for (int i = 0; i < toAdd.size(); i++) {
                toAdd.get(i).loadDisplayData(this);
                toAdd.get(i).splitText(backgroundSetterService);
            }
            
            float totalHeight = 0;
            for (int i = 0; i < toAdd.size(); i++) {
                totalHeight += fontSize_h * toAdd.get(i).textValueSplit.length + fontSize_kM;
            }
            totalHeight -= fontSize_kM;
            totalHeight /= 2f;
            
            ArrayList<ArrayList<TodoListEntry>> splitEntries = new ArrayList<>();
            ArrayList<TodoListEntry> toAddSplit = new ArrayList<>();
            for (int i = toAdd.size() - 1; i >= 0; i--) {
                ArrayList<TodoListEntry> splits = new ArrayList<>();
                for (int i2 = toAdd.get(i).textValueSplit.length - 1; i2 >= 0; i2--) {
                    if (preferences.getBoolean(ITEM_FULL_WIDTH_LOCK, SETTINGS_DEFAULT_ITEM_FULL_WIDTH_LOCK)) {
                        toAdd.get(i).rWidth = displayWidth / 2f - toAdd.get(i).borderThickness;
                    }
                    TodoListEntry splitEntry;
                    if (toAdd.get(i).fromSystemCalendar) {
                        splitEntry = new TodoListEntry(toAdd.get(i).event);
                    } else {
                        splitEntry = new TodoListEntry(backgroundSetterService, toAdd.get(i).params, toAdd.get(i).getGroupName(), groups);
                    }
                    copyDisplayData(toAdd.get(i), splitEntry);
                    splitEntry.changeParameter(TEXT_VALUE, toAdd.get(i).textValueSplit[i2]);
                    toAddSplit.add(splitEntry);
                    splits.add(splitEntry);
                }
                splitEntries.add(splits);
                toAddSplit.add(new TodoListEntry());
            }
            
            for (int i = 0; i < toAddSplit.size(); i++) {
                if (toAddSplit.get(i).adaptiveColorEnabled && !toAddSplit.get(i).textValue.equals(BLANK_TEXT)) {
                    int width = (int) (toAddSplit.get(i).rWidth * 2);
                    int height = (int) (fontSize_h / 2f + fontSize_kM);
                    int VOffset = (int) (displayCenter.y + totalHeight - fontSize_kM * (i + 1));
                    int HOffset = (src.getWidth() - width) / 2;
                    
                    Rect destRect = new Rect(HOffset, VOffset, width + HOffset, height + VOffset);
                    
                    Bitmap cutBitmap = Bitmap.createBitmap(
                            destRect.right,
                            destRect.bottom, Bitmap.Config.ARGB_8888);
                    
                    Canvas canvas1 = new Canvas(cutBitmap);
                    canvas1.drawBitmap(src, destRect, destRect, null);
                    
                    toAddSplit.get(i).adaptiveColor = getAverageColor(cutBitmap);
                    cutBitmap.recycle();
                }
            }
            
            for (ArrayList<TodoListEntry> splits : splitEntries) {
                if (splits.get(0).adaptiveColorEnabled) {
                    int red = 0;
                    int green = 0;
                    int blue = 0;
                    for (int i = 0; i < splits.size(); i++) {
                        red += (splits.get(i).adaptiveColor >> 16) & 0xFF;
                        green += (splits.get(i).adaptiveColor >> 8) & 0xFF;
                        blue += (splits.get(i).adaptiveColor & 0xFF);
                    }
                    for (int i = 0; i < splits.size(); i++) {
                        splits.get(i).adaptiveColor = Color.argb(
                                255,
                                red / splits.size(),
                                green / splits.size(),
                                blue / splits.size());
                    }
                }
            }
            
            for (int i = 0; i < toAddSplit.size(); i++) {
                if (!toAddSplit.get(i).textValue.equals(BLANK_TEXT)) {
                    toAddSplit.get(i).initializeBgAndPadPaints();
                }
            }
            
            drawBgOnCanvas(toAddSplit, canvas, totalHeight);
            drawTextListOnCanvas(toAddSplit, canvas, totalHeight);
        }
    }
    
    private static void copyDisplayData(TodoListEntry from, TodoListEntry to) {
        to.padPaint = from.padPaint;
        to.bgPaint = from.bgPaint;
        to.textPaint = from.textPaint;
        to.maxChars = from.maxChars;
        to.rWidth = from.rWidth;
    }
    
    private void drawTextListOnCanvas(ArrayList<TodoListEntry> toAdd, Canvas canvas, float maxHeight) {
        for (int i = 0; i < toAdd.size(); i++) {
            if (!toAdd.get(i).textValue.equals(BLANK_TEXT)) {
                canvas.drawText(toAdd.get(i).textValue, displayCenter.x, displayCenter.y + maxHeight - fontSize_kM * i, toAdd.get(i).textPaint);
            }
        }
    }
    
    private void drawBgOnCanvas(ArrayList<TodoListEntry> toAdd, Canvas canvas, float maxHeight) {
        for (int i = 0; i < toAdd.size(); i++) {
            if (!toAdd.get(i).textValue.equals(BLANK_TEXT)) {
                
                if (toAdd.get(i).borderThickness > 0) {
                    drawRectRelativeToTheCenter(canvas, toAdd.get(i).padPaint, maxHeight,
                            -toAdd.get(i).rWidth - toAdd.get(i).borderThickness,
                            fontSize_h / 2f - fontSize_kM * i,
                            toAdd.get(i).rWidth + toAdd.get(i).borderThickness,
                            -fontSize_kM * (i + 1) - toAdd.get(i).borderThickness);
                }
                
                drawRectRelativeToTheCenter(canvas, toAdd.get(i).bgPaint, maxHeight,
                        -toAdd.get(i).rWidth,
                        fontSize_h / 2f - fontSize_kM * i,
                        toAdd.get(i).rWidth,
                        -fontSize_kM * (i + 1));
                
                if (toAdd.get(i).fromSystemCalendar) {
                    drawRectRelativeToTheCenter(canvas, toAdd.get(i).indicatorPaint, maxHeight,
                            -toAdd.get(i).rWidth + 10,
                            fontSize_h / 2f - fontSize_kM * i,
                            -toAdd.get(i).rWidth + 35,
                            -fontSize_kM * (i + 1));
                }
                
            }
        }
    }
    
    private void drawRectRelativeToTheCenter(Canvas canvas, Paint paint, float maxHeight, float left, float top, float right, float bottom) {
        
        canvas.drawRect(displayCenter.x + left,
                displayCenter.y + maxHeight + top,
                displayCenter.x + right,
                displayCenter.y + maxHeight + bottom, paint);
    }
    
    private ArrayList<TodoListEntry> filterItems(ArrayList<TodoListEntry> input, Context context) {
        ArrayList<TodoListEntry> toAdd = new ArrayList<>();
        currentBitmapLongestText = "";
        for (int i = 0; i < input.size(); i++) {
            if (input.get(i).getLockViewState()) {
                toAdd.add(input.get(i));
                String textVal = input.get(i).textValue + input.get(i).getDayOffset(currentDay, context);
                String time_string = input.get(i).getTimeSpan(context);
                if (textVal.length() > currentBitmapLongestText.length()) {
                    currentBitmapLongestText = textVal;
                }
                if (time_string.length() > currentBitmapLongestText.length()) {
                    currentBitmapLongestText = time_string;
                }
            }
        }
        return toAdd;
    }
}
