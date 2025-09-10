
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;

public class SimpleWeather {
    
    static final HttpClient HTTP = HttpClient.newHttpClient();
    static final Gson GSON = new Gson();
    public static void main(String[] args) throws Exception {
        
        try (Scanner sc = new Scanner(System.in)) {
            System.out.print("Enter city: ");
            String city = sc.nextLine().trim();
            if (city.isEmpty()) {
                System.out.println("City cannot be empty.");
                return;
            }

            // 1) Geocode: city -> (lat, lon) using Open-Meteo Geocoding 
            String geoUrl = String.format("https://geocoding-api.open-meteo.com/v1/search?name=%s&count=1",
                        city);

            JsonObject geo = getJson(geoUrl);
            if (geo == null || !geo.has("results") || geo.getAsJsonArray("results").size() == 0) {
                System.out.println("City not found.");
                return;
            }

            JsonObject loc = geo.getAsJsonArray("results").get(0).getAsJsonObject();
            double lat = loc.get("latitude").getAsDouble();
            double lon = loc.get("longitude").getAsDouble();
            String label = loc.get("name").getAsString() + ", " + loc.get("country").getAsString();

            // 2) Weather: (lat, lon) -> current conditions (no key)
            String wUrl = String.format(
                "https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s&current=temperature_2m,relative_humidity_2m,wind_speed_10m&temperature_unit=fahrenheit",
                lat, lon
            );

            JsonObject w = getJson(wUrl);
            if (w == null || !w.has("current")) {
                System.out.println("Weather fetch failed.");
                return;
            }

            JsonObject current = w.getAsJsonObject("current");
            double temp = current.get("temperature_2m").getAsDouble();
            int humidity = current.get("relative_humidity_2m").getAsInt();
            double wind = current.get("wind_speed_10m").getAsDouble();

            // Final output
            System.out.printf(
                "%s (%.4f, %.4f): %.1f°f, Humidity %d%%, Wind %.1f m/s%n",
                label, lat, lon, temp, humidity, wind
            );
        }
    }

    /**
     * GETs the given URL and parses the body into a JsonObject.
     * Returns null if the HTTP status is not 200.
     */
    
    static JsonObject getJson(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "SimpleWeatherApp") 
                .GET()
                .build();

        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) return null;

        return JsonParser.parseString(resp.body()).getAsJsonObject();
    }
}
