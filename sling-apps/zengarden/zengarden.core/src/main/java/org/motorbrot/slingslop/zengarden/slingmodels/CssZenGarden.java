package org.motorbrot.slingslop.zengarden.slingmodels;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import javax.inject.Inject;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.uri.SlingUriBuilder;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Picks a design. Assigned by htl, url or random
 */
@Model(adaptables = {SlingHttpServletRequest.class})
public class CssZenGarden {

  private final Design[] CSS_DESIGNS = new Design[]{
    new Design("041", "door to my garden", "Patrick Lauke", "http://redux.deviantart.com/"),
    new Design("083", "Springtime", "Boër Attila", ""),
    new Design("084", "Start Listening!", "Liz Lubowitz", "http://hiptobeasquare.com"),
    new Design("099", "Wiggles the Wonderworm", "Joseph Pearson", "http://www.make-believe.org/"),
    new Design("139", "Neat & Tidy", "Oli Dale", "http://www.designerstalk.com/"),
    new Design("142", "Invasion of the Body Switchers", "Andy Clarke", "http://www.stuffandnonsense.co.uk/"),
    new Design("145", "Paravion", "Emiliano Pennisi", "http://www.peamarte.it/01/metro.html"),
    new Design("146", "Urban", "Matt, Kim &amp; Nicole", "http://www.learnnewmedia.com"),
    new Design("156", "Table Layout Assassination", "Marko Krsul & Marko Dugonjic", "http://web.burza.hr/"),
    new Design("171", "Shaolin Yokobue", "Javier Cabrera", "http://www.emaginacion.com.ar/hacks/"),
    new Design("177", "Zen City Morning", "Ray Henry", "http://www.reh3.com/"),
    new Design("193", "Leggo My Ego", "Jon Tan", "http://www.gr0w.com/"),
    new Design("194", "Dark Rose", "Rose Fu", "http://www.rosefu.net/"),
    new Design("195", "Dazzling Beauty", "Deny Sri Supriyono", "http://blog.denysri.com/"),
    new Design("198", "The Original", "Joachim Shotter", "http://www.bluejam.com/"),
    new Design("199", "Zen Army", "Carl Desmond", "https://www.niceguy.blog/"),
    new Design("200", "Icicle Outback", "Timo Virtanen", "http://www.timovirtanen.com/"),
    new Design("202", "Retro Theater", "Eric RogŽ", "http://space-sheeps.info/"),
    new Design("205", "spring360", "Rene Hornig", "http://www.medialab360.com/"),
    new Design("213", "Under the Sea", "Eric Stoltz", "http://www.ericstoltz.com/"),
    new Design("214", "Verde Moderna", "Dave Shea", "http://www.mezzoblue.com/"),
    new Design("215", "A Robot Named Jimmy", "meltmedia", "http://meltmedia.com"),
    new Design("217", "Screen Filler", "Elliot Jay Stocks", "http://elliotjaystocks.com/"),
    new Design("218", "Apothecary", "Trent Walton", "http://www.trentwalton.com/"),
    new Design("219", "Steel", "Steffen Knoeller", "http://www.steffen-knoeller.de/"),
    new Design("221", "Mid Century Modern", "Andrew Lohman", "http://www.andrewlohman.com/"),
    
    // Vibe coded modern, responsive styles
    new Design("401", "Blue Vibe Code", "Claude Sonnet 4", "https://claude.ai/new"),
    new Design("402", "Light Vibe Code", "Claude Sonnet 4", "https://claude.ai/new"),
    new Design("403", "Modern Serenity ", "Grok by xAI", "https://x.ai/"),
    new Design("405", "Commodore 64 Retro Zen Garden ", "Grok by xAI", "https://x.ai/")

  };

  private static final String REQUEST_ATTRIBUTE_CSS_ZEN_GARDEN_DESIGN = CssZenGarden.class.getName() + "_Design";
  private static final String REQUEST_ATTRIBUTE_HTL_SELECT_DESIGN = "design_id";
  private static final Logger LOG = LoggerFactory.getLogger(CssZenGarden.class);

  private Design design;
  
  private final SlingHttpServletRequest request;

  /**
   * Constructor injection
   * @param request via Sling-Model injection
   */
  @Inject
  public CssZenGarden(@Self SlingHttpServletRequest request) {
    this.request = request;
    // Keep the same design per request, when model is called in different components
    this.design = (Design)request.getAttribute(REQUEST_ATTRIBUTE_CSS_ZEN_GARDEN_DESIGN);
    if (this.design == null) {
      // check if template forced an id
      this.design = designFromSightly(request);
      if (this.design == null) {
        // check if url has one
        this.design = designFromSuffix(request);
        if (this.design == null) {
          // pick one randomly
          Random rand = new Random();
          this.design = CSS_DESIGNS[rand.nextInt(CSS_DESIGNS.length)];
          LOG.info("CSS-id is: " + this.design.cssId);
        }
      }
      request.setAttribute(REQUEST_ATTRIBUTE_CSS_ZEN_GARDEN_DESIGN, this.design);
    }
  }

