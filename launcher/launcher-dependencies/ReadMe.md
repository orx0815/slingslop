# Slingslop Launcher Dependencies BOM

This module is a **Bill of Materials (BOM)** — a `pom`-packaged artifact containing only a `dependencyManagement` section.

It provides managed versions for all OSGi bundles present in the Apache Sling runtime, generated from the Sling Starter `slingosgifeature`:

```
org.apache.sling:org.apache.sling.starter:slingosgifeature:nosample_base:<version>
```

## Purpose

OSGi bundles in this project compile against APIs that are already present in the Sling runtime.  
Those dependencies should be declared with `<scope>provided</scope>` — but without having to repeat version numbers in every module.

By importing this BOM you get all those versions managed centrally:

```xml
<dependency>
  <groupId>org.motorbrot</groupId>
  <artifactId>org.motorbrot.slingslop.launcher-dependencies</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <type>pom</type>
  <scope>import</scope>
</dependency>
```

This import is already present in the root `pom.xml` `dependencyManagement` section, so all child modules inherit it automatically.

## Why no parent POM?

This BOM intentionally has **no `<parent>`**. If it inherited from `slingslop.parent`, and `slingslop.parent` imported this BOM, Maven would detect a cycle and fail.


## What is generated

- The BOM entries are generated from Sling `nosample_base` feature JSON.
- Source (default):
  - `~/.m2/repository/org/apache/sling/org.apache.sling.starter/14-SNAPSHOT/org.apache.sling.starter-14-SNAPSHOT-nosample_base.slingosgifeature`
- Output:
  - `launcher/launcher-dependencies/pom.xml`

## Regenerate BOM

### Via Python script

From repository root:

```bash
python3 launcher/launcher-dependencies/generate_bom.py --sling-version 14-SNAPSHOT
```

Or with an explicit feature file:

```bash
python3 launcher/launcher-dependencies/generate_bom.py \
  --feature-file /absolute/path/to/org.apache.sling.starter-<version>-nosample_base.slingosgifeature
```

### Via Maven profile

From here, when the pom.xml regenerates itself:

```bash
mvn -Pgenerate-launcher-bom generate-resources
```

## Notes

- The generated BOM reflects only artifacts that are actually listed in the Sling feature `bundles` section.
- This means it is runtime-feature-driven, not a complete compile-time API catalog.
- In practice, some dependencies used only for compilation may be missing from this BOM, for example:
  - `org.osgi:osgi.core`
  - `org.osgi:osgi.annotation`
  - `jakarta.servlet:jakarta.servlet-api`
  - `javax.jcr:jcr`
  - `org.apache.jackrabbit:jackrabbit-api`
  - `org.slf4j:slf4j-api`
- Why this happens: Sling starter may include equivalent/runtime bundles with different coordinates (or embedded APIs), while your source code depends on canonical API artifacts.
- Recommended pattern:
  1. Import this generated BOM in the **parent** `dependencyManagement` (for everything covered by Sling feature bundles).
  2. Add a small fallback set for missing compile-time API coordinates in the **same parent** `dependencyManagement`.
  3. Keep fallback list minimal and only for unresolved artifacts.

Current example in this project: `pom.xml` imports this BOM and defines the shared fallback API coordinates for all child modules.
