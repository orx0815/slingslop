package org.motorbrot.sling.dma.servlets;

import java.io.IOException;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.SlingJakartaHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingJakartaAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persists a focus point on a DML asset's {@code metadata} node.
 *
 * <p>Endpoint: {@code POST {assetPath}.focus-point.json} with form parameters
 * {@code focusX} and {@code focusY} (both in percent, range {@code 0..100}).
 *
 * <p>After saving, every rendition that gets (re-)generated for the asset uses
 * this focus point. Existing renditions are NOT regenerated automatically —
 * authors trigger that from the picker / dashboard.
 */
@Component(service = Servlet.class)
@SlingServletResourceTypes(
    resourceTypes = "motorbrot/dma/components/asset",
    methods = "POST",
    selectors = "focus-point",
    extensions = "json"
)
public class FocusPointServlet extends SlingJakartaAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(FocusPointServlet.class);

    @Override
    protected void doPost(SlingJakartaHttpServletRequest request, SlingJakartaHttpServletResponse response)
            throws ServletException, IOException {

        Double fx = parseDouble(request.getParameter("focusX"));
        Double fy = parseDouble(request.getParameter("focusY"));
        if (fx == null || fy == null) {
            response.setStatus(400);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"focusX and focusY are required (0..100)\"}");
            return;
        }
        fx = clamp(fx);
        fy = clamp(fy);

        try {
            Resource resource = request.getResource();
            Node assetNode = resource.adaptTo(Node.class);
            if (assetNode == null) {
                response.setStatus(404);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"asset not found\"}");
                return;
            }
            Node meta = assetNode.hasNode("metadata")
                    ? assetNode.getNode("metadata")
                    : assetNode.addNode("metadata", "nt:unstructured");
            meta.setProperty("dmaFocusX", fx);
            meta.setProperty("dmaFocusY", fy);

            Session session = request.getResourceResolver().adaptTo(Session.class);
            if (session != null) {
                session.save();
            }

            LOG.info("Saved focus point ({}%, {}%) on {}", fx, fy, assetNode.getPath());
            response.setStatus(200);
            response.setContentType("application/json");
            response.getWriter().write(String.format(
                    "{\"focusX\":%s,\"focusY\":%s,\"assetPath\":\"%s\"}",
                    fx, fy, assetNode.getPath()));
        } catch (Exception e) {
            LOG.error("Failed to persist focus point", e);
            response.setStatus(500);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private static Double parseDouble(String s) {
        if (s == null || s.isEmpty()) return null;
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return null; }
    }

    private static double clamp(double v) {
        if (Double.isNaN(v)) return 50d;
        return Math.max(0d, Math.min(100d, v));
    }
}
