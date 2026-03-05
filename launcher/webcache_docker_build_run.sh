
echo '-------------------------------------------------------------------------------------------'
echo '[NOTE] Launching Slingslop webcache conatiner'
echo '-------------------------------------------------------------------------------------------'

docker build --tag 'ghcr.io/orx0815/slingslop_webcache:latest' ../docker/webcache
#docker volume create slingslop-volume
docker run -t -i -p 80:80 --rm -v slingslop-volume:/opt/sling/launcher --env RENDERER_URL=172.17.0.1 --env AUTHOR_URL=172.17.0.1 ghcr.io/orx0815/slingslop_webcache:latest
