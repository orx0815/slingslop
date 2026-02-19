# Slingslop Docker Support

This is a sample Docker Compose configuration for using Apache Sling Slingslop in a containerized environment.

It will start a container with the Slingslop backed by a shared volume and a webcache container pre-configured to proxy and cache three subdomains.

## Dependencies

This requires:

- [Docker](https://docs.docker.com/install/)
- [Docker Compose](https://docs.docker.com/compose/install/)

## Building

To build, run the command:

    docker compose build

If you are using snapshots, to force a rebuild, run:

    docker compose build --no-cache --force-rm

## Volume

This Docker Compose setup creates a volume _slingslop-volume_ for the Apache Sling Slingslop repository. To destroy this volume call:

    docker volume rm docker_slingslop-volume

## Use

To use the containers, run the command:

    docker compose up

Then map the URLs *www.motorbrot.local* and *editor.motorbrot.local* to your docker host. On local hosts, you can add the following entries into your /etc/hosts file:

    127.0.0.1 www.motorbrot.local
    127.0.0.1 editor.motorbrot.local


