mvn package
mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)
docker build -t pazzio/docker-test .