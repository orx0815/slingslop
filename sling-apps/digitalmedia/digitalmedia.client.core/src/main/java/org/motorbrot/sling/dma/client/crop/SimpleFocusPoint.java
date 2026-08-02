package org.motorbrot.sling.dma.client.crop;

/** Plain immutable {@link FocusPoint}. Inputs are clamped to {@code [0, 100]}. */
public final class SimpleFocusPoint implements FocusPoint {

    private final double xPercent;
    private final double yPercent;

    public SimpleFocusPoint(double xPercent, double yPercent) {
        this.xPercent = clamp(xPercent);
        this.yPercent = clamp(yPercent);
    }

    @Override public double getXPercent() { return xPercent; }
    @Override public double getYPercent() { return yPercent; }

    private static double clamp(double v) {
        if (Double.isNaN(v)) return 50d;
        return Math.max(0d, Math.min(100d, v));
    }

    @Override
    public String toString() {
        return "FocusPoint(" + xPercent + "%, " + yPercent + "%)";
    }
}
