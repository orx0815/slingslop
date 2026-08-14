package org.motorbrot.slingbench.parser;

/**
 * One row of a parsed benchmark result: aggregate metrics for a single
 * concurrency level, summed/averaged across every page hit at that level
 * (works whether the raw output has one wrk summary block per level, or
 * many sequential hey blocks per level).
 */
public class ConcurrencyMetric {

  private final int concurrency;
  private final double totalRequestsPerSec;
  private final double avgLatencyMs;
  private final double p99LatencyMs;

  public ConcurrencyMetric(int concurrency, double totalRequestsPerSec, double avgLatencyMs, double p99LatencyMs) {
    this.concurrency = concurrency;
    this.totalRequestsPerSec = totalRequestsPerSec;
    this.avgLatencyMs = avgLatencyMs;
    this.p99LatencyMs = p99LatencyMs;
  }

  public int getConcurrency() {
    return concurrency;
  }

  public double getTotalRequestsPerSec() {
    return totalRequestsPerSec;
  }

  public double getAvgLatencyMs() {
    return avgLatencyMs;
  }

  public double getP99LatencyMs() {
    return p99LatencyMs;
  }
}
