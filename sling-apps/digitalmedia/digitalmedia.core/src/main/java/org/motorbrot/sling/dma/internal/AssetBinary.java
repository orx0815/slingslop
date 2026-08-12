package org.motorbrot.sling.dma.internal;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Resolves the nt:resource that carries a DMA asset's original binary
 * (the inner node of the {@code jcr:content} nt:file wrapper).
 */
public final class AssetBinary {

    private AssetBinary() {
    }

    public static Node originalResource(Node assetNode) throws RepositoryException {
        // Original binary lives on the inner nt:resource of the jcr:content nt:file wrapper.
        return assetNode.getNode("jcr:content").getNode("jcr:content");
    }
}
