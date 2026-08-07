# Alf vs Agent Smith

**Two agents. One arena. No escape.**

A Matrix-themed Sling application featuring a versus-style showdown between ALF (the creative chaos facilitator) and Agent Smith (the methodical architect). Built with HTMX-driven hypermedia, zen-editable components, and a dark OKLCH color system inspired by the Matrix film.

## Quick Start

```bash
# Build the full project
mvn clean install -DskipITs

# Launch locally
cd launcher && ./launch.sh
# → http://localhost:8080/content/alf-vs-agent/home.html
```

## Structure

| Module | Description |
|---|---|
| `alf-vs-agent.core` | OSGi bundle with Sling Models |
| `alf-vs-agent.ui.apps` | HTL templates, components, frontend build |
| `alf-vs-agent.sample-content` | Sample content (home, style guide, content page) |

## Design

- **Colors:** Matrix green (OKLCH hue 142) + digital amber (hue 85), dark theme
- **Fonts:** Monospace body, Matrix-inspired headings
- **Animations:** Scroll reveal, glitch effects, scanlines, cursor blink
- **Navigation:** Top bar + footer

## Pages

- `/content/alf-vs-agent/home.html` — Hero + versus fighter cards
- `/content/alf-vs-agent/styleguide.html` — Living style guide
- `/content/alf-vs-agent/content-page.html` — Mission intel content page
