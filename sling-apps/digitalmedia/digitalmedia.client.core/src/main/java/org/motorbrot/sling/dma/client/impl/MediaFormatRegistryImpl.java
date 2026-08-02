package org.motorbrot.sling.dma.client.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.motorbrot.sling.dma.client.MediaFormat;
import org.motorbrot.sling.dma.client.MediaFormatRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = MediaFormatRegistry.class, immediate = true)
public class MediaFormatRegistryImpl implements MediaFormatRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(MediaFormatRegistryImpl.class);

    private final Map<Long, Entry> entries = new ConcurrentHashMap<>();

    @Reference(
        service = MediaFormat.class,
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC
    )
    protected void bindFormat(MediaFormat format, ServiceReference<MediaFormat> ref) {
        if (format == null || format.getName() == null || format.getName().isEmpty()) {
            LOG.warn("Ignoring MediaFormat service without a name: {}", ref);
            return;
        }
        Long id = (Long) ref.getProperty(Constants.SERVICE_ID);
        String provider = providerLabel(ref.getBundle());
        entries.put(id, new Entry(format, provider));
        LOG.info("Registered MediaFormat '{}' ({}x{}) from {}",
                format.getName(), format.getWidth(), format.getHeight(), provider);
    }

    protected void unbindFormat(MediaFormat format, ServiceReference<MediaFormat> ref) {
        Long id = (Long) ref.getProperty(Constants.SERVICE_ID);
        Entry removed = entries.remove(id);
        if (removed != null) {
            LOG.info("Unregistered MediaFormat '{}'", removed.format.getName());
        }
    }

    @Override
    public Collection<MediaFormat> getAll() {
        List<MediaFormat> out = new ArrayList<>();
        for (Entry e : entries.values()) {
            out.add(e.format);
        }
        out.sort(Comparator.comparing((MediaFormat f) -> f.getName()));
        return Collections.unmodifiableList(out);
    }

    @Override
    public Optional<MediaFormat> getByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        for (Entry e : entries.values()) {
            if (name.equals(e.format.getName())) {
                return Optional.of(e.format);
            }
        }
        return Optional.empty();
    }

    @Override
    public Map<String, List<MediaFormat>> getByProvider() {
        Map<String, List<MediaFormat>> grouped = new TreeMap<>();
        for (Entry e : entries.values()) {
            grouped.computeIfAbsent(e.provider, p -> new ArrayList<>()).add(e.format);
        }
        Map<String, List<MediaFormat>> out = new LinkedHashMap<>();
        for (Map.Entry<String, List<MediaFormat>> g : grouped.entrySet()) {
            List<MediaFormat> sorted = new ArrayList<>(g.getValue());
            sorted.sort(Comparator.comparing(MediaFormat::getName));
            out.put(g.getKey(), Collections.unmodifiableList(sorted));
        }
        return Collections.unmodifiableMap(out);
    }

    private static String providerLabel(Bundle bundle) {
        if (bundle == null) {
            return "(unknown)";
        }
        String name = bundle.getHeaders().get(Constants.BUNDLE_NAME);
        if (name != null && !name.isEmpty()) {
            return name;
        }
        return bundle.getSymbolicName();
    }

    private static final class Entry {
        final MediaFormat format;
        final String provider;

        Entry(MediaFormat format, String provider) {
            this.format = format;
            this.provider = provider;
        }
    }
}
