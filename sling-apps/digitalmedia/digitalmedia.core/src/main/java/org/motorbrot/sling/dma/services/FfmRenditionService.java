package org.motorbrot.sling.dma.services;

import org.motorbrot.sling.dma.client.MediaFormat;
import org.motorbrot.sling.dma.client.MediaFormatRegistry;
import org.motorbrot.sling.dma.client.RenditionValidator;
import org.motorbrot.sling.dma.client.SimpleMediaFormat;
import org.motorbrot.sling.dma.client.crop.CropBox;
import org.motorbrot.sling.dma.client.crop.FocusPoint;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

/**
 * {@link RenditionService} implementation that renders the preview thumbnail via
 * native ImageMagick (MagickWand) using the Java Foreign Function &amp; Memory API.
 * The preview is rendered as a Polaroid with the original {@code WIDTHxHEIGHT} as
 * caption.
 *
 * <p>This is the single {@link RenditionService} published in the container.
 * All rendition operations are performed natively when the library is available;
 * only the preview additionally gets a Polaroid border and {@code WIDTHxHEIGHT}
 * caption. When the native library is missing, the service is disabled, or a
 * native call fails, operations transparently delegate to
 * {@link Java2DRenditionService}. Because there is only one
 * {@code RenditionService}, consumer binding is deterministic — there is no
 * ranking race with the Java2D baseline.
 *
 * <p>Set {@code enabled = false} on the {@code DML FFM Rendition Service}
 * configuration to switch native preview generation off; the service then behaves
 * exactly like the Java2D baseline.
 */
@Component(
        service = RenditionService.class,
        immediate = true)
@Designate(ocd = FfmRenditionService.Config.class)
public class FfmRenditionService implements RenditionService {

    @ObjectClassDefinition(name = "DML FFM Rendition Service")
    public @interface Config {
        @AttributeDefinition(name = "Library path",
                description = "Absolute path to libMagickWand-6. Empty = auto-detect via -Dimagemagick.library, $IMAGEMAGICK_LIBRARY, or OS defaults.")
        String library_path() default "";

        @AttributeDefinition(name = "Enabled",
                description = "When false, native preview generation is disabled and this service delegates to the Java2D baseline.")
        boolean enabled() default true;
    }

    private static final Logger LOG = LoggerFactory.getLogger(FfmRenditionService.class);

    // ImageMagick filter constant: LanczosFilter = 22
    private static final int LANCZOS_FILTER = 22;

    private static final List<String> LINUX_DEFAULTS = List.of(
            "/usr/lib/x86_64-linux-gnu/libMagickWand-6.Q16.so",
            "/usr/lib/x86_64-linux-gnu/libMagickWand-6.Q16.so.7",
            "/usr/lib/x86_64-linux-gnu/libMagickWand-6.Q16HDRI.so",
            "/usr/lib/x86_64-linux-gnu/libMagickWand-6.Q16HDRI.so.7",
            "/usr/lib64/libMagickWand-6.Q16.so",
            "/usr/lib64/libMagickWand-6.Q16HDRI.so"
    );

    private static final List<String> MAC_DEFAULTS = List.of(
            "/opt/homebrew/lib/libMagickWand-6.Q16.dylib",
            "/opt/homebrew/lib/libMagickWand-6.Q16HDRI.dylib",
            "/usr/local/lib/libMagickWand-6.Q16.dylib",
            "/usr/local/lib/libMagickWand-6.Q16HDRI.dylib"
    );

    private boolean available;
    private String loadedFrom;

    @Reference
    private MediaFormatRegistry mediaFormatRegistry;

    /** Pure-Java baseline used for delegation and as a fallback. */
    @Reference
    private Java2DRenditionService fallback;

