package prototype.xd.scheduler.utilities;

import static prototype.xd.scheduler.entities.TodoListEntry.blankTextValue;
import static prototype.xd.scheduler.utilities.BackgroundChooser.defaultBackgroundName;
import static prototype.xd.scheduler.utilities.BackgroundChooser.getBackgroundAccordingToDayAndTime;
import static prototype.xd.scheduler.utilities.BitmapUtilities.fingerPrintAndSaveBitmap;
import static prototype.xd.scheduler.utilities.BitmapUtilities.getAverageColor;
import static prototype.xd.scheduler.utilities.BitmapUtilities.hashBitmap;
import static prototype.xd.scheduler.utilities.BitmapUtilities.noFingerPrint;
import static prototype.xd.scheduler.utilities.BitmapUtilities.readStream;
import static prototype.xd.scheduler.utilities.Keys.ITEM_FULL_WIDTH_LOCK;
import static prototype.xd.scheduler.utilities.Keys.SETTINGS_DEFAULT_ITEM_FULL_WIDTH_LOCK;
import static prototype.xd.scheduler.utilities.Keys.TEXT_VALUE;
import static prototype.xd.scheduler.utilities.Logger.ContentType.INFO;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.Utilities.loadTodoEntries;
import static prototype.xd.scheduler.utilities.Utilities.rootDir;

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
import android.view.Display;
import android.view.WindowManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import prototype.xd.scheduler.BackgroundSetterService;
import prototype.xd.scheduler.entities.TodoListEntry;

public class LockScreenBitmapDrawer {
    
    public String currentBitmapLongestText = "";
    
    public final int displayWidth;
    public final int displayHeight;
    public final DisplayMetrics displayMetrics;
    private final Point displayCenter;
    private final SharedPreferences preferences;
    
    private volatile boolean busy = false;
    
    private final WallpaperManager wallpaperManager;
    
    private int previous_hash;
    
    public LockScreenBitmapDrawer(Context context) {
        wallpaperManager = WallpaperManager.getInstance(context);
        preferences = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        
        displayMetrics = new DisplayMetrics();
        display.getRealMetrics(displayMetrics);
        
        previous_hash = 0;
        
        displayWidth = displayMetrics.widthPixels;
        displayHeight = displayMetrics.heightPixels;
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
    
    private void setLockScreenBitmap(Bitmap bitmap) throws IOException {
        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK);
    }
    
    public void constructBitmap(BackgroundSetterService backgroundSetterService) {
        if (!busy) {
            busy = true;
            new Thread(() -> {
                try {
                    Bitmap bitmap = getBitmapFromLockScreen();
                    File bg = getBackgroundAccordingToDayAndTime();
                    
                    if (noFingerPrint(bitmap)) {
                        bitmap = fingerPrintAndSaveBitmap(bitmap, bg, displayMetrics);
                    } else {
                        if (bg.exists()) {
                            bitmap.recycle();
                            bitmap = readStream(new FileInputStream(bg));
                        } else {
                            File defFile = new File(rootDir, defaultBackgroundName);
                            if (defFile.exists()) {
                                bitmap.recycle();
                                bitmap = readStream(new FileInputStream(defFile));
                            } else {
                                throw new FileNotFoundException("No available background to load");
                            }
                        }
                    }
                    
                    float time = System.nanoTime();
                    log(INFO, "setting wallpaper");
                    drawStringsOnBitmap(backgroundSetterService, bitmap);
                    setLockScreenBitmap(bitmap);
                    log(INFO, "set wallpaper in " + (System.nanoTime() - time) / 1000000000f + "s");
                    
                    bitmap.recycle();
                } catch (InterruptedException e) {
                    log(INFO, e.getMessage());
                } catch (Exception e) {
                    logException(e);
                }
                busy = false;
            }).start();
        }
    }
    
