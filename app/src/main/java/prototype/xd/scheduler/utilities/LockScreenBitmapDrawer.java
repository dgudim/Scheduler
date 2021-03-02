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
import java.util.ArrayList;
import java.util.Arrays;

import prototype.xd.scheduler.entities.TodoListEntry;

import static prototype.xd.scheduler.utilities.DateManager.*;
import static prototype.xd.scheduler.utilities.Logger.INFO;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.Utilities.loadEntries;

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
            toAdd.get(i).reloadParams();
            // TODO: 26.02.2021 update only if needed
        }

        //float maxHeight = (toAdd.size() * kM) / 2;
        float maxHeight = 0;

        drawBgOnCanvas(toAdd, canvas, maxHeight);
        drawTextListOnCanvas(toAdd, canvas, maxHeight);
    }

    private static void drawTextListOnCanvas(ArrayList<TodoListEntry> toAdd, Canvas canvas, float maxHeight) {
        for (int i = toAdd.size() - 1; i >= 0; i--) {
            canvas.drawText(toAdd.get(i).textValue, displayCenter.x, displayCenter.y + maxHeight - toAdd.get(i).kM * i, toAdd.get(i).textPaint);
        }
    }

    private static void drawBgOnCanvas(ArrayList<TodoListEntry> toAdd, Canvas canvas, float maxHeight) {
        for (int i = toAdd.size() - 1; i >= 0; i--) {

            canvas.drawRect(displayCenter.x + toAdd.get(i).rWidth + toAdd.get(i).padSize,
                    displayCenter.y + maxHeight + toAdd.get(i).h / 2f - toAdd.get(i).kM * i,
                    displayCenter.x - toAdd.get(i).rWidth - toAdd.get(i).padSize,
                    displayCenter.y + maxHeight - toAdd.get(i).kM * (i + 1) - toAdd.get(i).padSize, toAdd.get(i).padPaint);

            canvas.drawRect(displayCenter.x + toAdd.get(i).rWidth,
                    displayCenter.y + maxHeight + toAdd.get(i).h / 2f - toAdd.get(i).kM * i,
                    displayCenter.x - toAdd.get(i).rWidth,
                    displayCenter.y + maxHeight - toAdd.get(i).kM * (i + 1), toAdd.get(i).bgPaint);
        }
    }

    private static ArrayList<TodoListEntry> filterItems(ArrayList<TodoListEntry> input) {
        ArrayList<TodoListEntry> toAdd = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            if ((input.get(i).showOnLock && !input.get(i).completed) || input.get(i).showOnLock_ifCompleted) {
                toAdd.add(input.get(i));
                String addition = "";
                if (input.get(i).completed) {
                    addition = " (Выполнено)";
                    input.get(i).splitText(" (Выполнено)");
                }
                if (input.get(i).textValue.length() + addition.length() > currentBitmapLongestText.length()) {
                    currentBitmapLongestText = input.get(i).textValue + addition;
                }
            }
        }
        return toAdd;
    }
}