    // Method handles
    private MethodHandle mhWandGenesis;
    private MethodHandle mhWandTerminus;
    private MethodHandle mhNewMagickWand;
    private MethodHandle mhDestroyMagickWand;
    private MethodHandle mhReadImageBlob;
    private MethodHandle mhGetImageBlob;
    private MethodHandle mhRelinquishMemory;
    private MethodHandle mhGetImageWidth;
    private MethodHandle mhGetImageHeight;
    private MethodHandle mhResizeImage;
    private MethodHandle mhCropImage;
    private MethodHandle mhResetImagePage;
    private MethodHandle mhSetImageFormat;
    private MethodHandle mhNewPixelWand;
    private MethodHandle mhDestroyPixelWand;
    private MethodHandle mhPixelSetColor;
    private MethodHandle mhSetBackgroundColor;
    private MethodHandle mhNewDrawingWand;
    private MethodHandle mhDestroyDrawingWand;
    private MethodHandle mhDrawSetFontSize;
    private MethodHandle mhDrawSetTextAlignment;
    private MethodHandle mhExtentImage;
    private MethodHandle mhAnnotateImage;

    @Activate
    protected void activate(Config config) {
        if (!config.enabled()) {
            LOG.info("FfmRenditionService disabled by configuration; delegating to Java2D baseline");
            return;
        }
        String resolved = resolveLibraryPath(config.library_path());
        if (resolved == null) {
            LOG.warn("ImageMagick MagickWand library not found. FFM thumbnail generation disabled. "
                    + "Set -Dimagemagick.library=/path/to/libMagickWand-7... or env IMAGEMAGICK_LIBRARY.");
            return;
        }
        try {
            System.load(resolved);
            this.loadedFrom = resolved;
            bindHandles();
            mhWandGenesis.invoke();
            this.available = true;
            LOG.info("FfmRenditionService active. Loaded MagickWand from {}", resolved);
        } catch (UnsatisfiedLinkError | RuntimeException e) {
            LOG.warn("Failed to load MagickWand from {} - FFM thumbnail disabled: {}", resolved, e.toString());
        } catch (Throwable t) {
            LOG.warn("Unexpected error binding MagickWand - FFM thumbnail disabled", t);
        }
    }

    @Deactivate
    protected void deactivate() {
        if (available && mhWandTerminus != null) {
            try {
                mhWandTerminus.invoke();
            } catch (Throwable t) {
                LOG.warn("MagickWandTerminus failed", t);
            }
        }
        available = false;
    }

    public boolean isAvailable() {
        return available;
    }

    public String getLoadedFrom() {
        return loadedFrom;
    }

    // ---------------------------------------------------------------------
    // RenditionService — native resize/crop when available, else Java2D fallback.
    // ---------------------------------------------------------------------

    @Override
    public byte[] generateRendition(InputStream imageData, MediaFormat format) {
        return generateRendition(imageData, format, null);
    }

    @Override
    public byte[] generateRendition(InputStream imageData, MediaFormat format, FocusPoint focusPoint) {
        if (!available) {
            return fallback.generateRendition(imageData, format, focusPoint);
        }
        byte[] bytes = readAll(imageData);
        if (bytes == null) {
            return null;
        }
        byte[] out = nativeRendition(bytes, format, focusPoint, true);
        if (out != null) {
            return out;
        }
        LOG.warn("FFM rendition returned null for {}; falling back to Java2D", format.getName());
        return fallback.generateRendition(new ByteArrayInputStream(bytes), format, focusPoint);
    }

    @Override
    public byte[] generateCroppedRendition(InputStream imageData, MediaFormat format, FocusPoint focusPoint) {
        if (!available) {
            return fallback.generateCroppedRendition(imageData, format, focusPoint);
        }
        byte[] bytes = readAll(imageData);
        if (bytes == null) {
            return null;
        }
        byte[] out = nativeRendition(bytes, format, focusPoint, false);
        if (out != null) {
            return out;
        }
        LOG.warn("FFM cropped rendition returned null for {}; falling back to Java2D", format.getName());
        return fallback.generateCroppedRendition(new ByteArrayInputStream(bytes), format, focusPoint);
    }

