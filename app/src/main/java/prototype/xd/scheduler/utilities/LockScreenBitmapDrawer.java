package prototype.xd.scheduler.utilities;

import static prototype.xd.scheduler.MainActivity.displayMetrics;
import static prototype.xd.scheduler.MainActivity.preferences;
import static prototype.xd.scheduler.entities.TodoListEntry.blankTextValue;
import static prototype.xd.scheduler.utilities.BackgroundChooser.defaultBackgroundName;
import static prototype.xd.scheduler.utilities.BackgroundChooser.getBackgroundAccordingToDayAndTime;
import static prototype.xd.scheduler.utilities.BitmapUtilities.fingerPrintBitmap;
import static prototype.xd.scheduler.utilities.BitmapUtilities.getAverageColor;
import static prototype.xd.scheduler.utilities.BitmapUtilities.noFingerPrint;
import static prototype.xd.scheduler.utilities.DateManager.availableDays;
import static prototype.xd.scheduler.utilities.Keys.*;
import static prototype.xd.scheduler.utilities.Logger.ContentType.INFO;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.Utilities.loadTodoEntries;
import static prototype.xd.scheduler.utilities.Utilities.rootDir;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import prototype.xd.scheduler.entities.TodoListEntry;

public class LockScreenBitmapDrawer {
    
    public static String currentBitmapLongestText = "";
    
    public static int displayWidth;
    public static int displayHeight;
    private Point displayCenter;
    
    private boolean initialised = false;
    private boolean forceMaxRWidth = false;
    public boolean currentlyProcessingBitmap = false;
    public boolean needBitmapProcessing = false;
    
    WallpaperManager wallpaperManager;
    
    private Context context;
    
    Bitmap cachedBitmapFromLockScreen;
    
    public LockScreenBitmapDrawer(Context context) {
        initialiseBitmapDrawer(context);
    }
    
