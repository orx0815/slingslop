package org.motorbrot.sling.dma.slingmodels;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.motorbrot.sling.dma.models.MediaFormat;
import org.motorbrot.sling.dma.services.MediaFormatService;

/**
 * Sling Model for rendering a single rendition item.
 * Reads the format name from the "format" request parameter.
 */
@Model(adaptables = SlingJakartaHttpServletRequest.class)
public class RenditionItemModel {

    @SlingObject
    private SlingJakartaHttpServletRequest request;

    @SlingObject
    private Resource resource;

    @OSGiService
    private MediaFormatService mediaFormatService;

    private String formatName;
    private int width;
    private int height;
    private String downloadPath;

    @PostConstruct
    protected void init() {
        formatName = (String) request.getAttribute("renditionFormat");
        if (formatName != null) {
            MediaFormat fmt = mediaFormatService.getFormat(formatName).orElse(null);
            if (fmt != null) {
                width = fmt.getWidth();
                height = fmt.getHeight();
            }
        }
        downloadPath = resource.getPath() + "/renditions/" + formatName;
    }

    public String getFormatName() {
        return formatName;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getDownloadPath() {
        return downloadPath;
    }
}
