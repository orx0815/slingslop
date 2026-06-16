
echo '-------------------------------------------------------------------------------------------'
echo '            Building & launching Slingslop docker container in Composite NodeStore mode    '
echo '            (/apps and /libs are immutable, baked into the image;                          '
echo '             only the writable content sits in the slingslop-content volume)               '
echo '            See docs/composite-nodestore.md                                                '
echo '-------------------------------------------------------------------------------------------'

set -e

mvn clean package -Ddocker.skip=false -Pcomposite-image

docker run -p 8080:8080 --rm \
    -v slingslop-content:/opt/sling/launcher \
    ghcr.io/orx0815/slingslop:snapshot-composite
