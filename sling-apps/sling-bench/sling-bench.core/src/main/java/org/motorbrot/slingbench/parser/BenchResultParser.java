package org.motorbrot.slingbench.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the plain-text output of {@code vt-bench.sh} (see
 * {@code sling-apps/sling-matrix/vt-bench.sh}) into structured metrics.
 *
 * <p>Handles both output shapes the script can produce:</p>
 * <ul>
 *   <li><b>wrk</b> — one aggregate summary block per concurrency level</li>
 *   <li><b>hey</b> — one block per page, several per concurrency level</li>
 * </ul>
 *
 * <p>Per concurrency level, every {@code Requests/sec:} occurrence in that
 * level's section is summed (correct for both shapes: a single wrk total, or
 * many per-page hey totals), every {@code Average:} occurrence is averaged,
 * and the worst (max) {@code 99% in} occurrence is kept. Lines that failed to
 * complete (e.g. hey's {@code NaN secs}) simply do not match the numeric
 * pattern and are skipped automatically.</p>
 */
public final class BenchResultParser {

  private static final Pattern TOOL = Pattern.compile("tool\\s*:\\s*(\\S+)");
  private static final Pattern BASE_URL = Pattern.compile("base_url\\s*:\\s*(\\S+)");
  private static final Pattern DELAY_MS = Pattern.compile("delay_ms\\s*:\\s*(\\d+)");
  private static final Pattern DURATION = Pattern.compile("duration\\s*:\\s*(\\S+)");
  private static final Pattern VIRTUAL_THREADS = Pattern.compile("httpVirtualThreads:\\s*(true|false)");

  private static final Pattern CONCURRENCY_HEADER = Pattern.compile("concurrency\\s*=\\s*(\\d+)");
  private static final Pattern REQUESTS_PER_SEC = Pattern.compile("Requests/sec:\\s*([0-9]+\\.?[0-9]*)");
  private static final Pattern AVERAGE_SECS = Pattern.compile("Average:\\s*([0-9]+\\.?[0-9]*)\\s*secs");
  private static final Pattern P99_SECS = Pattern.compile("99% in\\s*([0-9]+\\.?[0-9]*)\\s*secs");

  private static final Pattern CPU_SUMMARY = Pattern.compile(
      "avg_cpu=([0-9.]+)%\\s+min_cpu=([0-9.]+)%\\s+max_cpu=([0-9.]+)%\\s+peak_rss=(\\d+)MB");

  private BenchResultParser() {
  }

  public static BenchResult parse(String rawText) {
    String tool = firstGroup(TOOL, rawText);
    String baseUrl = firstGroup(BASE_URL, rawText);
    String delayMsStr = firstGroup(DELAY_MS, rawText);
    String duration = firstGroup(DURATION, rawText);
    String vt = firstGroup(VIRTUAL_THREADS, rawText);

    Integer delayMs = delayMsStr == null ? null : Integer.valueOf(delayMsStr);
    Boolean httpVirtualThreads = vt == null ? null : Boolean.valueOf(vt);

    List<ConcurrencyMetric> metrics = parseConcurrencySections(rawText);

    return new BenchResult(tool, baseUrl, delayMs, duration, httpVirtualThreads, metrics);
  }

  private static List<ConcurrencyMetric> parseConcurrencySections(String rawText) {
    List<ConcurrencyMetric> metrics = new ArrayList<>();

    Matcher header = CONCURRENCY_HEADER.matcher(rawText);
    List<Integer> starts = new ArrayList<>();
    List<Integer> levels = new ArrayList<>();
    while (header.find()) {
      starts.add(header.start());
      levels.add(Integer.valueOf(header.group(1)));
    }

    for (int i = 0; i < starts.size(); i++) {
      int from = starts.get(i);
      int to = (i + 1 < starts.size()) ? starts.get(i + 1) : rawText.length();
      String section = rawText.substring(from, to);

      double totalRps = sumAll(REQUESTS_PER_SEC, section);
      double avgLatencySecs = averageAll(AVERAGE_SECS, section);
      double worstP99Secs = maxAll(P99_SECS, section);

      metrics.add(new ConcurrencyMetric(
          levels.get(i),
          totalRps,
          avgLatencySecs * 1000.0,
          worstP99Secs * 1000.0));
    }

    return metrics;
  }

  /** Optional CPU/RSS summary line that vt-bench.sh appends to its own log. */
  public static double[] parseCpuSummary(String rawText) {
    Matcher m = CPU_SUMMARY.matcher(rawText);
    if (!m.find()) {
      return null;
    }
    return new double[] {
        Double.parseDouble(m.group(1)),
        Double.parseDouble(m.group(2)),
        Double.parseDouble(m.group(3)),
        Double.parseDouble(m.group(4))
    };
  }

  /** Parses the {@code timestamp,cpu%,rss_mb} sampler CSV vt-bench.sh writes alongside the result. */
  public static List<CpuSample> parseCpuCsv(String csvText) {
    List<CpuSample> samples = new ArrayList<>();
    String[] lines = csvText.split("\\r?\\n");
    for (String line : lines) {
      String trimmed = line.trim();
      if (trimmed.isEmpty() || trimmed.startsWith("timestamp")) {
        continue;
      }
      String[] cols = trimmed.split(",");
      if (cols.length != 3) {
        continue;
      }
      try {
        samples.add(new CpuSample(cols[0], Double.parseDouble(cols[1]), Double.parseDouble(cols[2])));
      } catch (NumberFormatException ignored) {
        // skip malformed row
      }
    }
    return samples;
  }

  private static String firstGroup(Pattern pattern, String text) {
    Matcher m = pattern.matcher(text);
    return m.find() ? m.group(1) : null;
  }

  private static double sumAll(Pattern pattern, String text) {
    double sum = 0;
    Matcher m = pattern.matcher(text);
    while (m.find()) {
      sum += Double.parseDouble(m.group(1));
    }
    return sum;
  }

  private static double averageAll(Pattern pattern, String text) {
    double sum = 0;
    int count = 0;
    Matcher m = pattern.matcher(text);
    while (m.find()) {
      sum += Double.parseDouble(m.group(1));
      count++;
    }
    return count == 0 ? 0 : sum / count;
  }

  private static double maxAll(Pattern pattern, String text) {
    double max = 0;
    Matcher m = pattern.matcher(text);
    while (m.find()) {
      max = Math.max(max, Double.parseDouble(m.group(1)));
    }
    return max;
  }

  /** Formats a double without a trailing {@code .0} for compact display. */
  public static String formatNumber(double value) {
    if (value == Math.floor(value) && !Double.isInfinite(value)) {
      return String.format(Locale.ROOT, "%.0f", value);
    }
    return String.format(Locale.ROOT, "%.1f", value);
  }
}
