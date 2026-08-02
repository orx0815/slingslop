package org.motorbrot.slingmatrix.mediaformats;

import org.motorbrot.sling.dma.client.MediaFormat;
import org.motorbrot.sling.dma.client.SimpleMediaFormat;
import org.osgi.service.component.annotations.Component;

/** Feature image — desktop / tablet, 4:3 landscape, 1200×900. */
@Component(service = MediaFormat.class)
public class MatrixFeatureDesktopMediaFormat extends SimpleMediaFormat {
    public MatrixFeatureDesktopMediaFormat() {
        super("matrix-feature-desktop", 1200, 900,
                "Feature \u2014 desktop (4:3)",
                "Feature image rendition for desktop/tablet viewports", false);
    }
}
