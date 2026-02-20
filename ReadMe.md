# Slingslop

## Hypermedia Driven Applications (HDA) based on Apache Sling.  
Using HTMX|Datastar|Alpine-Ajax to GET component **markup** and to POST content via Sling's OOTB endpoints

(The name is a pun about the Apache Sling Slingshot sample application, that didn't get much love either.)

### Key Directories

- `sling-apps/`  
Contains the actual applications. Each one consists of Java buisiness-logic in the form of OSGi bundles (e.g. `zengarden.core`)
 and UI content for html/css + as little JavaScript as possible (e.g. zengarden.ui.apps).
- `content-packages/`: Holds sample content. These become part of the launcher/docker-image. Also a complete-package to install everything into a running instace.
- `docker/`: Contains `docker-compose.yml` with example configurations for the web cache proxy.
- `launcher/`: The core module responsible for building the runnable Sling application using the `slingfeature-maven-plugin`. The feature definitions are in `launcher/src/main/features`.

## Local Development Workflow

### Prerequisites

1.  **Java 21** and
2.  **Maven 3.9.1x** for building the project.
3.  **Docker** Optional, for running the apps and their web-proxy a in a container

### Building the Project

To build the entire `slingslop` project, run the following command from the root directory:

```bash
mvn clean install -DskipITs
```

To build the Docker image as well, including integration-tests, use:

```bash
mvn clean install -Ddocker.skip=false
```

### Running the Application

There are two primary ways to run the application locally:

1.  **Using the Launcher Script (Recommended for development):**
    The `launcher/launch.sh` script starts the application directly without Docker. This is useful for quick development cycles.

    ```bash
    ./launcher/launch.sh
    ```

2.  **Using Docker Compose:**
    This method uses the `docker-compose.yml` file to start the application and the web cache.

    ```bash
    # From the root directory
    docker-compose -f docker/docker-compose.yml up --build
    ```
    You will also need to add entries to your `/etc/hosts` file as described in `docker/README.md` to access the different sites like you would in production.

3. toDo
   - sling starter plain plus installing the complete-pkg
   - sling:install cq:install wcmio-content-package:install sling:fsmount
