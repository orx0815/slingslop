# Slingslop

[![CI](https://img.shields.io/github/actions/workflow/status/orx0815/slingslop/maven-main.yml?branch=main&style=for-the-badge&logo=githubactions&logoColor=white&label=CI)](https://github.com/orx0815/slingslop/actions/workflows/maven-main.yml)
[![Publish](https://img.shields.io/github/actions/workflow/status/orx0815/slingslop/publish-ghcr.yml?style=for-the-badge&logo=githubactions&logoColor=white&label=Publish)](https://github.com/orx0815/slingslop/actions/workflows/publish-ghcr.yml)
[![Java 25](https://img.shields.io/badge/Java-25-007396?style=for-the-badge&logo=openjdk&logoColor=white)](#prerequisites)
[![Maven 3.9.12+](https://img.shields.io/badge/Maven-3.9.12%2B-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)](#prerequisites)

## Hypermedia Driven Applications (HDA) based on Apache Sling.  
Using HTMX|Datastar|Alpine-Ajax to GET component **markup** and to POST content via Sling's OOTB endpoints

(The name is a pun about the Apache Sling Slingshot sample application, that didn't get much love either.)

### Key Directories

- `sling-apps/`  
	Contains the actual applications.  Each one consists of two types of  modules:  
	-  OSGi bundles (e.g. `zengarden.core`)  for java business-logic in the form of OSGi-Services, -Components and classic Servlets.  Also Sling-Models, referenced by templates. Very powerful ⚠️
	-  UI content-packages (e.g. zengarden.ui.apps) for html templates and CSS. Also as little JavaScript as possible.
	  The `frontent` sub-folder has npm-scripts to deal with that: TypeScript compilation, ESLint checks, dependencies and bundling everything into a minified js.
	
- `content-packages/`  
	Holds sample-content (text and images) in `Jackrabbit FileVault` (VLT)  zips.  
	Usually this is used to transfer content between instances, in this case they also become part of the launcher/docker-image.   
	A complete-package to install everything at once into a running instance.  
	(Note the three different kinds of  [packageType](https://jackrabbit.apache.org/filevault-package-maven-plugin/generate-metadata-mojo.html#packageType) : `application`|`content`|`container` using different validators.) 
	
- `docker/`  
  Contains `docker-compose.yml` with example configurations for the web cache proxy.
  
- `launcher/`  
  The core module responsible for building the runnable Sling application using the `slingfeature-maven-plugin`. The feature definitions are in `launcher/src/main/features`.

## Local Development Workflow

### Prerequisites

1.  **Java 25** and
2.  **Maven 3.9.12+** for building the project.
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

There are four primary ways to run the application locally:

1.  **Using the Launcher Script (Recommended for development):**
    The `launcher/launch.sh` script starts the application directly without Docker. This is useful for quick development cycles.

    ```bash
    ./launcher/launch.sh
    ```

    The The CSS Zen Garden can be enjoyed at: http://localhost:8080/content/slingslop/zengarden/home.html  
    The admin-ui at http://localhost:8080/  

2.  **Using Docker Compose:**
    This method uses the `docker-compose.yml` file to start the application and the web cache proxy.

    ```bash
    # From the root directory
    docker-compose -f docker/docker-compose.yml up --build
    ```
    You will also need to add entries to your `/etc/hosts` file as described in `docker/README.md` to access the different sites like you would in production.

    http://editor.motorbrot.local/  
    http://www.motorbrot.local/  

3.  **Start the official Sling Docker image** backed by 

    - Oak SegmentStore with  

       ```bash
       docker volume create sling-launcher
       docker run --rm -p 8080:8080 -v sling-launcher:/opt/sling/launcher apache/sling:13
       ```  

    - Oak MongoDB DocumentStore with  
    
      ```bash
      docker volume create sling-launcher
      docker run --rm -p 27017:27017 mongo:4.4.6
      docker run --rm -p 8081:8080 -v sling-launcher:/opt/sling/launcher apache/sling:13 oak_mongo
      ```
     and install the ./content-packages/complete/target/slingslop.complete-x.y.z-SNAPSHOT.zip 

4.  **Run the prebuilt Slingslop image from GHCR (quickest for newbies):**

    ```bash
    docker pull ghcr.io/orx0815/slingslop:latest
    docker volume create sling-launcher
    docker run --rm -p 8080:8080 -v sling-launcher:/opt/sling/launcher ghcr.io/orx0815/slingslop:latest
    ```

    Then open http://localhost:8080/content/slingslop/zengarden/home.html

    Helpful notes:
    - If the package is private, log in first: `echo <YOUR_GITHUB_PAT> | docker login ghcr.io -u <YOUR_GITHUB_USERNAME> --password-stdin`
    - Use a specific published tag instead of `latest` when you want reproducible runs.

### Developing the Application

With a running instance, each bundle or package can be deployed separately. This allows fast code->compile->run  development cycles.

**Inside an OSGi bundle** project, run :

```bash
mvn install sling:install
```  

**Inside an content-package** project , run :

```bash
mvn install wcmio-content-package:install
```  

**This one works in both**:

```bash
mvn -Pfast cq:install
```  

### File System Resource Provider
When working on an **application content project** ( html/css/js/sightly/jsp/freemarker/thymeleaf ) the fastest is to mount the local filesystem directly into JCR:

```bash
mvn sling:fsmount
```  

That way you can save a file inside your IDE and immediately reload your browser.  
When done, you should unmount again:  

```bash
mvn sling:fsmount
```
followed by `cq:install` to have a "normal" state in JCR.