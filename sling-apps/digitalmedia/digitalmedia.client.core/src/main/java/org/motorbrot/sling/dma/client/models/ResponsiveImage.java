package org.motorbrot.sling.dma.client.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.jcr.Node;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.motorbrot.sling.dma.client.MediaFormat;
import org.motorbrot.sling.dma.client.MediaFormatRegistry;
import org.motorbrot.sling.dma.client.RenditionValidator;
import org.motorbrot.sling.dma.client.crop.FocusPoint;
import org.motorbrot.sling.dma.client.crop.SimpleFocusPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sling Model that renders a responsive image from a DML asset.
 *
 * <p>Reads its configuration from the consuming component resource:
 * <ul>
 *   <li>{@code fileReference} — JCR path to the DML asset</li>
 *   <li>{@code dmaFormats} — String[] of {@link MediaFormat} names (in display order)</li>
 *   <li>{@code dmaMedia} — String[] of CSS media-query strings, same length as
 *       {@code dmaFormats}. Empty string marks the fallback (used as {@code <img src>}).</li>
 *   <li>{@code dmaAlt} — alternative text</li>
 * </ul>
 *
 * <p>The focus point comes from the asset's {@code jcr:content/metadata} node
 * ({@code dmaFocusX}, {@code dmaFocusY} in percent), defaulting to centre.
 *
 * <p>Authoring logic — HTL decides what to render based on:
 * <ul>
 *   <li>{@link #isAnyInvalid()} — at least one configured format can't be satisfied
 *       (too small or aspect mismatch). For anonymous users HTL should omit the image
 *       entirely; for logged-in users it should render the invalid badge.</li>
 *   <li>{@link #isAnyMissing()} — at least one rendition isn't generated yet. HTL
 *       can render a "Generate" button posting to
 *       {@code asset.generate-rendition.html?format=...}.</li>
 * </ul>
 */
@Model(
    adaptables = { Resource.class, SlingJakartaHttpServletRequest.class },
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class ResponsiveImage {

    private static final Logger LOG = LoggerFactory.getLogger(ResponsiveImage.class);

    /** Suffix for component-local focus-cropped renditions. */
    private static final String CROPPED_SUFFIX = "_cropped";

    @SlingObject
    private Resource resource;

    @SlingObject
    private ResourceResolver resourceResolver;

    @OSGiService
    private MediaFormatRegistry registry;

    @ValueMapValue
    private String fileReference;

    @ValueMapValue
    private String[] dmaFormats;

    @ValueMapValue
    private String[] dmaMedia;

    @ValueMapValue
    private String dmaAlt;

    private final List<Source> sources = new ArrayList<>();
    private Source fallback;
    private Resource assetResource;
    private int sourceWidth;
    private int sourceHeight;
    private FocusPoint focusPoint = FocusPoint.CENTER;

    @PostConstruct
    protected void init() {
        if (fileReference == null || fileReference.isEmpty()) {
            return;
        }
        assetResource = resourceResolver.getResource(fileReference);
        if (assetResource == null) {
            LOG.debug("ResponsiveImage: fileReference not found {}", fileReference);
            return;
        }

        readSourceMetadata(assetResource);
        readFocusPoint(assetResource);

        String[] formatNames = dmaFormats == null ? new String[0] : dmaFormats;
        String[] media = dmaMedia == null ? new String[0] : dmaMedia;

        for (int i = 0; i < formatNames.length; i++) {
            String name = formatNames[i];
            if (name == null || name.isEmpty()) {
                continue;
            }
            Optional<MediaFormat> fmtOpt = registry == null ? Optional.empty() : registry.getByName(name);
            if (!fmtOpt.isPresent()) {
                LOG.debug("ResponsiveImage: format '{}' not registered", name);
                continue;
            }
            MediaFormat fmt = fmtOpt.get();
            String mq = i < media.length && media[i] != null ? media[i] : "";
            Source src = buildSource(fmt, mq);
            sources.add(src);
            if (mq.isEmpty() && fallback == null) {
                fallback = src;
            }
        }
        if (fallback == null && !sources.isEmpty()) {
            fallback = sources.get(sources.size() - 1);
        }
    }

    private Source buildSource(MediaFormat fmt, String media) {
        RenditionValidator.Result vr = RenditionValidator.validate(sourceWidth, sourceHeight, fmt);
        // Prefer a component-local focus-cropped rendition when present. It is
        // generated for formats the shared DML asset is too small for, and lives
        // under the component ({component}/renditions/{name}_cropped).
        boolean cropped = resource.getChild("renditions/" + fmt.getName() + CROPPED_SUFFIX) != null;
        if (cropped) {
            String url = resource.getPath() + "/renditions/" + fmt.getName() + CROPPED_SUFFIX;
            return new Source(fmt, media, url, true, vr, true);
        }
        boolean exists = hasRendition(assetResource, fmt.getName());
        String url = assetResource.getPath() + "/renditions/" + fmt.getName();
        return new Source(fmt, media, url, exists, vr, false);
    }

    private void readSourceMetadata(Resource asset) {
        Resource meta = asset.getChild("metadata");
        if (meta != null) {
            ValueMap vm = meta.getValueMap();
            sourceWidth = vm.get("width", 0);
            sourceHeight = vm.get("height", 0);
        }
        if (sourceWidth > 0 && sourceHeight > 0) {
            return;
        }
        Node n = asset.adaptTo(Node.class);
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
        } catch (Exception e) {
            LOG.debug("ResponsiveImage: could not read width/height of {}", asset.getPath(), e);
        }
    }

    private void readFocusPoint(Resource asset) {
        // Component-local focus (written by the crop-rendition flow) wins over
        // the shared asset's focus, so a per-placement crop stays stable.
        ValueMap own = resource.getValueMap();
        Double ownX = own.get("dmaFocusX", Double.class);
        Double ownY = own.get("dmaFocusY", Double.class);
        if (ownX != null && ownY != null) {
            focusPoint = new SimpleFocusPoint(ownX, ownY);
            return;
        }
        Resource meta = asset.getChild("metadata");
        if (meta == null) {
            return;
        }
        ValueMap vm = meta.getValueMap();
        Double fx = vm.get("dmaFocusX", Double.class);
        Double fy = vm.get("dmaFocusY", Double.class);
        if (fx != null && fy != null) {
            focusPoint = new SimpleFocusPoint(fx, fy);
        }
    }

    private static boolean hasRendition(Resource asset, String name) {
        Resource r = asset.getChild("renditions/" + name);
        return r != null;
    }

    // --- exposed to HTL ---

    public boolean isConfigured() { return assetResource != null && !sources.isEmpty(); }
    public String getAssetPath() { return assetResource == null ? null : assetResource.getPath(); }
    public String getComponentPath() { return resource == null ? null : resource.getPath(); }
    public String getAlt() { return dmaAlt == null ? "" : dmaAlt; }
    public int getSourceWidth() { return sourceWidth; }
    public int getSourceHeight() { return sourceHeight; }
    public FocusPoint getFocusPoint() { return focusPoint; }
    public List<Source> getSources() { return Collections.unmodifiableList(sources); }
    public Source getFallback() { return fallback; }

    public boolean isAnyMissing() {
        for (Source s : sources) {
            if (!s.isExists() && s.isValid()) {
                return true;
            }
        }
        return false;
    }

    public boolean isAnyInvalid() {
        // A source served from a component-local cropped rendition is no longer
        // "invalid" for rendering — only formats still awaiting a crop block the
        // anonymous <picture>.
        for (Source s : sources) {
            if (s.isNeedsCrop()) {
                return true;
            }
        }
        return false;
    }

    public List<Source> getInvalidSources() {
        List<Source> out = new ArrayList<>();
        for (Source s : sources) {
            if (!s.isValid()) out.add(s);
        }
        return out;
    }

    public List<Source> getMissingSources() {
        List<Source> out = new ArrayList<>();
        for (Source s : sources) {
            if (!s.isExists() && s.isValid()) out.add(s);
        }
        return out;
    }

    /**
     * Formats the shared DML asset is too small for and that don't yet have a
     * component-local cropped rendition. HTL offers a focus-point editor +
     * "Generate cropped rendition" for these.
     */
    public List<Source> getCropSources() {
        List<Source> out = new ArrayList<>();
        for (Source s : sources) {
            if (s.isNeedsCrop()) out.add(s);
        }
        return out;
    }

    public boolean isAnyNeedsCrop() {
        for (Source s : sources) {
            if (s.isNeedsCrop()) return true;
        }
        return false;
    }

    /** Per-format view-data exposed to HTL. */
    public static final class Source {
        private final MediaFormat format;
        private final String media;
        private final String url;
        private final boolean exists;
        private final RenditionValidator.Result validation;
        private final boolean cropped;

        Source(MediaFormat format, String media, String url, boolean exists,
               RenditionValidator.Result validation, boolean cropped) {
            this.format = format;
            this.media = media;
            this.url = url;
            this.exists = exists;
            this.validation = validation;
            this.cropped = cropped;
        }
        public String getFormatName() { return format.getName(); }
        public String getLabel() { return format.getLabel(); }
        public int getWidth() { return format.getWidth(); }
        public int getHeight() { return format.getHeight(); }
        public double getAspectRatio() { return format.getAspectRatio(); }
        public String getMedia() { return media; }
        public String getUrl() { return url; }
        public boolean isExists() { return exists; }
        public boolean isFallback() { return media == null || media.isEmpty(); }
        public boolean isValid() { return validation == null || validation.isOk(); }
        public boolean isTooSmall() { return validation != null && validation.isTooSmall(); }
        public boolean isAspectMismatch() { return validation != null && validation.isAspectMismatch(); }
        public String getValidationMessage() { return validation == null ? "" : validation.getMessage(); }
        /** This source is served from a component-local focus-cropped rendition. */
        public boolean isCropped() { return cropped; }
        /** Invalid for the shared asset and not yet manually cropped for this component. */
        public boolean isNeedsCrop() { return !isValid() && !cropped; }
    }
}
