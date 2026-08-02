package org.motorbrot.sling.dma.client;

import org.motorbrot.sling.dma.client.crop.CropBox;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Pure-function rules describing whether an original image can produce a given
 * {@link MediaFormat} rendition. No JCR / no I/O.
 */
@ProviderType
public final class RenditionValidator {

    /**
     * Tolerance for aspect-ratio mismatch detection (5% of the target ratio).
     * Retained for API compatibility — current validation accepts any aspect
     * difference and resolves it via focus-aware cropping (see {@link #cropFor}).
     */
    public static final double ASPECT_TOLERANCE = 0.05d;

    public enum Status {
        /** Source can satisfy the format (possibly after focus-aware cropping). */
        OK,
        /** Source — or its target-aspect crop — is smaller than the format. */
        TOO_SMALL,
        /**
         * Deprecated. Aspect mismatches are no longer hard failures; they are
         * resolved by cropping around the focus point. Retained so existing
         * callers keep compiling.
         */
        @Deprecated
        ASPECT_MISMATCH
    }

    public static final class Result {
        private final Status status;
        private final String message;

        public Result(Status status, String message) {
            this.status = status;
            this.message = message;
        }
        public Status getStatus() { return status; }
        public String getMessage() { return message; }
        public boolean isOk() { return status == Status.OK; }
        public boolean isTooSmall() { return status == Status.TOO_SMALL; }
        public boolean isAspectMismatch() { return status == Status.ASPECT_MISMATCH; }
        @Override public String toString() { return status + ": " + message; }
    }

    private RenditionValidator() {}

    /**
     * Validates that the source dimensions can satisfy the requested format.
     *
     * <p>For aspect-preserving formats the source is scaled into the box, so the
     * only failure mode is "smaller than the format in both dimensions".
     *
     * <p>For fixed-aspect formats the largest target-aspect rectangle that fits
     * inside the source ({@link CropBox#fromFocus(int, int, double, org.motorbrot.sling.dma.client.crop.FocusPoint)})
     * must itself be at least as large as the format. If it is, the rendition
     * can be produced by focus-aware cropping followed by scaling, so the
     * result is {@link Status#OK} regardless of the source aspect ratio.
     */
    public static Result validate(int sourceWidth, int sourceHeight, MediaFormat format) {
        if (format == null) {
            return new Result(Status.OK, "no format");
        }
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            return new Result(Status.TOO_SMALL, "Source dimensions unknown");
        }
        if (format.isMaintainAspectRatio()) {
            if (sourceWidth < format.getWidth() && sourceHeight < format.getHeight()) {
                return new Result(Status.TOO_SMALL,
                        "Source " + sourceWidth + "x" + sourceHeight
                                + " is smaller than format " + format.getName()
                                + " (" + format.getWidth() + "x" + format.getHeight() + ").");
            }
            return new Result(Status.OK, "");
        }
        double targetRatio = format.getAspectRatio();
        if (targetRatio <= 0d) {
            return new Result(Status.OK, "");
        }
        // Largest target-aspect rectangle that fits inside the source.
        int cropW;
        int cropH;
        double sourceRatio = (double) sourceWidth / (double) sourceHeight;
        if (sourceRatio > targetRatio) {
            cropH = sourceHeight;
            cropW = (int) Math.round(cropH * targetRatio);
        } else {
            cropW = sourceWidth;
            cropH = (int) Math.round(cropW / targetRatio);
        }
        if (cropW < format.getWidth() || cropH < format.getHeight()) {
            return new Result(Status.TOO_SMALL,
                    String.format(
                            "Source %dx%d would crop to %dx%d for aspect %.3f — smaller than format %s (%dx%d).",
                            sourceWidth, sourceHeight, cropW, cropH, targetRatio,
                            format.getName(), format.getWidth(), format.getHeight()));
        }
        return new Result(Status.OK, "");
    }

    /**
     * Derives the crop window that should be applied to satisfy the target format.
     * Returns {@code null} if the format has no aspect constraint (no crop needed),
     * or if {@link #validate} would return {@link Status#TOO_SMALL} for these inputs
     * (no valid crop exists — the source is genuinely too small).
     */
    public static CropBox cropFor(int sourceWidth, int sourceHeight,
                                  MediaFormat format,
                                  org.motorbrot.sling.dma.client.crop.FocusPoint focus) {
        if (format == null || format.isMaintainAspectRatio()) {
            return null;
        }
        if (!validate(sourceWidth, sourceHeight, format).isOk()) {
            return null;
        }
        return CropBox.fromFocus(sourceWidth, sourceHeight, format.getAspectRatio(),
                focus == null ? org.motorbrot.sling.dma.client.crop.FocusPoint.CENTER : focus);
    }
}