    private void drawStringsOnBitmap(BackgroundSetterService backgroundSetterService, Bitmap src) throws InterruptedException {
        
        Canvas canvas = new Canvas(src);
        
        ArrayList<TodoListEntry> toAdd = filterItems(loadTodoEntries(backgroundSetterService));
        int currentHash = toAdd.hashCode() + preferences.getAll().hashCode() + hashBitmap(src);
        if (previous_hash == currentHash) {
            throw new InterruptedException("No need to update the bitmap, list is the same, bailing out");
        }
        previous_hash = currentHash;
        
        if (!toAdd.isEmpty()) {
            
            for (int i = 0; i < toAdd.size(); i++) {
                toAdd.get(i).loadDisplayData(backgroundSetterService.lockScreenBitmapDrawer);
                toAdd.get(i).splitText(backgroundSetterService);
            }
            
            float totalHeight = 0;
            for (int i = 0; i < toAdd.size(); i++) {
                totalHeight += toAdd.get(i).h * toAdd.get(i).textValueSplit.length + toAdd.get(i).kM;
            }
            totalHeight -= toAdd.get(0).kM;
            totalHeight /= 2f;
            
            ArrayList<ArrayList<TodoListEntry>> splitEntries = new ArrayList<>();
            ArrayList<TodoListEntry> toAddSplit = new ArrayList<>();
            for (int i = toAdd.size() - 1; i >= 0; i--) {
                ArrayList<TodoListEntry> splits = new ArrayList<>();
                for (int i2 = toAdd.get(i).textValueSplit.length - 1; i2 >= 0; i2--) {
                    if (preferences.getBoolean(ITEM_FULL_WIDTH_LOCK, SETTINGS_DEFAULT_ITEM_FULL_WIDTH_LOCK)) {
                        toAdd.get(i).rWidth = displayWidth / 2f - toAdd.get(i).bevelThickness;
                    }
                    TodoListEntry splitEntry;
                    if (toAdd.get(i).fromSystemCalendar) {
                        splitEntry = new TodoListEntry(toAdd.get(i).event);
                    } else {
                        splitEntry = new TodoListEntry(toAdd.get(i).params, toAdd.get(i).group.name);
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
                if (toAddSplit.get(i).adaptiveColorEnabled && !toAddSplit.get(i).textValue.equals(blankTextValue)) {
                    int width = (int) (toAddSplit.get(i).rWidth * 2);
                    int height = (int) (toAddSplit.get(i).h / 2f + toAddSplit.get(i).kM);
                    int VOffset = (int) (displayCenter.y + totalHeight - toAddSplit.get(i).kM * (i + 1));
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
                if (!toAddSplit.get(i).textValue.equals(blankTextValue)) {
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
        to.h = from.h;
        to.kM = from.kM;
        to.maxChars = from.maxChars;
        to.rWidth = from.rWidth;
    }
    
    private void drawTextListOnCanvas(ArrayList<TodoListEntry> toAdd, Canvas canvas, float maxHeight) {
        for (int i = 0; i < toAdd.size(); i++) {
            if (!toAdd.get(i).textValue.equals(blankTextValue)) {
                canvas.drawText(toAdd.get(i).textValue, displayCenter.x, displayCenter.y + maxHeight - toAdd.get(i).kM * i, toAdd.get(i).textPaint);
            }
        }
    }
    
    private void drawBgOnCanvas(ArrayList<TodoListEntry> toAdd, Canvas canvas, float maxHeight) {
        for (int i = 0; i < toAdd.size(); i++) {
            if (!toAdd.get(i).textValue.equals(blankTextValue)) {
                
                drawRectRelativeToTheCenter(canvas, toAdd.get(i).padPaint, maxHeight,
                        -toAdd.get(i).rWidth - toAdd.get(i).bevelThickness,
                        toAdd.get(i).h / 2f - toAdd.get(i).kM * i,
                        toAdd.get(i).rWidth + toAdd.get(i).bevelThickness,
                        -toAdd.get(i).kM * (i + 1) - toAdd.get(i).bevelThickness);
                
                drawRectRelativeToTheCenter(canvas, toAdd.get(i).bgPaint, maxHeight,
                        -toAdd.get(i).rWidth,
                        toAdd.get(i).h / 2f - toAdd.get(i).kM * i,
                        toAdd.get(i).rWidth,
                        -toAdd.get(i).kM * (i + 1));
            }
        }
    }
    
    private void drawRectRelativeToTheCenter(Canvas canvas, Paint paint, float maxHeight, float left, float top, float right, float bottom) {
        
        canvas.drawRect(displayCenter.x + left,
                displayCenter.y + maxHeight + top,
                displayCenter.x + right,
                displayCenter.y + maxHeight + bottom, paint);
    }
    
    private ArrayList<TodoListEntry> filterItems(ArrayList<TodoListEntry> input) {
        ArrayList<TodoListEntry> toAdd = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            if (input.get(i).getLockViewState()) {
                toAdd.add(input.get(i));
                if (input.get(i).textValue.length() > currentBitmapLongestText.length()) {
                    currentBitmapLongestText = input.get(i).textValue;
                }
            }
        }
        return toAdd;
    }
}
