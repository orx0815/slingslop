package org.motorbrot.sling.dma.services.formats;

import org.motorbrot.sling.dma.client.MediaFormat;
import org.motorbrot.sling.dma.client.SimpleMediaFormat;
import org.osgi.service.component.annotations.Component;

/** 800x600 web format (non aspect-preserving). */
@Component(service = MediaFormat.class)
public class WebMediaFormat extends SimpleMediaFormat {
    public WebMediaFormat() {
        super("web", 800, 600, "Web", "Web optimized format", false);
    }
}
