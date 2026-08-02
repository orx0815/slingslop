package org.motorbrot.sling.dma.services;

import org.apache.tika.Tika;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
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
            if (mimeType.equals("image/svg+xml")) {
                return "svg";
            }
            return "image";
        } else if (mimeType.startsWith("video/")) {
            return "video";
        } else if (mimeType.startsWith("audio/")) {
            return "audio";
        } else if (mimeType.equals("application/pdf")) {
            return "pdf";
        } else if (mimeType.startsWith("text/")) {
            return "text";
        } else if (mimeType.contains("zip") || mimeType.contains("compressed")
                || mimeType.contains("tar") || mimeType.contains("rar")
                || mimeType.equals("application/x-7z-compressed")) {
            return "archive";
        } else if (mimeType.contains("spreadsheet") || mimeType.contains("excel")
                || mimeType.equals("application/vnd.ms-excel")
                || mimeType.contains("sheet")) {
            return "spreadsheet";
        } else if (mimeType.contains("presentation") || mimeType.contains("powerpoint")
                || mimeType.equals("application/vnd.ms-powerpoint")) {
            return "presentation";
        } else if (mimeType.contains("word") || mimeType.contains("msword")) {
            return "document";
        }

        return "document";
    }

    /**
     * Checks if the file type supports image rendition generation.
     */
    public boolean supportsImageRenditions(String mimeType) {
        // SVG is vector — ImageIO cannot decode it; skip rendition generation for it
        return mimeType != null
                && mimeType.startsWith("image/")
                && !mimeType.equals("image/svg+xml");
    }

    /**
     * Reads the pixel dimensions of an image without decoding the full raster.
     * Returns {@code null} when the bytes are not a recognisable raster image
     * (e.g. SVG, broken file). Width/height are written as {@code int}.
     */
    public int[] extractImageDimensions(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            if (iis == null) {
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, true, true);
                int w = reader.getWidth(0);
                int h = reader.getHeight(0);
                return new int[] { w, h };
            } finally {
                reader.dispose();
            }
        } catch (Exception e) {
            LOG.debug("extractImageDimensions failed", e);
            return null;
        }
    }
}
