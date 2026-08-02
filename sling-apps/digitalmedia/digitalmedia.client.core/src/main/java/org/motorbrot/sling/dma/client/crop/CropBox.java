package org.motorbrot.sling.dma.client.crop;

/**
 * Pixel crop window in the source image, top-left origin. All four fields
 * are positive integers; {@code x + width <= sourceWidth}, {@code y + height
 * <= sourceHeight} after construction (validated by the caller / factory).
 */
public final class CropBox {

    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public CropBox(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("CropBox must be positive: " + width + "x" + height);
        }
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("CropBox origin must be >= 0: (" + x + "," + y + ")");
        }
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public double aspectRatio() { return (double) width / (double) height; }

    /**
     * Derive a {@link CropBox} from a source size, a target aspect ratio and a focus point.
     *
     * <p>The crop window is the largest rectangle of the requested ratio that still fits inside
     * the source. The window is centred on the focus point and slid inside the source bounds so
     * the focus pixel remains visible.
     *
     * @param sourceWidth  source pixel width
     * @param sourceHeight source pixel height
     * @param targetRatio  desired width/height ratio
     * @param focus        focus point, never {@code null}
     * @return a crop box, or {@code null} if inputs are degenerate
     */
    public static CropBox fromFocus(int sourceWidth, int sourceHeight, double targetRatio, FocusPoint focus) {
        if (sourceWidth <= 0 || sourceHeight <= 0 || targetRatio <= 0d || focus == null) {
            return null;
        }
        double sourceRatio = (double) sourceWidth / (double) sourceHeight;
        int cropW;
        int cropH;
        if (sourceRatio > targetRatio) {
            // source wider than target → shrink width
            cropH = sourceHeight;
            cropW = (int) Math.round(cropH * targetRatio);
        } else {
            // source taller than target → shrink height
            cropW = sourceWidth;
            cropH = (int) Math.round(cropW / targetRatio);
        }
        cropW = Math.min(cropW, sourceWidth);
        cropH = Math.min(cropH, sourceHeight);

        int focusX = focus.xPixel(sourceWidth);
        int focusY = focus.yPixel(sourceHeight);

        int x = focusX - cropW / 2;
        int y = focusY - cropH / 2;
        x = Math.max(0, Math.min(sourceWidth - cropW, x));
        y = Math.max(0, Math.min(sourceHeight - cropH, y));
        return new CropBox(x, y, cropW, cropH);
    }

    @Override
    public String toString() {
        return "CropBox(" + x + "," + y + "," + width + "x" + height + ")";
    }
}
