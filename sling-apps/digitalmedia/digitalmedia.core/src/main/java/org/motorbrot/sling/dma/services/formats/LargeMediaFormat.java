package org.motorbrot.sling.dma.services.formats;

import org.motorbrot.sling.dma.client.MediaFormat;
import org.motorbrot.sling.dma.client.SimpleMediaFormat;
import org.osgi.service.component.annotations.Component;

/** 1200x1200 large display rendition. */
@Component(service = MediaFormat.class)
public class LargeMediaFormat extends SimpleMediaFormat {
    public LargeMediaFormat() {
        super("large", 1200, 1200, "Large", "Large display format", true);
    }
}
