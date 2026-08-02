package org.motorbrot.slingmatrix.mediaformats;

import org.motorbrot.sling.dma.client.MediaFormat;
import org.motorbrot.sling.dma.client.SimpleMediaFormat;
import org.osgi.service.component.annotations.Component;

/** Feature image — mobile, 1:1 square, 600×600. */
@Component(service = MediaFormat.class)
public class MatrixFeatureMobileMediaFormat extends SimpleMediaFormat {
    public MatrixFeatureMobileMediaFormat() {
        super("matrix-feature-mobile", 600, 600,
                "Feature \u2014 mobile (1:1)",
                "Feature image rendition for mobile viewports", false);
    }
}
