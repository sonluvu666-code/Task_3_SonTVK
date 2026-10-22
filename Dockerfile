FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN chmod +x mvnw && ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/demo-0.0.1-SNAPSHOT.jar app.jar
COPY --from=build /workspace/home.html /workspace/index.html /workspace/login.html /workspace/dashboard.html /workspace/profile.html /workspace/login-history.html /workspace/todo.html ./
COPY --from=build /workspace/css ./css
COPY --from=build /workspace/js ./js
COPY --from=build /workspace/images ./images
EXPOSE 8081
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8081} -jar /app/app.jar"]
