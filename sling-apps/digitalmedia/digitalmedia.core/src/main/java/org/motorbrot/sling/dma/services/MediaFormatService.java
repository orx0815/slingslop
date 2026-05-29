package org.motorbrot.sling.dma.services;

import org.motorbrot.sling.dma.models.MediaFormat;
import org.osgi.service.component.annotations.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing MediaFormat configurations.
 * Provides predefined formats for common use cases.
 */
@Component(service = MediaFormatService.class, immediate = true)
public class MediaFormatService {

    private final List<MediaFormat> formats;

    public MediaFormatService() {
        List<MediaFormat> formatList = new ArrayList<>();

        // Preview format for grid display
        formatList.add(new MediaFormat(
            "preview",
            300,
            300,
            "Preview thumbnail for asset grid",
            true
        ));

        // Thumbnail format for smaller displays
        formatList.add(new MediaFormat(
            "thumbnail",
            150,
            150,
            "Small thumbnail",
            true
        ));

        // Large format for detail view
        formatList.add(new MediaFormat(
            "large",
            1200,
            1200,
            "Large display format",
            true
        ));

        // Web optimized format
        formatList.add(new MediaFormat(
            "web",
            800,
            600,
            "Web optimized format",
            false
        ));

        this.formats = Collections.unmodifiableList(formatList);
    }

    /**
     * Gets all available media formats.
     */
    public List<MediaFormat> getAllFormats() {
        return formats;
    }

    /**
     * Gets a specific media format by name.
     */
    public Optional<MediaFormat> getFormat(String name) {
        return formats.stream()
                .filter(f -> f.getName().equals(name))
                .findFirst();
    }

    /**
     * Gets the default preview format.
     */
    public MediaFormat getPreviewFormat() {
        return getFormat("preview")
                .orElseThrow(() -> new IllegalStateException("Preview format not found"));
    }
}
