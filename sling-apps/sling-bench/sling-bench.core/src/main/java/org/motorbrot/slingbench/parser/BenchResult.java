package org.motorbrot.slingbench.parser;

import java.util.List;

/**
 * Fully parsed vt-bench.sh run: the header metadata plus one
 * {@link ConcurrencyMetric} per tested concurrency level, in ascending order.
 */
public class BenchResult {

  private final String tool;
  private final String baseUrl;
  private final Integer delayMs;
  private final String duration;
  private final Boolean httpVirtualThreads;
  private final List<ConcurrencyMetric> concurrencyMetrics;

  public BenchResult(String tool, String baseUrl, Integer delayMs, String duration,
      Boolean httpVirtualThreads, List<ConcurrencyMetric> concurrencyMetrics) {
    this.tool = tool;
    this.baseUrl = baseUrl;
    this.delayMs = delayMs;
    this.duration = duration;
    this.httpVirtualThreads = httpVirtualThreads;
    this.concurrencyMetrics = concurrencyMetrics;
  }

  public String getTool() {
    return tool;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public Integer getDelayMs() {
    return delayMs;
  }

  public String getDuration() {
    return duration;
  }

  public Boolean getHttpVirtualThreads() {
    return httpVirtualThreads;
  }

  public List<ConcurrencyMetric> getConcurrencyMetrics() {
    return concurrencyMetrics;
  }
}
