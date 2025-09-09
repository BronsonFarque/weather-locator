# Weather Locator (Java)

Simple command‑line app that asks for a city, geocodes it to latitude/longitude using **Open‑Meteo Geocoding**, then fetches **current weather** from **Open‑Meteo Forecast** (no API key required). Output includes city label, coordinates, temperature (°F), humidity, and wind speed.

---

## Features

* Plain Java 11+ (uses built‑in `java.net.http.HttpClient`).
* Uses **Gson** to parse JSON.
* No API keys.

---

## Code

`SimpleWeather.java` (main class)

```java
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

            // 1) Geocode
            String geoUrl = String.format(
                "https://geocoding-api.open-meteo.com/v1/search?name=%s&count=1",
                city
            );
            JsonObject geo = getJson(geoUrl);
            if (geo == null || !geo.has("results") || geo.getAsJsonArray("results").size() == 0) {
                System.out.println("City not found.");
                return;
            }
            JsonObject loc = geo.getAsJsonArray("results").get(0).getAsJsonObject();
            double lat = loc.get("latitude").getAsDouble();
            double lon = loc.get("longitude").getAsDouble();
            String label = loc.get("name").getAsString() + ", " + loc.get("country").getAsString();

            // 2) Weather
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

            System.out.printf(
                "%s (%.4f, %.4f): %.1f°f, Humidity %d%%, Wind %.1f m/s%n",
                label, lat, lon, temp, humidity, wind
            );
        }
    }

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
```

---

## Prerequisites

* **Java 11 or newer** (for `java.net.http`).
* **Gson** library on the classpath.

---

## Build & Run

### Plain `javac`/`java` (download Gson JAR)

1. Download `gson-*.jar` from Maven Central.
2. Put the JAR in a `lib/` folder next to your source file(s).
3. Compile:

   ```bash
   javac -cp lib/gson-<version>.jar src/SimpleWeather.java -d out
   ```
4. Run (macOS/Linux):

   ```bash
   java -cp out:lib/gson-<version>.jar SimpleWeather
   ```

   On Windows use `;` instead of `:`:

   ```bat
   java -cp out;lib\gson-<version>.jar SimpleWeather
   ```

---

## Usage

```
Enter city: Dallas
Dallas, United States (32.7767, -96.7970): 95.4°f, Humidity 41%, Wind 3.2 m/s
```

* Units: temperature in **°F**, wind in **m/s** (Open‑Meteo default), humidity in **%**.

---

## Notes

* APIs used:

  * Geocoding: `https://geocoding-api.open-meteo.com/v1/search`
  * Forecast: `https://api.open-meteo.com/v1/forecast`
* No API keys required. Be considerate with request volume.
* For °C, remove `temperature_unit=fahrenheit` from the forecast URL.

