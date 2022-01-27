package prototype.xd.scheduler.utilities;

import static java.lang.Math.max;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.DisplayMetrics;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class BitmapUtilities {
    
    
    public static Bitmap fingerPrintAndSaveBitmap(Bitmap bitmap, File output, DisplayMetrics displayMetrics) throws FileNotFoundException {
        Bitmap cut_bitmap = Bitmap.createBitmap(bitmap,
                (int) (bitmap.getWidth() / 2f - displayMetrics.widthPixels / 2f),
                (int) (bitmap.getHeight() / 2f - displayMetrics.heightPixels / 2f),
                displayMetrics.widthPixels, displayMetrics.heightPixels);
        fingerPrintBitmap(cut_bitmap);
        cut_bitmap.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(output));
        
        Bitmap resizedBitmap = createScaledBitmap(cut_bitmap, (int) (cut_bitmap.getWidth() / 4f), (int) (cut_bitmap.getHeight() / 4f), BitmapUtilities.ScalingLogic.FIT);
        resizedBitmap.compress(Bitmap.CompressFormat.PNG, 50, new FileOutputStream(output.getAbsolutePath() + "_min.png"));
        resizedBitmap.recycle();
        bitmap.recycle();
        return cut_bitmap;
    }
    
    public static Bitmap readStream(FileInputStream inputStream){
        Bitmap orig = BitmapFactory.decodeStream(inputStream);
        Bitmap copy = orig.copy(Bitmap.Config.ARGB_8888, true);
        orig.recycle();
        return copy;
    }
    
    /**
     * Utility function for decoding an image resource. The decoded bitmap will
     * be optimized for further scaling to the requested destination dimensions
     * and scaling logic.
     *
     * @param res          The resources object containing the image data
     * @param resId        The resource id of the image data
     * @param dstWidth     Width of destination area
     * @param dstHeight    Height of destination area
     * @param scalingLogic Logic to use to avoid image stretching
     * @return Decoded bitmap
     */
    public static Bitmap decodeResource(Resources res, int resId, int dstWidth, int dstHeight,
                                        ScalingLogic scalingLogic) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);
        options.inJustDecodeBounds = false;
        options.inSampleSize = calculateSampleSize(options.outWidth, options.outHeight, dstWidth,
                dstHeight, scalingLogic);
        
        return BitmapFactory.decodeResource(res, resId, options);
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
     * Calculate optimal down-sampling factor given the dimensions of a source
     * image, the dimensions of a destination area and a scaling logic.
     *
     * @param srcWidth     Width of source image
     * @param srcHeight    Height of source image
     * @param dstWidth     Width of destination area
     * @param dstHeight    Height of destination area
     * @param scalingLogic Logic to use to avoid image stretching
     * @return Optimal down scaling sample size for decoding
     */
    public static int calculateSampleSize(int srcWidth, int srcHeight, int dstWidth, int dstHeight,
                                          ScalingLogic scalingLogic) {
        if (scalingLogic == ScalingLogic.FIT) {
            final float srcAspect = (float) srcWidth / (float) srcHeight;
            final float dstAspect = (float) dstWidth / (float) dstHeight;
            
            if (srcAspect > dstAspect) {
                return srcWidth / dstWidth;
            } else {
                return srcHeight / dstHeight;
            }
        } else {
            final float srcAspect = (float) srcWidth / (float) srcHeight;
            final float dstAspect = (float) dstWidth / (float) dstHeight;
            
            if (srcAspect > dstAspect) {
                return srcHeight / dstHeight;
            } else {
                return srcWidth / dstWidth;
            }
        }
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
    
    public static Paint createNewPaint(int color) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        paint.setColor(color);
        return paint;
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
    
    public static int getAverageColor(Bitmap bitmap) {
        if (bitmap == null) return Color.TRANSPARENT;
        
        int redBucket = 0;
        int greenBucket = 0;
        int blueBucket = 0;
        
        int pixelCount = bitmap.getWidth() * bitmap.getHeight();
        int[] pixels = new int[pixelCount];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        
        int actualPixelCount = 0;
        
        for (int y = 0, h = bitmap.getHeight(); y < h; y++) {
            for (int x = 0, w = bitmap.getWidth(); x < w; x++) {
                int color = pixels[x + y * w]; // x + y * width
                if (((color >> 16) & 0xFF + (color >> 8) & 0xFF + color & 0xFF) != 0) {
                    redBucket += (color >> 16) & 0xFF; // Color.red
                    greenBucket += (color >> 8) & 0xFF; // Color.greed
                    blueBucket += (color & 0xFF); // Color.blue
                    actualPixelCount++;
                }
            }
        }
        
        pixelCount = actualPixelCount;
        
        if (max(max(redBucket, greenBucket), blueBucket) / pixelCount < 200) {
            redBucket += 50 * pixelCount;
            greenBucket += 50 * pixelCount;
            blueBucket += 50 * pixelCount;
        }
        
        return Color.argb(
                255,
                redBucket / pixelCount,
                greenBucket / pixelCount,
                blueBucket / pixelCount);
    }
    
    public static void fingerPrintBitmap(Bitmap bitmap) {
        for (int i = 0; i < 3; i++) {
            for (int i2 = 0; i2 < bitmap.getWidth(); i2++) {
                bitmap.setPixel(i2, i, 0);//<<<-------------------------------------------------------------------------------------
            }                                                                                                                           //|
        }                                                                                                                               //|
    }                                                                                                                                   //|
    
    public static boolean noFingerPrint(Bitmap bitmap) {                                                                                //|
        for (int i = 0; i < 3; i++) {                                                                                                   //|
            for (int i2 = 0; i2 < bitmap.getWidth(); i2++) {                                                                            //|
                int pixel = bitmap.getPixel(i2, i);                                                                                     //|
                if (!(pixel == -16777216 || pixel == 0)) { // -16777216 is the decoded value 0 is just after setting the fingerprint here |
                    return true;
                }
            }
        }
        return false;
    }
    
}
