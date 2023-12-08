package prototype.xd.scheduler.utilities;

import static java.lang.Math.max;
import static prototype.xd.scheduler.utilities.Static.DEFAULT_TIME_OFFSET_COLOR_MIX_FACTOR;
import static prototype.xd.scheduler.utilities.Static.DISPLAY_METRICS_DENSITY;
import static prototype.xd.scheduler.utilities.Static.DISPLAY_METRICS_HEIGHT;
import static prototype.xd.scheduler.utilities.Static.DISPLAY_METRICS_WIDTH;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.HardwareRenderer;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.hardware.HardwareBuffer;
import android.media.ImageReader;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.HarmonizedColors;
import com.google.android.material.color.HarmonizedColorsOptions;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.slider.Slider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.function.IntUnaryOperator;

import prototype.xd.scheduler.R;

public final class ImageUtilities {
    
    public static final String NAME = Utilities.class.getSimpleName();
    
    private ImageUtilities() throws InstantiationException {
        throw new InstantiationException(NAME);
    }
    
    public static final class BitmapEffectsPipe {
        
        private final int blurRadiusPx;
        private final int noisePercent;
        private final int transparencyPercent;
        private final float transparencyVal;
        private final boolean glow;
        private final boolean highlightEdge;
        private final boolean active;
        
        public BitmapEffectsPipe() {
            
            transparencyPercent = Static.EFFECT_TRANSPARENCY.get();
            transparencyVal = (float) (1 - (transparencyPercent / 100.0));
            blurRadiusPx = Static.EFFECT_BLUR_RADIUS.get();
            noisePercent = Static.EFFECT_BLUR_GRAIN.get();
            glow = Static.EFFECT_GLOW.get();
            highlightEdge = Static.EFFECT_HIGHLIGHT_EDGE.get();
            
            if (blurRadiusPx == 0 && (transparencyPercent == 0 || transparencyPercent == 100)) {
                active = false;
                Logger.warning(NAME, "Not initializing bitmap processing pipeline, conditions not met");
                return;
            }
            
            active = true;
            
            Logger.info(NAME, "Initialized bitmap processing pipeline");
        }
        
        public boolean isActive() {
            return active;
        }
        
        @RequiresApi(api = Build.VERSION_CODES.S)
        @NonNull
        private RenderEffect getEffects(final int mixinColor) {
            RenderEffect blurRenderEffect;
            RenderEffect tintRenderEffect = null;
            
            RenderEffect finalRenderEffect = null;
            
            if (transparencyPercent > 0 && transparencyPercent < 100) {
                Color col = Color.valueOf(mixinColor);
                tintRenderEffect = RenderEffect.createColorFilterEffect(
                        new PorterDuffColorFilter(
                                Color.argb(transparencyVal, col.red(), col.green(), col.blue()),
                                glow ? PorterDuff.Mode.ADD : PorterDuff.Mode.SRC_OVER)
                );
                finalRenderEffect = tintRenderEffect;
            }
            if (blurRadiusPx > 0) {
                blurRenderEffect = RenderEffect.createBlurEffect(
                        blurRadiusPx, blurRadiusPx,
                        Shader.TileMode.MIRROR
                );
                if (finalRenderEffect != null) {
                    if (highlightEdge) {
                        finalRenderEffect = RenderEffect.createChainEffect(blurRenderEffect, tintRenderEffect);
                    } else {
                        finalRenderEffect = RenderEffect.createChainEffect(tintRenderEffect, blurRenderEffect);
                    }
                } else {
                    finalRenderEffect = blurRenderEffect;
                }
            }
            
            return Objects.requireNonNull(finalRenderEffect);
        }
        
        private static Bitmap mutatePixels(@NonNull Bitmap source, @NonNull IntUnaryOperator mutator) {
            source = makeMutable(source);
            
            int width = source.getWidth();
            int height = source.getHeight();
            int[] pixels = new int[width * height];
            
            // get pixel array from source
            source.getPixels(pixels, 0, width, 0, 0, width, height);
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = mutator.applyAsInt(pixels[i]);
            }
            source.setPixels(pixels, 0, width, 0, 0, width, height);
            return source;
        }
        
