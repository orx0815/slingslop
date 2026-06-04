package org.motorbrot.sling.dma.services;

import org.apache.tika.Tika;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for extracting metadata from uploaded files using Apache Tika.
 */
@Component(service = MetadataExtractionService.class, immediate = true)
public class MetadataExtractionService {

    private static final Logger LOG = LoggerFactory.getLogger(MetadataExtractionService.class);
    private final Tika tika = new Tika();

    /**
     * Extracts metadata from an input stream.
     *
     * @param inputStream the file input stream
     * @param filename the original filename
     * @return map of metadata properties
     */
    public Map<String, Object> extractMetadata(InputStream inputStream, String filename) {
        Map<String, Object> metadata = new HashMap<>();

        try {
            // Detect MIME type using filename hint
            String mimeType = tika.detect(inputStream, filename);
            metadata.put("mimeType", mimeType);
            metadata.put("filename", filename);

            // Extract basic file type
            String fileType = getFileType(mimeType);
            metadata.put("fileType", fileType);

            LOG.info("Extracted metadata for file: {} - Type: {}", filename, mimeType);

        } catch (Exception e) {
            LOG.error("Failed to extract metadata for file: " + filename, e);
            metadata.put("mimeType", "application/octet-stream");
            metadata.put("fileType", "unknown");
            metadata.put("error", e.getMessage());
        }

        return metadata;
    }

    /**
     * Determines the general file type category from MIME type.
     */
    private String getFileType(String mimeType) {
        if (mimeType == null) {
            return "unknown";
        }

        if (mimeType.startsWith("image/")) {
            return "image";
        } else if (mimeType.startsWith("video/")) {
            return "video";
        } else if (mimeType.startsWith("audio/")) {
            return "audio";
        } else if (mimeType.equals("application/pdf")) {
            return "pdf";
        } else if (mimeType.startsWith("text/")) {
            return "text";
        } else if (mimeType.contains("zip") || mimeType.contains("compressed")) {
            return "archive";
        }

        return "document";
    }

    /**
     * Checks if the file type supports image rendition generation.
     */
    public boolean supportsImageRenditions(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }
}
