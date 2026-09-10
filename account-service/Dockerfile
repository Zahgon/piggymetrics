FROM eclipse-temurin:21-jre
MAINTAINER Alexander Lukyanchikov <sqshq@sqshq.com>

# Quarkus fast-jar layout produced by `mvn package`
ADD ./target/quarkus-app/lib/ /app/lib/
ADD ./target/quarkus-app/*.jar /app/
ADD ./target/quarkus-app/app/ /app/app/
ADD ./target/quarkus-app/quarkus/ /app/quarkus/

CMD ["java", "-Xmx200m", "-jar", "/app/quarkus-run.jar"]

EXPOSE 6000
