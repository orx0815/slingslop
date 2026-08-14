package org.motorbrot.vtbenchprobe;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * App-independent virtual-thread-vs-platform-thread benchmark probe.
 *
 * <p>Registered via the Sling engine's filter whiteboard with
 * {@code sling.filter.scope=REQUEST}, so it runs BEFORE resource resolution --
 * it intercepts every request on this Sling instance, in every app, without
 * needing a resourceType script or Sling Model in that app's own bundle.
 * Replaces the old sling-matrix-only {@code BenchmarkModel} + selector script.</p>
 *
 * <p><b>Disabled by default.</b> Turn it on at
 * {@code /system/console/configMgr} -> "VT Bench Probe", or drop an OSGi config
 * file for PID {@code org.motorbrot.vtbenchprobe.VtBenchProbeFilter}.</p>
 *
 * <p>Usage once enabled: append the configured query parameter (default
 * {@code _vtbenchDelay}) to ANY URL on the instance, e.g.
 * {@code http://localhost:8080/content/anything.html?_vtbenchDelay=100}. The
 * response carries {@code X-VT-Bench-Thread} / {@code -Delay-Ms} /
 * {@code -Elapsed-Ms} headers either way; with {@code renderRealPage=false}
 * (default) the body is also a tiny plain-text summary and the real resource
 * is never resolved/rendered -- the purest possible thread-pool probe.</p>
 */
@Component(service = Filter.class, property = {
    "sling.filter.scope=REQUEST",
    "service.ranking:Integer=" + Integer.MIN_VALUE
})
@Designate(ocd = VtBenchProbeFilter.Config.class)
public class VtBenchProbeFilter implements Filter {

  @ObjectClassDefinition(name = "VT Bench Probe",
      description = "App-independent virtual-thread benchmark probe. Sleeps the current "
          + "request thread for a configurable duration to simulate blocking I/O, so wrk/hey "
          + "can drive concurrency past the platform-thread pool size against ANY url on this "
          + "instance. This is a load-testing tool -- disabled by default, and NOT meant to be "
          + "left on in production.")
  public @interface Config {

    @AttributeDefinition(name = "Enabled", description = "Master on/off switch.")
    boolean enabled() default false;

    @AttributeDefinition(name = "Query parameter name",
        description = "Presence of this query parameter on ANY request triggers the probe; "
            + "its value is the delay in milliseconds.")
    String queryParamName() default "_vtbenchDelay";

    @AttributeDefinition(name = "Max delay (ms)",
        description = "Safety cap -- the requested delay is clamped to this value so the "
            + "probe can never be used to hold a request thread open indefinitely.")
    long maxDelayMs() default 5000;

    @AttributeDefinition(name = "Render real page after sleeping",
        description = "false (default): short-circuit -- sleep, write a tiny plain-text body, "
            + "stop. Fastest, purest thread-pool test. true: sleep, then let the request "
            + "continue to the real resource so the real page also renders -- a more realistic "
            + "'slow backend call inside a real request' simulation.")
    boolean renderRealPage() default false;
  }

  private volatile Config config;

  @Activate
  @Modified
  protected void activate(Config config) {
    this.config = config;
  }

  @Override
  public void init(FilterConfig filterConfig) {
    // no-op
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    Config cfg = config;
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    String param = cfg == null ? null : httpRequest.getParameter(cfg.queryParamName());

    if (cfg == null || !cfg.enabled() || param == null) {
      chain.doFilter(request, response);
      return;
    }

    long requestedDelay;
    try {
      requestedDelay = Long.parseLong(param);
    } catch (NumberFormatException e) {
      requestedDelay = 100;
    }
    long delayMs = Math.max(0, Math.min(requestedDelay, cfg.maxDelayMs()));

    Thread thread = Thread.currentThread();
    long start = System.nanoTime();
    try {
      Thread.sleep(delayMs);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    long elapsedMs = (System.nanoTime() - start) / 1_000_000L;

    HttpServletResponse httpResponse = (HttpServletResponse) response;
    httpResponse.setHeader("X-VT-Bench-Thread", thread.isVirtual() ? "virtual" : "platform");
    httpResponse.setHeader("X-VT-Bench-Delay-Ms", Long.toString(delayMs));
    httpResponse.setHeader("X-VT-Bench-Elapsed-Ms", Long.toString(elapsedMs));

    if (cfg.renderRealPage()) {
      chain.doFilter(request, response);
      return;
    }

    httpResponse.setContentType("text/plain;charset=UTF-8");
    httpResponse.setStatus(HttpServletResponse.SC_OK);
    httpResponse.getWriter().write(
        (thread.isVirtual() ? "VT" : "PT") + " thread=" + thread.getName()
            + " delay=" + delayMs + "ms elapsed=" + elapsedMs + "ms\n");
  }

  @Override
  public void destroy() {
    // no-op
  }
}
