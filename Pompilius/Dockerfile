FROM sbtscala/scala-sbt:eclipse-temurin-21.0.8_9_1.12.11_3.8.3 AS builder

WORKDIR /app

COPY . .

RUN sbt stage

FROM eclipse-temurin:21.0.8_9-jre

WORKDIR /app

COPY --from=builder /app/target/universal/stage /app

EXPOSE 9000
VOLUME ["/root/tmp/strataShare/attachments"]
CMD ["bin/pompilius"]
