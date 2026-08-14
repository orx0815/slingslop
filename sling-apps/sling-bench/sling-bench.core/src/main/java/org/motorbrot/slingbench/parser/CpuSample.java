package org.motorbrot.slingbench.parser;

/**
 * One sample of the vt-bench.sh CPU/RSS sampler CSV
 * ({@code timestamp,cpu%,rss_mb}).
 */
public class CpuSample {

  private final String timestamp;
  private final double cpuPercent;
  private final double rssMb;

  public CpuSample(String timestamp, double cpuPercent, double rssMb) {
    this.timestamp = timestamp;
    this.cpuPercent = cpuPercent;
    this.rssMb = rssMb;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public double getCpuPercent() {
    return cpuPercent;
  }

  public double getRssMb() {
    return rssMb;
  }
}
