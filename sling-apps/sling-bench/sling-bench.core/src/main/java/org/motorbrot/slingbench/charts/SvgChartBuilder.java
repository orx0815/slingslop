package org.motorbrot.slingbench.charts;

import java.util.List;
import java.util.Locale;

/**
 * Renders small, dependency-free inline SVG charts server-side, so a run's
 * charts show up with zero client-side JavaScript (see Appendix C of the
 * Agent Smith skill: prefer CSS/server rendering over JS bundles).
 *
 * <p>Every method returns a complete {@code <svg>...</svg>} string, meant to
 * be embedded via {@code ${model.someChartSvg @ context='html'}} in HTL.</p>
 */
public final class SvgChartBuilder {

  private static final String FONT = "font-family:var(--font-mono, monospace);font-size:11px;";

  private SvgChartBuilder() {
  }

  /**
   * A single-series bar chart, one bar per label.
   */
  public static String barChart(List<String> labels, List<Double> values, int width, int height,
      String barColor, String unitSuffix) {
    if (labels.isEmpty()) {
      return emptySvg(width, height);
    }
    int padLeft = 48;
    int padBottom = 28;
    int padTop = 12;
    int chartWidth = width - padLeft - 12;
    int chartHeight = height - padTop - padBottom;

    double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(1);
    if (max <= 0) {
      max = 1;
    }

    int barSlot = chartWidth / labels.size();
    int barWidth = (int) Math.max(6, barSlot * 0.6);

    StringBuilder svg = new StringBuilder();
    svg.append(String.format(Locale.ROOT,
        "<svg viewBox=\"0 0 %d %d\" width=\"100%%\" height=\"%d\" role=\"img\" aria-label=\"bar chart\" class=\"svg-chart\">",
        width, height, height));
    svg.append(String.format(Locale.ROOT,
        "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" class=\"svg-chart-axis\"/>",
        padLeft, padTop, padLeft, padTop + chartHeight));
    svg.append(String.format(Locale.ROOT,
        "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" class=\"svg-chart-axis\"/>",
        padLeft, padTop + chartHeight, padLeft + chartWidth, padTop + chartHeight));

    for (int i = 0; i < labels.size(); i++) {
      double value = values.get(i);
      int barHeight = (int) Math.round((value / max) * chartHeight);
      int x = padLeft + i * barSlot + (barSlot - barWidth) / 2;
      int y = padTop + chartHeight - barHeight;
      svg.append(String.format(Locale.ROOT,
          "<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" fill=\"%s\" class=\"svg-chart-bar\"><title>%s%s</title></rect>",
          x, y, barWidth, Math.max(barHeight, 1), barColor, formatNumber(value), esc(unitSuffix)));
      svg.append(String.format(Locale.ROOT,
          "<text x=\"%d\" y=\"%d\" text-anchor=\"middle\" class=\"svg-chart-label\" style=\"%s\">%s</text>",
          x + barWidth / 2, y - 4, FONT, formatNumber(value)));
      svg.append(String.format(Locale.ROOT,
          "<text x=\"%d\" y=\"%d\" text-anchor=\"middle\" class=\"svg-chart-axis-label\" style=\"%s\">%s</text>",
          x + barWidth / 2, padTop + chartHeight + 16, FONT, esc(labels.get(i))));
    }

    svg.append("</svg>");
    return svg.toString();
  }

