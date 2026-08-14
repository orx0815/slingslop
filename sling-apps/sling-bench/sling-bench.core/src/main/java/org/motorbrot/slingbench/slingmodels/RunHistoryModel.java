package org.motorbrot.slingbench.slingmodels;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.PostConstruct;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.motorbrot.slingbench.SlingBenchConstants;
import org.motorbrot.slingbench.charts.SvgChartBuilder;
import org.motorbrot.slingbench.parser.BenchResultParser;

/**
 * Backing model for the {@code run-history} component: lists every uploaded
 * run under {@link SlingBenchConstants#RUNS_ROOT}, newest first, and builds a
 * virtual-thread-vs-platform-thread comparison chart across all of them.
 */
@Model(adaptables = Resource.class)
public class RunHistoryModel {

  private static final String COLOR_VT = "var(--color-primary, #22d3ee)";
  private static final String COLOR_PT = "var(--color-secondary, #f59e0b)";

  @Self
  private Resource resource;

  private List<RunSummary> runs;
  private String comparisonChartSvg;

  @PostConstruct
  protected void init() {
    runs = new ArrayList<>();
    ResourceResolver resolver = resource.getResourceResolver();
    Resource runsRoot = resolver.getResource(SlingBenchConstants.RUNS_ROOT);

    Map<Long, List<Double>> vtByConcurrency = new TreeMap<>();
    Map<Long, List<Double>> ptByConcurrency = new TreeMap<>();

    if (runsRoot != null) {
      for (Resource runFolder : runsRoot.getChildren()) {
        Resource content = runFolder.getChild("jcr:content");
        if (content == null) {
          continue;
        }
        ValueMap vm = content.getValueMap();

        String label = vm.get("jcr:title", runFolder.getName());
        String tool = vm.get("tool", "unknown");
        Boolean vt = vm.get("httpVirtualThreads", Boolean.FALSE);
        Calendar uploadedAt = vm.get("uploadedAt", Calendar.class);
        Long[] levels = vm.get("concurrencyLevels", Long[].class);
        Double[] totalRps = vm.get("totalRps", Double[].class);

        double peakRps = 0;
        if (totalRps != null) {
          for (Double rps : totalRps) {
            if (rps != null && rps > peakRps) {
              peakRps = rps;
            }
          }
        }

        if (levels != null && totalRps != null) {
          Map<Long, List<Double>> bucket = Boolean.TRUE.equals(vt) ? vtByConcurrency : ptByConcurrency;
          for (int i = 0; i < levels.length && i < totalRps.length; i++) {
            bucket.computeIfAbsent(levels[i], k -> new ArrayList<>()).add(totalRps[i]);
          }
        }

        String uploadedAtFormatted = uploadedAt == null ? "unknown"
            : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(uploadedAt.getTime());

        runs.add(new RunSummary(runFolder.getName(), label, uploadedAtFormatted,
            uploadedAt, tool, Boolean.TRUE.equals(vt), BenchResultParser.formatNumber(peakRps),
            runFolder.getPath() + ".html"));
      }
    }

    runs.sort(Comparator.comparing(RunSummary::getUploadedAtSortKey,
        Comparator.nullsLast(Comparator.<Calendar>reverseOrder())));

    comparisonChartSvg = buildComparisonChart(vtByConcurrency, ptByConcurrency);
  }

  private String buildComparisonChart(Map<Long, List<Double>> vtByConcurrency, Map<Long, List<Double>> ptByConcurrency) {
    if (vtByConcurrency.isEmpty() || ptByConcurrency.isEmpty()) {
      return null;
    }
    TreeMap<Long, List<Double>> merged = new TreeMap<>(vtByConcurrency);
    merged.putAll(ptByConcurrency);

    List<String> labels = new ArrayList<>();
    List<Double> vtAverages = new ArrayList<>();
    List<Double> ptAverages = new ArrayList<>();
    for (Long level : merged.keySet()) {
      labels.add(String.valueOf(level));
      vtAverages.add(average(vtByConcurrency.get(level)));
      ptAverages.add(average(ptByConcurrency.get(level)));
    }

    return SvgChartBuilder.dualBarChart(labels, vtAverages, ptAverages,
        "virtual threads", "platform threads", 560, 240, COLOR_VT, COLOR_PT);
  }

  private static double average(List<Double> values) {
    if (values == null || values.isEmpty()) {
      return 0;
    }
    double sum = 0;
    for (Double value : values) {
      sum += value;
    }
    return sum / values.size();
  }

  public List<RunSummary> getRuns() {
    return runs;
  }

  public boolean isEmpty() {
    return runs.isEmpty();
  }

  public String getComparisonChartSvg() {
    return comparisonChartSvg;
  }

  public boolean isHasComparisonChart() {
    return comparisonChartSvg != null;
  }

  /** One row of the run-history table. */
  public static class RunSummary {
    private final String id;
    private final String label;
    private final String uploadedAtFormatted;
    private final Calendar uploadedAtSortKey;
    private final String tool;
    private final boolean httpVirtualThreads;
    private final String peakRps;
    private final String url;

    RunSummary(String id, String label, String uploadedAtFormatted, Calendar uploadedAtSortKey,
        String tool, boolean httpVirtualThreads, String peakRps, String url) {
      this.id = id;
      this.label = label;
      this.uploadedAtFormatted = uploadedAtFormatted;
      this.uploadedAtSortKey = uploadedAtSortKey;
      this.tool = tool;
      this.httpVirtualThreads = httpVirtualThreads;
      this.peakRps = peakRps;
      this.url = url;
    }

    public String getId() {
      return id;
    }

    public String getLabel() {
      return label;
    }

    public String getUploadedAtFormatted() {
      return uploadedAtFormatted;
    }

    Calendar getUploadedAtSortKey() {
      return uploadedAtSortKey;
    }

    public String getTool() {
      return tool;
    }

    public boolean isHttpVirtualThreads() {
      return httpVirtualThreads;
    }

    public String getPeakRps() {
      return peakRps;
    }

    public String getUrl() {
      return url;
    }
  }
}
