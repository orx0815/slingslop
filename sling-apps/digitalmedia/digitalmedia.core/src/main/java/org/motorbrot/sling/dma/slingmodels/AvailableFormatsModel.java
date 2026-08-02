package org.motorbrot.sling.dma.slingmodels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import javax.jcr.Node;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.motorbrot.sling.dma.client.MediaFormat;
import org.motorbrot.sling.dma.client.MediaFormatRegistry;
import org.motorbrot.sling.dma.client.RenditionValidator;

/**
 * Sling Model exposing every {@link MediaFormat} known to the registry,
 * grouped by the provider bundle that registered it, annotated with whether
 * the rendition already exists for the current asset resource and whether the
 * source image is large enough to satisfy the format
 * ({@link RenditionValidator}).
 *
 * <p>Backs the DML dashboard's "Generate Renditions" panel so that formats
 * shipped by consuming sling-apps appear automatically. A format is offered as
 * an active (Stiffkey Blue) generate button only when the source can produce it
 * <em>exactly</em> — the source aspect ratio matches the format and the source
 * has enough pixels (no cropping, no upscaling). Formats that would require a
 * crop (aspect mismatch) or an upscale (too small) are shown greyed-out and
 * inactive. The {@code preview} format is exempt from this rule and always
 * offered, since it is the asset-grid thumbnail.
 */
@Model(adaptables = { SlingJakartaHttpServletRequest.class, Resource.class })
public class AvailableFormatsModel {

    /** Name of the preview format, which is always offered regardless of source size/aspect. */
    private static final String PREVIEW_FORMAT = "preview";

    @SlingObject
    private Resource resource;

    @OSGiService
    private MediaFormatRegistry registry;

    private final Map<String, List<Entry>> byProvider = new LinkedHashMap<>();
    private final List<Entry> missing = new ArrayList<>();
    private int sourceWidth;
    private int sourceHeight;

    @PostConstruct
    protected void init() {
        if (registry == null) {
            return;
        }
        readSourceMetadata();
        Map<String, List<MediaFormat>> grouped = registry.getByProvider();
        for (Map.Entry<String, List<MediaFormat>> e : grouped.entrySet()) {
            List<Entry> entries = new ArrayList<>();
            for (MediaFormat fmt : e.getValue()) {
                boolean exists = resource != null
                        && resource.getChild("renditions/" + fmt.getName()) != null;
                Validity v = validityFor(sourceWidth, sourceHeight, fmt);
                Entry entry = new Entry(fmt, e.getKey(), exists, v.valid, v.reason);
                entries.add(entry);
                if (!exists) {
                    missing.add(entry);
                }
            }
            byProvider.put(e.getKey(), Collections.unmodifiableList(entries));
        }
    }

    /**
     * Strict DML-panel validity: the source must produce the format exactly, with
     * no cropping and no upscaling. The {@code preview} format is always valid.
     *
     * <p>This is intentionally stricter than {@link RenditionValidator#validate}
     * (which resolves aspect mismatches via focus-aware cropping) because the
     * dashboard's bulk-generate buttons do not offer a focus-crop UI — that lives
     * in the per-component responsive-image flow.
     */
    private static Validity validityFor(int w, int h, MediaFormat fmt) {
        if (PREVIEW_FORMAT.equals(fmt.getName())) {
            return new Validity(true, "");
        }
        if (w <= 0 || h <= 0) {
            return new Validity(false, "source dimensions unknown");
        }
        if (fmt.isMaintainAspectRatio()) {
            // Aspect-preserving: no crop needed; only require the source to be large
            // enough that scaling into the box does not upscale.
            boolean ok = w >= fmt.getWidth() || h >= fmt.getHeight();
            return new Validity(ok, ok ? "" : "too small");
        }
        // Fixed-aspect format: the source aspect must match (no crop) and the source
        // must be at least the format size in both dimensions (no upscale).
        double target = fmt.getAspectRatio();
        if (target <= 0d) {
            return new Validity(false, "invalid format");
        }
        double source = (double) w / (double) h;
        boolean aspectMatches = Math.abs(source - target) <= RenditionValidator.ASPECT_TOLERANCE * target;
        if (!aspectMatches) {
            return new Validity(false, "aspect ratio mismatch");
        }
        boolean enoughPixels = w >= fmt.getWidth() && h >= fmt.getHeight();
        if (!enoughPixels) {
            return new Validity(false, "too small");
        }
        return new Validity(true, "");
    }

    private void readSourceMetadata() {
        if (resource == null) {
            return;
        }
        Resource meta = resource.getChild("metadata");
        if (meta != null) {
            ValueMap vm = meta.getValueMap();
            sourceWidth = vm.get("width", 0);
            sourceHeight = vm.get("height", 0);
        }
        if (sourceWidth > 0 && sourceHeight > 0) {
            return;
        }
        Node n = resource.adaptTo(Node.class);
        if (n == null) {
            return;
        }
        try {
            if (n.hasNode("metadata")) {
                Node m = n.getNode("metadata");
                if (sourceWidth == 0 && m.hasProperty("width")) {
                    sourceWidth = (int) m.getProperty("width").getLong();
                }
                if (sourceHeight == 0 && m.hasProperty("height")) {
                    sourceHeight = (int) m.getProperty("height").getLong();
                }
            }
        } catch (Exception ignored) {
            // dimensions stay 0 → every fixed-size format reports invalid
        }
    }

    public Map<String, List<Entry>> getByProvider() {
        return Collections.unmodifiableMap(byProvider);
    }

    public List<Entry> getMissing() {
        return Collections.unmodifiableList(missing);
    }

    public boolean isAnyMissing() {
        return !missing.isEmpty();
    }

    /** Immutable result of the strict DML-panel validity check. */
    private static final class Validity {
        final boolean valid;
        final String reason;
        Validity(boolean valid, String reason) {
            this.valid = valid;
            this.reason = reason;
        }
    }

    public static final class Entry {
        private final MediaFormat format;
        private final String provider;
        private final boolean exists;
        private final boolean valid;
        private final String invalidReason;

        Entry(MediaFormat format, String provider, boolean exists, boolean valid, String invalidReason) {
            this.format = format;
            this.provider = provider;
            this.exists = exists;
            this.valid = valid;
            this.invalidReason = invalidReason;
        }
        public String getName() { return format.getName(); }
        public String getLabel() { return format.getLabel(); }
        public int getWidth() { return format.getWidth(); }
        public int getHeight() { return format.getHeight(); }
        public String getDescription() { return format.getDescription(); }
        public String getProvider() { return provider; }
        public boolean isExists() { return exists; }
        public boolean isMissing() { return !exists; }
        /** Source can produce this format exactly (Stiffkey Blue / active). */
        public boolean isValid() { return valid; }
        /** Source cannot produce this format exactly (greyed-out / inactive in the DML panel). */
        public boolean isInvalid() { return !valid; }
        /** Why the format is inactive ("aspect ratio mismatch", "too small", …); empty when valid. */
        public String getInvalidReason() { return invalidReason; }
    }
}
