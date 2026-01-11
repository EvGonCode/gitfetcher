package api.gitfetcher;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class Controller {

    private final GithubService service;

    Controller(GithubService service) {
        this.service = service;
    }

    @GetMapping("/repositories/{username}")
    public ResponseEntity<List<GithubService.RepositoryResponse>> getUserRepositories(@PathVariable String username) {
        return ResponseEntity.ok(service.fetchRepositories(username));
    }

    @ExceptionHandler(HttpClientErrorException.NotFound.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(HttpClientErrorException.NotFound ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "status", HttpStatus.NOT_FOUND.value(),
                        "message", "User not found"
                ));
    }

}