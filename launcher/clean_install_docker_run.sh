
echo '-------------------------------------------------------------------------------------------'
echo '                        Launching Slingslop docker container '
echo '-------------------------------------------------------------------------------------------'

mvn clean package -Ddocker.skip=false
docker run -p 8080:8080 --rm ghcr.io/orx0815/slingslop:snapshot
