#!/usr/bin/env python3

import argparse
import json
from pathlib import Path


BOM_GROUP_ID = "org.motorbrot"
BOM_ARTIFACT_ID = "org.motorbrot.slingslop.launcher-dependencies"
BOM_VERSION = "0.0.1-SNAPSHOT"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Generate launcher-dependencies BOM from Sling .slingosgifeature"
    )
    parser.add_argument(
        "--sling-version",
        default="14",
        help="Sling starter version in local m2 (default: 14)",
    )
    parser.add_argument(
        "--feature-file",
        help="Absolute path to .slingosgifeature; overrides --sling-version lookup",
    )
    parser.add_argument(
        "--output",
        default=str(Path(__file__).with_name("pom.xml")),
        help="Output pom.xml path (default: launcher/launcher-dependencies/pom.xml)",
    )
    return parser.parse_args()


def resolve_feature_file(args: argparse.Namespace) -> Path:
    if args.feature_file:
        return Path(args.feature_file)

    return (
        Path.home()
        / ".m2"
        / "repository"
        / "org"
        / "apache"
        / "sling"
        / "org.apache.sling.starter"
        / args.sling_version
        / f"org.apache.sling.starter-{args.sling_version}-nosample_base.slingosgifeature"
    )


def load_entries(feature_file: Path):
    data = json.loads(feature_file.read_text())
    bundles = data.get("bundles", [])

    entries = []
    seen = set()

    for bundle in bundles:
        bundle_id = bundle["id"] if isinstance(bundle, dict) else bundle
        parts = bundle_id.split(":")

        if len(parts) == 3:
            group_id, artifact_id, version = parts
            dep_type = None
            classifier = None
        elif len(parts) == 5:
            group_id, artifact_id, dep_type, classifier, version = parts
        else:
            continue

        key = (group_id, artifact_id, dep_type, classifier, version)
        if key in seen:
            continue

        seen.add(key)
        entries.append((group_id, artifact_id, version, dep_type, classifier))

    return data, entries


def render_pom(feature_id: str, entries) -> str:
    lines = [
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
        "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"",
        "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"",
        "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">",
        "",
        "  <modelVersion>4.0.0</modelVersion>",
        "",
        f"  <groupId>{BOM_GROUP_ID}</groupId>",
        f"  <artifactId>{BOM_ARTIFACT_ID}</artifactId>",
        f"  <version>{BOM_VERSION}</version>",
        "  <packaging>pom</packaging>",
        "",
        "  <properties>",
        "    <sling.starter.version>14</sling.starter.version>",
        "  </properties>",
        "",
        "  <name>Slingslop - Launcher Dependencies BOM</name>",
        f"  <description>Generated dependencyManagement BOM from org.apache.sling.starter nosample_base feature ({feature_id}).</description>",
        "",
        "  <dependencyManagement>",
        "    <dependencies>",
    ]

    for group_id, artifact_id, version, dep_type, classifier in entries:
        lines.extend(
            [
                "      <dependency>",
                f"        <groupId>{group_id}</groupId>",
                f"        <artifactId>{artifact_id}</artifactId>",
            ]
        )
        if dep_type:
            lines.append(f"        <type>{dep_type}</type>")
        if classifier:
            lines.append(f"        <classifier>{classifier}</classifier>")
        lines.extend(
            [
                f"        <version>{version}</version>",
                "        <scope>provided</scope>",
                "      </dependency>",
            ]
        )

    lines.extend(
        [
            "    </dependencies>",
            "  </dependencyManagement>",
            "",
            "  <profiles>",
            "    <profile>",
            "      <id>generate-launcher-bom</id>",
            "      <build>",
            "        <plugins>",
            "          <plugin>",
            "            <groupId>org.codehaus.mojo</groupId>",
            "            <artifactId>exec-maven-plugin</artifactId>",
            "            <version>3.6.3</version>",
            "            <executions>",
            "              <execution>",
            "                <id>generate-launcher-dependencies-bom</id>",
            "                <phase>generate-resources</phase>",
            "                <goals>",
            "                  <goal>exec</goal>",
            "                </goals>",
            "                <configuration>",
            "                  <executable>python3</executable>",
            "                  <workingDirectory>${project.basedir}</workingDirectory>",
            "                  <arguments>",
            "                    <argument>${project.basedir}/generate_bom.py</argument>",
            "                    <argument>--sling-version</argument>",
            "                    <argument>${sling.starter.version}</argument>",
            "                    <argument>--output</argument>",
            "                    <argument>${project.basedir}/pom.xml</argument>",
            "                  </arguments>",
            "                </configuration>",
            "              </execution>",
            "            </executions>",
            "          </plugin>",
            "        </plugins>",
            "      </build>",
            "    </profile>",
            "  </profiles>",
            "</project>",
            "",
        ]
    )

    return "\n".join(lines)


def main() -> int:
    args = parse_args()
    feature_file = resolve_feature_file(args)
    output_file = Path(args.output)

    if not feature_file.exists():
        raise FileNotFoundError(f"Feature file not found: {feature_file}")

    data, entries = load_entries(feature_file)
    output_file.write_text(render_pom(data.get("id", ""), entries))

    print(f"Generated {output_file} from {feature_file}")
    print(f"Managed dependencies: {len(entries)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())