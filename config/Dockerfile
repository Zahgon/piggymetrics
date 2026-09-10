FROM eclipse-temurin:21-jre
MAINTAINER Alexander Lukyanchikov <sqshq@sqshq.com>

# Quarkus fast-jar layout produced by `mvn package`
ADD ./target/quarkus-app/lib/ /app/lib/
ADD ./target/quarkus-app/*.jar /app/
ADD ./target/quarkus-app/app/ /app/app/
ADD ./target/quarkus-app/quarkus/ /app/quarkus/

CMD ["java", "-Xmx200m", "-jar", "/app/quarkus-run.jar"]

# was: /actuator/health
HEALTHCHECK --interval=30s --timeout=30s CMD curl -f http://localhost:8888/q/health || exit 1

EXPOSE 8888
