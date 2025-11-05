# Frontend README
> The frontend is built using Java and Spring Boot framework. It serves as the user interface for application, allowing users to interact with the backend services through HTTP requests.

### How to run the Frontend

1. Make sure you have Java and Maven installed on your machine.
2. Navigate to the `frontend` directory in your terminal.
3. Build the project using Maven:
   ```
   mvn clean package
   ```
4. Run the application:
   ```
    mvn spring-boot:run
   ```
5. Open your web browser and go to `http://localhost:8090` to access the frontend.

## API Endpoints

The client interacts with the frontend server through the following endpoints:

- `POST /upload` :
  - **input:** XML file containing map data (nodes and edges).
  - **output:** Parsed JSON object containing nodes and edges.
- `POST /uploadDeliveries` :
  - **input:** JSON object containing delivery points and their details.
  - **output:** Parsed JSON object containing delivery point information.