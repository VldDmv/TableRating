package org.criticizer.service.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.criticizer.dto.AutocompleteResponse;
import org.criticizer.dto.AutocompleteResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Transformation tests against canned upstream payloads (the real APIs are key-less but not
 * reachable from CI): Steam store search, iTunes Search and TVMaze each map into the normalized
 * {@link AutocompleteResponse}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExternalApiClient Tests")
class ExternalApiClientTest {

    @Mock private RestTemplate restTemplate;

    private ExternalApiClient client() {
        return new ExternalApiClient(restTemplate, new ObjectMapper());
    }

    private void stubResponse(String urlFragment, String body) {
        when(restTemplate.getForEntity(contains(urlFragment), eq(String.class)))
                .thenReturn(ResponseEntity.ok(body));
    }

    @Test
    @DisplayName("Steam: maps items to normalized results with header-image cover")
    void steamMapsItems() {
        stubResponse(
                "store.steampowered.com",
                """
                {"total":2,"items":[
                  {"type":"app","name":"Hades","id":1145360,
                   "tiny_image":"https://cdn.akamai.steamstatic.com/steam/apps/1145360/capsule_231x87.jpg",
                   "metascore":"93"},
                  {"type":"app","name":"Hades II","id":1145350,
                   "tiny_image":"https://cdn.akamai.steamstatic.com/steam/apps/1145350/capsule_231x87.jpg",
                   "metascore":""}
                ]}
                """);

        AutocompleteResponse response = client().searchSteamGames("hades", 10);

        assertThat(response.results()).hasSize(2);
        AutocompleteResult first = response.results().get(0);
        assertThat(first.name()).isEqualTo("Hades");
        assertThat(first.rating()).isEqualTo("93");
        assertThat(first.imageUrl()).contains("capsule_231x87.jpg");
        assertThat(first.coverUrl())
                .isEqualTo("https://cdn.cloudflare.steamstatic.com/steam/apps/1145360/header.jpg");
        // blank metascore becomes null
        assertThat(response.results().get(1).rating()).isNull();
    }

    @Test
    @DisplayName("Steam: respects the limit")
    void steamRespectsLimit() {
        stubResponse(
                "store.steampowered.com",
                """
                {"items":[{"name":"A","id":1},{"name":"B","id":2},{"name":"C","id":3}]}
                """);

        assertThat(client().searchSteamGames("a", 2).results()).hasSize(2);
    }

    @Test
    @DisplayName("iTunes: maps trackName, year and upscaled artwork")
    void itunesMapsResults() {
        stubResponse(
                "itunes.apple.com",
                """
                {"resultCount":1,"results":[
                  {"trackName":"Inception","releaseDate":"2010-07-16T07:00:00Z",
                   "artworkUrl100":"https://is1-ssl.mzstatic.com/image/thumb/x/100x100bb.jpg"}
                ]}
                """);

        AutocompleteResponse response = client().searchItunesMovies("inception", 10);

        AutocompleteResult movie = response.results().get(0);
        assertThat(movie.name()).isEqualTo("Inception");
        assertThat(movie.year()).isEqualTo(2010);
        assertThat(movie.imageUrl()).endsWith("100x100bb.jpg");
        assertThat(movie.coverUrl()).endsWith("600x600bb.jpg");
    }

    @Test
    @DisplayName("TVMaze: unwraps {score, show} and maps rating and images")
    void tvMazeMapsResults() {
        stubResponse(
                "api.tvmaze.com",
                """
                [{"score":1.7,"show":{"id":169,"name":"Breaking Bad","premiered":"2008-01-20",
                  "rating":{"average":9.2},
                  "image":{"medium":"https://tv/medium.jpg","original":"https://tv/original.jpg"}}},
                 {"score":1.1,"show":{"id":170,"name":"No Image Show","premiered":null,
                  "rating":{"average":null},"image":null}}]
                """);

        AutocompleteResponse response = client().searchTvMazeShows("breaking", 10);

        assertThat(response.results()).hasSize(2);
        AutocompleteResult first = response.results().get(0);
        assertThat(first.name()).isEqualTo("Breaking Bad");
        assertThat(first.year()).isEqualTo(2008);
        assertThat(first.rating()).isEqualTo("9.2");
        assertThat(first.imageUrl()).isEqualTo("https://tv/medium.jpg");
        assertThat(first.coverUrl()).isEqualTo("https://tv/original.jpg");

        AutocompleteResult second = response.results().get(1);
        assertThat(second.year()).isNull();
        assertThat(second.rating()).isNull();
        assertThat(second.imageUrl()).isNull();
    }

    @Test
    @DisplayName("Malformed upstream JSON yields an empty result, not an exception")
    void malformedJsonYieldsEmpty() {
        stubResponse("store.steampowered.com", "<html>not json</html>");

        assertThat(client().searchSteamGames("x", 5).results()).isEmpty();
    }
}
