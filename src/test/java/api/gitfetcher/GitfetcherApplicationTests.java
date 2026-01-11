package api.gitfetcher;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class GitfetcherApplicationTests {

	@RegisterExtension
	static WireMockExtension wireMockServer = WireMockExtension.newInstance()
			.options(wireMockConfig().dynamicPort())
			.build();

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("github.api.url", wireMockServer::baseUrl);
	}

	@LocalServerPort
	private int port;

	private RestClient getTestClient() {
		return RestClient.create("http://localhost:" + port);
	}

	@Test
	void shouldReturnRepositories_whenUserExists() {
		wireMockServer.stubFor(get(urlPathEqualTo("/users/octocat/repos"))
				.willReturn(aResponse()
						.withHeader("Content-Type", "application/json")
						.withBody("""
                                [
                                    {"name": "Hello-World", "owner": {"login": "octocat"}, "fork": false},
                                    {"name": "Forked-Repo", "owner": {"login": "octocat"}, "fork": true}
                                ]
                                """)));

		wireMockServer.stubFor(get(urlPathEqualTo("/repos/octocat/Hello-World/branches"))
				.willReturn(aResponse()
						.withHeader("Content-Type", "application/json")
						.withBody("""
                                [
                                    {"name": "master", "commit": {"sha": "12345sha"}}
                                ]
                                """)));
		GithubService.RepositoryResponse[] response = getTestClient().get()
				.uri("/api/repositories/octocat")
				.retrieve()
				.body(GithubService.RepositoryResponse[].class);

		assertThat(response).isNotNull();
		assertThat(response).hasSize(1);

		var repo = response[0];
		assertThat(repo.repositoryName()).isEqualTo("Hello-World");
		assertThat(repo.ownerLogin()).isEqualTo("octocat");
		assertThat(repo.branches().get(0).name()).isEqualTo("master");
		assertThat(repo.branches().get(0).lastCommitSha()).isEqualTo("12345sha");
	}

	@Test
	void shouldReturn404_whenUserNotFound() {
		wireMockServer.stubFor(get(urlPathEqualTo("/users/nonexistent/repos"))
				.willReturn(aResponse()
						.withStatus(404)));

		try {
			getTestClient().get()
					.uri("/api/repositories/nonexistent")
					.retrieve()
					.toBodilessEntity();
		} catch (HttpClientErrorException.NotFound e) {
			String body = e.getResponseBodyAsString();
			assertThat(body).contains("\"status\":404");
			assertThat(body).contains("\"message\":\"User not found\"");
		}
	}
}