    @SuppressLint("MissingPermission")
    public void initialiseBitmapDrawer(Context context) {
        wallpaperManager = WallpaperManager.getInstance(context);
        ParcelFileDescriptor wallpaperFile = wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_LOCK);
        if(wallpaperFile == null){
            wallpaperFile = wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_SYSTEM);
        }
        cachedBitmapFromLockScreen = BitmapFactory.decodeFileDescriptor(wallpaperFile.getFileDescriptor()).copy(Bitmap.Config.ARGB_8888, true);
        if (!initialised) {
            displayWidth = displayMetrics.widthPixels;
            displayHeight = displayMetrics.heightPixels;
            displayCenter = new Point(displayWidth / 2, displayHeight / 2);
            initialised = true;
        }
        this.context = context;
    }
    
    private void setLockScreenBitmap(Bitmap bitmap) throws IOException {
        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK);
        cachedBitmapFromLockScreen.recycle();
        cachedBitmapFromLockScreen = bitmap.copy(Bitmap.Config.ARGB_8888, true);
    }
    
    public void checkQueue() {
        if (needBitmapProcessing && !currentlyProcessingBitmap) {
            constructBitmap();
            needBitmapProcessing = false;
        }
    }
    
    public void constructBitmap() {
        if (!currentlyProcessingBitmap) {
            if (!initialised) {
                throw new RuntimeException("Bitmap drawer not initialised");
            }
            currentlyProcessingBitmap = true;
            forceMaxRWidth = preferences.getBoolean(ITEM_FULL_WIDTH_LOCK, SETTINGS_DEFAULT_ITEM_FULL_WIDTH_LOCK);
            
            final File bg = getBackgroundAccordingToDayAndTime();
            final String[] currentName = {bg.getName()};
            
            if (noFingerPrint(cachedBitmapFromLockScreen) && !currentName[0].equals(defaultBackgroundName)) {
                final AlertDialog.Builder alert = new AlertDialog.Builder(context);
                
                alert.setTitle("В какой день использовать новый фон?");
                
                alert.setItems(availableDays, (dialog, which) -> {
                    currentName[0] = availableDays[which] + ".png";
                    if (availableDays[which].equals("общий")) {
                        currentName[0] = defaultBackgroundName;
                    }
                    dialog.dismiss();
                });
                
                alert.setOnDismissListener(dialog -> startBitmapThread(currentName[0]));
                
                alert.show();
            } else {
                startBitmapThread(currentName[0]);
            }
        } else {
            needBitmapProcessing = true;
        }
    }
    
    private void startBitmapThread(final String selectedDay) {
        new Thread(() -> {
            try {
                Bitmap bitmap;
                File bg = getBackgroundAccordingToDayAndTime();
                Bitmap bitmapFromLock = cachedBitmapFromLockScreen;
                
                if (noFingerPrint(bitmapFromLock)) {
                    
                    bitmapFromLock = Bitmap.createBitmap(bitmapFromLock, (int) (bitmapFromLock.getWidth() / 2f - displayWidth / 2f), 0, displayWidth, bitmapFromLock.getHeight());
                    fingerPrintBitmap(bitmapFromLock).compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(new File(rootDir, selectedDay)));
                    
                    bitmap = BitmapFactory.decodeStream(new FileInputStream(new File(rootDir, selectedDay))).copy(Bitmap.Config.ARGB_8888, true);
                    
                    if (!selectedDay.equals(bg.getName())) {
                        cachedBitmapFromLockScreen.recycle();
                        cachedBitmapFromLockScreen = bitmap;
                        startBitmapThread(null);
                        throw new InterruptedException("New background set to other date, constructing with current instead");
                    }
                } else {
                    if (bg.exists()) {
                        bitmap = BitmapFactory.decodeStream(new FileInputStream(bg))
                                .copy(Bitmap.Config.ARGB_8888, true);
                    } else {
                        File defFile = new File(rootDir, defaultBackgroundName);
                        if (defFile.exists()) {
                            bitmap = BitmapFactory.decodeStream(new FileInputStream(defFile))
                                    .copy(Bitmap.Config.ARGB_8888, true);
                        } else {
                            throw new FileNotFoundException("No available background to load");
                        }
                    }
                }
                
                float time = System.nanoTime();
                log(INFO, "setting wallpaper");
                drawStringsOnBitmap(bitmap);
                setLockScreenBitmap(bitmap);
                log(INFO, "set wallpaper in " + (System.nanoTime() - time) / 1000000000f + "s");
                
                bitmap.recycle();
            } catch (Exception e) {
                logException(e);
            }
            currentlyProcessingBitmap = false;
        }).start();
    }
    
    private void drawStringsOnBitmap(Bitmap src) {
        
        Canvas canvas = new Canvas(src);
        
        ArrayList<TodoListEntry> toAdd = filterItems(loadTodoEntries(context));
        if (!toAdd.isEmpty()) {
            
            for (int i = 0; i < toAdd.size(); i++) {
                toAdd.get(i).initialiseDisplayData();
                toAdd.get(i).splitText();
            }
            
            float totalHeight = 0;
            for (int i = 0; i < toAdd.size(); i++) {
                totalHeight += toAdd.get(i).h * toAdd.get(i).textValueSplit.length + toAdd.get(i).kM;
            }
            totalHeight += toAdd.get(0).kM * 2;
            totalHeight /= 2f;
            
            ArrayList<ArrayList<TodoListEntry>> splitEntries = new ArrayList<>();
            ArrayList<TodoListEntry> toAddSplit = new ArrayList<>();
            for (int i = toAdd.size() - 1; i >= 0; i--) {
                ArrayList<TodoListEntry> splits = new ArrayList<>();
                for (int i2 = toAdd.get(i).textValueSplit.length - 1; i2 >= 0; i2--) {
                    if (forceMaxRWidth) {
                        toAdd.get(i).rWidth = displayWidth / 2f - toAdd.get(i).bevelThickness;
                    }
                    TodoListEntry splitEntry = new TodoListEntry(toAdd.get(i).params, toAdd.get(i).group.name);
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
    
    private static ArrayList<TodoListEntry> filterItems(ArrayList<TodoListEntry> input) {
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
