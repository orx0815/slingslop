package org.motorbrot.slingbench.slingmodels;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.motorbrot.slingbench.charts.SvgChartBuilder;
import org.motorbrot.slingbench.parser.BenchResultParser;

/**
 * Backing model for a single run-detail page
 * ({@code sling-bench/pages/rundetail}). Adapted directly from the
 * {@code jcr:content} resource that carries the metrics persisted by
 * {@code BenchmarkRunUploadServlet} — no child components needed, this model
 * reads its own resource's properties.
 */
@Model(adaptables = Resource.class)
public class RunDetailModel {

  private static final String COLOR_PRIMARY = "var(--color-primary, #22d3ee)";
  private static final String COLOR_SECONDARY = "var(--color-secondary, #f59e0b)";

  @Self
  private Resource resource;

  private ValueMap properties;

  private String label;
  private String uploadedBy;
  private String uploadedAtFormatted;
  private String tool;
  private Long delayMs;
  private String duration;
  private boolean httpVirtualThreads;
  private String baseUrl;

  private List<String> concurrencyLabels;
  private List<Row> rows;

  private boolean hasCpuData;

  @PostConstruct
  protected void init() {
    properties = resource.getValueMap();

    label = properties.get("jcr:title", resource.getName());
    uploadedBy = properties.get("uploadedBy", "unknown");
    tool = properties.get("tool", "unknown");
    delayMs = properties.get("delayMs", Long.class);
    duration = properties.get("duration", "");
    httpVirtualThreads = properties.get("httpVirtualThreads", Boolean.FALSE);
    baseUrl = properties.get("baseUrl", "");

    Calendar uploadedAt = properties.get("uploadedAt", Calendar.class);
    uploadedAtFormatted = uploadedAt == null ? "unknown"
        : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(uploadedAt.getTime());

    Long[] levels = properties.get("concurrencyLevels", Long[].class);
    Double[] totalRps = properties.get("totalRps", Double[].class);
    Double[] avgLatencyMs = properties.get("avgLatencyMs", Double[].class);
    Double[] p99Ms = properties.get("p99Ms", Double[].class);

    levels = levels == null ? new Long[0] : levels;
    totalRps = totalRps == null ? new Double[0] : totalRps;
    avgLatencyMs = avgLatencyMs == null ? new Double[0] : avgLatencyMs;
    p99Ms = p99Ms == null ? new Double[0] : p99Ms;

    concurrencyLabels = new ArrayList<>();
    rows = new ArrayList<>();
    for (int i = 0; i < levels.length; i++) {
      String levelLabel = String.valueOf(levels[i]);
      concurrencyLabels.add(levelLabel);
      double rps = i < totalRps.length ? totalRps[i] : 0;
      double avg = i < avgLatencyMs.length ? avgLatencyMs[i] : 0;
      double p99 = i < p99Ms.length ? p99Ms[i] : 0;
      rows.add(new Row(levelLabel, BenchResultParser.formatNumber(rps),
          BenchResultParser.formatNumber(avg), BenchResultParser.formatNumber(p99)));
    }

    hasCpuData = properties.get("hasCpuData", Boolean.FALSE);
  }

  public String getLabel() {
    return label;
  }

  public String getUploadedBy() {
    return uploadedBy;
  }

  public String getUploadedAtFormatted() {
    return uploadedAtFormatted;
  }

  public String getTool() {
    return tool;
  }

  public Long getDelayMs() {
    return delayMs;
  }

  public String getDuration() {
    return duration;
  }

  public boolean isHttpVirtualThreads() {
    return httpVirtualThreads;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public List<Row> getRows() {
    return rows;
  }

  public boolean isEmpty() {
    return rows.isEmpty();
  }

  public String getRpsChartSvg() {
    return SvgChartBuilder.barChart(concurrencyLabels, doubles("totalRps"), 480, 220, COLOR_PRIMARY, " req/s");
  }

  public String getAvgLatencyChartSvg() {
    return SvgChartBuilder.lineChart(concurrencyLabels, doubles("avgLatencyMs"), 480, 180, COLOR_SECONDARY);
  }

  public String getP99LatencyChartSvg() {
    return SvgChartBuilder.lineChart(concurrencyLabels, doubles("p99Ms"), 480, 180, "var(--color-danger, #f87171)");
  }

  public boolean isHasCpuData() {
    return hasCpuData;
  }

  public String getCpuPercentChartSvg() {
    if (!hasCpuData) {
      return null;
    }
    List<String> timestamps = strings("cpuTimestamps");
    return SvgChartBuilder.lineChart(timestamps, doubles("cpuPercent"), 480, 160, COLOR_PRIMARY);
  }

  public String getRssChartSvg() {
    if (!hasCpuData) {
      return null;
    }
    List<String> timestamps = strings("cpuTimestamps");
    return SvgChartBuilder.lineChart(timestamps, doubles("cpuRssMb"), 480, 160, COLOR_SECONDARY);
  }

  public String getRawResultText() {
    return properties.get("rawResultText", "");
  }

  public String getRawCpuCsv() {
    return properties.get("rawCpuCsv", "");
  }

  private List<Double> doubles(String propertyName) {
    Double[] values = properties.get(propertyName, Double[].class);
    List<Double> list = new ArrayList<>();
    if (values == null) {
      return list;
    }
    for (Double value : values) {
      list.add(value == null ? Double.valueOf(0) : value);
    }
    return list;
  }

  private List<String> strings(String propertyName) {
    String[] values = properties.get(propertyName, String[].class);
    List<String> list = new ArrayList<>();
    if (values == null) {
      return list;
    }
    for (String value : values) {
      list.add(value);
    }
    return list;
  }

  /** One table row: a concurrency level and its aggregate metrics, pre-formatted. */
  public static class Row {
    private final String concurrency;
    private final String totalRps;
    private final String avgLatencyMs;
    private final String p99Ms;

    Row(String concurrency, String totalRps, String avgLatencyMs, String p99Ms) {
      this.concurrency = concurrency;
      this.totalRps = totalRps;
      this.avgLatencyMs = avgLatencyMs;
      this.p99Ms = p99Ms;
    }

    public String getConcurrency() {
      return concurrency;
    }

    public String getTotalRps() {
      return totalRps;
    }

    public String getAvgLatencyMs() {
      return avgLatencyMs;
    }

    public String getP99Ms() {
      return p99Ms;
    }
  }
}
