package org.motorbrot.sling.dma.servlets;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingJakartaPostProcessor;
import org.motorbrot.sling.dma.client.MediaFormat;
import org.motorbrot.sling.dma.client.MediaFormatRegistry;
import org.motorbrot.sling.dma.client.RenditionValidator;
import org.motorbrot.sling.dma.client.crop.FocusPoint;
import org.motorbrot.sling.dma.client.crop.SimpleFocusPoint;
import org.motorbrot.sling.dma.services.RenditionService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps component-local, focus-cropped renditions in sync with the component's
 * {@code fileReference} and focus point.
 *
 * <p>Runs on the standard Sling POST that saves a consuming component (e.g. a
 * {@code feature-image} node's edit form). Whenever the POST touches
 * {@code fileReference}, {@code dmaFocusX} or {@code dmaFocusY}, any previously
 * generated {@code {component}/renditions/*_cropped} binaries are stale: they
 * were cropped from the <em>old</em> image and/or focus. This processor:
 * <ol>
 *   <li>removes the component's {@code renditions} node (dropping stale crops), and</li>
 *   <li>regenerates a fresh cropped rendition for every configured
 *       {@link MediaFormat} the (new) source image is too small for
 *       (per {@link RenditionValidator}), using the (new) focus point.</li>
 * </ol>
 *
 * <p>Formats the source can satisfy directly get no {@code _cropped} rendition —
 * the responsive-image model serves those from the shared DML asset instead.
 *
 * <p>This mirrors {@link CropRenditionServlet} (the explicit editor action) but
 * fires automatically on save, so changing the picked image regenerates crops
 * without the old ones lingering.
 */
@Component(service = SlingJakartaPostProcessor.class)
public class CropRenditionPostProcessor implements SlingJakartaPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(CropRenditionPostProcessor.class);
    private static final String CROPPED_SUFFIX = "_cropped";
    private static final String RENDITIONS_NODE = "renditions";

    @Reference
    private RenditionService renditionService;

    @Reference
    private MediaFormatRegistry mediaFormatRegistry;

    @Override
    public void process(SlingJakartaHttpServletRequest request, List<Modification> modifications)
            throws Exception {

        if (!touchesImageOrFocus(modifications)) {
            return;
        }

        Resource component = request.getResource();
        if (component == null) {
            return;
        }
        ValueMap props = component.getValueMap();
        String fileReference = props.get("fileReference", String.class);
        if (fileReference == null || fileReference.isEmpty()) {
            return;
        }

        Node componentNode = component.adaptTo(Node.class);
        if (componentNode == null) {
            return;
        }

        // 1) Drop stale cropped renditions (from the previous image/focus).
        if (componentNode.hasNode(RENDITIONS_NODE)) {
            componentNode.getNode(RENDITIONS_NODE).remove();
        }

        // 2) Regenerate for formats the (new) source is too small for.
        ResourceResolver resolver = request.getResourceResolver();
        Resource asset = resolver.getResource(fileReference);
        if (asset == null) {
            LOG.debug("crop-sync: referenced asset {} not found; stale crops cleared", fileReference);
            return;
        }
        Node assetNode = asset.adaptTo(Node.class);
        if (assetNode == null || !assetNode.hasNode("jcr:content")) {
            return;
        }

        String[] formatNames = props.get("dmaFormats", String[].class);
        if (formatNames == null || formatNames.length == 0) {
            return;
        }

        int[] dims = readSourceDimensions(asset);
        int sourceWidth = dims[0];
        int sourceHeight = dims[1];
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            LOG.debug("crop-sync: unknown source dimensions for {}; skipping regeneration", fileReference);
            return;
        }

        FocusPoint focus = readFocus(props);
        Session session = resolver.adaptTo(Session.class);
        if (session == null) {
            return;
        }

        int generated = 0;
        for (String name : formatNames) {
            if (name == null || name.isEmpty()) {
                continue;
            }
            Optional<MediaFormat> fmtOpt = mediaFormatRegistry.getByName(name);
            if (!fmtOpt.isPresent()) {
                continue;
            }
            MediaFormat fmt = fmtOpt.get();
            // Only formats the shared asset cannot satisfy need a component crop.
            if (RenditionValidator.validate(sourceWidth, sourceHeight, fmt).isOk()) {
                continue;
            }
            InputStream originalData = assetNode.getNode("jcr:content")
                    .getProperty("jcr:data").getBinary().getStream();
            byte[] data = renditionService.generateCroppedRendition(originalData, fmt, focus);
            if (data == null) {
                continue;
            }
            storeRendition(session, componentNode, name + CROPPED_SUFFIX, data);
            generated++;
        }

        LOG.info("crop-sync: regenerated {} cropped rendition(s) under {} focus=({}%, {}%)",
                generated, componentNode.getPath(), focus.getXPercent(), focus.getYPercent());
    }

    private static boolean touchesImageOrFocus(List<Modification> modifications) {
        if (modifications == null) {
            return false;
        }
        for (Modification m : modifications) {
            String src = m.getSource();
            if (src == null) {
                continue;
            }
            if (src.endsWith("/fileReference")
                    || src.endsWith("/dmaFocusX")
                    || src.endsWith("/dmaFocusY")) {
                return true;
            }
        }
        return false;
    }

    private static int[] readSourceDimensions(Resource asset) {
        Resource meta = asset.getChild("metadata");
        if (meta != null) {
            ValueMap vm = meta.getValueMap();
            int w = vm.get("width", 0);
            int h = vm.get("height", 0);
            if (w > 0 && h > 0) {
                return new int[] { w, h };
            }
        }
        Node n = asset.adaptTo(Node.class);
        if (n != null) {
            try {
                if (n.hasNode("metadata")) {
                    Node m = n.getNode("metadata");
                    int w = m.hasProperty("width") ? (int) m.getProperty("width").getLong() : 0;
                    int h = m.hasProperty("height") ? (int) m.getProperty("height").getLong() : 0;
                    return new int[] { w, h };
                }
            } catch (Exception e) {
                LOG.debug("crop-sync: could not read dimensions of {}", asset.getPath(), e);
            }
        }
        return new int[] { 0, 0 };
    }

    private static FocusPoint readFocus(ValueMap props) {
        Double fx = props.get("dmaFocusX", Double.class);
        Double fy = props.get("dmaFocusY", Double.class);
        if (fx == null || fy == null) {
            return FocusPoint.CENTER;
        }
        return new SimpleFocusPoint(fx, fy);
    }

    private static void storeRendition(Session session, Node componentNode, String nodeName, byte[] data)
            throws Exception {
        Node renditionsNode = componentNode.hasNode(RENDITIONS_NODE)
                ? componentNode.getNode(RENDITIONS_NODE)
                : componentNode.addNode(RENDITIONS_NODE, "nt:unstructured");

        Node renditionNode = renditionsNode.hasNode(nodeName)
                ? renditionsNode.getNode(nodeName)
                : renditionsNode.addNode(nodeName, "nt:file");

        Node content = renditionNode.hasNode("jcr:content")
                ? renditionNode.getNode("jcr:content")
                : renditionNode.addNode("jcr:content", "nt:resource");

        content.setProperty("jcr:data", session.getValueFactory().createBinary(
                new ByteArrayInputStream(data)));
        content.setProperty("jcr:mimeType", "image/jpeg");
        content.setProperty("jcr:lastModified", Calendar.getInstance());
    }
}
