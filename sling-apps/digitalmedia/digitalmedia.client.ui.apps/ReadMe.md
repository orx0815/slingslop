# DML Client — Slim Responsive-Image API for Sling Apps

This bundle pair lets **any other sling-app** embed a DML-backed responsive
image (with focus-aware cropping and on-demand rendition generation) without
depending on the full Digital Media Library dashboard.

| Module | What it ships |
|---|---|
| `digitalmedia.client.core` | `MediaFormat` API, `MediaFormatRegistry`, `RenditionValidator`, `CropBox` / `FocusPoint`, and the `ResponsiveImage` Sling Model. |
| `digitalmedia.client.ui.apps` | HTL component `motorbrot/dma-client/components/responsive-image` and the editor picker widget `motorbrot/dma-client/widget/picker`. |

The DML dashboard (`digitalmedia.core` + `digitalmedia.ui.apps`) is **only** needed
to upload, store, and serve assets. Once an asset exists at e.g.
`/content/motorbrot/dma/assets/foo.jpg`, any consumer can reference it.

---

## 1. Add the dependency

Consumer `pom.xml` (the OSGi bundle that publishes `MediaFormat` services):

```xml
<dependency>
  <groupId>org.motorbrot</groupId>
  <artifactId>digitalmedia.client.core</artifactId>
  <version>${project.version}</version>
  <scope>provided</scope>
</dependency>
```

The `ui.apps` half is installed as a content package — add it to your
`launcher` / `content-packages/complete` aggregate. No code dependency needed
in your own `ui.apps` module.

## 2. Publish your `MediaFormat`s

Each consumer declares the rendition sizes it needs as OSGi services. They are
collected via the whiteboard pattern, so the DML dashboard's "Generate
Renditions" panel picks them up automatically.

```java
@Component(service = MediaFormat.class)
public class MatrixFeatureDesktopMediaFormat extends SimpleMediaFormat {
    public MatrixFeatureDesktopMediaFormat() {
        super("matrix-feature-desktop", 1200, 900,
              "Feature — desktop (4:3)",
              "Feature image rendition for desktop/tablet viewports",
              false /* maintainAspectRatio */);
    }
}
```

Constructor args of `SimpleMediaFormat`: `name`, `width`, `height`, `label`,
`description`, `maintainAspectRatio`.

- `maintainAspectRatio = false` → the rendition is **exactly** `width × height`,
  cropped to the format's aspect ratio around the asset's focus point.
- `maintainAspectRatio = true` → the rendition is scaled to fit **inside** the
  box, preserving the source aspect ratio (no crop, no squish).

The `name` is global and becomes the JCR node name of the stored rendition —
keep it namespaced per app (e.g. `matrix-feature-desktop`, not `desktop`).

## 3. Embed the responsive image in your HTL

Either delegate from your own component:

```html
<sly data-sly-resource="${ resource.path
       @ resourceType='motorbrot/dma-client/components/responsive-image' }"/>
```

…or use `sling:resourceSuperType="motorbrot/dma-client/components/responsive-image"`
on your component and override only what you need.

**The component reads its config from the consuming resource's ValueMap** —
not from the parent HTL bindings. So you must persist these properties on the
component node:

| Property | Type | Meaning |
|---|---|---|
| `fileReference` | `String` | JCR path of the DML asset, e.g. `/content/motorbrot/dma/assets/foo.jpg` |
| `dmaFormats` | `String[]` | Ordered list of `MediaFormat` names |
| `dmaMedia` | `String[]` | CSS media queries, **same order as `dmaFormats`**. The entry whose media query is missing/empty is the `<img>` fallback. |
| `dmaAlt` | `String` | Alt text |

> **Pitfall — don't try this:**
> ```html
> <sly data-sly-set.dmaFormats="${['matrix-feature-desktop','matrix-feature-mobile']}"/>
> <sly data-sly-resource="…/responsive-image"/>
> ```
> The included Sling Model reads from `resource.getValueMap()`, not from your
> HTL bindings. The properties must live on the JCR node.

Sample-content seed:

```xml
<feature-image-left
    sling:resourceType="sling-matrix/components/feature-image"
    fileReference="/content/motorbrot/dma/assets/hero.jpg"
    dmaAlt="A hero shot"
    dmaFormats="[matrix-feature-desktop,matrix-feature-mobile]"
    dmaMedia="[(min-width: 768px)]"/>  <!-- one entry shorter → mobile is fallback -->
```

## 4. Wire up the picker in your edit form

The picker widget writes `fileReference` + alt text + focus point in one go.

```html
<sly data-sly-use.picker="/apps/motorbrot/dma-client/widget/picker/picker.html"
     data-sly-call="${picker.field @
        name='fileReference',
        value=properties.fileReference,
        label='Feature image',
        altName='dmaAlt',
        altValue=properties.dmaAlt,
        ratio='4:3',
        assetsRoot='/content/motorbrot/dma/assets'}"/>

<!-- Persist the per-consumer format list. Multi-value via repeated input name -->
<!-- + explicit @TypeHint=String[]. Omit the trailing dmaMedia entry to mark    -->
<!-- the fallback (Sling POST strips trailing empty values anyway).             -->
<input type="hidden" name="dmaFormats@TypeHint" value="String[]" form="editor-form"/>
<input type="hidden" name="dmaFormats" value="matrix-feature-desktop" form="editor-form"/>
<input type="hidden" name="dmaFormats" value="matrix-feature-mobile" form="editor-form"/>
<input type="hidden" name="dmaMedia@TypeHint" value="String[]" form="editor-form"/>
<input type="hidden" name="dmaMedia" value="(min-width: 768px)" form="editor-form"/>
```

