package org.motorbrot.sling.dma.services.formats;

import org.motorbrot.sling.dma.client.MediaFormat;
import org.motorbrot.sling.dma.client.SimpleMediaFormat;
import org.osgi.service.component.annotations.Component;

/** Default 300x300 preview thumbnail used by the DML asset grid. */
@Component(service = MediaFormat.class)
public class PreviewMediaFormat extends SimpleMediaFormat {
    public PreviewMediaFormat() {
        super("preview", 300, 300, "Preview", "Preview thumbnail for asset grid", true);
    }
}
