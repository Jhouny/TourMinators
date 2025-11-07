# Backend README
> The backend is built using Java and Spring Boot framework. It handles the core logic and path planning for the application, providing RESTful APIs for the frontend to interact with.

## Getting Started

To build, test and execute the backend server, navigate to the `backend` directory and run:

```bash
mvn clean package spring-boot:run
```

## API Endpoints

The backend exposes the following RESTful API endpoint:

- `POST /runTSP` : 
  - **input:** JSON object containing the list of valid nodes, points of interest and start node.
  - **output:** JSON object containing the optimized path, time taken to reach each node.

## Java Doc

Java Doc documentation can be generated using the following command:

```bash
mvn site
```

You can find the generated documentation in `target/site/apidocs/index.html`.