package api.gitfetcher;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
class GithubService {

    private final RestClient restClient;
    private final int timeoutMs = 5000;

    GithubService(@Value("${github.api.url:https://api.github.com}") String baseUrl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    public List<RepositoryResponse> fetchRepositories(String username) {
        GithubRepo[] repos = restClient.get()
                .uri("/users/{username}/repos", username)
                .retrieve()
                .body(GithubRepo[].class);

        if (repos == null) {
            return List.of();
        }
        return List.of(repos).stream()
                .filter(repo -> !repo.fork())
                .map(repo -> {
                    List<BranchInfo> branches = fetchBranches(repo.owner().login(), repo.name());
                    return new RepositoryResponse(repo.name(), repo.owner().login(), branches);
                })
                .toList();
    }

    private List<BranchInfo> fetchBranches(String owner, String repoName) {
        GithubBranch[] branches = restClient.get()
                .uri("/repos/{owner}/{repo}/branches", owner, repoName)
                .retrieve()
                .body(GithubBranch[].class);

        if (branches == null) {
            return List.of();
        }

        return List.of(branches).stream()
                .map(branch -> new BranchInfo(branch.name(), branch.commit().sha()))
                .toList();
    }

    public record RepositoryResponse(String repositoryName, String ownerLogin, List<BranchInfo> branches) {}
    public record BranchInfo(String name, String lastCommitSha) {}

    record GithubRepo(String name, Owner owner, boolean fork) {}
    record Owner(String login) {}

    record GithubBranch(String name, Commit commit) {}
    record Commit(String sha) {}
}