package mycode.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

@Service
public class MetricsCollectionService {
  private static String gridDBRestUrl;
  private static String gridDBApiKey;

  @Value("${griddb.rest.url}")
  public void setgridDBRestUrl(String in) {
    gridDBRestUrl = in;
  }

  @Value("${griddb.api.key}")
  public void setgridDBApiKey(String in) {
    gridDBApiKey = in;
  }

  public void collect() {
    try {
      // Fetch JSON Data from GitHub
      String jsonResponse = fetchJSONFromGitHub(
          "https://raw.githubusercontent.com/statsbomb/open-data/master/data/events/15946.json");
      JSONArray events = new JSONArray(jsonResponse);

      // Process and Send Data to GridDB Cloud
      sendBatchToGridDB(events);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static String fetchJSONFromGitHub(String urlString) throws Exception {
    URL url = new URL(urlString);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    conn.setRequestProperty("Accept", "application/json");

    if (conn.getResponseCode() != 200) {
      throw new RuntimeException("Failed to fetch data: HTTP error code : " + conn.getResponseCode());
    }

    Scanner scanner = new Scanner(url.openStream());
    StringBuilder response = new StringBuilder();
    while (scanner.hasNext()) {
      response.append(scanner.nextLine());
    }
    scanner.close();
    return response.toString();
  }

  private static void sendBatchToGridDB(JSONArray events) {
    JSONArray batchData = new JSONArray();
    boolean startProcessing = false;
    for (int i = 0; i < events.length(); i++) {
      JSONObject event = events.getJSONObject(i);
      JSONArray row = new JSONArray();

      if (event.has("index") && event.getInt("index") == 10) {
        startProcessing = true;
      }

      if (!startProcessing) {
        continue; // Skip records until we reach index == 7
      }

      // Extract and format fields
      String formattedTimestamp = formatTimestamp(event.optString("timestamp", null));
      row.put(formattedTimestamp);
      row.put(event.optJSONObject("player") != null ? event.getJSONObject("player").optString("name", null) : null);
      row.put(event.optJSONObject("type") != null ? event.getJSONObject("type").optString("name", null) : null);

      JSONObject passOutcome = event.optJSONObject("pass");
      JSONObject shotOutcome = event.optJSONObject("shot");
      if (passOutcome == null && shotOutcome == null) {
        continue;
      }

      if (passOutcome != null) {
        if (passOutcome.has("outcome")) {
          row.put(passOutcome.getJSONObject("outcome").optString("name", null));
        } else {
          row.put(JSONObject.NULL);
        }
      } else if (shotOutcome != null) {
        if (shotOutcome.has("outcome")) {
          row.put(shotOutcome.getJSONObject("outcome").optString("name", null));
        } else {
          row.put(JSONObject.NULL);
        }
      } else {
        row.put(JSONObject.NULL);
      }

      row.put(event.optInt("minute", -1));
      row.put(event.optInt("second", -1));
      row.put(event.optJSONObject("team") != null ? event.getJSONObject("team").optString("name", null) : null);

      batchData.put(row);
    }

    sendPutRequest(batchData);
  }

  private static String formatTimestamp(String inputTimestamp) {
    try {
      String todayDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
      return todayDate + "T" + inputTimestamp + "Z";
    } catch (Exception e) {
      return "null"; // Default if parsing fails
    }
  }

  private static void sendPutRequest(JSONArray batchData) {
    try {
      URL url = new URL(gridDBRestUrl);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setDoOutput(true);
      conn.setRequestMethod("PUT");
      conn.setRequestProperty("Content-Type", "application/json");
      conn.setRequestProperty("Authorization", gridDBApiKey);
      // Encode username and password for Basic Auth

      // Send JSON Data
      OutputStream os = conn.getOutputStream();
      os.write(batchData.toString().getBytes());
      os.flush();

      int responseCode = conn.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
        System.out.println("Batch inserted successfully.");
      } else {
        System.out.println("Failed to insert batch. Response: " + responseCode);
      }

      conn.disconnect();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