The picker's focus dot writes `dmaFocusX` / `dmaFocusY` (percent, 0–100)
straight onto the **asset's** `metadata` node — so every consumer of that
asset benefits from the same focus point.

---

## Where renditions live

Renditions are stored **on the asset itself**, not on the consuming component.
Layout in JCR:

```
/content/motorbrot/dma/assets/foo.jpg                    (nt:unstructured, isAsset=true)
├── jcr:content                                          (nt:resource)
│   └── jcr:data                                         ← original binary
├── metadata                                             (nt:unstructured)
│   ├── width            (Long)   ← extracted by Tika+ImageIO on upload
│   ├── height           (Long)
│   ├── mimeType         (String)
│   ├── fileSize         (Long)
│   ├── fileType         (String) "image" | "document" | …
│   ├── dmaFocusX        (Double) 0–100 % horizontal focus
│   └── dmaFocusY        (Double) 0–100 % vertical focus
└── renditions                                           (nt:unstructured)
    ├── preview                                          (nt:file, JPEG)
    ├── matrix-feature-desktop                           (nt:file, JPEG, 1200×900)
    └── matrix-feature-mobile                            (nt:file, JPEG, 600×600)
```

Each rendition node is a normal `nt:file`, so it can be served directly:

```
GET /content/motorbrot/dma/assets/foo.jpg/renditions/matrix-feature-desktop
```

This is exactly the URL the `<picture>` / `<img>` elements emit — no servlet
selector, no extension, just Sling's default binary streaming.

## When renditions are generated

| Trigger | Who | When |
|---|---|---|
| **`preview` rendition** | `AssetUploadServlet` | Immediately on upload (always created — used by the picker thumbnails and focus overlay). |
| **`<name>` rendition** | `RenditionGenerationServlet` | Lazily, on-demand, when an editor clicks the **Generate** button on the responsive-image figcaption. The button POSTs `{asset}.generate-rendition.html?format={name}`. |
| **Bulk** | DML dashboard "Generate Renditions" panel | Editor lists missing renditions across all assets and triggers them in bulk. |

`ResponsiveImage` never auto-creates anything. Decision tree:

```
For each (format, media) pair on the component node:
  1. validate(sourceWidth, sourceHeight, format)
     OK         → can be produced from this source
     TOO_SMALL  → source physically too small (no crop will help) → "too small" badge
  2. exists?
     yes → render <source>/<img>
     no  → "Generate" button (editors) / silently skipped (anonymous)
```

Anonymous viewers see the `<picture>` **only when**:
1. every configured format passes `validate()` (`!anyInvalid`), **and**
2. the fallback rendition file already exists on disk.

Otherwise the image is omitted entirely — no broken-image placeholder.

## Focus-aware cropping

When a format has a different aspect ratio than the source (e.g. 4:3 source →
1:1 mobile rendition), `RenditionService` does **not** refuse. Instead:

1. `CropBox.fromFocus()` computes the largest target-aspect rectangle that fits
   inside the source, centred on the asset's focus point and slid to stay
   inside the source bounds.
2. `BufferedImage.getSubimage()` extracts that crop.
3. The crop is scaled to `format.width × format.height`.

`RenditionValidator.validate()` therefore reports `TOO_SMALL` only when **the
cropped rectangle itself** is smaller than the format's dimensions, i.e. when
the source cannot satisfy the format **even after** the best possible focus
crop.

## Cache & lifecycle

- Sling's default `nt:file` GET handler serves renditions with `Last-Modified`
  and `ETag` headers — the front cache (`docker/webcache/`) caches them like
  any other static asset.
- Re-clicking **Generate** for the same `format` overwrites the existing
  rendition node in place (same UUID, new `jcr:data`). Browsers will revalidate
  via `If-Modified-Since`.
- Deleting an asset deletes its renditions with it (they are child nodes).
- Changing the focus point does **not** invalidate existing renditions. An
  editor must click Generate again to refresh them with the new crop.

---

## Reference implementation

`sling-apps/sling-matrix/sling-matrix.core/src/main/java/org/motorbrot/slingmatrix/mediaformats/`
contains the two `MediaFormat` services used by the demo:

- `MatrixFeatureDesktopMediaFormat` — 1200×900, 4:3
- `MatrixFeatureMobileMediaFormat` — 600×600, 1:1

`sling-apps/sling-matrix/sling-matrix.ui.apps/src/main/content/jcr_root/apps/sling-matrix/components/feature-image/`
is the consumer component: a 2-column section that delegates its image to
`motorbrot/dma-client/components/responsive-image`. The demo page at
<http://localhost:8080/content/sling-matrix/home/sling/sling-dml-demo.html>
shows it end-to-end.