    /**
     * Generates the preview rendition. Uses native ImageMagick when available;
     * otherwise (native library missing, service disabled, or native failure)
     * delegates to {@link Java2DRenditionService}.
     */
    @Override
    public byte[] generatePreviewRendition(InputStream imageData) {
        byte[] bytes = readAll(imageData);
        if (bytes == null) {
            return null;
        }

        if (available) {
            byte[] out = generateThumbnailWithDimensions(
                    new ByteArrayInputStream(bytes), previewFormat());
            if (out != null) {
                return out;
            }
            LOG.warn("FFM preview generation returned null; falling back to Java2D");
        }
        return fallback.generatePreviewRendition(new ByteArrayInputStream(bytes));
    }

    private byte[] readAll(InputStream imageData) {
        try {
            return imageData.readAllBytes();
        } catch (IOException e) {
            LOG.warn("Could not read input stream for rendition", e);
            return null;
        }
    }

    private MediaFormat previewFormat() {
        return mediaFormatRegistry.getByName("preview")
                .orElseThrow(() -> new IllegalStateException(
                        "Preview format not found \u2014 is digitalmedia.core's PreviewMediaFormat installed?"));
    }

    /**
     * Native (MagickWand) resize with optional focus-aware crop. No Polaroid
     * border and no caption — that decoration is exclusive to the preview.
     *
     * @param refuseTooSmall when {@code true} (matches
     *        {@link RenditionService#generateRendition}) generation is refused for
     *        sources the {@link RenditionValidator} reports as {@code TOO_SMALL};
     *        when {@code false} (matches {@code generateCroppedRendition}) the
     *        largest fitting crop is used and the result is never upscaled.
     * @return JPEG bytes, or {@code null} on failure / intentional refusal.
     */
    private byte[] nativeRendition(byte[] input, MediaFormat format, FocusPoint focusPoint, boolean refuseTooSmall) {
        if (!available) {
            return null;
        }
        FocusPoint focus = focusPoint == null ? FocusPoint.CENTER : focusPoint;
        MemorySegment wand = null;
        try (Arena arena = Arena.ofConfined()) {
            wand = (MemorySegment) mhNewMagickWand.invoke();
            if (wand == null || wand.address() == 0L) {
                LOG.warn("NewMagickWand returned null");
                return null;
            }

            MemorySegment blob = arena.allocate(input.length);
            MemorySegment.copy(input, 0, blob, ValueLayout.JAVA_BYTE, 0, input.length);
            int rc = (int) mhReadImageBlob.invoke(wand, blob, (long) input.length);
            if (rc == 0) {
                LOG.warn("MagickReadImageBlob failed");
                return null;
            }

            int origWidth = (int) (long) mhGetImageWidth.invoke(wand);
            int origHeight = (int) (long) mhGetImageHeight.invoke(wand);

            if (refuseTooSmall) {
                RenditionValidator.Result vr = RenditionValidator.validate(origWidth, origHeight, format);
                if (vr.isTooSmall()) {
                    LOG.warn("Refusing to generate {}: {}", format.getName(), vr.getMessage());
                    return null;
                }
            }

            // Focus-aware crop for fixed-aspect formats.
            CropBox crop;
            if (refuseTooSmall) {
                crop = RenditionValidator.cropFor(origWidth, origHeight, format, focus);
            } else if (!format.isMaintainAspectRatio() && format.getAspectRatio() > 0d) {
                crop = CropBox.fromFocus(origWidth, origHeight, format.getAspectRatio(), focus);
            } else {
                crop = null;
            }

            long srcWidth = origWidth;
            long srcHeight = origHeight;
            if (crop != null) {
                int cr = (int) mhCropImage.invoke(wand,
                        (long) crop.getWidth(), (long) crop.getHeight(),
                        (long) crop.getX(), (long) crop.getY());
                if (cr == 0) {
                    LOG.warn("MagickCropImage failed");
                    return null;
                }
                // Clear the virtual-canvas offset left by the crop so JPEG output is clean.
                mhResetImagePage.invoke(wand, arena.allocateFrom("0x0+0+0"));
                srcWidth = crop.getWidth();
                srcHeight = crop.getHeight();
            }

            long[] tgt = computeTargetSize(srcWidth, srcHeight, format);
            int rs = (int) mhResizeImage.invoke(wand, tgt[0], tgt[1], LANCZOS_FILTER, 1.0);
            if (rs == 0) {
                LOG.warn("MagickResizeImage failed");
                return null;
            }

            mhSetImageFormat.invoke(wand, arena.allocateFrom("JPEG"));

            MemorySegment lengthOut = arena.allocate(ValueLayout.JAVA_LONG);
            MemorySegment blobPtr = (MemorySegment) mhGetImageBlob.invoke(wand, lengthOut);
            if (blobPtr == null || blobPtr.address() == 0L) {
                LOG.warn("MagickGetImageBlob returned null");
                return null;
            }
            long length = lengthOut.get(ValueLayout.JAVA_LONG, 0);
            byte[] out = blobPtr.reinterpret(length).toArray(ValueLayout.JAVA_BYTE);
            mhRelinquishMemory.invoke(blobPtr);

            LOG.info("FFM rendition: {} {}x{} -> {}x{} ({} bytes) crop={} focus={}",
                    format.getName(), origWidth, origHeight, tgt[0], tgt[1], out.length, crop, focus);
            return out;
        } catch (Throwable t) {
            LOG.warn("FFM rendition generation failed for format: " + format.getName(), t);
            return null;
        } finally {
            safeDestroy(mhDestroyMagickWand, wand);
        }
    }

