package prototype.xd.scheduler.utilities;

import static java.lang.Math.max;
import static prototype.xd.scheduler.utilities.Keys.DEFAULT_TIME_OFFSET_COLOR_MIX_FACTOR;
import static prototype.xd.scheduler.utilities.Keys.DISPLAY_METRICS_HEIGHT;
import static prototype.xd.scheduler.utilities.Keys.DISPLAY_METRICS_WIDTH;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.slider.Slider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class GraphicsUtilities {
    
    
    public static Bitmap fingerPrintAndSaveBitmap(Bitmap bitmap, File output) throws IOException {
        bitmap = makeMutable(bitmap);
        Bitmap cutBitmap = createScaledBitmap(bitmap,
                DISPLAY_METRICS_WIDTH.get(),
                DISPLAY_METRICS_HEIGHT.get(),
                ScalingLogic.CROP);
        fingerPrintBitmap(cutBitmap);
        
        FileOutputStream outputStream = new FileOutputStream(output);
        cutBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        outputStream.close();
        
        Bitmap resizedBitmap = createScaledBitmap(cutBitmap, (int) (cutBitmap.getWidth() / 4f), (int) (cutBitmap.getHeight() / 4f), GraphicsUtilities.ScalingLogic.FIT);
        
        FileOutputStream outputStreamMin = new FileOutputStream(output.getAbsolutePath() + "_min.png");
        resizedBitmap.compress(Bitmap.CompressFormat.PNG, 50, outputStreamMin);
        outputStreamMin.close();
        
        return cutBitmap;
    }
    
    public static Bitmap readStream(FileInputStream inputStream) throws IOException {
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        inputStream.close();
        return bitmap;
    }
    
    public static Bitmap makeMutable(Bitmap bitmap) {
        if (!bitmap.isMutable())
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true); // make bitmap mutable if not already
        return bitmap;
    }
    
    /**
     * Utility function for creating a scaled version of an existing bitmap
     *
     * @param unscaledBitmap Bitmap to scale
     * @param dstWidth       Wanted width of destination bitmap
     * @param dstHeight      Wanted height of destination bitmap
     * @param scalingLogic   Logic to use to avoid image stretching
     * @return New scaled bitmap object
     */
    public static Bitmap createScaledBitmap(Bitmap unscaledBitmap, int dstWidth, int dstHeight, ScalingLogic scalingLogic) {
        
        Rect srcRect = calculateSrcRect(unscaledBitmap.getWidth(), unscaledBitmap.getHeight(), dstWidth, dstHeight, scalingLogic);
        Rect dstRect = calculateDstRect(unscaledBitmap.getWidth(), unscaledBitmap.getHeight(), dstWidth, dstHeight, scalingLogic);
        Bitmap scaledBitmap = Bitmap.createBitmap(dstRect.width(), dstRect.height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(scaledBitmap);
        canvas.drawBitmap(unscaledBitmap, srcRect, dstRect, new Paint(Paint.FILTER_BITMAP_FLAG));
        
        return scaledBitmap;
    }
    
    /**
     * ScalingLogic defines how scaling should be carried out if source and
     * destination image has different aspect ratio.
     * <p>
     * CROP: Scales the image the minimum amount while making sure that at least
     * one of the two dimensions fit inside the requested destination area.
     * Parts of the source image will be cropped to realize this.
     * <p>
     * FIT: Scales the image the minimum amount while making sure both
     * dimensions fit inside the requested destination area. The resulting
     * destination dimensions might be adjusted to a smaller size than
     * requested.
     */
    public enum ScalingLogic {
        CROP, FIT
    }
    
    /**
     * Calculates source rectangle for scaling bitmap
     *
     * @param srcWidth     Width of source image
     * @param srcHeight    Height of source image
     * @param dstWidth     Width of destination area
     * @param dstHeight    Height of destination area
     * @param scalingLogic Logic to use to avoid image stretching
     * @return Optimal source rectangle
     */
    public static Rect calculateSrcRect(int srcWidth, int srcHeight, int dstWidth, int dstHeight,
                                        ScalingLogic scalingLogic) {
        if (scalingLogic == ScalingLogic.CROP) {
            final float srcAspect = (float) srcWidth / (float) srcHeight;
            final float dstAspect = (float) dstWidth / (float) dstHeight;
            
            if (srcAspect > dstAspect) {
                final int srcRectWidth = (int) (srcHeight * dstAspect);
                final int srcRectLeft = (srcWidth - srcRectWidth) / 2;
                return new Rect(srcRectLeft, 0, srcRectLeft + srcRectWidth, srcHeight);
            } else {
                final int srcRectHeight = (int) (srcWidth / dstAspect);
                final int scrRectTop = (srcHeight - srcRectHeight) / 2;
                return new Rect(0, scrRectTop, srcWidth, scrRectTop + srcRectHeight);
            }
        } else {
            return new Rect(0, 0, srcWidth, srcHeight);
        }
    }
    
    /**
     * Calculates destination rectangle for scaling bitmap
     *
     * @param srcWidth     Width of source image
     * @param srcHeight    Height of source image
     * @param dstWidth     Width of destination area
     * @param dstHeight    Height of destination area
     * @param scalingLogic Logic to use to avoid image stretching
     * @return Optimal destination rectangle
     */
    public static Rect calculateDstRect(int srcWidth, int srcHeight, int dstWidth, int dstHeight,
                                        ScalingLogic scalingLogic) {
        if (scalingLogic == ScalingLogic.FIT) {
            final float srcAspect = (float) srcWidth / (float) srcHeight;
            final float dstAspect = (float) dstWidth / (float) dstHeight;
            
            if (srcAspect > dstAspect) {
                return new Rect(0, 0, dstWidth, (int) (dstWidth / srcAspect));
            } else {
                return new Rect(0, 0, (int) (dstHeight * srcAspect), dstHeight);
            }
        } else {
            return new Rect(0, 0, dstWidth, dstHeight);
        }
    }
    
    public static int swapRedAndGreenChannels(int color) {
        Color col = Color.valueOf(color);
        return Color.rgb(col.green(), col.red(), col.blue());
    }
    
    public static int getOnBgColor(int surfaceColor) {
        // get a color that will look good on the specified surfaceColor
        return mixTwoColors(getIntensityColor(surfaceColor), surfaceColor, 0.3);
    }
    
    // return black or white based on the background color
    public static int getIntensityColor(int bgColor) {
        Color col = Color.valueOf(bgColor);
        float intensity = col.red() * 0.299f + col.green() * 0.587f + col.blue() * 0.114f;
        if (intensity > 0.729411f) {
            return Color.BLACK;
        }
        return Color.WHITE;
    }
    
    public static int mixTwoColors(int color1, int color2, double balance) {
        Color c1 = Color.valueOf(color1);
        Color c2 = Color.valueOf(color2);
        float a = (float) (c1.alpha() * (1 - balance) + c2.alpha() * balance);
        float r = (float) (c1.red() * (1 - balance) + c2.red() * balance);
        float g = (float) (c1.green() * (1 - balance) + c2.green() * balance);
        float b = (float) (c1.blue() * (1 - balance) + c2.blue() * balance);
        return Color.argb(a, r, g, b);
    }
    
    // get harmonized color with the background
    public static int getHarmonizedFontColorWithBg(int color, int backgroundColor) {
        // harmonize with extrapolated primary color
        return MaterialColors.harmonize(color, getOnBgColor(backgroundColor));
    }
    
    // mix and harmonize (25% background color, 75% font color + harmonized with background);
    public static int getHarmonizedSecondaryFontColorWithBg(int color, int backgroundColor) {
        return getHarmonizedFontColorWithBg(mixTwoColors(color, backgroundColor, Keys.DEFAULT_CALENDAR_EVENT_TIME_COLOR_MIX_FACTOR), backgroundColor);
    }
    
    // mix color with bg color based on balance (from 1 to 10)
    public static int mixColorWithBg(int inputColor, int backgroundColor, int balance) {
        if(balance <= 0) {
            return inputColor;
        }
        return mixTwoColors(MaterialColors.harmonize(inputColor, backgroundColor),
                backgroundColor, (balance - 1) / 9d);
    }
    
    public static int getExpiredUpcomingColor(int baseColor, int tintColor) {
        return mixTwoColors(baseColor, tintColor, DEFAULT_TIME_OFFSET_COLOR_MIX_FACTOR);
    }
    
    public static int getAverageColor(int[] pixels) {
        
        int redBucket = 0;
        int greenBucket = 0;
        int blueBucket = 0;
        
        int nonBlackPixelCount = 0;
        
        for (int color : pixels) {
            int red = (color >> 16) & 0xFF;
            int green = (color >> 8) & 0xFF;
            int blue = color & 0xFF;
            if (red + green + blue != 0) {
                redBucket += red;
                greenBucket += green;
                blueBucket += blue;
                nonBlackPixelCount++;
            }
        }
        
        if (nonBlackPixelCount == 0) {
            return Color.BLACK;
        }
        
        if (max(max(redBucket, greenBucket), blueBucket) / nonBlackPixelCount < 200) {
            redBucket += 50 * nonBlackPixelCount;
            greenBucket += 50 * nonBlackPixelCount;
            blueBucket += 50 * nonBlackPixelCount;
        }
        
        return Color.argb(
                255,
                redBucket / nonBlackPixelCount,
                greenBucket / nonBlackPixelCount,
                blueBucket / nonBlackPixelCount);
    }
    
    public static class SliderTinter {
        
        ColorStateList sliderPrimaryColor;
        ColorStateList sliderOnSurfaceColor;
        ColorStateList sliderHaloColor;
        
        public SliderTinter(@NonNull Context context, @ColorInt int sliderAccentColor) {
            sliderAccentColor = MaterialColors.harmonizeWithPrimary(context, sliderAccentColor);
            
            sliderPrimaryColor = ColorStateList.valueOf(sliderAccentColor);
            sliderOnSurfaceColor = ColorStateList.valueOf(getOnBgColor(sliderAccentColor));
            sliderHaloColor = ColorStateList.valueOf(mixTwoColors(sliderAccentColor, Color.TRANSPARENT, 0.5));
        }
        
        public void tintSlider(Slider slider) {
            slider.setThumbTintList(sliderPrimaryColor);
            slider.setHaloTintList(sliderHaloColor);
            slider.setTrackActiveTintList(sliderPrimaryColor);
            slider.setTickActiveTintList(sliderOnSurfaceColor);
            slider.setTickInactiveTintList(sliderPrimaryColor);
        }
        
    }
    
    public static void fingerPrintBitmap(Bitmap bitmap) {
        bitmap.setPixel(0, 0, Color.DKGRAY);
        bitmap.setPixel(1, 0, Color.GREEN);
        bitmap.setPixel(0, 1, Color.YELLOW);
    }
    
    public static boolean noFingerPrint(Bitmap bitmap) {
        int pixel1 = bitmap.getPixel(0, 0);
        int pixel2 = bitmap.getPixel(1, 0);
        int pixel3 = bitmap.getPixel(0, 1);
        
        return !((pixel1 == Color.DKGRAY)
                && (pixel2 == Color.GREEN)
                && (pixel3 == Color.YELLOW));
    }
    
    public static int hashBitmap(Bitmap bitmap) {
        int[] buffer = new int[bitmap.getWidth() * bitmap.getHeight() / 16];
        bitmap.getPixels(buffer, 0, bitmap.getWidth() / 4, 0, 0, bitmap.getWidth() / 4, bitmap.getHeight() / 4);
        return Arrays.hashCode(buffer);
    }
    
}