  /**
   * Reads number from suffix and tries to find a design with that id
   *
   * @param request like slimpogrine/home.html/401
   * @return Design with that id or null
   */
  @Nullable
  private Design designFromSuffix(SlingHttpServletRequest request) {
    String suffix = request.getRequestPathInfo().getSuffix();
    suffix = StringUtils.getDigits(suffix);
    suffix = StringUtils.substring(suffix, 0, 3);
    Design designFromSuffix = byId(suffix);
    return designFromSuffix;
  }
  
  /**
   * Design id can get passed in via sightly:
   * data-sly-use.cssPicker="${'org.motorbrot.slimpogrine.slingmodels.CssZenGarden' @ design_id='401'}"
   * 
   * @param request 
   * @return Design with that id or null
   */
  @Nullable
  private Design designFromSightly(SlingHttpServletRequest request) {
    String idByHtl = (String)request.getAttribute(REQUEST_ATTRIBUTE_HTL_SELECT_DESIGN);
    Design designFromSuffix = byId(idByHtl);
    return designFromSuffix;
  }

  @Nullable
  private Design byId(String cssId) {
    return Arrays.stream(CSS_DESIGNS)
      .filter(d -> d.cssId.equals(cssId))
      .findAny()
      .orElse(null);
  }

  /**
   * Sightly getter
   * @return current CSS info 
   */
  public Design getDesign() {
    return this.design;
  }

  /**
   * Sightly getter
   * @return next CSS of our curated list
   */
  public Design getNextDesign() {
    int nextIdx = ArrayUtils.indexOf(CSS_DESIGNS, this.design) + 1;
    if (nextIdx == CSS_DESIGNS.length) {
      nextIdx = 0;
    }
    return CSS_DESIGNS[nextIdx];
  }
  
  /**
   * Sightly getter
   * @return previous CSS 
   */
  public Design getPreviousDesign() {
    int prevIdx = ArrayUtils.indexOf(CSS_DESIGNS, this.design) - 1;
    if (prevIdx == -1) {
      prevIdx = CSS_DESIGNS.length - 1;
    }
    return CSS_DESIGNS[prevIdx];
  }
  
  
  /**
   * Get random URL of zengarden's css (Sightly getter)
   * @return URL to css
   */
  public String getRandomCssUrl() {
    return "/apps/slingslop/zengarden/css_zen_garden/" + this.design.cssId + "/" + this.design.cssId + ".css";
  }
  
  /**
   * Links to current Page with given design 
   * @param forcedDesign will be added as suffix
   * @return url
   */
  private String getUrlWithDesign(Design forcedDesign) {
    
    String resourcePath = request.getRequestPathInfo().getResourcePath();
    
    // Map the resource path to absolute and shortened URL (including host and port)
    // https://sling.apache.org/documentation/the-sling-engine/mappings-for-resource-resolution.html#reverse-outgoing-mapping
    // ToDo: add mappings to /etc/map
    String externalUrl = request.getResourceResolver().map(resourcePath);
    
    String url = SlingUriBuilder.parse(externalUrl, request.getResourceResolver())
      .setExtension("html")
      .setSuffix("/" + forcedDesign.cssId)
      .build().toString();
    
    LOG.info("Mapped Url: " + url);
    return url;
  }


  /**
   * Data object holding info about a zengarden design like it's original creator
   * Gets passed to the HTL/Sightly template
   */
  public class Design {

    private final String cssId;
    private final String name;
    private final String designerName;
    private final String designerUrl;

    private Design(String cssId, String name, String designerName, String designerUrl) {
      this.cssId = cssId;
      this.name = name;
      this.designerName = designerName;
      this.designerUrl = designerUrl;
    }

    /**
     * Id from csszengarden.com
     * @return ID of the design
     */
    public String getCssId() {
      return cssId;
    }

    /**
     * @return Name of the CSS
     */
    public String getName() {
      return name;
    }

    /**
     * @return Name of the designer
     */
    public String getDesignerName() {
      return designerName;
    }

    /**
     * @return URL to designer's website (Sightly getter)
     */
    public String getDesignerUrl() {
      return designerUrl;
    }
    
    /**
     * @return URL to current page with this design forced
     */
    public String getUrlWithFixedCss() {
      return getUrlWithDesign(this);
    }

    @Override
    public int hashCode() {
      int hash = 5;
      hash = 29 * hash + Objects.hashCode(this.cssId);
      return hash;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final Design other = (Design)obj;
      return Objects.equals(this.cssId, other.cssId);
    }

  }

}
