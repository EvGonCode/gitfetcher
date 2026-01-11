# GitFetcher API

A Spring Boot application that retrieves non-fork GitHub repositories for a specified user, listing their branches and last commit SHAs.

## Technology Stack

* **Java:** 25
* **Spring Boot:** 4.0
* **Build Tool:** Maven
* **Testing:** JUnit 5, WireMock

## Endpoint

### Get User Repositories
**Request:**
`GET /api/repositories/{username}`

**Response:**
```json
{
    "repositoryName": "authentication-service",
    "ownerLogin": "EvGonCode",
    "branches": [
        {
            "name": "master",
            "lastCommitSha": "539f05ad899d68578eee7cbffb93f4046aec3d5a"
        }
    ]
}
```

## Response (404 Not Found) 
If the username does not exist on GitHub:

```json
{
    "message": "User not found",
    "status": 404
}
```

## How to Run

### 1. Build the application:
    `./mvnw clean install`

### 2. Run the application
    `./mvnw spring-boot:run`

The application will start on port `8080` (default).

### 3. To run the integration tests:
    `./mvnw test`