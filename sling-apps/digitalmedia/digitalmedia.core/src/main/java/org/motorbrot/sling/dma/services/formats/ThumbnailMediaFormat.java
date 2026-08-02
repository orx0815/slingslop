package org.motorbrot.sling.dma.services.formats;

import org.motorbrot.sling.dma.client.MediaFormat;
import org.motorbrot.sling.dma.client.SimpleMediaFormat;
import org.osgi.service.component.annotations.Component;

/** 150x150 thumbnail. */
@Component(service = MediaFormat.class)
public class ThumbnailMediaFormat extends SimpleMediaFormat {
    public ThumbnailMediaFormat() {
        super("thumbnail", 150, 150, "Thumbnail", "Small thumbnail", true);
    }
}
