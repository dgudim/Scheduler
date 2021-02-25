package prototype.xd.scheduler.utilities;

import android.annotation.SuppressLint;
import android.app.WallpaperManager;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import androidx.core.math.MathUtils;

import org.apache.commons.lang.WordUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import static prototype.xd.scheduler.utilities.DateManager.*;
import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.ScalingUtilities.createNewPaint;
import static prototype.xd.scheduler.utilities.Utilities.loadEntries;

public class LockScreenBitmapDrawer {

    private static String currentBitmapLongestText = "";
    private static int globalI;

    private static Paint textPaint, whitePaint, redPaint, grayPaint, grayRedPaint, greenPaint, grayGreenPaint;
    public static int displayWidth;
    public static int displayHeight;
    private static int maxChars;
    private static Point displayCenter;
    public static float h;
    private static float kM;
    private static int pad, pad_yesterday, pad_global;

    static boolean showYesterdayCompletedItemsOnLock;
    public static boolean showYesterdayCompletedItemsInList;
    public static boolean showYesterdayItemsInList;
    static boolean showYesterdayItemsOnLock;
    static boolean showTodayCompletedTasksOnLock;
    public static boolean showGlobalItemsOnLock;

    private static boolean initialised = false;

    static WallpaperManager wallpaperManager_static;
    static DisplayMetrics displayMetrics_static;
    static SharedPreferences preferences_static;

    public static void initialiseBitmapDrawer(WallpaperManager wallpaperManager, final SharedPreferences preferences, DisplayMetrics displayMetrics) {
        wallpaperManager_static = wallpaperManager;
        displayMetrics_static = displayMetrics;
        preferences_static = preferences;
        if (!initialised) {
            pad = preferences.getInt("defaultBevelThickness", 5);
            pad_yesterday = preferences.getInt("yesterdayBevelThickness", 5);
            pad_global = preferences.getInt("globalBevelThickness", 5);

            int defaultBgColor = preferences.getInt("todayBgColor", 0xFFFFFFFF);
            int yesterdayBgColor = preferences.getInt("yesterdayBgColor", 0xFFFFCCCC);
            int defaultPadColor = preferences.getInt("todayBevelColor", 0xFF888888);
            int yesterdayPadColor = preferences.getInt("yesterdayBevelColor", 0xFFFF8888);
            int globalBgColor = preferences.getInt("globalBgColor", 0xFFCCFFCC);
            int globalPadColor = preferences.getInt("globalBevelColor", 0xFF88FF88);

            showYesterdayCompletedItemsOnLock = preferences.getBoolean("yesterdayItemsLock", false);
            showYesterdayCompletedItemsInList = preferences.getBoolean("yesterdayItemsList", false);
            showYesterdayItemsInList = preferences.getBoolean("yesterdayTasks", true);
            showYesterdayItemsOnLock = preferences.getBoolean("yesterdayTasksLock", true);

            showTodayCompletedTasksOnLock = preferences.getBoolean("completedTasks", false);
            showGlobalItemsOnLock = preferences.getBoolean("globalTasksLock", true);

            whitePaint = createNewPaint(defaultBgColor);
            redPaint = createNewPaint(yesterdayBgColor);
            grayPaint = createNewPaint(defaultPadColor);
            grayRedPaint = createNewPaint(yesterdayPadColor);
            greenPaint = createNewPaint(globalBgColor);
            grayGreenPaint = createNewPaint(globalPadColor);

            displayWidth = displayMetrics.widthPixels;
            displayHeight = displayMetrics.heightPixels;
            displayCenter = new Point(displayWidth / 2, displayHeight / 2);

            int fontSize = preferences.getInt("fontSize", 21);

            h = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, fontSize, displayMetrics);
            textPaint = createNewPaint(Color.BLACK);
            textPaint.setTextSize(h);
            textPaint.setTextAlign(Paint.Align.CENTER);
            kM = h * 1.1f;
            maxChars = (int) ((displayWidth - Math.max(pad, pad_yesterday) * 2) / (textPaint.measureText("qwerty_") / 5f)) - 2;

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

        ArrayList<String> toAdd = filterOnlyNotDoneItems(loadEntries(currentDate), showTodayCompletedTasksOnLock);
        ArrayList<String> toAdd_yesterday = filterOnlyNotDoneItems(loadEntries(yesterdayDate), showYesterdayCompletedItemsOnLock);
        ArrayList<String> toAdd_global = filterOnlyNotDoneItems(loadEntries("list_global"), true);

        float rWidth = MathUtils.clamp(textPaint.measureText(currentBitmapLongestText) / 2 + 10, 1, displayWidth / 2f - pad);
        float rWidth_yesterday = MathUtils.clamp(textPaint.measureText(currentBitmapLongestText) / 2 + 10, 1, displayWidth / 2f - pad_yesterday);
        float rWidth_global = MathUtils.clamp(textPaint.measureText(currentBitmapLongestText) / 2 + 10, 1, displayWidth / 2f - pad_global);

        int size = toAdd.size();
        if (showYesterdayItemsOnLock) {
            size += toAdd_yesterday.size();
        }
        if (showGlobalItemsOnLock) {
            size += toAdd_global.size();
        }

        float maxHeight = (size * kM) / 2;

        globalI = 0;
        drawBgOnCanvas(toAdd, canvas, maxHeight, rWidth, pad, 0);
        if (showYesterdayItemsOnLock) {
            drawBgOnCanvas(toAdd_yesterday, canvas, maxHeight, rWidth_yesterday, pad_yesterday, 1);
        }
        if (showGlobalItemsOnLock) {
            drawBgOnCanvas(toAdd_global, canvas, maxHeight, rWidth_global, pad_global, 2);
        }

        globalI = 0;
        drawTextListOnCanvas(toAdd, canvas, maxHeight);
        if (showYesterdayItemsOnLock) {
            drawTextListOnCanvas(toAdd_yesterday, canvas, maxHeight);
        }
        if (showGlobalItemsOnLock) {
            drawTextListOnCanvas(toAdd_global, canvas, maxHeight);
        }
    }

