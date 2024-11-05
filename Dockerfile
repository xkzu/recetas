FROM amazoncorretto:17

WORKDIR /app
COPY target/recetas-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081

CMD [ "java", "-jar", "app.jar" ]