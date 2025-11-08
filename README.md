# TourMinators &nbsp; &nbsp; [![Unit Tests with Maven](https://github.com/Jhouny/TourMinators/actions/workflows/maven-test.yml/badge.svg)](https://github.com/Jhouny/TourMinators/actions/workflows/maven-test.yml) [![Deploy static content to Pages](https://github.com/Jhouny/TourMinators/actions/workflows/generate_docs.yml/badge.svg)](https://github.com/Jhouny/TourMinators/actions/workflows/generate_docs.yml)

Path optimization solver for the Traveling Salesman Problem (TSP) using WA* and Branch and Bound solution. The project is structured into two main modules : a backend module for the core logic and path planning; and a frontend module for user interaction. It is built using Java with Spring Boot for the backend and plain JavaScript for the frontend.

## Documentation
Comprehensive documentation is available at: [TourMinators Documentation](https://jhouny.github.io/TourMinators/)

If you wish to generate the documentation locally, you can use the following Maven command in each module directory:

```bash
mvn site
```

You can find the generated documentation in `target/site/apidocs/index.html`.

## Project Structure

To build, test and execute each server, navigate to its directory and run:

e.g. in the `backend` directory:
```bash
mvn clean package spring-boot:run
```

## Module READMEs
### Frontend : [Frontend README](frontend/README.md)
### Backend : [Backend README](backend/README.md)