    private String resolveLibraryPath(String configured) {
        // 1. -Dimagemagick.library
        String sysProp = System.getProperty("imagemagick.library");
        if (sysProp != null && !sysProp.isBlank() && Files.exists(Paths.get(sysProp))) {
            return sysProp;
        }
        // 2. $IMAGEMAGICK_LIBRARY
        String env = System.getenv("IMAGEMAGICK_LIBRARY");
        if (env != null && !env.isBlank() && Files.exists(Paths.get(env))) {
            return env;
        }
        // 3. OSGi config
        if (configured != null && !configured.isBlank() && Files.exists(Paths.get(configured))) {
            return configured;
        }
        // 4. OS defaults
        String os = System.getProperty("os.name", "").toLowerCase();
        List<String> candidates;
        if (os.contains("mac")) {
            candidates = MAC_DEFAULTS;
        } else if (os.contains("win")) {
            candidates = findWindowsCandidates();
        } else {
            candidates = LINUX_DEFAULTS;
        }
        for (String c : candidates) {
            if (Files.exists(Paths.get(c))) {
                return c;
            }
        }
        return null;
    }

    private List<String> findWindowsCandidates() {
        Path programFiles = Paths.get(System.getenv().getOrDefault("ProgramFiles", "C:\\Program Files"));
        if (!Files.isDirectory(programFiles)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(programFiles)) {
            return stream
                    .filter(p -> p.getFileName().toString().startsWith("ImageMagick-6"))
                    .map(p -> p.resolve("CORE_RL_MagickWand_.dll").toString())
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private void bindHandles() throws Throwable {
        Linker linker = Linker.nativeLinker();
        SymbolLookup lib = SymbolLookup.loaderLookup();

        mhWandGenesis = down(linker, lib, "MagickWandGenesis", FunctionDescriptor.ofVoid());
        mhWandTerminus = down(linker, lib, "MagickWandTerminus", FunctionDescriptor.ofVoid());
        mhNewMagickWand = down(linker, lib, "NewMagickWand",
                FunctionDescriptor.of(ValueLayout.ADDRESS));
        mhDestroyMagickWand = down(linker, lib, "DestroyMagickWand",
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        mhReadImageBlob = down(linker, lib, "MagickReadImageBlob",
                FunctionDescriptor.of(ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
        mhGetImageBlob = down(linker, lib, "MagickGetImageBlob",
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        mhRelinquishMemory = down(linker, lib, "MagickRelinquishMemory",
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        mhGetImageWidth = down(linker, lib, "MagickGetImageWidth",
                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
        mhGetImageHeight = down(linker, lib, "MagickGetImageHeight",
                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
        mhResizeImage = down(linker, lib, "MagickResizeImage",
                FunctionDescriptor.of(ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
                        ValueLayout.JAVA_INT, ValueLayout.JAVA_DOUBLE));
        mhCropImage = down(linker, lib, "MagickCropImage",
                FunctionDescriptor.of(ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
                        ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG));
        mhResetImagePage = down(linker, lib, "MagickResetImagePage",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        mhSetImageFormat = down(linker, lib, "MagickSetImageFormat",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        mhNewPixelWand = down(linker, lib, "NewPixelWand",
                FunctionDescriptor.of(ValueLayout.ADDRESS));
        mhDestroyPixelWand = down(linker, lib, "DestroyPixelWand",
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        mhPixelSetColor = down(linker, lib, "PixelSetColor",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        mhSetBackgroundColor = down(linker, lib, "MagickSetBackgroundColor",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        mhNewDrawingWand = down(linker, lib, "NewDrawingWand",
                FunctionDescriptor.of(ValueLayout.ADDRESS));
        mhDestroyDrawingWand = down(linker, lib, "DestroyDrawingWand",
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        mhDrawSetFontSize = down(linker, lib, "DrawSetFontSize",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_DOUBLE));
        mhDrawSetTextAlignment = down(linker, lib, "DrawSetTextAlignment",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        mhExtentImage = down(linker, lib, "MagickExtentImage",
                FunctionDescriptor.of(ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
                        ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG));
        mhAnnotateImage = down(linker, lib, "MagickAnnotateImage",
                FunctionDescriptor.of(ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                        ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE,
                        ValueLayout.JAVA_DOUBLE, ValueLayout.ADDRESS));
    }

    private static MethodHandle down(Linker linker, SymbolLookup lib, String name, FunctionDescriptor fd) {
        return linker.downcallHandle(
                lib.find(name).orElseThrow(() -> new RuntimeException("Symbol not found: " + name)),
                fd);
    }

    /**
     * Generates a {@code format.width x format.height} JPEG thumbnail with a white
     * border and the original {@code WIDTHxHEIGHT} annotated at the bottom.
     * Returns JPEG bytes, or {@code null} on failure.
     */
    private byte[] generateThumbnailWithDimensions(InputStream imageData, MediaFormat format) {
        if (!available) {
            return null;
        }
        byte[] input;
        try {
            input = imageData.readAllBytes();
        } catch (IOException e) {
            LOG.warn("Could not read input stream", e);
            return null;
        }

        final int BORDER_SIDE = 10;
        final int CAPTION_AREA = 30;

        MemorySegment wand = null;
        MemorySegment drawingWand = null;
        MemorySegment bgWand = null;
        try (Arena arena = Arena.ofConfined()) {
            wand = (MemorySegment) mhNewMagickWand.invoke();
            if (wand == null || wand.address() == 0L) {
                LOG.warn("NewMagickWand returned null");
                return null;
            }

            MemorySegment blob = arena.allocate(input.length);
            MemorySegment.copy(input, 0, blob, ValueLayout.JAVA_BYTE, 0, input.length);
            int rc = (int) mhReadImageBlob.invoke(wand, blob, (long) input.length);
            if (rc == 0) {
                LOG.warn("MagickReadImageBlob failed");
                return null;
            }

            // Set wand-level background to white — MagickExtentImage uses this
            bgWand = (MemorySegment) mhNewPixelWand.invoke();
            mhPixelSetColor.invoke(bgWand, arena.allocateFrom("white"));
            mhSetBackgroundColor.invoke(wand, bgWand);

            long origWidth = (long) mhGetImageWidth.invoke(wand);
            long origHeight = (long) mhGetImageHeight.invoke(wand);

            // Resize to fit within the inner area (leaving room for border + caption)
            long innerW = format.getWidth() - 2L * BORDER_SIDE;
            long innerH = format.getHeight() - 2L * BORDER_SIDE - CAPTION_AREA;
            MediaFormat inner = new SimpleMediaFormat("_inner", (int) innerW, (int) innerH, "", true);
            long[] tgt = computeTargetSize(origWidth, origHeight, inner);
            int rs = (int) mhResizeImage.invoke(wand, tgt[0], tgt[1], LANCZOS_FILTER, 1.0);
            if (rs == 0) {
                LOG.warn("MagickResizeImage failed");
                return null;
            }

            // Extend canvas to target size, centering image with extra space at bottom
            long canvasW = format.getWidth();
            long canvasH = format.getHeight();
            long xOff = -(canvasW - tgt[0]) / 2;
            long yOff = -((canvasH - CAPTION_AREA - tgt[1]) / 2);
            int er = (int) mhExtentImage.invoke(wand, canvasW, canvasH, xOff, yOff);
            if (er == 0) {
                LOG.warn("MagickExtentImage failed");
                return null;
            }

            // Annotate with WxH caption at bottom center
            drawingWand = (MemorySegment) mhNewDrawingWand.invoke();
            mhDrawSetFontSize.invoke(drawingWand, 14.0);
            mhDrawSetTextAlignment.invoke(drawingWand, 2); // CenterAlign

            String caption = origWidth + "x" + origHeight;
            MemorySegment captionStr = arena.allocateFrom(caption);
            double textX = canvasW / 2.0;
            double textY = canvasH - 10.0;
            int ar2 = (int) mhAnnotateImage.invoke(wand, drawingWand, textX, textY, 0.0, captionStr);
            if (ar2 == 0) {
                LOG.warn("MagickAnnotateImage failed");
            }

            // Output as JPEG (matches the image/jpeg mime type stored by AssetUploadServlet)
            mhSetImageFormat.invoke(wand, arena.allocateFrom("JPEG"));

            MemorySegment lengthOut = arena.allocate(ValueLayout.JAVA_LONG);
            MemorySegment blobPtr = (MemorySegment) mhGetImageBlob.invoke(wand, lengthOut);
            if (blobPtr == null || blobPtr.address() == 0L) {
                LOG.warn("MagickGetImageBlob returned null");
                return null;
            }
            long length = lengthOut.get(ValueLayout.JAVA_LONG, 0);
            MemorySegment sized = blobPtr.reinterpret(length);
            byte[] out = sized.toArray(ValueLayout.JAVA_BYTE);
            mhRelinquishMemory.invoke(blobPtr);

            LOG.info("FFM thumbnail: {}x{} -> {}x{} extent to {}x{} ({} bytes), caption='{}'",
                    origWidth, origHeight, tgt[0], tgt[1], canvasW, canvasH, out.length, caption);
            return out;
        } catch (Throwable t) {
            LOG.warn("FFM thumbnail generation failed", t);
            return null;
        } finally {
            safeDestroy(mhDestroyDrawingWand, drawingWand);
            safeDestroy(mhDestroyPixelWand, bgWand);
            safeDestroy(mhDestroyMagickWand, wand);
        }
    }

    private static void safeDestroy(MethodHandle handle, MemorySegment seg) {
        if (handle == null || seg == null || seg.address() == 0L) {
            return;
        }
        try {
            handle.invoke(seg);
        } catch (Throwable ignored) {
            // best-effort cleanup
        }
    }

    private static long[] computeTargetSize(long origW, long origH, MediaFormat format) {
        long maxW = format.getWidth();
        long maxH = format.getHeight();
        long maxDimension = Math.max(maxW, maxH);
        long tw, th;
        if (format.isMaintainAspectRatio()) {
            double ar = (double) origW / origH;
            if (origW > origH) {
                tw = maxDimension;
                th = (long) (tw / ar);
            } else {
                th = maxDimension;
                tw = (long) (th * ar);
            }
            if (tw > maxW) {
                tw = maxW;
                th = (long) (tw / ar);
            }
            if (th > maxH) {
                th = maxH;
                tw = (long) (th * ar);
            }
        } else {
            tw = maxW;
            th = maxH;
        }
        // No upscaling
        if (tw > origW || th > origH) {
            tw = origW;
            th = origH;
        }
        return new long[]{Math.max(1, tw), Math.max(1, th)};
    }
}
