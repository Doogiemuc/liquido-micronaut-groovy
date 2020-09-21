FROM openjdk:14-alpine
COPY build/libs/liquido-*-all.jar liquido.jar
EXPOSE 8080
CMD ["java", "-Dcom.sun.management.jmxremote", "-Xmx128m", "-jar", "liquido.jar"]