  /**
   * A grouped (side-by-side) two-series bar chart — used to compare
   * virtual-thread vs platform-thread runs at the same concurrency levels.
   */
  public static String dualBarChart(List<String> labels, List<Double> seriesA, List<Double> seriesB,
      String labelA, String labelB, int width, int height, String colorA, String colorB) {
    if (labels.isEmpty()) {
      return emptySvg(width, height);
    }
    int padLeft = 48;
    int padBottom = 28;
    int padTop = 24;
    int chartWidth = width - padLeft - 12;
    int chartHeight = height - padTop - padBottom;

    double max = Math.max(
        seriesA.stream().mapToDouble(Double::doubleValue).max().orElse(1),
        seriesB.stream().mapToDouble(Double::doubleValue).max().orElse(1));
    if (max <= 0) {
      max = 1;
    }

    int slot = chartWidth / labels.size();
    int barWidth = (int) Math.max(5, slot * 0.32);
    int gap = (int) Math.max(2, slot * 0.06);

    StringBuilder svg = new StringBuilder();
    svg.append(String.format(Locale.ROOT,
        "<svg viewBox=\"0 0 %d %d\" width=\"100%%\" height=\"%d\" role=\"img\" aria-label=\"comparison bar chart\" class=\"svg-chart\">",
        width, height, height));

    svg.append(String.format(Locale.ROOT,
        "<rect x=\"%d\" y=\"2\" width=\"10\" height=\"10\" fill=\"%s\"/><text x=\"%d\" y=\"11\" style=\"%s\">%s</text>",
        padLeft, colorA, padLeft + 14, FONT, esc(labelA)));
    svg.append(String.format(Locale.ROOT,
        "<rect x=\"%d\" y=\"2\" width=\"10\" height=\"10\" fill=\"%s\"/><text x=\"%d\" y=\"11\" style=\"%s\">%s</text>",
        padLeft + 120, colorB, padLeft + 134, FONT, esc(labelB)));

    svg.append(String.format(Locale.ROOT,
        "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" class=\"svg-chart-axis\"/>",
        padLeft, padTop, padLeft, padTop + chartHeight));
    svg.append(String.format(Locale.ROOT,
        "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" class=\"svg-chart-axis\"/>",
        padLeft, padTop + chartHeight, padLeft + chartWidth, padTop + chartHeight));

    for (int i = 0; i < labels.size(); i++) {
      int groupX = padLeft + i * slot + (slot - (barWidth * 2 + gap)) / 2;

      double a = seriesA.get(i);
      int hA = (int) Math.round((a / max) * chartHeight);
      svg.append(String.format(Locale.ROOT,
          "<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" fill=\"%s\" class=\"svg-chart-bar\"><title>%s: %s</title></rect>",
          groupX, padTop + chartHeight - hA, barWidth, Math.max(hA, 1), colorA, esc(labelA), formatNumber(a)));

      double b = seriesB.get(i);
      int hB = (int) Math.round((b / max) * chartHeight);
      svg.append(String.format(Locale.ROOT,
          "<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" fill=\"%s\" class=\"svg-chart-bar\"><title>%s: %s</title></rect>",
          groupX + barWidth + gap, padTop + chartHeight - hB, barWidth, Math.max(hB, 1), colorB, esc(labelB), formatNumber(b)));

      svg.append(String.format(Locale.ROOT,
          "<text x=\"%d\" y=\"%d\" text-anchor=\"middle\" class=\"svg-chart-axis-label\" style=\"%s\">%s</text>",
          groupX + barWidth + gap / 2, padTop + chartHeight + 16, FONT, esc(labels.get(i))));
    }

    svg.append("</svg>");
    return svg.toString();
  }

  /**
   * A single-series line chart with dots at each data point.
   */
  public static String lineChart(List<String> labels, List<Double> values, int width, int height,
      String strokeColor) {
    if (labels.isEmpty()) {
      return emptySvg(width, height);
    }
    int padLeft = 48;
    int padBottom = 28;
    int padTop = 12;
    int chartWidth = width - padLeft - 12;
    int chartHeight = height - padTop - padBottom;

    double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(1);
    if (max <= 0) {
      max = 1;
    }

    StringBuilder points = new StringBuilder();
    StringBuilder dots = new StringBuilder();
    StringBuilder labelsSvg = new StringBuilder();

    int n = labels.size();
    for (int i = 0; i < n; i++) {
      int x = padLeft + (n == 1 ? chartWidth / 2 : (int) Math.round((double) i / (n - 1) * chartWidth));
      double value = values.get(i);
      int y = padTop + chartHeight - (int) Math.round((value / max) * chartHeight);
      if (i > 0) {
        points.append(' ');
      }
      points.append(x).append(',').append(y);
      dots.append(String.format(Locale.ROOT,
          "<circle cx=\"%d\" cy=\"%d\" r=\"3\" fill=\"%s\"><title>%s</title></circle>",
          x, y, strokeColor, formatNumber(value)));
      labelsSvg.append(String.format(Locale.ROOT,
          "<text x=\"%d\" y=\"%d\" text-anchor=\"middle\" class=\"svg-chart-axis-label\" style=\"%s\">%s</text>",
          x, padTop + chartHeight + 16, FONT, esc(labels.get(i))));
    }

    StringBuilder svg = new StringBuilder();
    svg.append(String.format(Locale.ROOT,
        "<svg viewBox=\"0 0 %d %d\" width=\"100%%\" height=\"%d\" role=\"img\" aria-label=\"line chart\" class=\"svg-chart\">",
        width, height, height));
    svg.append(String.format(Locale.ROOT,
        "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" class=\"svg-chart-axis\"/>",
        padLeft, padTop, padLeft, padTop + chartHeight));
    svg.append(String.format(Locale.ROOT,
        "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" class=\"svg-chart-axis\"/>",
        padLeft, padTop + chartHeight, padLeft + chartWidth, padTop + chartHeight));
    svg.append(String.format(Locale.ROOT,
        "<polyline points=\"%s\" fill=\"none\" stroke=\"%s\" stroke-width=\"2\" class=\"svg-chart-line\"/>",
        points, strokeColor));
    svg.append(dots);
    svg.append(labelsSvg);
    svg.append("</svg>");
    return svg.toString();
  }

  private static String emptySvg(int width, int height) {
    return String.format(Locale.ROOT,
        "<svg viewBox=\"0 0 %d %d\" width=\"100%%\" height=\"%d\" class=\"svg-chart svg-chart--empty\">"
            + "<text x=\"12\" y=\"20\" style=\"%s\">no data</text></svg>",
        width, height, height, FONT);
  }

  private static String formatNumber(double value) {
    if (value == Math.floor(value) && !Double.isInfinite(value)) {
      return String.format(Locale.ROOT, "%.0f", value);
    }
    return String.format(Locale.ROOT, "%.1f", value);
  }

  private static String esc(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&apos;");
  }
}
