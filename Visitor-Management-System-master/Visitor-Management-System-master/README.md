# Visitor Management System - VMS

Welcome to the Visitor Management System project! This project is a robust solution built using Java, Spring Boot, Hibernate, MySQL, and Redis. It offers a comprehensive platform for managing visitors efficiently and securely. With Spring Boot powering the backend, RESTful APIs are easily implemented to facilitate seamless communication between components. Hibernate ensures smooth interaction with the MySQL database, providing reliable data storage and retrieval. Additionally, Redis is utilized for caching purposes, enhancing performance and scalability. Whether you're tracking visitor records, managing access permissions, or ensuring security protocols, this project offers a reliable and adaptable solution.

## Development

Update your local database connection in `application.properties` or create your own `application-local.properties` file to override
settings for development.

During development it is recommended to use the profile `local`. In IntelliJ `-Dspring.profiles.active=local` can be
added in the VM options of the Run Configuration after enabling this property in "Modify options".

Lombok must be supported by your IDE. For IntelliJ install the Lombok plugin and enable annotation processing -
[learn more](https://bootify.io/next-steps/spring-boot-with-lombok.html).

After starting the application it is accessible under `localhost:8080`.

## Build

The application can be built using the following command:

```
mvnw clean package
```

Start your application with the following command - here with the profile `production`:

```
java -Dspring.profiles.active=production -jar ./target/VMS-0.0.1-SNAPSHOT.jar
```

If required, a Docker image can be created with the Spring Boot plugin. Add `SPRING_PROFILES_ACTIVE=production` as
environment variable when running the container.

```
mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=com.java/vms
```

## Further readings

* [Maven docs](https://maven.apache.org/guides/index.html)  
* [Spring Boot reference](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)  
* [Spring Data JPA reference](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)  