    private static void drawTextListOnCanvas(ArrayList<String> toAdd, Canvas canvas, float maxHeight) {
        for (int i = toAdd.size() - 1; i >= 0; i--) {
            if (!toAdd.get(i).equals(" ")) {
                canvas.drawText(toAdd.get(i), displayCenter.x, displayCenter.y + maxHeight - kM * globalI, textPaint);
            }
            globalI++;
        }
    }

    private static void drawBgOnCanvas(ArrayList<String> toAdd, Canvas canvas, float maxHeight, float rWidth, int pad, int colorModifier) {
        Paint gPaint = grayPaint;
        Paint wPaint = whitePaint;
        switch (colorModifier) {
            case (1):
                gPaint = grayRedPaint;
                wPaint = redPaint;
                break;
            case (2):
                gPaint = grayGreenPaint;
                wPaint = greenPaint;
                break;
        }
        for (int i = toAdd.size() - 1; i >= 0; i--) {
            if (!toAdd.get(i).equals(" ")) {

                canvas.drawRect(displayCenter.x + rWidth + pad,
                        displayCenter.y + maxHeight + h / 2f - kM * globalI,
                        displayCenter.x - rWidth - pad,
                        displayCenter.y + maxHeight - kM * (globalI + 1) - pad, gPaint);

                canvas.drawRect(displayCenter.x + rWidth,
                        displayCenter.y + maxHeight + h / 2f - kM * globalI,
                        displayCenter.x - rWidth,
                        displayCenter.y + maxHeight - kM * (globalI + 1), wPaint);
            }
            globalI++;
        }
    }

    private static ArrayList<String> filterOnlyNotDoneItems(ArrayList<String> input, boolean skipFilter) {
        ArrayList<String> toAdd = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            if (input.get(i).endsWith("_0") || skipFilter) {
                String addition = "";
                if (input.get(i).endsWith("_1")) {
                    addition = " (Выполнено)";
                }
                String addS = input.get(i).substring(0, input.get(i).length() - 2);
                if (textPaint.measureText(addS + addition) > displayWidth) {
                    toAdd.addAll(Arrays.asList(makeNewLines(addS + addition)));
                } else {
                    toAdd.add(addS + addition);
                }
                toAdd.add(" ");
                if (addS.length() + addition.length() > currentBitmapLongestText.length()) {
                    currentBitmapLongestText = addS + addition;
                }
            }
        }
        return toAdd;
    }


    private static String[] makeNewLines(String input) {
        return WordUtils.wrap(input, maxChars, "\n", true).split("\n");
    }

}
