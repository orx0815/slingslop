package org.motorbrot.slingbench.servlets;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.Servlet;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.SlingJakartaHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingJakartaAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.motorbrot.slingbench.SlingBenchConstants;
import org.motorbrot.slingbench.parser.BenchResult;
import org.motorbrot.slingbench.parser.BenchResultParser;
import org.motorbrot.slingbench.parser.ConcurrencyMetric;
import org.motorbrot.slingbench.parser.CpuSample;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persists an uploaded {@code vt-bench.sh} run.
 *
 * <p>Accepts a multipart POST with:</p>
 * <ul>
 *   <li>{@code label} — optional free-text name for the run</li>
 *   <li>{@code resultFile} — required, the {@code vt-bench-*.txt} output</li>
 *   <li>{@code cpuFile} — optional, the {@code cpu-*.csv} sampler output</li>
 * </ul>
 *
 * <p>Requires an authenticated (non-anonymous) user. On success, creates a
 * page node under {@link SlingBenchConstants#RUNS_ROOT} and redirects to its
 * rendered run-detail view.</p>
 */
@Component(service = Servlet.class)
@SlingServletPaths("/bin/slingbench/upload")
public class BenchmarkRunUploadServlet extends SlingJakartaAllMethodsServlet {

  private static final long serialVersionUID = 1L;

  private static final Logger LOG = LoggerFactory.getLogger(BenchmarkRunUploadServlet.class);

  /** Defensive upload cap — this is an internal tool, not a public file store. */
  private static final long MAX_UPLOAD_BYTES = 5_000_000L;

  @Override
  protected void doPost(SlingJakartaHttpServletRequest request, SlingJakartaHttpServletResponse response)
      throws IOException {

    ResourceResolver resolver = request.getResourceResolver();
    String userId = resolver.getUserID();
    if (userId == null || "anonymous".equals(userId)) {
      response.sendError(403, "Log in to upload a benchmark run.");
      return;
    }

    RequestParameter resultFileParam = request.getRequestParameter("resultFile");
    if (resultFileParam == null || StringUtils.isBlank(resultFileParam.getFileName())) {
      response.sendError(400, "Please choose a vt-bench result file (.txt).");
      return;
    }
    if (resultFileParam.getSize() > MAX_UPLOAD_BYTES) {
      response.sendError(413, "Result file is too large (max 5 MB).");
      return;
    }

    String rawText = new String(resultFileParam.get(), StandardCharsets.UTF_8);
    BenchResult result = BenchResultParser.parse(rawText);

    RequestParameter cpuFileParam = request.getRequestParameter("cpuFile");
    boolean hasCpuData = cpuFileParam != null && !StringUtils.isBlank(cpuFileParam.getFileName());
    String rawCpuCsv = null;
    List<CpuSample> cpuSamples = null;
    if (hasCpuData) {
      if (cpuFileParam.getSize() > MAX_UPLOAD_BYTES) {
        response.sendError(413, "CPU sampler file is too large (max 5 MB).");
        return;
      }
      rawCpuCsv = new String(cpuFileParam.get(), StandardCharsets.UTF_8);
      cpuSamples = BenchResultParser.parseCpuCsv(rawCpuCsv);
    }

    String label = StringUtils.defaultIfBlank(request.getParameter("label"),
        "Run " + new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT).format(Calendar.getInstance().getTime()));

    try {
      String runPath = persistRun(resolver, label, result, rawText, hasCpuData, rawCpuCsv, cpuSamples);
      resolver.commit();
      response.sendRedirect(runPath + ".html");
    } catch (PersistenceException e) {
      LOG.error("Failed to persist benchmark run upload", e);
      response.sendError(500, "Could not save the run — see server log for details.");
    }
  }

  private String persistRun(ResourceResolver resolver, String label, BenchResult result, String rawText,
      boolean hasCpuData, String rawCpuCsv, List<CpuSample> cpuSamples) throws PersistenceException {

    Resource runsRoot = resolver.getResource(SlingBenchConstants.RUNS_ROOT);
    if (runsRoot == null) {
      throw new PersistenceException(SlingBenchConstants.RUNS_ROOT + " does not exist — check launcher repoinit");
    }

    String runId = uniqueRunId(resolver, runsRoot, label);

    Map<String, Object> folderProps = new HashMap<>();
    folderProps.put("jcr:primaryType", "sling:Folder");
    folderProps.put("sling:resourceType", SlingBenchConstants.PAGE_RESOURCE_TYPE);
    Resource runFolder = resolver.create(runsRoot, runId, folderProps);

    List<ConcurrencyMetric> metrics = result.getConcurrencyMetrics();
    Long[] levels = new Long[metrics.size()];
    Double[] totalRps = new Double[metrics.size()];
    Double[] avgLatencyMs = new Double[metrics.size()];
    Double[] p99Ms = new Double[metrics.size()];
    for (int i = 0; i < metrics.size(); i++) {
      ConcurrencyMetric metric = metrics.get(i);
      levels[i] = Long.valueOf(metric.getConcurrency());
      totalRps[i] = Double.valueOf(metric.getTotalRequestsPerSec());
      avgLatencyMs[i] = Double.valueOf(metric.getAvgLatencyMs());
      p99Ms[i] = Double.valueOf(metric.getP99LatencyMs());
    }

    Map<String, Object> contentProps = new HashMap<>();
    contentProps.put("jcr:primaryType", "nt:unstructured");
    contentProps.put("sling:resourceType", SlingBenchConstants.RUNDETAIL_RESOURCE_TYPE);
    contentProps.put("jcr:title", label);
    contentProps.put("uploadedBy", resolver.getUserID());
    contentProps.put("uploadedAt", Calendar.getInstance());
    contentProps.put("tool", StringUtils.defaultString(result.getTool(), "unknown"));
    if (result.getDelayMs() != null) {
      contentProps.put("delayMs", Long.valueOf(result.getDelayMs()));
    }
    contentProps.put("duration", StringUtils.defaultString(result.getDuration(), ""));
    contentProps.put("httpVirtualThreads", Boolean.TRUE.equals(result.getHttpVirtualThreads()));
    contentProps.put("baseUrl", StringUtils.defaultString(result.getBaseUrl(), ""));
    contentProps.put("concurrencyLevels", levels);
    contentProps.put("totalRps", totalRps);
    contentProps.put("avgLatencyMs", avgLatencyMs);
    contentProps.put("p99Ms", p99Ms);
    contentProps.put("rawResultText", rawText);
    contentProps.put("hasCpuData", hasCpuData);

    if (hasCpuData && cpuSamples != null && !cpuSamples.isEmpty()) {
      String[] cpuTimestamps = new String[cpuSamples.size()];
      Double[] cpuPercent = new Double[cpuSamples.size()];
      Double[] cpuRssMb = new Double[cpuSamples.size()];
      for (int i = 0; i < cpuSamples.size(); i++) {
        CpuSample sample = cpuSamples.get(i);
        cpuTimestamps[i] = sample.getTimestamp();
        cpuPercent[i] = Double.valueOf(sample.getCpuPercent());
        cpuRssMb[i] = Double.valueOf(sample.getRssMb());
      }
      contentProps.put("cpuTimestamps", cpuTimestamps);
      contentProps.put("cpuPercent", cpuPercent);
      contentProps.put("cpuRssMb", cpuRssMb);
      contentProps.put("rawCpuCsv", rawCpuCsv);
    }

    resolver.create(runFolder, "jcr:content", contentProps);

    return runFolder.getPath();
  }

  private static String uniqueRunId(ResourceResolver resolver, Resource runsRoot, String label) {
    String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(Calendar.getInstance().getTime());
    String slug = slugify(label);
    String base = StringUtils.isBlank(slug) ? timestamp : timestamp + "-" + slug;
    String candidate = base;
    int suffix = 2;
    while (resolver.getResource(runsRoot.getPath() + "/" + candidate) != null) {
      candidate = base + "-" + suffix;
      suffix++;
    }
    return candidate;
  }

  private static String slugify(String value) {
    if (value == null) {
      return "";
    }
    String slug = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
    slug = slug.replaceAll("^-+|-+$", "");
    return slug.length() > 40 ? slug.substring(0, 40) : slug;
  }
}
