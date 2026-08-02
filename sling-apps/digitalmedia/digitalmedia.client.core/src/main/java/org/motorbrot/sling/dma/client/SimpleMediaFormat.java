package org.motorbrot.sling.dma.client;

/**
 * Convenience base class for declaring a {@link MediaFormat} as a tiny OSGi
 * {@code @Component(service = MediaFormat.class)} class:
 *
 * <pre>{@code
 * @Component(service = MediaFormat.class)
 * public class FeatureDesktopFormat extends SimpleMediaFormat {
 *     public FeatureDesktopFormat() {
 *         super("matrix-feature-desktop", 1200, 900, "Feature image \u2014 desktop (4:3)", false);
 *     }
 * }
 * }</pre>
 */
public class SimpleMediaFormat implements MediaFormat {

    private final String name;
    private final int width;
    private final int height;
    private final String label;
    private final String description;
    private final boolean maintainAspectRatio;

    public SimpleMediaFormat(String name, int width, int height) {
        this(name, width, height, name, "", false);
    }

    public SimpleMediaFormat(String name, int width, int height, String description, boolean maintainAspectRatio) {
        this(name, width, height, name, description, maintainAspectRatio);
    }

    public SimpleMediaFormat(String name, int width, int height, String label, String description, boolean maintainAspectRatio) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.label = label;
        this.description = description;
        this.maintainAspectRatio = maintainAspectRatio;
    }

    @Override public String getName() { return name; }
    @Override public int getWidth() { return width; }
    @Override public int getHeight() { return height; }
    @Override public String getLabel() { return label; }
    @Override public String getDescription() { return description; }
    @Override public boolean isMaintainAspectRatio() { return maintainAspectRatio; }

    @Override
    public String toString() {
        return "MediaFormat{" + name + " " + width + "x" + height + "}";
    }
}
