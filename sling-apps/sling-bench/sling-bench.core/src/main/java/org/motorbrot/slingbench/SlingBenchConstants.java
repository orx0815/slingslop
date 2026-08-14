package org.motorbrot.slingbench;

/**
 * Shared path constants for Sling Bench.
 */
public final class SlingBenchConstants {

  /**
   * Where uploaded runs are persisted. Nested under the app's content root but
   * deliberately EXCLUDED from the sample-content package's filter (see
   * sling-bench.sample-content/.../META-INF/vault/filter.xml) so redeploying
   * sample content never wipes uploaded runs. Pre-created via launcher
   * repoinit so it exists before the first upload.
   */
  public static final String RUNS_ROOT = "/content/sling-bench/runs";

  public static final String PAGE_RESOURCE_TYPE = "sling-bench/pages/page";
  public static final String RUNDETAIL_RESOURCE_TYPE = "sling-bench/pages/rundetail";

  private SlingBenchConstants() {
  }
}
