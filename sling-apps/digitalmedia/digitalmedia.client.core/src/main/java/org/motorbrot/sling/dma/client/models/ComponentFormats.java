package org.motorbrot.sling.dma.client.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.motorbrot.sling.dma.client.MediaFormat;
import org.motorbrot.sling.dma.client.MediaFormatRegistry;

/**
 * Exposes a consuming component's configured {@link MediaFormat}s to HTL.
 *
 * <p>Reads {@code dmaFormats} (String[] of format names) from the component
 * resource and resolves each against the {@link MediaFormatRegistry}. The DML
 * picker widget uses this to render, inside the edit modal, a per-format crop
 * overlay + "Generate cropped rendition(s)" action for formats the picked image
 * is too small for. The actual too-small test happens client-side (it depends on
 * the selected image's natural size), so this model only surfaces the format
 * geometry.
 */
@Model(
    adaptables = Resource.class,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class ComponentFormats {

    @OSGiService
    private MediaFormatRegistry registry;

    @ValueMapValue
    private String[] dmaFormats;

    private final List<Fmt> formats = new ArrayList<>();

    @PostConstruct
    protected void init() {
        if (dmaFormats == null || registry == null) {
            return;
        }
        for (String name : dmaFormats) {
            if (name == null || name.isEmpty()) {
                continue;
            }
            registry.getByName(name).ifPresent(f -> formats.add(new Fmt(f)));
        }
    }

    public boolean isHasFormats() {
        return !formats.isEmpty();
    }

    public List<Fmt> getFormats() {
        return Collections.unmodifiableList(formats);
    }

    /** View-data for a single format, exposed to HTL. */
    public static final class Fmt {
        private final MediaFormat format;

        Fmt(MediaFormat format) {
            this.format = format;
        }

        public String getName() { return format.getName(); }
        public String getLabel() { return format.getLabel(); }
        public int getWidth() { return format.getWidth(); }
        public int getHeight() { return format.getHeight(); }
        public boolean isMaintainAspectRatio() { return format.isMaintainAspectRatio(); }
    }
}
