# Stage 1: Build native image with GraalVM
FROM ghcr.io/graalvm/native-image-community:25 AS builder
WORKDIR /app

# Install Maven
ARG MAVEN_VERSION=3.9.9
RUN curl -fsSL https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
    | tar xz -C /opt \
    && ln -s /opt/apache-maven-${MAVEN_VERSION}/bin/mvn /usr/local/bin/mvn

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build native binary
COPY src ./src
RUN mvn -Pnative native:compile -DskipTests -B

# Stage 2: Minimal runtime (no JVM needed)
FROM debian:bookworm-slim
WORKDIR /app
RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring
COPY --from=builder /app/target/java-native-render-25 app
EXPOSE 8080
ENTRYPOINT ["./app", "-Xmx512m", "-XX:MaxDirectMemorySize=64m"]
