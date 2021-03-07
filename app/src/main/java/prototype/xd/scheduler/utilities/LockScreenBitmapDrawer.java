package prototype.xd.scheduler.utilities;

import android.annotation.SuppressLint;
import android.app.WallpaperManager;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.DisplayMetrics;

import androidx.core.math.MathUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import prototype.xd.scheduler.entities.TodoListEntry;

import static prototype.xd.scheduler.utilities.DateManager.*;
import static prototype.xd.scheduler.utilities.Logger.ERROR;
import static prototype.xd.scheduler.utilities.Logger.INFO;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.Utilities.loadEntries;
import static prototype.xd.scheduler.utilities.Utilities.loadObject;
import static prototype.xd.scheduler.utilities.Utilities.rootDir;
import static prototype.xd.scheduler.utilities.Utilities.saveObject;

public class LockScreenBitmapDrawer {

    public static String currentBitmapLongestText = "";

    public static int displayWidth;
    public static int displayHeight;
    private static Point displayCenter;

    private static boolean initialised = false;

    static WallpaperManager wallpaperManager_static;
    public static DisplayMetrics displayMetrics_static;
    public static SharedPreferences preferences_static;

    public static void initialiseBitmapDrawer(WallpaperManager wallpaperManager, final SharedPreferences preferences, DisplayMetrics displayMetrics) {
        wallpaperManager_static = wallpaperManager;
        displayMetrics_static = displayMetrics;
        preferences_static = preferences;
        if (!initialised) {
            displayWidth = displayMetrics.widthPixels;
            displayHeight = displayMetrics.heightPixels;
            displayCenter = new Point(displayWidth / 2, displayHeight / 2);
            initialised = true;
        }
    }

    @SuppressLint("MissingPermission")
    public static void constructBitmap() {
        if (!initialised) {
            throw new RuntimeException("Bitmap drawer not initialised");
        }
        new Thread(new Runnable() {
            public void run() {
                try {

                    Bitmap bitmap;
                    File bg = new File(Utilities.rootDir, "bg.jpg");

                    if (!bg.exists()) {
                        bitmap = BitmapFactory.decodeFileDescriptor(wallpaperManager_static.getWallpaperFile(WallpaperManager.FLAG_LOCK).getFileDescriptor())
                                .copy(Bitmap.Config.ARGB_8888, true);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(bg));
                    } else {
                        bitmap = BitmapFactory.decodeStream(new FileInputStream(bg))
                                .copy(Bitmap.Config.ARGB_8888, true);
                    }

                    drawStringsOnBitmap(bitmap);

                    wallpaperManager_static.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK);
                    log(INFO, "set wallpaper");

                    bitmap.recycle();
                } catch (Exception e) {
                    logException(e);
                }
            }
        }).start();
    }


    private static void drawStringsOnBitmap(Bitmap src) {

        Canvas canvas = new Canvas(src);
        canvas.drawBitmap(src, 0, 0, null);

        ArrayList<TodoListEntry> toAdd = filterItems(loadEntries());
        for (int i = 0; i < toAdd.size(); i++) {
            toAdd.get(i).initialiseDisplayData();
            toAdd.get(i).splitText();
        }

        float totalHeight = 0;
        for (int i = 0; i < toAdd.size(); i++) {
            totalHeight += toAdd.get(i).h * toAdd.get(i).textValueSplit.length;
            if (toAdd.get(i).textValueSplit.length == 1) {
                totalHeight += toAdd.get(i).kM;
            }
        }
        totalHeight /= 2f;

        ArrayList<TodoListEntry> toAddSplit = new ArrayList<>();
        for (int i = toAdd.size() - 1; i >= 0; i--) {
            for (int i2 = toAdd.get(i).textValueSplit.length - 1; i2 >= 0; i2--) {
                TodoListEntry splitEntry = new TodoListEntry(toAdd.get(i).params, toAdd.get(i).group.name);
                copyDisplayData(toAdd.get(i), splitEntry);
                splitEntry.changeParameter(TodoListEntry.TEXT_VALUE, toAdd.get(i).textValueSplit[i2]);
                toAddSplit.add(splitEntry);
            }
            TodoListEntry blankEntry = new TodoListEntry();
            blankEntry.textValue = "_BLANK_";
            blankEntry.h = 10;
            blankEntry.kM = 10 * 1.1f;
            toAddSplit.add(blankEntry);
        }

        drawBgOnCanvas(toAddSplit, canvas, totalHeight);
        drawTextListOnCanvas(toAddSplit, canvas, totalHeight);
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

    private static void drawTextListOnCanvas(ArrayList<TodoListEntry> toAdd, Canvas canvas, float maxHeight) {
        for (int i = 0; i < toAdd.size(); i++) {
            if (!toAdd.get(i).textValue.equals("_BLANK_")) {
                canvas.drawText(toAdd.get(i).textValue, displayCenter.x, displayCenter.y + maxHeight - toAdd.get(i).kM * i, toAdd.get(i).textPaint);
            }
        }
    }

    private static void drawBgOnCanvas(ArrayList<TodoListEntry> toAdd, Canvas canvas, float maxHeight) {
        for (int i = 0; i < toAdd.size(); i++) {
            if (!toAdd.get(i).textValue.equals("_BLANK_")) {
                drawRectRelativeToTheCenter(canvas, toAdd.get(i).padPaint, maxHeight,
                        -toAdd.get(i).rWidth - toAdd.get(i).padSize,
                        toAdd.get(i).h / 2f - toAdd.get(i).kM * i,
                        toAdd.get(i).rWidth + toAdd.get(i).padSize,
                        -toAdd.get(i).kM * (i + 1) - toAdd.get(i).padSize);

                drawRectRelativeToTheCenter(canvas, toAdd.get(i).bgPaint, maxHeight,
                        -toAdd.get(i).rWidth,
                        toAdd.get(i).h / 2f - toAdd.get(i).kM * i,
                        toAdd.get(i).rWidth,
                        -toAdd.get(i).kM * (i + 1));
            }
        }
    }

    private static void drawRectRelativeToTheCenter(Canvas canvas, Paint paint, float maxHeight, float left, float top, float right, float bottom) {
        canvas.drawRect(displayCenter.x + left,
                displayCenter.y + maxHeight + top,
                displayCenter.x + right,
                displayCenter.y + maxHeight + bottom, paint);
    }

    private static ArrayList<TodoListEntry> filterItems(ArrayList<TodoListEntry> input) {
        ArrayList<TodoListEntry> toAdd = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            if ((input.get(i).showOnLock && !input.get(i).completed) || input.get(i).showOnLock_ifCompleted) {
                toAdd.add(input.get(i));
                String addition = "";
                if (input.get(i).completed) {
                    addition = " (Выполнено)";
                }
                input.get(i).textValue += addition;
                if (input.get(i).textValue.length() + addition.length() > currentBitmapLongestText.length()) {
                    currentBitmapLongestText = input.get(i).textValue + addition;
                }
            }
        }
        return toAdd;
    }
}
