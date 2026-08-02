package org.motorbrot.sling.dma.slingmodels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.motorbrot.sling.dma.client.MediaFormat;
import org.motorbrot.sling.dma.client.MediaFormatRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sling Model backing the {@code renditions} selector on the asset component.
 * Lists existing renditions with their format dimensions.
 */
@Model(adaptables = SlingJakartaHttpServletRequest.class)
public class RenditionListModel {

    private static final Logger LOG = LoggerFactory.getLogger(RenditionListModel.class);

    @SlingObject
    private Resource resource;

    @OSGiService
    private MediaFormatRegistry mediaFormatRegistry;

    private List<RenditionItem> renditions;

    @PostConstruct
    protected void init() {
        renditions = new ArrayList<>();
        try {
            Node assetNode = resource.adaptTo(Node.class);
            if (assetNode != null && assetNode.hasNode("renditions")) {
                NodeIterator iter = assetNode.getNode("renditions").getNodes();
                while (iter.hasNext()) {
                    String name = iter.nextNode().getName();
                    Optional<MediaFormat> fmt = mediaFormatRegistry.getByName(name);
                    int w = fmt.map(MediaFormat::getWidth).orElse(0);
                    int h = fmt.map(MediaFormat::getHeight).orElse(0);
                    String downloadPath = resource.getPath() + "/renditions/" + name;
                    renditions.add(new RenditionItem(name, w, h, downloadPath));
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not list renditions", e);
        }
    }

    public List<RenditionItem> getRenditions() {
        return Collections.unmodifiableList(renditions);
    }

    public boolean isEmpty() {
        return renditions.isEmpty();
    }

    public static class RenditionItem {
        private final String formatName;
        private final int width;
        private final int height;
        private final String downloadPath;

        RenditionItem(String formatName, int width, int height, String downloadPath) {
            this.formatName = formatName;
            this.width = width;
            this.height = height;
            this.downloadPath = downloadPath;
        }

        public String getFormatName() { return formatName; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public String getDownloadPath() { return downloadPath; }
    }
}
