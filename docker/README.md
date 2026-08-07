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

## Running

To use the containers, run the command:

    docker compose up

## Volume

This Docker Compose setup creates a volume _slingslop-volume_ for the Apache Sling Slingslop repository. To destroy this volume call:

    docker compose down && docker volume rm docker_slingslop-volume

## Host config

Then map the URLs *www.motorbrot.local*, *zengarden.motorbrot.local* and *editor.motorbrot.local* to your docker host. On local hosts, you can add the following entries into your /etc/hosts file:

    127.0.0.1 www.motorbrot.local
    127.0.0.1 zengarden.motorbrot.local
    127.0.0.1 editor.motorbrot.local

## Webcache bench (apache vs nginx vs varnish)

`webcache-bench/` runs the three webcache engines side-by-side from the **same
CONGA-generated** config, against your dev Sling on the host, so you can diff
their behaviour after tweaking a webcache template. Run instructions are in
[webcache-bench/docker-compose.yml](webcache-bench/docker-compose.yml); the
config comes from the `local-bench` CONGA environment. See
[devops/webcache.md](../devops/webcache.md) for the engine trade-offs.



