package org.motorbrot.sling.dma.models;

/**
 * Represents a media format configuration for rendition generation.
 * Inspired by wcm.io Media Handler MediaFormat concept.
 */
public class MediaFormat {

    private final String name;
    private final int width;
    private final int height;
    private final String description;
    private final boolean maintainAspectRatio;

    public MediaFormat(String name, int width, int height, String description, boolean maintainAspectRatio) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.description = description;
        this.maintainAspectRatio = maintainAspectRatio;
    }

    public String getName() {
        return name;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getDescription() {
        return description;
    }

    public boolean isMaintainAspectRatio() {
        return maintainAspectRatio;
    }

    /**
     * Gets the maximum dimension (used for aspect ratio scaling).
     */
    public int getMaxDimension() {
        return Math.max(width, height);
    }

    @Override
    public String toString() {
        return String.format("MediaFormat{name='%s', width=%d, height=%d, maintainAspectRatio=%s}",
                name, width, height, maintainAspectRatio);
    }
}
