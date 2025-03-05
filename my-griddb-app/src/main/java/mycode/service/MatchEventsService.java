package mycode.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

@Service
public class MatchEventsService {

  private static final String GRIDDB_URL = "https://cloud5114.griddb.com:443/griddb/v2/gs_clustermfcloud5114/dbs/9UkMCtv4/containers/match_events/rows";
  private static final String AUTH_HEADER = "Basic TTAyY0GlhbEAx";

  private final HttpClient httpClient = HttpClient.newHttpClient();
  private final ObjectMapper objectMapper = new ObjectMapper();

  public Map<Integer, Integer> getPassCountByFiveMin(String playerName) {
    try {
      // Build the HTTP request based on your curl
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(GRIDDB_URL))
          .header("Content-Type", "application/json")
          .header("Authorization", AUTH_HEADER)
          .POST(HttpRequest.BodyPublishers.ofString("{\"offset\": 0, \"limit\": 55555}"))
          .build();

      // Fetch the response
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      JsonNode rootNode = objectMapper.readTree(response.body());
      JsonNode rows = rootNode.get("rows");

      // Process data: count passes every 5 minutes
      Map<Integer, Integer> passCountByFiveMin = new HashMap<>();

      for (JsonNode row : rows) {
        String currentPlayer = row.get(1).asText();
        String eventType = row.get(2).asText();
        int minute = row.get(4).asInt();

        if (playerName.equals(currentPlayer) && "Pass".equals(eventType)) {
          // Group by 5-minute intervals (0-4, 5-9, 10-14, etc.)
          int fiveMinInterval = (minute / 5) * 5;
          passCountByFiveMin.merge(fiveMinInterval, 1, Integer::sum);
        }
      }

      return passCountByFiveMin;

    } catch (Exception e) {
      e.printStackTrace();
      return new HashMap<>();
    }
  }
}