        /**
         * @noinspection UnsecureRandomNumberGeneration, deprecation
         */
        @NonNull
        public Bitmap processBitmap(@NonNull Bitmap bitmap, final int mixinColor, @NonNull Context context) {
            
            if (!active) {
                return bitmap;
            }
            
            if (Build.VERSION.SDK_INT >= 31) {
                var imageReader = ImageReader.newInstance(
                        bitmap.getWidth(), bitmap.getHeight(),
                        PixelFormat.RGBA_8888, 1,
                        HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE | HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
                );
                
                var renderNode = new RenderNode("BlurEffect");
                var hardwareRenderer = new HardwareRenderer();
                
                hardwareRenderer.setSurface(imageReader.getSurface());
                hardwareRenderer.setContentRoot(renderNode);
                renderNode.setPosition(0, 0, imageReader.getWidth(), imageReader.getHeight());
                
                renderNode.setRenderEffect(getEffects(mixinColor));
                
                var renderCanvas = renderNode.beginRecording();
                renderCanvas.drawBitmap(bitmap, 0F, 0F, null);
                renderNode.endRecording();
                hardwareRenderer.createRenderRequest().setWaitForPresent(true).syncAndDraw();
                
                var image = imageReader.acquireNextImage();
                var hardwareBuffer = image.getHardwareBuffer();
                Bitmap resBitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, null);
                hardwareBuffer.close();
                image.close();
                
                imageReader.close();
                renderNode.discardDisplayList();
                hardwareRenderer.destroy();
                
                bitmap = resBitmap;
            } else {
                if (transparencyPercent > 0 && transparencyPercent < 100) {
                    bitmap = mutatePixels(bitmap, pixel -> mixTwoColors(pixel, mixinColor, transparencyVal));
                }
                
                RenderScript rs = RenderScript.create(context);  // NOSONAR
                // use this constructor for best performance, because it uses
                // USAGE_SHARED mode which reuses memory
                final Allocation input = Allocation.createFromBitmap(rs, bitmap); // NOSONAR
                final Allocation output = Allocation.createTyped(rs, input.getType()); // NOSONAR
                final ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs)); // NOSONAR
                
                script.setRadius(blurRadiusPx);// NOSONAR
                script.setInput(input); // NOSONAR
                script.forEach(output); // NOSONAR
                
                output.copyTo(bitmap); // NOSONAR
            }
            
            if (noisePercent > 0) {
                Random random = new Random();
                
                double noiseVal = noisePercent / 100.0;
                bitmap = mutatePixels(bitmap, pixel -> mixTwoColors(pixel, mixinColor, noiseVal * random.nextFloat()));
            }
            
            return bitmap;
        }
    }
    
    @NonNull
    public static Bitmap fingerPrintAndSaveBitmap(@NonNull Bitmap bitmap, @NonNull File output) throws IOException {
        
        bitmap = makeMutable(bitmap);
        Bitmap cutBitmap = createScaledBitmap(bitmap,
                DISPLAY_METRICS_WIDTH.get(),
                DISPLAY_METRICS_HEIGHT.get(),
                ScalingLogic.CROP);
        fingerPrintBitmap(cutBitmap);
        
        try (FileOutputStream outputStream = new FileOutputStream(output)) {
            cutBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        }
        
        Bitmap resizedBitmap = createScaledBitmap(cutBitmap, (int) (cutBitmap.getWidth() / 4F), (int) (cutBitmap.getHeight() / 4F), ImageUtilities.ScalingLogic.FIT);
        
        try (FileOutputStream outputStreamMin = new FileOutputStream(output.getAbsolutePath() + "_min.png")) {
            resizedBitmap.compress(Bitmap.CompressFormat.PNG, 50, outputStreamMin);
        }
        
        return cutBitmap;
    }
    
    
    @NonNull
    public static Bitmap readBitmapFromFile(@NonNull File file) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            return BitmapFactory.decodeStream(inputStream);
        }
    }
    
    @NonNull
    public static Bitmap makeMutable(@NonNull final Bitmap bitmap) {
        if (!bitmap.isMutable()) {
            // make bitmap mutable if not already
            return bitmap.copy(Bitmap.Config.ARGB_8888, true);
        }
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
    @NonNull
    public static Bitmap createScaledBitmap(@NonNull Bitmap unscaledBitmap, int dstWidth, int dstHeight, @NonNull ScalingLogic scalingLogic) {
        
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
    @NonNull
    public static Rect calculateSrcRect(int srcWidth, int srcHeight, int dstWidth, int dstHeight,
                                        @NonNull ScalingLogic scalingLogic) {
        if (scalingLogic == ScalingLogic.CROP) {
            final float srcAspect = srcWidth / (float) srcHeight;
            final float dstAspect = dstWidth / (float) dstHeight;
            
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
    @NonNull
    public static Rect calculateDstRect(int srcWidth, int srcHeight, int dstWidth, int dstHeight,
                                        @NonNull ScalingLogic scalingLogic) {
        if (scalingLogic == ScalingLogic.FIT) {
            final float srcAspect = srcWidth / (float) srcHeight;
            final float dstAspect = dstWidth / (float) dstHeight;
            
            if (srcAspect > dstAspect) {
                return new Rect(0, 0, dstWidth, (int) (dstWidth / srcAspect));
            } else {
                return new Rect(0, 0, (int) (dstHeight * srcAspect), dstHeight);
            }
        } else {
            return new Rect(0, 0, dstWidth, dstHeight);
        }
    }
    
    public static int dpToPx(double sizeDp) {
        return (int) Math.round(sizeDp * DISPLAY_METRICS_DENSITY.get());
    }
    
    public static double pxToDp(double sizePx) {
        return sizePx / DISPLAY_METRICS_DENSITY.get();
    }
    
    public static int swapRedAndGreenChannels(@ColorInt int color) {
        Color col = Color.valueOf(color);
        return Color.rgb(col.green(), col.red(), col.blue());
    }
    
    
    public static int getOnBgColor(@ColorInt int surfaceColor) {
        // get a color that will look good on the specified surfaceColor
        return mixTwoColors(getIntensityColor(surfaceColor), surfaceColor, 0.3);
    }
    
    // return black or white based on the background color
    public static int getIntensityColor(@ColorInt int bgColor) {
        Color col = Color.valueOf(bgColor);
        float intensity = col.red() * 0.299F + col.green() * 0.587F + col.blue() * 0.114F;
        if (intensity > 0.729411F) {
            return Color.BLACK;
        }
        return Color.WHITE;
    }
    
    public static int dimColorToBg(@ColorInt int color, @ColorInt int bgColor) {
        return mixTwoColors(color, bgColor, Static.DEFAULT_DIM_FACTOR);
    }
    
    public static int dimColorToBg(@ColorInt int color, @NonNull Context context) {
        return mixTwoColors(color, MaterialColors.getColor(context, R.attr.colorSurface, Color.GRAY), Static.DEFAULT_DIM_FACTOR);
    }
    
    public static int dimColorToBg(@ColorInt int color, @NonNull Context context, @FloatRange(from = 0.0, to = 1.0) double balance) {
        return mixTwoColors(color, MaterialColors.getColor(context, R.attr.colorSurface, Color.GRAY), balance);
    }
    
    public static int mixTwoColors(@ColorInt int color1, @ColorInt int color2, @FloatRange(from = 0.0, to = 1.0) double balance) {
        Color c1 = Color.valueOf(color1);
        Color c2 = Color.valueOf(color2);
        float alpha = (float) (c1.alpha() * (1 - balance) + c2.alpha() * balance);
        float red = (float) (c1.red() * (1 - balance) + c2.red() * balance);
        float green = (float) (c1.green() * (1 - balance) + c2.green() * balance);
        float blue = (float) (c1.blue() * (1 - balance) + c2.blue() * balance);
        return Color.argb(alpha, red, green, blue);
    }
    
    // get harmonized color with the background
    public static int getHarmonizedFontColorWithBg(@ColorInt int color, @ColorInt int backgroundColor) {
        // harmonize with extrapolated primary color
        return MaterialColors.harmonize(color, getOnBgColor(backgroundColor));
    }
    
    // mix and harmonize (25% background color, 75% font color + harmonized with background);
    public static int getHarmonizedSecondaryFontColorWithBg(@ColorInt int color, @ColorInt int backgroundColor) {
        return getHarmonizedFontColorWithBg(mixTwoColors(color, backgroundColor, Static.DEFAULT_SECONDARY_TEXT_COLOR_MIX_FACTOR), backgroundColor);
    }
    
    // mix color with bg color based on balance (from 1 to 10)
    public static int mixColorWithBg(@ColorInt int inputColor, @ColorInt int backgroundColor, @IntRange(from = 0, to = 10) int balance) {
        if (balance == 0) {
            return inputColor;
        }
        return mixTwoColors(MaterialColors.harmonize(inputColor, backgroundColor),
                backgroundColor, (balance - 1) / 9D);
    }
    
    public static int getExpiredUpcomingColor(@ColorInt int baseColor, @ColorInt int tintColor) {
        return mixTwoColors(baseColor, tintColor, DEFAULT_TIME_OFFSET_COLOR_MIX_FACTOR);
    }
    
    public static void harmonizeColorsForActivity(@NonNull Activity activity, @NonNull @ColorRes int... colors) {
        HarmonizedColors.applyToContextIfAvailable(activity,
                new HarmonizedColorsOptions.Builder()
                        .setColorResourceIds(colors)
                        .build());
        DynamicColors.applyToActivityIfAvailable(activity);
    }
    
    public static int getAverageColor(@NonNull int[] pixels) {
        
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
        
        private final ColorStateList sliderPrimaryColor;
        private final ColorStateList sliderOnSurfaceColor;
        private final ColorStateList sliderHaloColor;
        
        public SliderTinter(@NonNull Context context, @ColorInt int sliderAccentColor) {
            sliderAccentColor = MaterialColors.harmonizeWithPrimary(context, sliderAccentColor);
            
            sliderPrimaryColor = ColorStateList.valueOf(sliderAccentColor);
            sliderOnSurfaceColor = ColorStateList.valueOf(getOnBgColor(sliderAccentColor));
            sliderHaloColor = ColorStateList.valueOf(dimColorToBg(sliderAccentColor, context));
        }
        
        public void tintSlider(@NonNull Slider slider) {
            slider.setThumbTintList(sliderPrimaryColor);
            slider.setHaloTintList(sliderHaloColor);
            slider.setTrackActiveTintList(sliderPrimaryColor);
            slider.setTickActiveTintList(sliderOnSurfaceColor);
            slider.setTickInactiveTintList(sliderPrimaryColor);
        }
        
    }
    
    public static void fingerPrintBitmap(@NonNull Bitmap bitmap) {
        bitmap.setPixel(0, 0, Color.DKGRAY);
        bitmap.setPixel(1, 0, Color.GREEN);
        bitmap.setPixel(0, 1, Color.YELLOW);
    }
    
    public static boolean hasNoFingerPrint(@NonNull Bitmap bitmap) {
        int pixel1 = bitmap.getPixel(0, 0);
        int pixel2 = bitmap.getPixel(1, 0);
        int pixel3 = bitmap.getPixel(0, 1);
        
        return !((pixel1 == Color.DKGRAY)
                && (pixel2 == Color.GREEN)
                && (pixel3 == Color.YELLOW));
    }
    
    public static int hashBitmap(@NonNull Bitmap bitmap) {
        int[] buffer = new int[bitmap.getWidth() * bitmap.getHeight() / 16];
        bitmap.getPixels(buffer, 0, bitmap.getWidth() / 4, 0, 0, bitmap.getWidth() / 4, bitmap.getHeight() / 4);
        return Arrays.hashCode(buffer);
    }
}
